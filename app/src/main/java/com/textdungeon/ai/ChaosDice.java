package com.textdungeon.ai;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChaosDice {
    private final OkHttpClient client;
    private final Gson gson;

    public ChaosDice(OkHttpClient client) {
        this.client = client;
        this.gson = new Gson();
    }

    public void roll(int floor, Stat stat, List<Item> itemList, BattleEvent currentEvent, AiCallback callback) {
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        JSONObject requestData = new JSONObject();
        try {
            requestData.put("floor", floor);

            String statJson = gson.toJson(stat);
            requestData.put("stat", new JSONObject(statJson));

            String itemsJson = gson.toJson(itemList);
            requestData.put("available_items", new JSONArray(itemsJson));

            String eventJson = gson.toJson(currentEvent);
            requestData.put("original_event", new JSONObject(eventJson));

        } catch (Exception e) {
            e.printStackTrace();
            mainThreadHandler.post(() -> callback.onError("데이터 조립 실패: " + e.getMessage()));
            return;
        }

        RequestBody body = RequestBody.create(requestData.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8000/add_chaos_choice") // 로컬 서버 주소
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainThreadHandler.post(() -> callback.onError("AI 서버 연결 실패 (네트워크 확인)"));
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        // 서버가 준 JSON을 다시 BattleEvent 객체로 변환
                        BattleEvent updatedEvent = gson.fromJson(responseData, BattleEvent.class);
                        mainThreadHandler.post(() -> callback.onSuccess(updatedEvent));
                    } catch (Exception e) {
                        e.printStackTrace();
                        mainThreadHandler.post(() -> callback.onError("혼돈의 결말을 해석하지 못했습니다."));
                    }
                } else {
                    mainThreadHandler.post(() -> callback.onError("혼돈의 신이 침묵합니다. (서버 응답 오류)"));
                }
            }
        });
    }
}