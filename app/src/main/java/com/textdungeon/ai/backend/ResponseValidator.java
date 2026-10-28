package com.textdungeon.ai.backend;

/** 응답을 게임에 그대로 쓸 수 있는지 검사한다. */
public interface ResponseValidator {

    /** 검사하지 않음 */
    ResponseValidator ANY = text -> null;

    /** @return 쓸 수 있으면 null, 아니면 실패 이유 */
    String check(String text);

    /** 통과한 응답만 callback.onResult로, 통과하지 못하면 이유를 callback.onError로 넘기는 콜백 */
    default LlmCallback guard(LlmCallback callback) {
        return new LlmCallback() {
            @Override
            public void onResult(String text) {
                String reason = check(text);
                if (reason == null) callback.onResult(text);
                else callback.onError(reason);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        };
    }
}
