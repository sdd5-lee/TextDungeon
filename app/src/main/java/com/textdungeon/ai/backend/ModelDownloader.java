package com.textdungeon.ai.backend;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 온디바이스 모델을 첫 실행 때 Hugging Face에서 받는다. Wi-Fi(데이터 요금 없는 네트워크)에서만 받고,
 * 받는 동안에도 게임은 그대로 하고 주사위는 클라우드로 간다.
 *
 * 받는 일은 시스템 DownloadManager가 한다. 앱이 꺼져 있어도 계속 받고, 연결이 끊기면 이어 받는다.
 * 받는 동안은 임시 이름(.download)으로 두고, 크기와 SHA-256이 맞을 때만 ModelLocator가 찾는 이름으로 바꾼다.
 */
public class ModelDownloader {
    private static final String TAG = "ModelDownloader";

    // 0-A에서 측정한 파일과 같은 리비전에 고정한다. main으로 받으면 저장소가 바뀔 때 다른 파일을 받게 된다
    static final String MODEL_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/"
            + "1caa527c1e88b55f046fcd8d2b4265606680663d/" + ModelLocator.MODEL_FILE;
    static final long MODEL_BYTES = 2_588_147_712L;
    static final String MODEL_SHA256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c";
    // 받은 뒤에도 기기에 이만큼은 남아 있어야 받기 시작한다
    private static final long SPARE_SPACE = 1L << 30;
    // 받기 실패나 해시 불일치가 이만큼 쌓이면 더 받지 않는다 (2.6GB를 계속 다시 받지 않게)
    private static final int MAX_FAILURES = 3;

    private static final String PREFS = "OnDeviceModel";
    private static final String KEY_DOWNLOAD_ID = "download_id";
    private static final String KEY_FAILURES = "failures";
    /** 설정 화면의 "AI 모델 받기" 스위치. 기본은 켬 */
    private static final String KEY_ENABLED = "enabled";

    public enum Status {
        /** 모델 파일이 있다 */
        READY,
        DOWNLOADING,
        /** Wi-Fi나 네트워크 연결을 기다림 */
        WAITING_NETWORK,
        /** 다 받고 크기와 해시를 확인하는 중 */
        VERIFYING,
        NOT_STARTED,
        NO_SPACE,
        /** 여러 번 실패해서 그만 받음 */
        FAILED,
        /** 기기 조건(ABI, 램)이 안 맞아 받지 않음 */
        UNSUPPORTED,
        /** 사용자가 설정에서 껐음 */
        DISABLED
    }

    public static final class Progress {
        public final Status status;
        /** DOWNLOADING일 때 0~100, 모르면 -1 */
        public final int percent;

        Progress(Status status, int percent) {
            this.status = status;
            this.percent = percent;
        }

        Progress(Status status) {
            this(status, -1);
        }
    }

    private final Context appContext;
    private final SharedPreferences prefs;
    private final Runnable onModelReady;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "model-download");
        t.setDaemon(true);
        return t;
    });

    // 실패 뒤 다시 받기는 프로세스마다 한 번만 한다 (메인 화면에 돌아올 때마다 다시 받지 않게)
    private volatile boolean enqueuedThisProcess;
    private volatile boolean verifying;
    private volatile boolean noSpace;

    /** @param onModelReady 모델 파일이 새로 생겼을 때 다운로드 스레드에서 부른다 */
    public ModelDownloader(Context appContext, Runnable onModelReady) {
        this.appContext = appContext.getApplicationContext();
        this.prefs = this.appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.onModelReady = onModelReady;

        // 앱이 살아 있을 때 다 받으면 바로 확인한다. 꺼져 있을 때 끝난 건 다음 start()에서 확인한다.
        // 완료 알림은 다른 앱(시스템 다운로드 앱)이 보내므로 EXPORTED로 받는다. 받은 뒤 상태는 DownloadManager에 다시 묻는다
        ContextCompat.registerReceiver(this.appContext, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id != -1 && id == downloadId()) start();
            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
    }

    /** 모델이 없으면 받기 시작하고, 다 받았으면 확인해서 설치한다. 메인 화면에 올 때마다 불러도 된다 */
    public void start() {
        executor.execute(this::check);
    }

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, true);
    }

    /**
     * 설정 스위치. 켜면 받기 시작하고, 끄면 받던 것을 취소하고 받아 둔 모델과 캐시를 지운다.
     * 끄기 전에 엔진을 내려 둬야 한다 (AiManager가 한다). 지우는 일은 다운로드 스레드에서 한다
     */
    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (enabled) {
            // 예전에 실패해서 그만 받았더라도 사용자가 다시 켜면 처음부터 받는다
            prefs.edit().remove(KEY_FAILURES).apply();
            enqueuedThisProcess = false;
            start();
        } else {
            executor.execute(this::deleteAll);
        }
    }

    private void deleteAll() {
        long id = downloadId();
        if (id != -1) {
            downloadManager().remove(id); // 받던 임시 파일도 같이 지운다
            setDownloadId(-1);
        }
        File target = ModelLocator.target(appContext);
        if (target != null) {
            deleteQuietly(target);
            deleteQuietly(tempFile(target));
        }
        deleteCaches();
        Log.i(TAG, "모델 받기 끔: 모델과 캐시를 지움");
    }

    /**
     * 엔진이 cacheDir에 모델 이름으로 시작하는 캐시를 만든다
     * (에뮬레이터에서 GPU 가중치 778MB + 프로그램 13MB, CPU 788MB). 엔진이 내려가 있을 때만 부른다
     */
    private void deleteCaches() {
        File[] caches = appContext.getCacheDir().listFiles(
                (dir, name) -> name.startsWith(ModelLocator.MODEL_FILE));
        if (caches == null) return;
        for (File f : caches) deleteQuietly(f);
    }

    private static void deleteQuietly(File f) {
        if (f.exists() && !f.delete()) Log.w(TAG, "지우지 못함: " + f);
    }

    private void check() {
        File target = ModelLocator.target(appContext);
        if (target == null) return;
        long id = downloadId();

        if (!isEnabled()) {
            // 꺼 둔 뒤에 끝난 다운로드가 있으면 치운다
            if (id != -1) deleteAll();
            return;
        }
        if (ModelLocator.find(appContext) != null) {
            // adb push 등으로 이미 있으면 받던 것은 취소한다
            if (id != -1) {
                downloadManager().remove(id);
                setDownloadId(-1);
            }
            return;
        }
        String unsupported = OnDeviceBackend.deviceUnsupportedReason(appContext);
        if (unsupported != null) {
            Log.i(TAG, "모델을 받지 않음: " + unsupported);
            return;
        }

        if (id != -1) {
            int status = queryStatus(id);
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                install(id, target);
                return;
            }
            if (status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_RUNNING
                    || status == DownloadManager.STATUS_PAUSED) {
                return;
            }
            if (status == DownloadManager.STATUS_FAILED) {
                Log.w(TAG, "모델 받기 실패 (reason " + queryReason(id) + ")");
                addFailure();
            } else {
                Log.i(TAG, "다운로드 기록이 없어짐 (사용자가 지웠거나 시스템이 정리)");
            }
            downloadManager().remove(id);
            setDownloadId(-1);
        }
        enqueue(target);
    }

    private void enqueue(File target) {
        if (enqueuedThisProcess || failures() >= MAX_FAILURES) return;
        File dir = target.getParentFile();
        if (dir == null || (!dir.isDirectory() && !dir.mkdirs())) {
            Log.w(TAG, "models 폴더를 만들 수 없음: " + dir);
            return;
        }
        long usable = dir.getUsableSpace();
        noSpace = usable < MODEL_BYTES + SPARE_SPACE;
        if (noSpace) {
            Log.i(TAG, "저장 공간 부족으로 받지 않음 (" + (usable >> 20) + "MB)");
            return;
        }

        File temp = tempFile(target);
        if (temp.exists() && !temp.delete()) Log.w(TAG, "임시 파일을 지우지 못함: " + temp);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(MODEL_URL))
                .setTitle("TextDungeon AI 모델 (신들의 이벤트, 혼돈의 주사위)")
                .setDescription("Wi-Fi에서만 받습니다 (2.6GB)")
                .setAllowedOverMetered(false)
                .setAllowedOverRoaming(false)
                .setDestinationUri(Uri.fromFile(temp));
        long id = downloadManager().enqueue(request);
        setDownloadId(id);
        enqueuedThisProcess = true;
        Log.i(TAG, "모델 받기 시작 (id " + id + ")");
    }

    /** 다 받은 임시 파일을 확인하고 제 이름으로 바꾼다. 다운로드 스레드에서만 부른다 */
    private void install(long id, File target) {
        File temp = tempFile(target);
        verifying = true;
        try {
            long start = SystemClock.elapsedRealtime();
            String problem = verify(temp, MODEL_BYTES, MODEL_SHA256);
            Log.i(TAG, "모델 확인 " + (SystemClock.elapsedRealtime() - start) + "ms: "
                    + (problem == null ? "통과" : problem));
            // 캐시 이름에 모델 파일의 수정 시각이 들어가서, 새로 받으면 예전 캐시(GPU·CPU 각 800MB 안팎)는 다시 쓰이지 않는다
            if (problem == null) deleteCaches();
            if (problem == null && temp.renameTo(target)) {
                // 이름을 바꾼 뒤에 지워야 DownloadManager가 받은 파일을 같이 지우지 않는다
                downloadManager().remove(id);
                setDownloadId(-1);
                prefs.edit().remove(KEY_FAILURES).apply();
                onModelReady.run();
                return;
            }
            if (problem == null) Log.w(TAG, "이름 바꾸기 실패: " + temp + " → " + target);
            addFailure();
            downloadManager().remove(id); // 임시 파일도 같이 지운다
            setDownloadId(-1);
            enqueue(target);
        } finally {
            verifying = false;
        }
    }

    /** 설정 화면 표시용. 메인 스레드에서 불러도 된다 (DownloadManager 조회 한 번) */
    public Progress progress() {
        if (OnDeviceBackend.deviceUnsupportedReason(appContext) != null) return new Progress(Status.UNSUPPORTED);
        if (!isEnabled()) return new Progress(Status.DISABLED);
        if (ModelLocator.find(appContext) != null) return new Progress(Status.READY);
        if (verifying) return new Progress(Status.VERIFYING);

        long id = downloadId();
        if (id != -1) {
            try (Cursor c = downloadManager().query(new DownloadManager.Query().setFilterById(id))) {
                if (c != null && c.moveToFirst()) {
                    int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL) return new Progress(Status.VERIFYING);
                    // 모바일 데이터에서는 PAUSED가 아니라 PENDING인 채로 Wi-Fi를 기다린다 (에뮬레이터 API 36에서 확인)
                    boolean waiting = status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_PAUSED;
                    if (waiting && !onUnmeteredNetwork()) return new Progress(Status.WAITING_NETWORK);
                    if (waiting || status == DownloadManager.STATUS_RUNNING) {
                        long done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        return new Progress(Status.DOWNLOADING, (int) (Math.max(0, done) * 100 / MODEL_BYTES));
                    }
                    // 실패는 다음 start()에서 처리한다
                }
            }
        }
        if (failures() >= MAX_FAILURES) return new Progress(Status.FAILED);
        if (noSpace) return new Progress(Status.NO_SPACE);
        return new Progress(Status.NOT_STARTED);
    }

    /** @return 문제가 없으면 null, 있으면 이유 */
    static String verify(File file, long expectedBytes, String expectedSha256) {
        if (!file.isFile()) return "파일 없음";
        if (file.length() != expectedBytes) return "크기가 다름 (" + file.length() + ")";
        try {
            String sha = sha256(file);
            return sha.equalsIgnoreCase(expectedSha256) ? null : "SHA-256이 다름 (" + sha + ")";
        } catch (IOException e) {
            return "읽기 실패: " + e.getMessage();
        }
    }

    static String sha256(File file) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] buffer = new byte[1 << 20];
        try (InputStream in = new FileInputStream(file)) {
            int n;
            while ((n = in.read(buffer)) != -1) md.update(buffer, 0, n);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : md.digest()) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    public boolean hasModel() {
        return ModelLocator.find(appContext) != null;
    }

    /** 이 기기에서 온디바이스 모델을 쓸 수 있는지 (ABI, 램). 아니면 설정 스위치를 막는다 */
    public boolean isSupported() {
        return OnDeviceBackend.deviceUnsupportedReason(appContext) == null;
    }

    private static File tempFile(File target) {
        return new File(target.getParentFile(), target.getName() + ".download");
    }

    private int queryStatus(long id) {
        try (Cursor c = downloadManager().query(new DownloadManager.Query().setFilterById(id))) {
            if (c == null || !c.moveToFirst()) return -1;
            return c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
        }
    }

    private int queryReason(long id) {
        try (Cursor c = downloadManager().query(new DownloadManager.Query().setFilterById(id))) {
            if (c == null || !c.moveToFirst()) return -1;
            return c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
        }
    }

    /** 지금 데이터 요금 없는 네트워크(보통 Wi-Fi)에 붙어 있는지. 받기 조건과 같다 */
    private boolean onUnmeteredNetwork() {
        ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
    }

    private DownloadManager downloadManager() {
        return (DownloadManager) appContext.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    private long downloadId() {
        return prefs.getLong(KEY_DOWNLOAD_ID, -1);
    }

    private void setDownloadId(long id) {
        prefs.edit().putLong(KEY_DOWNLOAD_ID, id).apply();
    }

    private int failures() {
        return prefs.getInt(KEY_FAILURES, 0);
    }

    private void addFailure() {
        prefs.edit().putInt(KEY_FAILURES, failures() + 1).apply();
    }
}
