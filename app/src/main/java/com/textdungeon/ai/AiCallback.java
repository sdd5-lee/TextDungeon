package com.textdungeon.ai;

public interface AiCallback {
        /**
         * AI 통신이 성공적으로 끝났을 때 호출됩니다.
         * @param choiceText 화면에 보여줄 선택지 버튼 이름 (예: "[혼돈] 춤을 춘다")
         * @param resultText 버튼을 눌렀을 때 나올 결과 텍스트
         */
        void onSuccess(String choiceText, String resultText);

        /**
         * 인터넷 끊김, 서버 오류 등으로 통신에 실패했을 때 호출됩니다.
         * @param errorMessage 에러 사유
         */
        void onError(String errorMessage);
}
