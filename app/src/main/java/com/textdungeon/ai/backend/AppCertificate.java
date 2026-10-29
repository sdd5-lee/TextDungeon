package com.textdungeon.ai.backend;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Log;

import java.security.MessageDigest;

/**
 * 이 앱을 서명한 인증서의 SHA-1 지문.
 * Google Cloud에서 API 키를 "Android 앱"으로 제한하면, REST 요청에 패키지 이름(X-Android-Package)과
 * 이 지문(X-Android-Cert)을 함께 보내야 통과한다. Google 라이브러리는 알아서 붙이지만 OkHttp는 직접 붙여야 한다.
 * 디버그 빌드는 디버그 키, Play로 받은 앱은 Play 앱 서명 키의 지문이 나온다 (키 제한에 둘 다 넣어야 함).
 */
public final class AppCertificate {
    private static final String TAG = "AppCertificate";
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private AppCertificate() {}

    /** 콜론 없는 대문자 16진수 40자. 읽지 못하면 null (그때는 헤더 없이 보낸다) */
    public static String sha1(Context context) {
        try {
            Signature signature = currentSignature(context);
            return signature == null ? null : sha1Hex(signature.toByteArray());
        } catch (Exception e) {
            Log.w(TAG, "서명 인증서를 읽지 못함", e);
            return null;
        }
    }

    @SuppressWarnings("deprecation") // GET_SIGNATURES는 API 28 미만에서만 쓴다
    private static Signature currentSignature(Context context) throws PackageManager.NameNotFoundException {
        PackageManager pm = context.getPackageManager();
        String pkg = context.getPackageName();
        Signature[] signatures;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageInfo info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES);
            // 키를 교체한 적이 있어도 지금 APK를 서명한 인증서만 받는다
            signatures = info.signingInfo == null ? null : info.signingInfo.getApkContentsSigners();
        } else {
            signatures = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures;
        }
        return signatures == null || signatures.length == 0 ? null : signatures[0];
    }

    static String sha1Hex(byte[] certificate) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(certificate);
        char[] out = new char[digest.length * 2];
        for (int i = 0; i < digest.length; i++) {
            out[i * 2] = HEX[(digest[i] >> 4) & 0xF];
            out[i * 2 + 1] = HEX[digest[i] & 0xF];
        }
        return new String(out);
    }
}
