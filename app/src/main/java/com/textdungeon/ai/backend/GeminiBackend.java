package com.textdungeon.ai.backend;

import android.util.Log;

import com.example.textdungeon.BuildConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Gemini REST API 백엔드.
 * 기존 EventGenerator/ChaosDice가 각자 들고 있던 OkHttp 코드를 한 곳으로 모았다.
 * 싱글 스레드 executor 대신 enqueue()를 써서 요청끼리 서로 기다리지 않는다.
 */
public class GeminiBackend implements LlmBackend {
    private static final String TAG = "GeminiBackend";
    /** Logcat에서 'tag:AI_RAW'로 걸러 보면 AI 요청/응답 원문만 볼 수 있다 (디버그 빌드에서만 출력) */
    public static final String RAW_TAG = "AI_RAW";
    private static final String ENDPOINT_FORMAT =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    // OkHttpClient는 커넥션 풀을 공유하는 게 권장 사항이라 static 하나만 둔다.
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final String model;
    private final String apiKey;

    public GeminiBackend(String model, String apiKey) {
        this.model = model;
        this.apiKey = apiKey;
    }

    @Override
    public void generate(String prompt, ResponseFormat format, LlmCallback callback) {
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        JsonArray parts = new JsonArray();
        parts.add(part);
        JsonObject content = new JsonObject();
        content.add("parts", parts);
        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject body = new JsonObject();
        body.add("contents", contents);
        if (format == ResponseFormat.JSON) {
            JsonObject config = new JsonObject();
            config.addProperty("responseMimeType", "application/json");
            body.add("generationConfig", config);
        }

        final long requestId = System.currentTimeMillis() % 100000;
        logRaw("요청 #" + requestId + " (" + format + ")", prompt);

        Request request = new Request.Builder()
                .url(String.format(ENDPOINT_FORMAT, model))
                .addHeader("x-goog-api-key", apiKey)
                .post(RequestBody.create(body.toString(), JSON_MEDIA))
                .build();

        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "네트워크 오류", e);
                callback.onError("네트워크 오류: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    String raw = r.body() != null ? r.body().string() : "";
                    if (!r.isSuccessful()) {
                        logRaw("응답 #" + requestId + " HTTP " + r.code(), raw);
                        Log.e(TAG, "HTTP " + r.code() + ": " + raw);
                        callback.onError("HTTP " + r.code());
                        return;
                    }
                    String text = extractText(raw);
                    logRaw("응답 #" + requestId, text != null ? text : raw);
                    if (text == null || text.trim().isEmpty()) {
                        Log.e(TAG, "응답에 텍스트 없음: " + raw);
                        callback.onError("빈 응답");
                        return;
                    }
                    callback.onResult(text);
                } catch (Exception e) {
                    Log.e(TAG, "응답 처리 오류", e);
                    callback.onError("응답 처리 오류: " + e.getMessage());
                }
            }
        });
    }

    /**
     * AI 원문을 Logcat에 남긴다. 한 줄이 약 4000자를 넘으면 Logcat이 잘라버리므로 나눠서 찍는다.
     * 출시 빌드(release)에서는 찍지 않는다.
     */
    public static void logRaw(String label, String text) {
        if (!BuildConfig.DEBUG) return;
        String body = text == null ? "(null)" : text;
        int chunk = 3500;
        int parts = Math.max(1, (body.length() + chunk - 1) / chunk);
        for (int i = 0; i < parts; i++) {
            String piece = body.substring(i * chunk, Math.min(body.length(), (i + 1) * chunk));
            Log.d(RAW_TAG, "[" + label + (parts > 1 ? " " + (i + 1) + "/" + parts : "") + "]\n" + piece);
        }
    }

    /** candidates[0].content.parts[*].text 를 이어 붙인다. 안전 필터로 막히면 candidates가 없을 수 있다. */
    private static String extractText(String raw) {
        JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
        if (!root.has("candidates")) return null;
        JsonArray candidates = root.getAsJsonArray("candidates");
        if (candidates.size() == 0) return null;
        JsonObject first = candidates.get(0).getAsJsonObject();
        if (!first.has("content")) return null;
        JsonObject content = first.getAsJsonObject("content");
        if (!content.has("parts")) return null;

        StringBuilder sb = new StringBuilder();
        for (JsonElement p : content.getAsJsonArray("parts")) {
            JsonObject po = p.getAsJsonObject();
            if (po.has("text")) sb.append(po.get("text").getAsString());
        }
        return sb.toString();
    }

    @Override
    public boolean isReady() {
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public String name() {
        return "Gemini(" + model + ")";
    }
}