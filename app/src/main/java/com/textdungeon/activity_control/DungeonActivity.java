package com.textdungeon.activity_control;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.textdungeon.R;
import com.google.android.material.snackbar.Snackbar;
import com.textdungeon.ai.AiCallback;
import com.textdungeon.ai.AiManager;
import com.textdungeon.ai.AiType;
import com.textdungeon.ai.backend.OnDeviceBackend;
import com.textdungeon.buttons.SingleClickListener;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Dungeon;
import com.textdungeon.model.Reward;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 던전 선택 (예전의 난이도 선택). 고른 던전의 그림을 크게 보여 주고 아래에 입장 조건·목표 층·배수와 설명을 쓴다.
 * 앞 던전을 클리어해야 다음 던전이 열린다. 입장하면 신들의 이벤트(온디바이스 AI)를 만들고 게임을 시작한다.
 */
public class DungeonActivity extends BaseActivity {
    private static final String TAG = "DungeonActivity";
    private static final int MAX_RETRY = 1;
    /** 모델이 올라오길 기다리는 최대 시간. S24 GPU는 4초 안팎 (0-B) */
    private static final long MODEL_WAIT_MS = 20_000L;
    private static final long MODEL_POLL_MS = 200L;
    /** 이벤트 하나를 기다리는 최대 시간. S24에서 틀 방식은 5초 안팎. 넘기면 남은 이벤트 없이 시작한다 */
    private static final long EVENT_TIMEOUT_MS = 30_000L;

    private static final int GOLD = 0xFFE9C176;
    private static final int TEXT = 0xFFE5E2E1;
    private static final int LABEL = 0xFF9A948C;
    private static final int TEXT_LOCKED = 0xFF8D867C;
    private static final int OK = 0xFF8FBF7A;
    private static final int NEED = 0xFFD9826A;

    private AlertDialog loadingDialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    DataControlTower dt;

    /** 이벤트 생성 중에는 입장 버튼을 다시 눌러도 무시 (연타로 요청이 여러 번 나가는 것 방지) */
    private boolean isGenerating = false;
    private int successCount = 0;
    private int failCount = 0;
    /** 기다리다 포기한 요청의 콜백이 늦게 와도 버리도록 요청마다 올린다 */
    private int requestSeq = 0;
    private List<AiType> gods;

    private ImageView ivImage;
    private View lockedCover, btnEnter, btnNoAi, btnBack;
    private TextView tvName, tvFloorBadge, tvDescription;
    private LinearLayout conditions, strip;
    private final Map<String, FrameLayout> thumbs = new HashMap<>();
    private Dungeon selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dungeon);
        dt = DataControlTower.getInstance(this);

        ivImage = findViewById(R.id.iv_dungeon_image);
        lockedCover = findViewById(R.id.locked_cover);
        tvName = findViewById(R.id.tv_dungeon_name);
        tvFloorBadge = findViewById(R.id.tv_floor_badge);
        tvDescription = findViewById(R.id.dungeon_description);
        conditions = findViewById(R.id.dungeon_conditions);
        strip = findViewById(R.id.dungeon_strip);
        btnEnter = findViewById(R.id.btn_enter);
        btnNoAi = findViewById(R.id.btn_no_ai);
        btnBack = findViewById(R.id.btn_back);

        fitImage();
        setSfx(btnEnter, btnNoAi, btnBack);
        btnBack.setOnClickListener(v -> finish());
        btnEnter.setOnClickListener(SingleClickListener.wrap(v -> enter(v, true)));
        btnNoAi.setOnClickListener(SingleClickListener.wrap(v -> enter(v, false)));

        buildStrip();
        // 처음에는 열린 던전 중 가장 깊은 곳을 골라 둔다
        Dungeon first = null;
        for (Dungeon d : dt.getDungeons()) {
            if (isOpen(d)) first = d;
        }
        if (first == null && !dt.getDungeons().isEmpty()) first = dt.getDungeons().get(0);
        if (first != null) select(first);
    }

    private boolean isOpen(Dungeon d) {
        return d.isOpen(clearedDungeons());
    }

    private Set<String> clearedDungeons() {
        return dt.getUserRecord().getClearedDungeons();
    }

    /** 화면이 길어 아래가 비면 그만큼 그림을 키운다 (가로 길이까지만) */
    private void fitImage() {
        View scroll = findViewById(R.id.dungeon_scroll);
        View content = findViewById(R.id.dungeon_content);
        View hero = findViewById(R.id.hero_frame);
        scroll.post(() -> {
            int extra = scroll.getHeight() - content.getHeight();
            if (extra <= 0) return;
            ViewGroup.LayoutParams lp = hero.getLayoutParams();
            lp.height = Math.min(lp.height + extra, hero.getWidth());
            hero.setLayoutParams(lp);
        });
    }

    // ── 던전 고르기 ──────────────────────────────────────

    /** 아래 줄의 던전 칸. 그림과 이름, 잠긴 던전은 어둡게 덮고 자물쇠 */
    private void buildStrip() {
        int size = dp(72);
        for (Dungeon d : dt.getDungeons()) {
            boolean open = isOpen(d);
            FrameLayout thumb = new FrameLayout(this);
            GradientDrawable back = new GradientDrawable();
            back.setColor(0xFF14110E);
            back.setCornerRadius(dp(8));
            thumb.setBackground(back);
            thumb.setClipToOutline(true);
            thumb.setContentDescription(d.getName());

            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GameImages.set(iv, d.getImgId(), R.drawable.dungeon_entrance);
            thumb.addView(iv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            if (!open) {
                View dim = new View(this);
                dim.setBackgroundColor(0xCC0E0D0C);
                thumb.addView(dim, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                ImageView lock = new ImageView(this);
                lock.setImageResource(R.drawable.ic_lock);
                lock.setColorFilter(0xFF5A544C);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(20), dp(20),
                        Gravity.CENTER_HORIZONTAL | Gravity.TOP);
                lp.topMargin = dp(16);
                thumb.addView(lock, lp);
            }

            TextView name = text(d.getName(), 10, open ? TEXT : TEXT_LOCKED);
            name.setGravity(Gravity.CENTER);
            name.setSingleLine();
            name.setPadding(dp(2), dp(2), dp(2), dp(2));
            name.setBackgroundColor(0x99000000);
            thumb.addView(name, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

            thumb.setOnClickListener(v -> select(d));
            setSfx(thumb);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd(dp(8));
            strip.addView(thumb, lp);
            thumbs.put(d.getId(), thumb);
        }
    }

    private void select(Dungeon d) {
        selected = d;
        boolean open = isOpen(d);
        for (Map.Entry<String, FrameLayout> e : thumbs.entrySet()) {
            e.getValue().setForeground(frame(e.getKey().equals(d.getId())));
        }
        GameImages.set(ivImage, d.getImgId(), R.drawable.dungeon_entrance);
        lockedCover.setVisibility(open ? View.GONE : View.VISIBLE);
        tvName.setText(d.getName());
        tvName.setTextColor(open ? GOLD : TEXT_LOCKED);
        boolean cleared = clearedDungeons().contains(d.getId());
        tvFloorBadge.setText("목표 · " + d.getClearFloor() + "층" + (cleared ? " · 클리어" : ""));
        tvDescription.setText(d.getDescription());
        btnEnter.setAlpha(open ? 1f : 0.45f);
        btnNoAi.setAlpha(open ? 1f : 0.45f);
        showConditions(d, open);
    }

    private void showConditions(Dungeon d, boolean open) {
        conditions.removeAllViews();
        Dungeon need = d.getRequires() != null ? dt.getDungeonManager().spawn(d.getRequires()) : null;
        String needName = need != null ? need.getName() : d.getRequires();
        if (d.getRequires() == null) {
            addRow("입장 조건", "처음부터 열림", OK);
        } else if (open) {
            addRow("입장 조건", "「" + needName + "」 클리어 ✓", OK);
        } else {
            addRow("입장 조건", "「" + needName + "」 클리어 필요", NEED);
        }
        addRow("목표", "1층부터 " + d.getClearFloor() + "층까지, 마지막 층의 주인을 쓰러뜨리기", TEXT);
        addRow("몬스터", "체력·공격력 ×" + d.getStatMultiplier(), TEXT);
        addRow("이벤트 보상", "×" + d.getRewardMultiplier(), TEXT);
        addRow("신들의 이벤트", aiFloors(d), TEXT);
        addRow("클리어 보상", String.format("%,d 젬", d.getClearGems()), GOLD);
    }

    /** 예: "3번 (1·21·41층)" */
    private String aiFloors(Dungeon d) {
        int count = aiEventCount(d);
        if (count == 0) return "없음";
        StringBuilder floors = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) floors.append('·');
            floors.append(Dungeon.aiEventFloor(i));
        }
        return count + "번 (" + floors + "층)";
    }

    /** 마지막 층을 넘는 AI 이벤트는 만들지 않는다 */
    private static int aiEventCount(Dungeon d) {
        int count = 0;
        while (count < d.getAiEvents() && Dungeon.aiEventFloor(count) < d.getClearFloor()) count++;
        return count;
    }

    private void addRow(String label, String value, int valueColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(5), 0, dp(5));
        TextView l = text(label, 13, LABEL);
        row.addView(l, new LinearLayout.LayoutParams(dp(108), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView v = text(value, 14, valueColor);
        row.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        conditions.addView(row);
    }

    // ── 입장 ────────────────────────────────────────────

    private void enter(View v, boolean withAi) {
        if (isGenerating || selected == null) return;   // 이미 생성 중이면 무시
        if (!isOpen(selected)) {
            Dungeon need = dt.getDungeonManager().spawn(selected.getRequires());
            Snackbar.make(v, "「" + (need != null ? need.getName() : selected.getRequires())
                    + "」을(를) 먼저 클리어해야 열립니다", Snackbar.LENGTH_SHORT).show();
            return;
        }
        isGenerating = true;
        setButtonsEnabled(false);
        dt.setDungeon(selected.getId());
        dt.saveGame(); // 이어하기가 고른 던전으로 이어지게
        // 이벤트와 주사위가 같은 온디바이스 모델을 쓴다. 올라오면 이벤트부터 만든다
        dt.getAiManager().warmUpOnDevice();
        if (withAi && aiEventCount(selected) > 0) {
            generateEventsAndStart(aiEventCount(selected));
        } else {
            startGame();
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        for (View b : new View[]{btnEnter, btnNoAi, btnBack}) {
            if (b != null) b.setEnabled(enabled);
        }
    }

    private void generateEventsAndStart(int targetCount) {
        successCount = 0;
        failCount = 0;
        gods = new ArrayList<>(Arrays.asList(AiType.values()));
        Collections.shuffle(gods);

        loadingDialog = new AlertDialog.Builder(this)
                .setTitle("신들의 개입")
                .setMessage("신들을 깨우는 중입니다...\n잠시 기다려 주십시오...")
                .setCancelable(false)
                .create();
        loadingDialog.show();
        waitForModel(targetCount, SystemClock.uptimeMillis() + MODEL_WAIT_MS);
    }

    /** 모델 로딩이 끝나면(준비됨, 실패, 모델 없음) 이벤트를 만든다. 쓸 수 없으면 AI 이벤트 없이 시작한다 */
    private void waitForModel(int targetCount, long deadline) {
        AiManager ai = dt.getAiManager();
        if (ai.getOnDeviceState() == OnDeviceBackend.State.LOADING && SystemClock.uptimeMillis() < deadline) {
            mainHandler.postDelayed(() -> waitForModel(targetCount, deadline), MODEL_POLL_MS);
            return;
        }
        if (!ai.isOnDeviceReady()) {
            Log.w(TAG, "온디바이스 모델을 쓸 수 없어 AI 이벤트 없이 시작 (" + ai.getOnDeviceState() + ")");
            Toast.makeText(this, "신들의 이벤트 없이 시작합니다\n" + ai.getOnDeviceStatusLine(), Toast.LENGTH_LONG).show();
            finishGeneration();
            return;
        }
        loadingDialog.setMessage(progressMessage(targetCount));
        generateNext(0, targetCount, 0);
    }

    /**
     * 이벤트를 하나씩 차례로 만든다 (온디바이스 모델은 한 번에 요청 하나만 받는다).
     * 1층, 21층, 41층, 61층에 생성된 이벤트 출현
     */
    private void generateNext(int index, int targetCount, int retryCount) {
        if (index >= targetCount) {
            finishGeneration();
            return;
        }
        AiType god = gods.get(index % gods.size());
        int targetFloor = Dungeon.aiEventFloor(index);
        int seq = ++requestSeq;
        Runnable timeout = () -> {
            if (seq != requestSeq) return;
            requestSeq++;
            Log.w(TAG, targetFloor + "층 이벤트가 " + EVENT_TIMEOUT_MS + "ms 안에 오지 않아 남은 이벤트 없이 시작");
            dt.getAiManager().cancelOnDevice();
            failCount += targetCount - index;
            finishGeneration();
        };
        mainHandler.postDelayed(timeout, EVENT_TIMEOUT_MS);

        dt.getAiManager().generateEvent(
                targetFloor,
                god,
                // 형상 등급별 아이템 후보: 하급은 이 층대, 중급은 더 깊은 층 장비, 상급은 심층 장비
                dt.getItemsForFloor(targetFloor - 5, targetFloor + 5),
                dt.getUpperGear(targetFloor),
                dt.getDeepFloorGear(),
                dt.getMonstersForFloor(targetFloor, targetFloor + 10), // 이 층대에 실제로 나오는 몬스터만
                dt.getDungeon().getRewardMultiplier(),
                Reward.expMultiplier(dt.getPlayer()),
                new AiCallback() {
                    @Override
                    public void onSuccess(GameEvent newEvent) {
                        runOnUiThread(() -> {
                            if (seq != requestSeq) return;
                            mainHandler.removeCallbacks(timeout);
                            dt.addAiEvent(targetFloor, newEvent);
                            successCount++;
                            updateProgress(targetCount);
                            generateNext(index + 1, targetCount, 0);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            if (seq != requestSeq) return;
                            mainHandler.removeCallbacks(timeout);
                            Log.e(TAG, targetFloor + "층 실패 (시도 " + (retryCount + 1) + "): " + errorMessage);
                            if (retryCount < MAX_RETRY) {
                                generateNext(index, targetCount, retryCount + 1);
                                return;
                            }
                            failCount++;
                            updateProgress(targetCount);
                            generateNext(index + 1, targetCount, 0);
                        });
                    }
                }
        );
    }

    private String progressMessage(int total) {
        String msg = "운명을 창조하는 중입니다...\n(" + successCount + " / " + total + ")";
        if (failCount > 0) msg += " 실패 " + failCount;
        return msg + "\n잠시 기다려 주십시오...";
    }

    private void updateProgress(int total) {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.setMessage(progressMessage(total));
    }

    private void finishGeneration() {
        if (isFinishing() || isDestroyed()) return;
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
        startGame();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        requestSeq++; // 화면을 떠난 뒤 오는 콜백은 버린다
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        super.onDestroy();
    }

    private void startGame() {
        Intent intent = new Intent(this, EventActivity.class);
        startActivity(intent);
        finish();
    }

    // ── 그리기 도우미 ────────────────────────────────────

    private TextView text(String s, float sp, int color) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.SERIF);
        return tv;
    }

    /** 던전 칸 테두리: 고른 칸은 금색 */
    private GradientDrawable frame(boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(8));
        d.setColor(0);
        d.setStroke(dp(selected ? 2 : 1.5f), selected ? GOLD : 0xFF3A342E);
        return d;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
