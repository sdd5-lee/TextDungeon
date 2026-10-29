package com.textdungeon.activity_control;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.widget.ImageView;

import org.junit.Before;
import org.junit.Test;

// ══════════════════════════════════════════════
//  GameImages 테스트
//  이름으로 이미지를 찾아 넣고, 없으면 기본 이미지 (예전 getIdentifier 코드와 같은 동작)
//  찾은 결과는 프로세스 동안 기억하므로 테스트마다 다른 이름을 쓴다
// ══════════════════════════════════════════════
public class GameImagesTest {

    private static final String PKG = "com.example.textdungeon";
    private Context context;
    private Resources resources;
    private ImageView view;

    @Before
    public void setUp() {
        context = mock(Context.class);
        resources = mock(Resources.class);
        view = mock(ImageView.class);
        when(context.getResources()).thenReturn(resources);
        when(context.getPackageName()).thenReturn(PKG);
        when(view.getContext()).thenReturn(context);
    }

    @Test
    public void 있는_이름이면_그_이미지를_넣는다() {
        when(resources.getIdentifier("mon_test_a", "drawable", PKG)).thenReturn(101);

        assertTrue(GameImages.set(view, "mon_test_a", 999));
        verify(view).setImageResource(101);
        verify(view, never()).setImageResource(999);
    }

    @Test
    public void 없는_이름이면_기본_이미지를_넣는다() {
        when(resources.getIdentifier("mon_test_missing", "drawable", PKG)).thenReturn(0);

        assertFalse(GameImages.set(view, "mon_test_missing", 999));
        verify(view).setImageResource(999);
    }

    @Test
    public void 없는데_기본_이미지도_0이면_그대로_둔다() {
        assertFalse(GameImages.set(view, "item_test_missing", 0));
        verify(view, never()).setImageResource(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    public void 이름이_비었으면_찾지_않고_기본_이미지() {
        assertFalse(GameImages.set(view, null, 999));
        assertFalse(GameImages.set(view, "  ", 999));
        verify(resources, never()).getIdentifier(anyString(), anyString(), anyString());
        verify(view, times(2)).setImageResource(999);
    }

    @Test
    public void 한_번_찾은_이름은_다시_찾지_않는다() {
        when(resources.getIdentifier("item_test_cached", "drawable", PKG)).thenReturn(202);

        assertTrue(GameImages.exists(context, "item_test_cached"));
        assertTrue(GameImages.set(view, "item_test_cached", 0));
        verify(resources, times(1)).getIdentifier("item_test_cached", "drawable", PKG);
    }
}
