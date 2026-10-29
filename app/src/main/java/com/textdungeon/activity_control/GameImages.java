package com.textdungeon.activity_control;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 게임 이미지(아이템, 몬스터, 이벤트, 직업, 마법, 글 속 아이콘)를 데이터에 적힌 이름으로 찾는다.
 * 이름으로 이미지를 찾는 곳은 모두 이 클래스를 거친다.
 *
 * 지금은 앱에 들어 있는 drawable만 찾는다. 나중에 DLC 이미지를 따로 받게 되면
 * 여기서 받은 폴더도 찾도록 바꾸면 되고, 화면 코드는 그대로 둔다.
 */
public final class GameImages {
    // 이름 → drawable id (없으면 0). getIdentifier가 느려서 한 번 찾은 건 기억해 둔다
    private static final Map<String, Integer> RES_IDS = new ConcurrentHashMap<>();

    private GameImages() {}

    public static boolean exists(Context context, String name) {
        return resId(context, name) != 0;
    }

    /** 이름에 맞는 이미지를 넣는다. 없으면 fallback을 넣고(0이면 그대로 둔다) false를 돌려준다 */
    public static boolean set(ImageView view, String name, @DrawableRes int fallback) {
        int id = resId(view.getContext(), name);
        if (id != 0) {
            view.setImageResource(id);
            return true;
        }
        if (fallback != 0) view.setImageResource(fallback);
        return false;
    }

    /** 글 속 아이콘처럼 Drawable이 필요할 때. 없으면 null */
    public static Drawable drawable(Context context, String name) {
        int id = resId(context, name);
        return id != 0 ? ContextCompat.getDrawable(context, id) : null;
    }

    /**
     * Glide로 불러올 때 (도감처럼 큰 이미지를 많이 띄우는 곳). 없으면 빈 요청이라 아무것도 그리지 않는다.
     * 리소스 id는 load(Integer)로 넘겨야 Glide가 앱 버전을 캐시 키에 넣는다 (업데이트로 id가 바뀌어도 엉뚱한 그림이 안 나옴)
     */
    public static RequestBuilder<Drawable> glide(Context context, String name) {
        int id = resId(context, name);
        RequestManager glide = Glide.with(context);
        return id != 0 ? glide.load(Integer.valueOf(id)) : glide.load((Object) null);
    }

    private static int resId(Context context, String name) {
        if (name == null || name.trim().isEmpty()) return 0;
        Integer cached = RES_IDS.get(name);
        if (cached != null) return cached;
        int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        RES_IDS.put(name, id);
        return id;
    }
}
