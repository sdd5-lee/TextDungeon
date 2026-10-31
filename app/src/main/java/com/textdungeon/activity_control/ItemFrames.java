package com.textdungeon.activity_control;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.ImageView;

import com.textdungeon.model.Item;
import com.textdungeon.model.ItemGrade;

/**
 * 아이템 칸의 등급 틀을 코드로 그린다: 어두운 바탕 + 가장자리에서 안쪽으로 은은하게 번지는 등급 빛 + 등급 색 테두리.
 * 영웅·전설은 테두리 안쪽에 가는 선이 하나 더 있다.
 * 가방·장비 칸, 아이템 창, 상점, 도감이 모두 이걸 쓴다. 그림이 없는 아이템은 종류 아이콘을 넣는다.
 */
public final class ItemFrames {
    // 가장자리 빛의 진하기 (일반 → 전설)
    private static final int[] GLOW_ALPHA = {0x40, 0x58, 0x68, 0x78, 0x90};

    private ItemFrames() {}

    /** 칸 하나에 쓸 틀. Drawable은 뷰끼리 나눠 쓰면 안 돼서 부를 때마다 새로 만든다 */
    public static Drawable frame(Context context, ItemGrade grade) {
        return new FrameDrawable(grade, context.getResources().getDisplayMetrics().density);
    }

    /** 아이템 그림을 넣는다. 그림이 없으면 종류 아이콘 */
    public static void setIcon(ImageView view, Item item) {
        String name = GameImages.exists(view.getContext(), item.getImgId()) ? item.getImgId() : typeIcon(item.getType());
        GameImages.set(view, name, 0);
        tintTypeIcon(view, name, item.getGrade());
    }

    /**
     * 종류 아이콘(ic_)은 검은 선이라 어두운 칸에서 안 보여서 등급 색으로 칠한다. 그림이면 칠을 뺀다.
     * 칸은 다시 쓰이므로 매번 부른다
     */
    public static void tintTypeIcon(ImageView view, String imgName, ItemGrade grade) {
        if (grade != null && imgName != null && imgName.startsWith("ic_")) {
            view.setColorFilter(grade.color);
        } else {
            view.clearColorFilter();
        }
    }

    /** 그림이 없을 때 쓰는 종류 아이콘 이름. 물약 아이콘은 따로 없어서 하급 포션 그림을 쓴다 */
    public static String typeIcon(String type) {
        switch (type == null ? "" : type.toLowerCase()) {
            case "weapon": return "ic_weapon";
            case "armor": return "ic_armor";
            case "consumables": return "item_img_34";
            default: return "ic_artifact";
        }
    }

    public static String typeName(String type) {
        switch (type == null ? "" : type.toLowerCase()) {
            case "weapon": return "무기";
            case "armor": return "방어구";
            case "consumables": return "소모품";
            default: return "장신구";
        }
    }

    /** 등급과 종류 한 줄. 예: "희귀 · 무기" */
    public static String label(Item item) {
        return item.getGrade().displayName + " · " + typeName(item.getType());
    }

    /** 설명 맨 위에 등급 색으로 label을 붙인다 (아이템 창·도감) */
    public static CharSequence withLabel(Item item, CharSequence body) {
        SpannableStringBuilder sb = new SpannableStringBuilder(label(item));
        sb.setSpan(new ForegroundColorSpan(item.getGrade().color), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb.append("\n\n").append(body);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static final class FrameDrawable extends Drawable {
        private final ItemGrade grade;
        private final float density;
        private final float radius;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint inner;
        private final RectF rect = new RectF();
        private final RectF innerRect = new RectF();

        FrameDrawable(ItemGrade grade, float density) {
            this.grade = grade;
            this.density = density;
            radius = 6 * density;
            fill.setColor(0xFF1E1E1E);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(2 * density);
            border.setColor(grade.color);
            if (grade.ordinal() >= ItemGrade.EPIC.ordinal()) {
                inner = new Paint(Paint.ANTI_ALIAS_FLAG);
                inner.setStyle(Paint.Style.STROKE);
                inner.setStrokeWidth(density);
                inner.setColor(withAlpha(grade.color, 0x70));
            } else {
                inner = null;
            }
        }

        @Override
        protected void onBoundsChange(Rect b) {
            float half = border.getStrokeWidth() / 2;
            rect.set(b.left + half, b.top + half, b.right - half, b.bottom - half);
            innerRect.set(rect);
            innerRect.inset(3 * density, 3 * density);
            float r = (float) Math.hypot(b.width(), b.height()) / 2;
            if (r <= 0) {
                glow.setShader(null);
                return;
            }
            // 가운데는 비우고 가장자리로 갈수록 등급 색이 짙어진다 (그림을 가리지 않게)
            glow.setShader(new RadialGradient(b.exactCenterX(), b.exactCenterY(), r,
                    new int[]{0, 0, withAlpha(grade.color, GLOW_ALPHA[grade.ordinal()])},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawRoundRect(rect, radius, radius, fill);
            if (glow.getShader() != null) canvas.drawRoundRect(rect, radius, radius, glow);
            canvas.drawRoundRect(rect, radius, radius, border);
            if (inner != null) {
                float r = Math.max(0, radius - 3 * density);
                canvas.drawRoundRect(innerRect, r, r, inner);
            }
        }

        // 칸을 흐리게 할 때는 뷰 알파를 쓴다
        @Override
        public void setAlpha(int alpha) {}

        @Override
        public void setColorFilter(ColorFilter colorFilter) {}

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
