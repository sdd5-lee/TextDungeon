package com.textdungeon.dialog_control;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ComposeShader;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
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
 * 가지마다 두 계열이 위로 자라고 (계열 순서는 MagicSchool), 계열 첫 마법 위에서 다시 두 갈래로 갈라진다.
 * 계열을 열면 그 가지가 계열 질감으로 바뀌고(화염은 흐르는 용암, 신성은 흰 자작나무),
 * 배운 마법까지 이어진 토막일수록 밝다. 마법을 누르면 크게 보이고 listener로 알린다.
 */
public class MagicTreeView extends FrameLayout {

    public interface Listener {
        void onMagicSelected(Magic magic);
    }

    /** 기초 마법에서 몇 단계 위인지에 따른 높이 (0 = 기초 마법, 1 = 가장 위) */
    private static final float[] TIER_Y = {0f, 0.40f, 0.66f, 0.93f};
    /**
     * 계열 첫 마법 위에서 두 갈래로 갈라진 마법은 계열 칸 안에서 좌우로 비키고(칸 너비 비율),
     * 옆 칸 마법과 부딪히지 않게 왼쪽 갈래는 낮게, 오른쪽 갈래는 높게 엇갈린다(높이 비율)
     */
    private static final float FORK_X = 0.27f;
    private static final float FORK_LIFT = 0.07f;
    /** 줄기가 세 가지로, 가지가 두 계열로 갈라지는 높이 */
    private static final float TRUNK_FORK = 0.17f;
    private static final float BOUGH_FORK = 0.29f;
    private static final int NODE_DP = 36;
    private static final int ROOT_DP = 46;
    private static final float SELECTED_SCALE = 1.35f;
    /** 가지 끝 위의 계열 이름 자리, 기초 마법 아래 뿌리 자리, 가지 끝 잔가지 길이(마법 가운데부터) */
    private static final int LABEL_DP = 22;
    private static final int ROOTS_DP = 26;
    private static final int TWIG_DP = 30;
    /** 양 끝 계열의 갈라진 마법이 눌려 커져도 잘리지 않게 좌우를 띄우는 폭 */
    private static final int INSET_DP = 12;
    /** 뿌리가 땅속으로 사라지는 높이 */
    private static final int GROUND_DP = 28;
    /** 굵기가 줄어드는 토막 하나를 몇 조각으로 나눠 그릴지 */
    private static final int PIECES = 8;

    private static final int STROKE_CAN_LEARN = 0xFF8FD18A;
    private static final int STROKE_LOCKED = 0xFF4A4640;
    private static final int EDGE = 0xFF0A0705;
    private static final int BACK_DARK = 0xFF0D0C0B;

    /** 빛이 드는 쪽 (왼쪽 위) */
    private static final float LIGHT_X = -0.6f;
    private static final float LIGHT_Y = -0.8f;
    /** 둥근 가지 음영: 빛이 드는 가장자리 → 밝은 결 → 가운데 → 그늘 → 그늘진 가장자리 */
    private static final float[] SHADE_AT = {0f, 0.2f, 0.45f, 0.78f, 1f};
    private static final int[] SHADE = {0x4D000000, 0x14FFF2DC, 0x00000000, 0x66000000, 0xC8000000};
    /** 얼음은 빛을 반사해 밝은 결이 또렷하다 */
    private static final int[] SHADE_ICE = {0x40000000, 0x33FFFFFF, 0x00000000, 0x55000000, 0xB0000000};
    /** 용암은 스스로 빛나서 그늘이 옅다 */
    private static final int[] SHADE_LAVA = {0x33000000, 0x1FFFE0A0, 0x00000000, 0x33000000, 0x73000000};
    /** 가지 둘레 빛을 몇 겹(굵기 dp)으로 번지게 할지. 겹칠수록 가지 가까이가 진하다 */
    private static final float[] HALO_DP = {3, 7, 12, 18};

    /** 계열마다 가지 질감과 질감 한 장(256px)이 화면에서 차지하는 길이(dp). 잠긴 계열은 BASIC 나무껍질 */
    private static final Map<MagicSchool, int[]> TEXTURES = new EnumMap<>(MagicSchool.class);

    static {
        TEXTURES.put(MagicSchool.BASIC, new int[]{R.drawable.tree_bark, 80});
        TEXTURES.put(MagicSchool.FIRE, new int[]{R.drawable.tree_lava, 70});
        TEXTURES.put(MagicSchool.FROST, new int[]{R.drawable.tree_ice, 90});
        TEXTURES.put(MagicSchool.NATURE, new int[]{R.drawable.tree_moss, 100});
        TEXTURES.put(MagicSchool.WIND, new int[]{R.drawable.tree_wind, 100});
        TEXTURES.put(MagicSchool.HOLY, new int[]{R.drawable.tree_birch, 90});
        TEXTURES.put(MagicSchool.DARK, new int[]{R.drawable.tree_obsidian, 75});
    }

    /** 열린 계열 가지 둘레에 번지는 빛 */
    private static final Map<MagicSchool, Integer> GLOWS = new EnumMap<>(MagicSchool.class);

    static {
        GLOWS.put(MagicSchool.FIRE, 0xFFFF6A1A);
        GLOWS.put(MagicSchool.NATURE, 0xFF8BC34A);
        GLOWS.put(MagicSchool.FROST, 0xFFBFE6FF);
        GLOWS.put(MagicSchool.WIND, 0xFF5FD3C0);
        GLOWS.put(MagicSchool.HOLY, 0xFFFFF3C0);
        GLOWS.put(MagicSchool.DARK, 0xFF8A5CC8);
    }

    /** 잠긴 계열은 빛 바랜 마른 나무, 열었지만 이어진 마법을 아직 못 배운 토막은 어둡게 */
    private static final ColorFilter LOCKED = tone(0.25f, 0.42f);
    private static final ColorFilter DORMANT = tone(0.85f, 0.68f);

    private static class Node {
        final Magic magic;
        Node parent;
        int depth;
        /** 갈래: -1 왼쪽(낮게), 1 오른쪽(높게), 0 가운데 */
        int side;
        boolean hasChild;
        ShapeableImageView view;
        TextView label;
        float x, y;

        Node(Magic magic) {
            this.magic = magic;
        }
    }

    /** 나무 한 토막. 끝의 마법(tip)이 속한 계열 질감으로 칠한다. 줄기·뿌리·큰 가지는 BASIC */
    private static class Branch {
        final MagicSchool school;
        final Node tip;
        /** 토막 전체 (빛·그림자·바람결은 이음매 없이 한 번에) */
        final Path full;
        final float avgWidth;
        final Path[] pieces = new Path[PIECES];
        final float[] widths = new float[PIECES];
        /** 조각마다 질감에 음영을 겹친 칠: 계열 질감, 잠겼을 때의 나무껍질 */
        final Shader[] skin = new Shader[PIECES];
        final Shader[] bark = new Shader[PIECES];
        /** 조각 가운데의 진행 방향(단위 벡터 x, y). 용암은 이쪽으로 흐른다 */
        final float[] dirs = new float[PIECES * 2];
        /** 불티·빛 알갱이 자리: x, y, 위상(0~1), 옆(±1), 그 자리 굵기 */
        final List<float[]> marks = new ArrayList<>();

        Branch(MagicSchool school, Node tip, Path full, float avgWidth) {
            this.school = school;
            this.tip = tip;
            this.full = full;
            this.avgWidth = avgWidth;
        }
    }

    private final List<Node> nodes = new ArrayList<>();
    private final List<Branch> branches = new ArrayList<>();
    private final Map<MagicSchool, Bitmap> textures = new EnumMap<>(MagicSchool.class);
    private final Paint skinPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint aura = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint();
    private final Paint groundPaint = new Paint();
    private final Matrix flowMatrix = new Matrix();
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
        for (Paint p : new Paint[]{skinPaint, edge, aura}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
        }
        edge.setColor(EDGE);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        for (Map.Entry<MagicSchool, int[]> e : TEXTURES.entrySet()) {
            textures.put(e.getKey(), BitmapFactory.decodeResource(getResources(), e.getValue()[0]));
        }
    }

    private static ColorFilter tone(float saturation, float brightness) {
        ColorMatrix m = new ColorMatrix();
        m.setSaturation(saturation);
        ColorMatrix scale = new ColorMatrix();
        scale.setScale(brightness, brightness, brightness, 1f);
        m.postConcat(scale);
        return new ColorMatrixColorFilter(m);
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
            if (n.parent != null) n.parent.hasChild = true;
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

        // 한 마법에서 둘로 갈라지면 json에 먼저 나온 쪽이 왼쪽 갈래. 갈라지지 않으면 앞 마법의 갈래를 따른다
        for (int d = 2; d < TIER_Y.length; d++) {
            for (Node n : nodes) {
                if (n.depth != d || n.parent == null) continue;
                List<Node> siblings = new ArrayList<>();
                for (Node o : nodes) {
                    if (o.parent == n.parent) siblings.add(o);
                }
                n.side = siblings.size() < 2 ? n.parent.side : siblings.indexOf(n) == 0 ? -1 : 1;
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
        float inset = dp(INSET_DP);
        float colW = (w - 2 * inset) / columns.size();

        for (Node n : nodes) {
            if (n.depth == 0) {
                n.x = rootX;
                n.y = rootY;
            } else {
                n.x = inset + colW * (columns.indexOf(n.magic.getSchool()) + 0.5f + n.side * FORK_X);
                n.y = rootY - span * (TIER_Y[n.depth] + n.side * FORK_LIFT);
            }
        }
        // 배경은 뿌리 자리에서 번지는 빛, 아래쪽은 뿌리가 묻히는 땅
        glowPaint.setShader(new RadialGradient(rootX, rootY, Math.max(w, h) * 0.8f,
                0xFF1E1912, BACK_DARK, Shader.TileMode.CLAMP));
        groundPaint.setShader(new LinearGradient(0, h - dp(GROUND_DP), 0, h,
                BACK_DARK & 0x00FFFFFF, BACK_DARK & 0xF2FFFFFF, Shader.TileMode.CLAMP));

        for (Node n : nodes) {
            int size = n.view.getMeasuredWidth();
            int l = Math.round(n.x - size / 2f);
            int t = Math.round(n.y - size / 2f);
            n.view.layout(l, t, l + size, t + size);
        }
        for (Node n : nodes) {
            if (n.label != null) placeLabel(n);
        }
        buildTree(w, h, rootX, rootY, span, inset, colW, columns);
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
    private void buildTree(int w, int h, float rootX, float rootY, float span, float inset, float colW, List<MagicSchool> columns) {
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
            boughX[g] = inset + colW * ((2 * g + last) / 2f + 0.5f);
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

        // 갈래마다 끝 마법 위에 잔가지 (갈라진 갈래는 바깥쪽으로 살짝 기운다)
        for (Node tip : nodes) {
            if (tip.depth == 0 || tip.hasChild) continue;
            float lean = dp(4) * (tip.side < 0 ? -1 : 1);
            Path p = new Path();
            p.moveTo(tip.x, tip.y);
            p.quadTo(tip.x - lean / 2f, tip.y - dp(TWIG_DP) * 0.6f, tip.x + lean, tip.y - dp(TWIG_DP));
            addBranch(p, dp(6.5f), dp(2), tip.magic.getSchool(), tip, rnd);
        }
    }

    /** 굵기가 w0에서 w1로 줄어드는 토막을 조각으로 나눠 조각마다 질감을 입히고, 알갱이 자리를 12dp마다 잡는다 */
    private void addBranch(Path path, float w0, float w1, MagicSchool school, Node tip, Random rnd) {
        Branch b = new Branch(school, tip, path, (w0 + w1) / 2f);
        PathMeasure pm = new PathMeasure(path, false);
        float len = pm.getLength();
        float[] pos = new float[2];
        float[] tan = new float[2];
        for (int i = 0; i < PIECES; i++) {
            Path piece = new Path();
            pm.getSegment(len * i / PIECES, Math.min(len, len * (i + 1) / PIECES + dp(1)), piece, true);
            b.pieces[i] = piece;
            float width = w0 + (w1 - w0) * (i + 0.5f) / PIECES;
            b.widths[i] = width;
            float mid = len * (i + 0.5f) / PIECES;
            pm.getPosTan(mid, pos, tan);
            b.dirs[2 * i] = tan[0];
            b.dirs[2 * i + 1] = tan[1];
            b.skin[i] = skin(school, pos, tan, mid, width);
            b.bark[i] = school == MagicSchool.BASIC ? b.skin[i] : skin(MagicSchool.BASIC, pos, tan, mid, width);
        }
        float step = dp(12);
        for (float d = step * 0.5f; d < len; d += step) {
            pm.getPosTan(d, pos, tan);
            b.marks.add(new float[]{pos[0], pos[1], rnd.nextFloat(), rnd.nextBoolean() ? 1 : -1,
                    w0 + (w1 - w0) * d / len});
        }
        branches.add(b);
    }

    /**
     * 질감의 세로결을 가지 방향으로 눕히고(이어진 조각끼리 결이 이어지게 토막 길이 dist만큼 민다),
     * 왼쪽 위에서 빛이 드는 둥근 음영을 겹친다.
     */
    private Shader skin(MagicSchool school, float[] pos, float[] tan, float dist, float width) {
        Bitmap tex = textures.get(school);
        float s = dp(TEXTURES.get(school)[1]) / (float) tex.getWidth();
        Matrix m = new Matrix();
        m.setScale(s, s);
        m.postTranslate(0, -dist);
        m.postRotate((float) Math.toDegrees(Math.atan2(tan[1], tan[0])) - 90f);
        m.postTranslate(pos[0], pos[1]);
        BitmapShader texture = new BitmapShader(tex, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        texture.setLocalMatrix(m);

        float nx = -tan[1];
        float ny = tan[0];
        if (nx * LIGHT_X + ny * LIGHT_Y < 0) {
            nx = -nx;
            ny = -ny;
        }
        float half = width / 2f;
        LinearGradient shade = new LinearGradient(pos[0] + nx * half, pos[1] + ny * half,
                pos[0] - nx * half, pos[1] - ny * half,
                school == MagicSchool.FIRE ? SHADE_LAVA : school == MagicSchool.FROST ? SHADE_ICE : SHADE,
                SHADE_AT, Shader.TileMode.CLAMP);
        return new ComposeShader(texture, shade, PorterDuff.Mode.SRC_OVER);
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
        // 용암은 숨 쉬듯 밝아졌다 어두워진다
        float heat = (float) Math.sin(t * 2.2f);
        boolean animate = false;

        // 열린 계열은 가지 둘레로 빛이 옅게 번진다 (넓은 겹부터 깔아 가장자리가 부드럽게)
        for (Branch b : branches) {
            if (b.school == MagicSchool.BASIC || !isOpen(b)) continue;
            float k = b.school == MagicSchool.FIRE ? 0.75f + 0.25f * heat : 1f;
            aura.setColor(GLOWS.get(b.school));
            aura.setAlpha((int) ((isLit(b) ? 16 : 6) * k));
            for (int i = HALO_DP.length - 1; i >= 0; i--) {
                aura.setStrokeWidth(b.avgWidth + dp(HALO_DP[i]));
                canvas.drawPath(b.full, aura);
            }
        }
        // 오른쪽 아래로 지는 그림자
        int save = canvas.save();
        canvas.translate(dp(3), dp(4));
        aura.setColor(Color.BLACK);
        aura.setAlpha(80);
        for (Branch b : branches) {
            aura.setStrokeWidth(b.avgWidth + dp(2));
            canvas.drawPath(b.full, aura);
        }
        canvas.restoreToCount(save);
        // 테두리를 모두 깐 뒤 질감을 올려 갈라지는 곳이 한 덩어리로 이어지게
        for (Branch b : branches) {
            for (int i = 0; i < PIECES; i++) {
                edge.setStrokeWidth(b.widths[i] + dp(1.6f));
                canvas.drawPath(b.pieces[i], edge);
            }
        }
        ColorFilter burning = tone(1f, 1f + 0.14f * heat);
        for (Branch b : branches) {
            boolean open = isOpen(b);
            boolean lit = isLit(b);
            boolean lava = open && b.school == MagicSchool.FIRE;
            skinPaint.setColorFilter(!open ? LOCKED : !lit ? DORMANT : lava ? burning : null);
            Shader[] skins = open ? b.skin : b.bark;
            // 용암은 가지를 따라 마법 쪽으로 천천히 흐른다 (질감 한 장 길이마다 처음으로)
            float flow = lava ? (t * dp(lit ? 7 : 3)) % dp(TEXTURES.get(MagicSchool.FIRE)[1]) : 0f;
            for (int i = 0; i < PIECES; i++) {
                if (lava) {
                    flowMatrix.setTranslate(b.dirs[2 * i] * flow, b.dirs[2 * i + 1] * flow);
                    skins[i].setLocalMatrix(flowMatrix);
                }
                skinPaint.setShader(skins[i]);
                skinPaint.setStrokeWidth(b.widths[i]);
                canvas.drawPath(b.pieces[i], skinPaint);
            }
            animate |= lava;
        }
        skinPaint.setShader(null);
        // 바람결, 불티·빛 알갱이
        for (Branch b : branches) {
            if (b.school == MagicSchool.BASIC || !isOpen(b)) continue;
            boolean lit = isLit(b);
            if (b.school == MagicSchool.WIND) drawGusts(canvas, b, lit, t);
            drawMotes(canvas, b, lit, t);
            animate = true;
        }
        // 뿌리는 땅속으로 사라진다
        canvas.drawRect(0, getHeight() - dp(GROUND_DP), getWidth(), getHeight(), groundPaint);
        if (animate) postInvalidateOnAnimation();
    }

    /** 가지 양옆을 따라 위로 스치는 가는 바람결 */
    private void drawGusts(Canvas canvas, Branch b, boolean lit, float t) {
        float dash = dp(12);
        float gap = dp(34);
        float period = dash + gap;
        line.setColor(0xFFD8F7EF);
        line.setAlpha(lit ? 110 : 45);
        line.setStrokeWidth(dp(1.1f));
        for (int side = -1; side <= 1; side += 2) {
            float phase = period - (t * dp(60) + (side > 0 ? period / 2f : 0f)) % period;
            line.setPathEffect(new DashPathEffect(new float[]{dash, gap}, phase));
            int save = canvas.save();
            canvas.translate(side * (b.avgWidth / 2f + dp(4)), 0);
            canvas.drawPath(b.full, line);
            canvas.restoreToCount(save);
        }
        line.setPathEffect(null);
    }

    /** 배운 토막은 모든 자리에서, 열기만 한 토막은 셋에 한 자리에서 */
    private void drawMotes(Canvas canvas, Branch b, boolean lit, float t) {
        for (int i = 0; i < b.marks.size(); i++) {
            if (!lit && i % 3 != 0) continue;
            float[] m = b.marks.get(i);
            float x = m[0], y = m[1], ph = m[2], side = m[3], bw = m[4];
            float wave = (float) Math.sin(t * 1.7f + ph * 6.283f);
            switch (b.school) {
                case FIRE: { // 용암 틈에서 튀어 오르는 불티
                    float k = (t * 0.45f + ph) % 1f;
                    float fade = (1 - k) * (float) Math.sqrt(1 - k);
                    spark(canvas, x + side * bw * 0.3f + wave * dp(4) * k, y - k * dp(36),
                            dp(1.1f) * (1 - 0.5f * k), 0xFFFFD27A, 0xFFFF6A1A, fade);
                    break;
                }
                case HOLY: { // 천천히 떠오르는 빛 알갱이
                    if (lit && i % 2 == 1) break;
                    float k = (t * 0.25f + ph) % 1f;
                    spark(canvas, x + side * (bw * 0.5f + dp(3)) + wave * dp(2), y - k * dp(26),
                            dp(0.9f), 0xFFFFF8E0, 0xFFFFE9A0, 0.8f * (float) Math.sin(Math.PI * k));
                    break;
                }
                case DARK: { // 피어오르는 그림자 연기
                    float k = (t * 0.3f + ph) % 1f;
                    fill.setColor(0xFF6A4596);
                    fill.setAlpha((int) (60 * Math.sin(Math.PI * k)));
                    canvas.drawCircle(x + side * bw * 0.25f + wave * dp(3), y - k * dp(24), dp(2.5f + 5 * k), fill);
                    break;
                }
                case FROST: { // 얼음 결에 잠깐 맺혔다 사라지는 빛
                    float f = (float) Math.pow(Math.max(0, Math.sin(t * 1.1f + ph * 18.85f)), 16);
                    if (f < 0.02f) break;
                    float gx = x + side * bw * 0.22f;
                    float r = dp(4.5f) * f;
                    line.setColor(0xFFF4FBFF);
                    line.setAlpha((int) (230 * f));
                    line.setStrokeWidth(dp(0.8f));
                    canvas.drawLine(gx - r, y, gx + r, y, line);
                    canvas.drawLine(gx, y - r, gx, y + r, line);
                    spark(canvas, gx, y, dp(1f), 0xFFFFFFFF, 0xFFBFE6FF, f);
                    break;
                }
                case NATURE: { // 가지 둘레를 떠도는 포자
                    if (i % 2 == 1) break;
                    float k = (t * 0.18f + ph) % 1f;
                    spark(canvas, x + side * (bw * 0.5f + dp(5)) + wave * dp(5), y - k * dp(16),
                            dp(1.1f), 0xFFE2F59A, 0xFF9BCB4A, (float) Math.sin(Math.PI * k));
                    break;
                }
                default:
                    break;
            }
        }
    }

    /** 작은 빛 알갱이: 둘레에 옅게 번지는 빛과 밝은 가운데 */
    private void spark(Canvas canvas, float x, float y, float r, int core, int glow, float alpha) {
        fill.setColor(glow);
        fill.setAlpha((int) (90 * alpha));
        canvas.drawCircle(x, y, r * 2.8f, fill);
        fill.setColor(core);
        fill.setAlpha((int) (240 * alpha));
        canvas.drawCircle(x, y, r, fill);
    }

    private int dp(float v) {
        return Math.round(v * density);
    }
}
