package com.textdungeon.ai.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

// ══════════════════════════════════════════════
//  ModelDownloader.verify 테스트
//  받은 파일은 크기와 SHA-256이 모두 맞을 때만 모델 이름으로 바꾼다
// ══════════════════════════════════════════════
public class ModelDownloaderTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    // "abc"의 SHA-256 (FIPS 180-2 예제 값)
    private static final String ABC_SHA256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    private File fileWith(String content) throws IOException {
        File f = tmp.newFile();
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    @Test
    public void sha256은_알려진_값과_같다() throws IOException {
        assertEquals(ABC_SHA256, ModelDownloader.sha256(fileWith("abc")));
    }

    @Test
    public void 크기와_해시가_맞으면_통과() throws IOException {
        assertNull(ModelDownloader.verify(fileWith("abc"), 3, ABC_SHA256));
        // 대문자 해시도 같게 본다
        assertNull(ModelDownloader.verify(fileWith("abc"), 3, ABC_SHA256.toUpperCase()));
    }

    @Test
    public void 크기가_다르면_해시를_보기_전에_실패() throws IOException {
        String problem = ModelDownloader.verify(fileWith("ab"), 3, ABC_SHA256);
        assertTrue(problem, problem.startsWith("크기가 다름"));
    }

    @Test
    public void 크기는_같고_내용이_다르면_실패() throws IOException {
        String problem = ModelDownloader.verify(fileWith("abd"), 3, ABC_SHA256);
        assertTrue(problem, problem.startsWith("SHA-256이 다름"));
    }

    @Test
    public void 파일이_없으면_실패() {
        assertEquals("파일 없음", ModelDownloader.verify(new File(tmp.getRoot(), "없음"), 3, ABC_SHA256));
    }

    @Test
    public void 모델_주소는_측정한_리비전에_고정되어_있다() {
        assertTrue(ModelDownloader.MODEL_URL.contains("/resolve/1caa527c1e88b55f046fcd8d2b4265606680663d/"));
        assertTrue(ModelDownloader.MODEL_URL.endsWith("/" + ModelLocator.MODEL_FILE));
    }
}
