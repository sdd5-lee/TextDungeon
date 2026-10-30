package com.textdungeon.dialog_control;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.RelativeCornerSize;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.textdungeon.activity_control.GameImages;
import com.textdungeon.model.Magic;
import com.textdungeon.model.MagicSchool;
import com.textdungeon.player.MagicScroll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 마법 나무. 가운데 기초 마법에서 계열마다 한 갈래씩 뻗고, 앞 마법(requires)을 따라 바깥으로 이어진다.
 * 계열 방향은 MagicSchool.angle, 단계별 거리는 TIER. 마법을 누르면 크게 보이고 listener로 알린다.
 */
public class MagicTreeView extends FrameLayout {

    public interface Listener {
        void onMagicSelected(Magic magic);
    }

    /** 가운데에서 몇 단계 떨어졌는지에 따른 거리 (가로·세로 반지름에 대한 비율) */
    private static final float[] TIER = {0f, 0.42f, 0.72f, 1f};
    private static final int NODE_DP = 52;
    private static final int ROOT_DP = 60;
    private static final float SELECTED_SCALE = 1.45f;

    private static final int LINE_LEARNED = 0xFFC99A45;
    private static final int LINE_OPEN = 0xFF6E5F43;
    private static final int LINE_LOCKED = 0xFF2E2A26;
    private static final int STROKE_CAN_LEARN = 0xFF8FD18A;
    private static final int STROKE_LOCKED = 0xFF4A4640;

    private static class Node {
        final Magic magic;
        Node parent;
        int depth;
        ShapeableImageView view;
        TextView label;
        float x, y;

        Node(Magic magic) {
            this.magic = magic;
        }
    }

    private final List<Node> nodes = new ArrayList<>();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint();
    private final float density;
    private MagicScroll scroll;
    private int wisdom;
    private Node selected;
    private Listener listener;

    public MagicTreeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        setWillNotDraw(false);
        setClipChildren(false);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp(1));
        ringPaint.setColor(0xFF221E1A);
    }

    public void setMagics(List<Magic> magics, MagicScroll scroll, Listener listener) {
        this.scroll = scroll;
        this.listener = listener;
        removeAllViews();
        nodes.clear();
        selected = null;

        Map<String, Node> byId = new HashMap<>();
        for (Magic m : magics) {
            Node n = new Node(m);
            nodes.add(n);
            byId.put(m.getId(), n);
        }
        for (Node n : nodes) {
            String req = n.magic.getRequires();
            n.parent = req != null ? byId.get(req) : null;
        }
        for (Node n : nodes) {
            int d = 0;
            for (Node p = n.parent; p != null && d < TIER.length - 1; p = p.parent) d++;
            n.depth = d;

            int size = dp(n.depth == 0 ? ROOT_DP : NODE_DP);
            ShapeableImageView v = new ShapeableImageView(getContext());
            v.setLayoutParams(new LayoutParams(size, size));
            v.setScaleType(ImageView.ScaleType.CENTER_CROP);
            v.setShapeAppearanceModel(ShapeAppearanceModel.builder()
                    .setAllCornerSizes(new RelativeCornerSize(0.5f)).build());
            int pad = dp(3);
            v.setPadding(pad, pad, pad, pad);
            v.setBackgroundColor(0x00000000);
            v.setContentDescription(n.magic.getName());
            GameImages.set(v, n.magic.getImg(), R.drawable.magic_default_icon);
            v.setOnClickListener(view -> select(n));
            n.view = v;
            addView(v);

            // 계열 이름은 각 갈래의 첫 마법 옆에
            if (n.depth == 1) {
                TextView label = new TextView(getContext());
                label.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                label.setText(n.magic.getSchool().displayName);
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                label.setTypeface(android.graphics.Typeface.SERIF);
                label.setGravity(Gravity.CENTER);
                n.label = label;
                addView(label);
            }
        }
    }

    /** 배운 상태·지혜가 바뀌면 다시 칠한다 */
    public void refresh(int wisdom) {
        this.wisdom = wisdom;
        for (Node n : nodes) styleNode(n);
        invalidate();
    }

    private void styleNode(Node n) {
        MagicScroll.LearnState state = scroll.getLearnState(n.magic, wisdom);
        int schoolColor = n.magic.getSchool().color;
        ShapeableImageView v = n.view;
        switch (state) {
            case LEARNED:
                v.setStrokeColor(ColorStateList.valueOf(schoolColor));
                v.setStrokeWidth(dp(3));
                v.clearColorFilter();
                v.setImageAlpha(255);
                break;
            case CAN_LEARN:
                v.setStrokeColor(ColorStateList.valueOf(STROKE_CAN_LEARN));
                v.setStrokeWidth(dp(2.5f));
                v.clearColorFilter();
                v.setImageAlpha(200);
                break;
            default:
                v.setStrokeColor(ColorStateList.valueOf(STROKE_LOCKED));
                v.setStrokeWidth(dp(2));
                ColorMatrix gray = new ColorMatrix();
                gray.setSaturation(0f);
                v.setColorFilter(new ColorMatrixColorFilter(gray));
                v.setImageAlpha(110);
                break;
        }
        if (n.label != null) {
            boolean open = scroll.isSchoolOpen(n.magic.getSchool());
            n.label.setTextColor(open ? schoolColor : 0xFF6A6862);
        }
    }

    private void select(Node n) {
        if (selected == n) return;
        if (selected != null) {
            selected.view.animate().scaleX(1f).scaleY(1f).setDuration(160).start();
            selected.view.setTranslationZ(0);
        }
        selected = n;
        n.view.setTranslationZ(dp(6));
        n.view.animate().scaleX(SELECTED_SCALE).scaleY(SELECTED_SCALE).setDuration(200).start();
        if (listener != null) listener.onMagicSelected(n.magic);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int w = right - left;
        int h = bottom - top;
        float cx = w / 2f;
        float cy = h / 2f;
        // 커졌을 때도 잘리지 않게 노드 반지름 × 확대율만큼 띄운다
        float margin = dp(NODE_DP) * SELECTED_SCALE / 2f + dp(4);
        float rx = cx - margin;
        float ry = cy - margin;

        for (Node n : nodes) {
            if (n.depth == 0) {
                n.x = cx;
                n.y = cy;
            } else {
                double a = Math.toRadians(n.magic.getSchool().angle);
                float f = TIER[Math.min(n.depth, TIER.length - 1)];
                n.x = cx + (float) Math.cos(a) * rx * f;
                n.y = cy + (float) Math.sin(a) * ry * f;
            }
            int size = n.view.getMeasuredWidth();
            int l = Math.round(n.x - size / 2f);
            int t = Math.round(n.y - size / 2f);
            n.view.layout(l, t, l + size, t + size);

            if (n.label != null) {
                int lw = n.label.getMeasuredWidth();
                int lh = n.label.getMeasuredHeight();
                // 아래로 뻗는 갈래는 다음 마법과 겹치지 않게 이름을 위에 둔다.
                // 마법을 눌러 커져도 이름을 가리지 않게 커진 크기 바깥에 둔다
                boolean below = Math.sin(Math.toRadians(n.magic.getSchool().angle)) < 0.9;
                float r = size * SELECTED_SCALE / 2f + dp(1);
                int lt = below ? Math.round(n.y + r) : Math.round(n.y - r - lh);
                int ll = Math.round(n.x - lw / 2f);
                n.label.layout(ll, lt, ll + lw, lt + lh);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float r = Math.max(w, h) / 2f;
        glowPaint.setShader(new RadialGradient(w / 2f, h / 2f, r,
                0xFF1E1912, 0xFF0D0C0B, Shader.TileMode.CLAMP));
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        canvas.drawRect(0, 0, w, h, glowPaint);

        // 단계별 고리 (패시브 나무 느낌)
        float margin = dp(NODE_DP) * SELECTED_SCALE / 2f + dp(4);
        float rx = w / 2f - margin;
        float ry = h / 2f - margin;
        for (int i = 1; i < TIER.length; i++) {
            RectF oval = new RectF(w / 2f - rx * TIER[i], h / 2f - ry * TIER[i],
                    w / 2f + rx * TIER[i], h / 2f + ry * TIER[i]);
            canvas.drawOval(oval, ringPaint);
        }

        // 앞 마법과 잇는 선: 배운 길은 금색, 지금 이어 갈 수 있는 길은 흐린 금색, 나머지는 어둡게
        if (scroll != null) {
            for (Node n : nodes) {
                if (n.parent == null) continue;
                boolean childLearned = scroll.hasMagic(n.magic.getId());
                boolean parentLearned = scroll.hasMagic(n.parent.magic.getId());
                boolean open = scroll.isSchoolOpen(n.magic.getSchool());
                if (childLearned) {
                    linePaint.setColor(LINE_LEARNED);
                    linePaint.setStrokeWidth(dp(4));
                } else if (parentLearned && open) {
                    linePaint.setColor(LINE_OPEN);
                    linePaint.setStrokeWidth(dp(3));
                } else {
                    linePaint.setColor(LINE_LOCKED);
                    linePaint.setStrokeWidth(dp(2));
                }
                canvas.drawLine(n.parent.x, n.parent.y, n.x, n.y, linePaint);
            }
        }
        super.dispatchDraw(canvas);
    }

    private int dp(float v) {
        return Math.round(v * density);
    }
}
