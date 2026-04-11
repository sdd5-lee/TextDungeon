package com.textdungeon.ai;

import android.os.Handler;
import android.os.Looper;

import com.textdungeon.event.BattleEvent;


public class ChaosDice {
    public void roll(BattleEvent currentEvent, AiCallback callback) {
        String prompt = "현재 플레이어가 마주한 상황 : ["+currentEvent.getDescription()+"]\n"
                +"이 상황을 완전히 뒤엎는거나 망쳐버리는 하나의 혼돈 성향의 선택지를 1개와"+
                " 그 선택지를 골랐을때 발생하는 보상, 그리고 뒤에 선택지에 따라서 대성공, 대실패를 붙여줘"+
                " 그리고 그 답을 반드시 json으로 줘";
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
           try {
               Thread.sleep(2000);

               String generatedChoice = "선택지: 춤을 추며 적의 무기에 머리를 들이민다.";

               String generatedResult = "[대성공] 적이 당신의 광기에 기겁하며 무기를 버리고 도망칩니다! \n보상: 최대 체력 +50, 500 골드 획득";
               mainThreadHandler.post(()->{
                   callback.onSuccess(generatedChoice, generatedResult);
               });
           } catch (Exception e) {
               mainThreadHandler.post(() -> {
               callback.onError("AI 통신 실패: 우주의 기운이 닿지 않았습니다.");
           });
           }
        });
    }
}
