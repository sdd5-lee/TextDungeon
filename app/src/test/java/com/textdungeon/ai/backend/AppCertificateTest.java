package com.textdungeon.ai.backend;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

// ══════════════════════════════════════════════
//  AppCertificate 테스트: X-Android-Cert에 넣는 SHA-1 형식 (콜론 없는 대문자 40자)
// ══════════════════════════════════════════════
public class AppCertificateTest {

    @Test
    public void 콜론_없는_대문자_16진수() throws Exception {
        // 빈 입력의 SHA-1 (keytool에서는 DA:39:A3:...:07:09로 보인다)
        assertEquals("DA39A3EE5E6B4B0D3255BFEF95601890AFD80709", AppCertificate.sha1Hex(new byte[0]));
        assertEquals("A9993E364706816ABA3E25717850C26C9CD0D89D",
                AppCertificate.sha1Hex("abc".getBytes(StandardCharsets.US_ASCII)));
    }
}
