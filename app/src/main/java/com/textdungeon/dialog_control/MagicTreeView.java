package com.textdungeon.dialog_control;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 세피로트의 나무. 맨 아래 기초 마법이 뿌리 자리이고, 줄기가 세 가지로 갈라진 뒤
 * 가지마다 두 계열이 한 줄씩 위로 자란다 (계열 순서는 MagicSchool).
 * 계열을 열면 그 가지가 계열 모습으로 바뀌고(화염은 불이 붙고 신성은 하얘진다),
 * 배운 마법까지 이어진 토막일수록 진하다. 마법을 누르면 크게 보이고 listener로 알린다.
 */
public class MagicTreeView extends FrameLayout {

    public interface Listener {
        void onMagicSelected(Magic magic);
    }

    /** 기초 마법에서 몇 단계 위인지에 따른 높이 (0 = 기초 마법, 1 = 가장 위) */
    private static final float[] TIER_Y = {0f, 0.40f, 0.70f, 1f};
    /** 줄기가 세 가지로, 가지가 두 계열로 갈라지는 높이 */
    private static final float TRUNK_FORK = 0.17f;
    private static final float BOUGH_FORK = 0.29f;
    private static final int NODE_DP = 40;
    private static final int ROOT_DP = 46;
    private static final float SELECTED_SCALE = 1.35f;
    /** 가지 끝 위의 계열 이름 자리, 기초 마법 아래 뿌리 자리, 가지 끝 잔가지 길이(마법 가운데부터) */
    private static final int LABEL_DP = 22;
    private static final int ROOTS_DP = 26;
    private static final int TWIG_DP = 30;
    /** 굵기가 줄어드는 토막 하나를 몇 조각으로 나눠 그릴지 */
    private static final int PIECES = 8;

    private static final int STROKE_CAN_LEARN = 0xFF8FD18A;
    private static final int STROKE_LOCKED = 0xFF4A4640;
    private static final int GOLD_VEIN = 0xFFE9C176;

    /** 나무 색: 테두리, 바탕, 밝은 결, 가운데 빛(0이면 없음) */
    private static final int[] BARE = {0xFF1A120C, 0xFF33261A, 0xFF46352A, 0};
    private static final Map<MagicSchool, int[]> LOOKS = new EnumMap<>(MagicSchool.class);

    static {
        LOOKS.put(MagicSchool.BASIC, new int[]{0xFF24170E, 0xFF5C3F27, 0xFF7D5A3A, 0});
        LOOKS.put(MagicSchool.FIRE, new int[]{0xFF1E0D06, 0xFF3B1D10, 0xFF6B2E14, 0xFFFF7A2A});
        LOOKS.put(MagicSchool.FROST, new int[]{0xFF2B4352, 0xFF8DB4C8, 0xFFD8EEF8, 0});
        LOOKS.put(MagicSchool.NATURE, new int[]{0xFF22200E, 0xFF5E5A28, 0xFF93B84A, 0});
        LOOKS.put(MagicSchool.WIND, new int[]{0xFF1F3A36, 0xFF6E9E96, 0xFFB8E6DD, 0});
        LOOKS.put(MagicSchool.HOLY, new int[]{0xFF8A7F66, 0xFFE8E0CC, 0xFFFFFFFF, 0});
        LOOKS.put(MagicSchool.DARK, new int[]{0xFF0E0A12, 0xFF251A2E, 0xFF4A3560, 0xFFA77BD8});
    }

    /** 계열을 열면 가지 둘레에 번지는 빛 */
    private static final Map<MagicSchool, Integer> GLOWS = new EnumMap<>(MagicSchool.class);

    static {
        GLOWS.put(MagicSchool.FIRE, 0xFFFF6A1A);
        GLOWS.put(MagicSchool.NATURE, 0xFF8BC34A);
        GLOWS.put(MagicSchool.FROST, 0xFFBFE6FF);
        GLOWS.put(MagicSchool.WIND, 0xFF5FD3C0);
        GLOWS.put(MagicSchool.HOLY, 0xFFFFF3C0);
        GLOWS.put(MagicSchool.DARK, 0xFF8A5CC8);
    }

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

    /** 나무 한 토막. 끝의 마법(tip)이 속한 계열 모습으로 칠한다. 줄기·뿌리·큰 가지는 BASIC */
    private static class Branch {
        final MagicSchool school;
        final Node tip;
        final Path[] pieces = new Path[PIECES];
        final float[] widths = new float[PIECES];
        /** 장식 자리: x, y, 접선 각도, 크기, 위상, 옆(±1), 그 자리 굵기 */
        final List<float[]> marks = new ArrayList<>();

        Branch(MagicSchool school, Node tip) {
            this.school = school;
            this.tip = tip;
        }
    }

    private final List<Node> nodes = new ArrayList<>();
    private final List<Branch> branches = new ArrayList<>();
    private final Paint wood = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint();
    private final Path flame = new Path();
    private final RectF oval = new RectF();
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
        wood.setStyle(Paint.Style.STROKE);
        wood.setStrokeCap(Paint.Cap.ROUND);
        wood.setStrokeJoin(Paint.Join.ROUND);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
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
            for (Node p = n.parent; p != null && d < TIER_Y.length - 1; p = p.parent) d++;
            n.depth = d;

            int size = dp(n.depth == 0 ? ROOT_DP : NODE_DP);
            ShapeableImageView v = new ShapeableImageView(getContext());
            v.setLayoutParams(new LayoutParams(size, size));
            v.setScaleType(ImageView.ScaleType.CENTER_CROP);
            v.setShapeAppearanceModel(ShapeAppearanceModel.builder()
                    .setAllCornerSizes(new RelativeCornerSize(0.5f)).build());
            int pad = dp(3);
            v.setPadding(pad, pad, pad, pad);
            // 흐린 그림 뒤로 가지가 비치지 않게 둥근 바탕을 깐다
            GradientDrawable back = new GradientDrawable();
            back.setShape(GradientDrawable.OVAL);
            back.setColor(0xFF14110E);
            v.setBackground(back);
            v.setContentDescription(n.magic.getName());
            GameImages.set(v, n.magic.getImg(), R.drawable.magic_default_icon);
            v.setOnClickListener(view -> select(n));
            n.view = v;
            addView(v);

            // 계열 이름은 그 계열 가지 끝 위에
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

    /** 커졌을 때도 잘리지 않게 노드 반지름 × 확대율만큼 띄운다 */
    private float margin() {
        return dp(NODE_DP) * SELECTED_SCALE / 2f + dp(4);
    }

    private static List<MagicSchool> columns() {
        List<MagicSchool> columns = new ArrayList<>();
        for (MagicSchool s : MagicSchool.values()) {
            if (s.needsCore()) columns.add(s);
        }
        return columns;
    }

    // ── 배치 ────────────────────────────────────────────

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int w = right - left;
        int h = bottom - top;
        float rootX = w / 2f;
        float rootY = h - margin() - dp(ROOTS_DP);
        float span = rootY - margin() - dp(LABEL_DP);
        List<MagicSchool> columns = columns();
        float colW = w / (float) columns.size();

        for (Node n : nodes) {
            if (n.depth == 0) {
                n.x = rootX;
                n.y = rootY;
            } else {
                n.x = colW * (columns.indexOf(n.magic.getSchool()) + 0.5f);
                n.y = rootY - span * TIER_Y[n.depth];
            }
        }
        // 배경은 뿌리 자리에서 번지는 빛
        glowPaint.setShader(new RadialGradient(rootX, rootY, Math.max(w, h) * 0.8f,
                0xFF1E1912, 0xFF0D0C0B, Shader.TileMode.CLAMP));

        for (Node n : nodes) {
            int size = n.view.getMeasuredWidth();
            int l = Math.round(n.x - size / 2f);
            int t = Math.round(n.y - size / 2f);
            n.view.layout(l, t, l + size, t + size);
        }
        for (Node n : nodes) {
            if (n.label != null) placeLabel(n);
        }
        buildTree(w, h, rootX, rootY, span, colW, columns);
    }

    /** 가지 끝(가장 위 마법)의 잔가지 위에 계열 이름 */
    private void placeLabel(Node n) {
        Node tip = topOf(n.magic.getSchool());
        int lw = n.label.getMeasuredWidth();
        int lh = n.label.getMeasuredHeight();
        float r = Math.max(tip.view.getMeasuredWidth() * SELECTED_SCALE / 2f, dp(TWIG_DP)) + dp(2);
        int ll = Math.round(tip.x - lw / 2f);
        int lt = Math.round(tip.y - r - lh);
        n.label.layout(ll, lt, ll + lw, lt + lh);
    }

    private Node topOf(MagicSchool school) {
        Node tip = null;
        for (Node o : nodes) {
            if (o.magic.getSchool() == school && (tip == null || o.y < tip.y)) tip = o;
        }
        return tip;
    }

    /** 뿌리 → 줄기 → 큰 가지 셋 → 계열 가지 → 마법 사이 토막 → 끝 잔가지 */
    private void buildTree(int w, int h, float rootX, float rootY, float span, float colW, List<MagicSchool> columns) {
        branches.clear();
        Random rnd = new Random(7);

        float[] rootEnds = {-0.44f, -0.2f, 0.14f, 0.42f};
        for (float f : rootEnds) {
            float ex = rootX + w * f;
            Path p = new Path();
            p.moveTo(rootX + f * dp(20), rootY);
            p.cubicTo(rootX + w * f * 0.25f, rootY + dp(22),
                    ex - w * f * 0.3f, h - dp(10 + rnd.nextInt(10)), ex, h + dp(4));
            addBranch(p, dp(10), dp(2), MagicSchool.BASIC, null, rnd);
        }

        float forkY = rootY - span * TRUNK_FORK;
        Path trunk = new Path();
        trunk.moveTo(rootX, rootY);
        trunk.cubicTo(rootX - dp(6), rootY - span * 0.06f, rootX + dp(5), rootY - span * 0.12f, rootX, forkY);
        addBranch(trunk, dp(22), dp(16), MagicSchool.BASIC, null, rnd);

        // 계열 둘이 한 가지를 나눠 쓴다
        float boughY = rootY - span * BOUGH_FORK;
        int groups = (columns.size() + 1) / 2;
        float[] boughX = new float[groups];
        for (int g = 0; g < groups; g++) {
            int last = Math.min(columns.size() - 1, 2 * g + 1);
            boughX[g] = colW * ((2 * g + last) / 2f + 0.5f);
            Path p = new Path();
            p.moveTo(rootX, forkY);
            p.cubicTo(rootX, forkY - span * 0.06f, boughX[g], boughY + span * 0.06f, boughX[g], boughY);
            addBranch(p, dp(15), dp(11), MagicSchool.BASIC, null, rnd);
        }

        for (Node n : nodes) {
            if (n.depth == 0) continue;
            MagicSchool school = n.magic.getSchool();
            Path p = new Path();
            if (n.depth == 1 || n.parent == null) {
                float gx = boughX[columns.indexOf(school) / 2];
                p.moveTo(gx, boughY);
                p.cubicTo(gx, boughY - span * 0.04f, n.x, n.y + span * 0.07f, n.x, n.y);
                addBranch(p, dp(11), dp(8.5f), school, n, rnd);
            } else {
                Node a = n.parent;
                float wob = dp(4) * (rnd.nextBoolean() ? 1 : -1);
                float len = a.y - n.y;
                p.moveTo(a.x, a.y);
                p.cubicTo(a.x + wob, a.y - len * 0.35f, n.x - wob, n.y + len * 0.35f, n.x, n.y);
                addBranch(p, dp(11.5f - 1.5f * n.depth), dp(10f - 1.5f * n.depth), school, n, rnd);
            }
        }

        for (MagicSchool school : columns) {
            Node tip = topOf(school);
            if (tip == null) continue;
            Path p = new Path();
            p.moveTo(tip.x, tip.y);
            p.quadTo(tip.x - dp(2), tip.y - dp(TWIG_DP) * 0.6f, tip.x + dp(4), tip.y - dp(TWIG_DP));
            addBranch(p, dp(6.5f), dp(2), school, tip, rnd);
        }
    }

    /** 굵기가 w0에서 w1로 줄어드는 토막을 조각으로 나누고, 장식 자리를 12dp마다 잡는다 */
    private void addBranch(Path path, float w0, float w1, MagicSchool school, Node tip, Random rnd) {
        Branch b = new Branch(school, tip);
        PathMeasure pm = new PathMeasure(path, false);
        float len = pm.getLength();
        for (int i = 0; i < PIECES; i++) {
            Path piece = new Path();
            pm.getSegment(len * i / PIECES, Math.min(len, len * (i + 1) / PIECES + dp(1)), piece, true);
            b.pieces[i] = piece;
            b.widths[i] = w0 + (w1 - w0) * (i + 0.5f) / PIECES;
        }
        float[] pos = new float[2];
        float[] tan = new float[2];
        float step = dp(12);
        for (float d = step * 0.5f; d < len; d += step) {
            pm.getPosTan(d, pos, tan);
            b.marks.add(new float[]{pos[0], pos[1], (float) Math.toDegrees(Math.atan2(tan[1], tan[0])),
                    0.75f + rnd.nextFloat() * 0.5f, rnd.nextFloat() * 6.283f, rnd.nextBoolean() ? 1 : -1,
                    w0 + (w1 - w0) * d / len});
        }
        branches.add(b);
    }

    // ── 그리기 ────────────────────────────────────────────

    private boolean isOpen(Branch b) {
        return b.school == MagicSchool.BASIC || scroll.isSchoolOpen(b.school);
    }

    /** 이 토막이 이어 주는 마법을 배웠는지. 줄기·뿌리는 기초 마법을 배웠는지 */
    private boolean isLit(Branch b) {
        if (b.tip != null) return scroll.hasMagic(b.tip.magic.getId());
        for (Node n : nodes) {
            if (n.depth == 0 && scroll.hasMagic(n.magic.getId())) return true;
        }
        return false;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), glowPaint);
        if (scroll != null && !branches.isEmpty()) drawTree(canvas);
        super.dispatchDraw(canvas);
    }

    private void drawTree(Canvas canvas) {
        float t = SystemClock.uptimeMillis() / 1000f;
        boolean animate = false;

        // 열린 계열은 가지 둘레로 빛이 번진다 (불은 일렁인다)
        for (Branch b : branches) {
            Integer glow = GLOWS.get(b.school);
            if (glow == null || !isOpen(b)) continue;
            float flicker = b.school == MagicSchool.FIRE ? 0.75f + 0.25f * (float) Math.sin(t * 7 + b.widths[0]) : 1f;
            wood.setColor(glow);
            wood.setAlpha((int) ((isLit(b) ? 70 : 28) * flicker));
            drawPieces(canvas, b, dp(9), 1f, 0);
        }
        // 테두리 → 바탕 → 밝은 결(왼쪽)과 어두운 주름(오른쪽)
        for (Branch b : branches) {
            wood.setColor(look(b)[0]);
            drawPieces(canvas, b, dp(2.5f), 1f, 0);
        }
        for (Branch b : branches) {
            wood.setColor(look(b)[1]);
            drawPieces(canvas, b, 0, 1f, 0);
        }
        for (Branch b : branches) {
            int[] c = look(b);
            wood.setColor(c[2]);
            drawPieces(canvas, b, 0, 0.22f, -0.2f);
            wood.setColor(c[0]);
            wood.setAlpha(160);
            drawPieces(canvas, b, 0, 0.12f, 0.22f);
        }
        // 가운데 빛: 불·어둠의 속불, 기초 마법을 배우면 줄기에 금빛
        for (Branch b : branches) {
            boolean lit = isLit(b);
            int core = isOpen(b) ? look(b)[3] : 0;
            if (core == 0 && b.school == MagicSchool.BASIC && lit) core = GOLD_VEIN;
            if (core == 0) continue;
            float flicker = b.school == MagicSchool.FIRE ? 0.85f + 0.15f * (float) Math.sin(t * 6 + b.widths[0]) : 1f;
            boolean vein = core == GOLD_VEIN;
            wood.setColor(core);
            wood.setAlpha((int) ((vein ? 120 : lit ? 230 : 110) * flicker));
            drawPieces(canvas, b, 0, vein ? 0.14f : 0.28f, 0);
        }
        // 계열 장식
        for (Branch b : branches) {
            if (b.school == MagicSchool.BASIC || !isOpen(b)) continue;
            drawMarks(canvas, b, isLit(b), t);
            animate = true;
        }
        if (animate) postInvalidateOnAnimation();
    }

    private int[] look(Branch b) {
        return isOpen(b) ? LOOKS.get(b.school) : BARE;
    }

    /** 조각마다 굵기(widths × scale + extra)로, 굵기 × shift만큼 옆으로 밀어 그린다 */
    private void drawPieces(Canvas canvas, Branch b, float extra, float scale, float shift) {
        int alpha = wood.getAlpha();
        for (int i = 0; i < PIECES; i++) {
            float w = b.widths[i] * scale + extra;
            wood.setStrokeWidth(w);
            wood.setAlpha(alpha);
            if (shift == 0) {
                canvas.drawPath(b.pieces[i], wood);
            } else {
                int save = canvas.save();
                canvas.translate(b.widths[i] * shift, 0);
                canvas.drawPath(b.pieces[i], wood);
                canvas.restoreToCount(save);
            }
        }
    }

    /** 배운 토막은 장식을 다 그리고, 열기만 한 토막은 셋에 하나만 작게 */
    private void drawMarks(Canvas canvas, Branch b, boolean lit, float t) {
        for (int i = 0; i < b.marks.size(); i++) {
            if (!lit && i % 3 != 0) continue;
            float[] m = b.marks.get(i);
            float x = m[0], y = m[1], angle = m[2], ph = m[4], side = m[5], bw = m[6];
            float s = m[3] * (lit ? 1f : 0.6f);
            switch (b.school) {
                case FIRE: {
                    float fx = x + side * bw * 0.25f;
                    float hgt = dp(17) * s * (0.8f + 0.25f * (float) Math.sin(t * 9 + ph));
                    float wd = dp(5.5f) * s;
                    drawFlame(canvas, fx, y, hgt, wd, 0xC8FF6A1A);
                    drawFlame(canvas, fx, y, hgt * 0.55f, wd * 0.55f, 0xE6FFD25A);
                    float rise = (t * dp(18) + ph * dp(6)) % dp(30);
                    fill.setColor(0xFFFFB347);
                    fill.setAlpha((int) (200 * (1 - rise / dp(30))));
                    canvas.drawCircle(fx + (float) Math.sin(t * 3 + ph) * dp(3), y - hgt - rise, dp(1.4f), fill);
                    break;
                }
                case FROST: {
                    float r = dp(4.5f) * s;
                    float cx = x + side * bw * 0.45f;
                    line.setColor(0xFFEAF8FF);
                    line.setAlpha((int) (170 + 70 * Math.sin(t * 2 + ph)));
                    line.setStrokeWidth(dp(1));
                    for (int k = 0; k < 3; k++) {
                        double a = Math.toRadians(angle + k * 60);
                        float dx = (float) Math.cos(a) * r;
                        float dy = (float) Math.sin(a) * r;
                        canvas.drawLine(cx - dx, y - dy, cx + dx, y + dy, line);
                    }
                    break;
                }
                case NATURE: {
                    int save = canvas.save();
                    canvas.translate(x + side * bw * 0.4f, y);
                    canvas.rotate(angle + side * (55 + 8 * (float) Math.sin(t * 1.5 + ph)));
                    oval.set(0, -dp(3) * s, dp(12) * s, dp(3) * s);
                    fill.setColor(i % 2 == 0 ? 0xFF6FAE3A : 0xFF8BC34A);
                    canvas.drawOval(oval, fill);
                    canvas.restoreToCount(save);
                    break;
                }
                case WIND: {
                    float r = dp(6) * s;
                    float cx = x + side * (bw * 0.5f + r * 0.6f);
                    oval.set(cx - r, y - r, cx + r, y + r);
                    line.setColor(0xFF5FD3C0);
                    line.setAlpha(150);
                    line.setStrokeWidth(dp(1.3f));
                    canvas.drawArc(oval, (t * 120 + ph * 57) % 360, 200, false, line);
                    break;
                }
                case HOLY: {
                    float k = 0.5f + 0.5f * (float) Math.sin(t * 3 + ph);
                    float r = dp(5) * s * (0.4f + 0.6f * k);
                    float cx = x + side * bw * 0.6f;
                    line.setColor(0xFFFFF6D8);
                    line.setAlpha((int) (120 + 135 * k));
                    line.setStrokeWidth(dp(1.2f));
                    canvas.drawLine(cx - r, y, cx + r, y, line);
                    canvas.drawLine(cx, y - r, cx, y + r, line);
                    break;
                }
                case DARK: {
                    float rise = (t * dp(12) + ph * dp(5)) % dp(26);
                    float f = rise / dp(26);
                    fill.setColor(0xFFA77BD8);
                    fill.setAlpha((int) (130 * (1 - f)));
                    canvas.drawCircle(x + side * bw * 0.3f + (float) Math.sin(t * 2 + ph) * dp(2), y - rise,
                            dp(2.5f + 3f * f) * s, fill);
                    break;
                }
                default:
                    break;
            }
        }
    }

    /** 위로 타오르는 물방울 모양 불꽃 (x, y가 밑동) */
    private void drawFlame(Canvas canvas, float x, float y, float hgt, float wd, int color) {
        flame.reset();
        flame.moveTo(x, y - hgt);
        flame.cubicTo(x + wd, y - hgt * 0.45f, x + wd * 0.8f, y, x, y);
        flame.cubicTo(x - wd * 0.8f, y, x - wd, y - hgt * 0.45f, x, y - hgt);
        fill.setColor(color);
        canvas.drawPath(flame, fill);
    }

    private int dp(float v) {
        return Math.round(v * density);
    }
}
