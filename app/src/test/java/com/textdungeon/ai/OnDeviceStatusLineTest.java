package com.textdungeon.ai;

import static org.junit.Assert.assertEquals;

import com.textdungeon.ai.backend.ModelDownloader.Status;
import com.textdungeon.ai.backend.OnDeviceBackend.State;

import org.junit.Test;

// ══════════════════════════════════════════════
//  설정 화면 "온디바이스 AI: …" 한 줄 테스트 (준비됨 / 받는 중 / 사용 불가)
// ══════════════════════════════════════════════
public class OnDeviceStatusLineTest {

    @Test
    public void 모델이_있으면_올라가기_전이라도_준비됨() {
        assertEquals("온디바이스 AI: 준비됨", AiManager.onDeviceStatusLine(Status.READY, -1, State.NOT_LOADED));
        assertEquals("온디바이스 AI: 준비됨", AiManager.onDeviceStatusLine(Status.READY, -1, State.READY));
    }

    @Test
    public void 모델이_있어도_초기화에_실패했으면_사용_불가() {
        assertEquals("온디바이스 AI: 사용 불가 (이 기기에서 실행 실패)",
                AiManager.onDeviceStatusLine(Status.READY, -1, State.FAILED));
    }

    @Test
    public void 받는_중에는_진행률이나_기다리는_이유를_보여준다() {
        assertEquals("온디바이스 AI: 받는 중 45%", AiManager.onDeviceStatusLine(Status.DOWNLOADING, 45, State.UNAVAILABLE));
        assertEquals("온디바이스 AI: 받는 중", AiManager.onDeviceStatusLine(Status.DOWNLOADING, -1, State.UNAVAILABLE));
        assertEquals("온디바이스 AI: 받는 중 (Wi-Fi 연결 대기)",
                AiManager.onDeviceStatusLine(Status.WAITING_NETWORK, -1, State.NOT_LOADED));
        assertEquals("온디바이스 AI: 받는 중 (파일 확인)",
                AiManager.onDeviceStatusLine(Status.VERIFYING, -1, State.NOT_LOADED));
    }

    @Test
    public void 받을_수_없으면_이유와_함께_사용_불가() {
        assertEquals("온디바이스 AI: 사용 불가 (지원하지 않는 기기)",
                AiManager.onDeviceStatusLine(Status.UNSUPPORTED, -1, State.UNAVAILABLE));
        assertEquals("온디바이스 AI: 사용 불가 (받기 실패)",
                AiManager.onDeviceStatusLine(Status.FAILED, -1, State.NOT_LOADED));
        assertEquals("온디바이스 AI: 사용 불가 (저장 공간 3.7GB 필요)",
                AiManager.onDeviceStatusLine(Status.NO_SPACE, -1, State.NOT_LOADED));
    }

    @Test
    public void 설정에서_끄면_꺼짐() {
        assertEquals("온디바이스 AI: 꺼짐 (주사위는 준비된 결과)",
                AiManager.onDeviceStatusLine(Status.DISABLED, -1, State.NOT_LOADED));
    }
}
