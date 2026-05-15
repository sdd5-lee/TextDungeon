package com.textdungeon.layout_control;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.textdungeon.data.DataControl;
import com.textdungeon.model.Item;
import com.textdungeon.player.Player;

import java.util.List;

/**
 * 상점 다이얼로그 — 이벤트 중 상점을 열 때 사용
 *
 * UI 구성:
 * - 상단: 플레이어 이름, 보유 골드, 뒤로가기
 * - 아이템 목록: 이름 + 가격(value) + 구매 버튼
 */
public class ShopDialog extends Dialog {

    private final Player player;
    private final DataControl<Item> itemManager;
    private final List<String> shopItemIds;

    private TextView goldText;
    private LinearLayout itemContainer;

    /**
     * @param context 호출한 Activity
     * @param player 플레이어
     * @param itemManager 아이템 데이터
     * @param shopItemIds 이 상점에서 판매할 아이템 ID 목록
     */
    public ShopDialog(Context context, Player player, DataControl<Item> itemManager, List<String> shopItemIds) {
        super(context);
        this.player = player;
        this.itemManager = itemManager;
        this.shopItemIds = shopItemIds;

        setupDialog();
    }

    private void setupDialog() {
        // 다이얼로그 크기 설정
        if (getWindow() != null) {
            getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // 메인 레이아웃
        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(0xFF2C2C2C);
        mainLayout.setPadding(40, 40, 40, 40);

        // ── 상단 헤더 ────────────────────────────
        mainLayout.addView(createHeader());

        // ── 구분선 ────────────────────────────
        mainLayout.addView(createDivider());

        // ── 아이템 목록 (스크롤) ────────────────────────────
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(400)
        ));

        itemContainer = new LinearLayout(getContext());
        itemContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(itemContainer);
        mainLayout.addView(scrollView);

        // ── 아이템 렌더링 ────────────────────────────
        renderItems();

        // ── 뒤로가기 버튼 ────────────────────────────
        mainLayout.addView(createCloseButton());

        setContentView(mainLayout);
    }

    // ─────────────────────────────────────────────────────────────
    // UI 컴포넌트 생성
    // ─────────────────────────────────────────────────────────────

    private LinearLayout createHeader() {
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);

        // 상점 제목
        TextView title = new TextView(getContext());
        title.setText("🏪 상점");
        title.setTextSize(24);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);
        header.addView(title);

        // 플레이어 정보
        TextView playerInfo = new TextView(getContext());
        playerInfo.setText(player.getName() + " (Lv." + player.getLevel() + ")");
        playerInfo.setTextSize(16);
        playerInfo.setTextColor(0xFFCCCCCC);
        playerInfo.setGravity(Gravity.CENTER);
        playerInfo.setPadding(0, 16, 0, 0);
        header.addView(playerInfo);

        // 골드 표시
        goldText = new TextView(getContext());
        updateGoldDisplay();
        goldText.setTextSize(18);
        goldText.setTextColor(0xFFFFD700);
        goldText.setGravity(Gravity.CENTER);
        goldText.setPadding(0, 8, 0, 0);
        header.addView(goldText);

        return header;
    }

    private LinearLayout createDivider() {
        LinearLayout divider = new LinearLayout(getContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(2)
        ));
        divider.setBackgroundColor(0xFF555555);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) divider.getLayoutParams();
        params.setMargins(0, 32, 0, 32);
        divider.setLayoutParams(params);
        return divider;
    }

    private void renderItems() {
        itemContainer.removeAllViews();

        if (shopItemIds == null || shopItemIds.isEmpty()) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("판매할 상품이 없습니다.");
            emptyText.setTextColor(0xFF888888);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 40, 0, 40);
            itemContainer.addView(emptyText);
            return;
        }

        for (String itemId : shopItemIds) {
            Item item = itemManager.spawn(itemId);
            if (item != null) {
                itemContainer.addView(createItemRow(item));
            }
        }
    }

    private LinearLayout createItemRow(Item item) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(20, 24, 20, 24);
        row.setBackgroundColor(0xFF3C3C3C);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, 16);
        row.setLayoutParams(rowParams);

        // 아이템 이름 + 가격
        LinearLayout infoLayout = new LinearLayout(getContext());
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        ));

        TextView nameText = new TextView(getContext());
        nameText.setText(item.getName());
        nameText.setTextSize(16);
        nameText.setTextColor(0xFFFFFFFF);
        infoLayout.addView(nameText);

        TextView priceText = new TextView(getContext());
        int price = item.getValue(); // 임시로 value를 가격으로 사용
        priceText.setText("💰 " + price + " G");
        priceText.setTextSize(14);
        priceText.setTextColor(0xFFFFD700);
        priceText.setPadding(0, 4, 0, 0);
        infoLayout.addView(priceText);

        row.addView(infoLayout);

        // 구매 버튼
        Button buyButton = new Button(getContext());
        buyButton.setText("구매");
        buyButton.setTextColor(0xFFFFFFFF);
        buyButton.setBackgroundColor(0xFF4CAF50);
        buyButton.setPadding(32, 16, 32, 16);
        buyButton.setOnClickListener(v -> onBuyItem(item, price));
        row.addView(buyButton);

        return row;
    }

    private Button createCloseButton() {
        Button closeButton = new Button(getContext());
        closeButton.setText("뒤로가기");
        closeButton.setTextColor(0xFFFFFFFF);
        closeButton.setBackgroundColor(0xFF666666);
        closeButton.setPadding(0, 32, 0, 32);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 32, 0, 0);
        closeButton.setLayoutParams(params);

        closeButton.setOnClickListener(v -> dismiss());
        return closeButton;
    }

    // ─────────────────────────────────────────────────────────────
    // 구매 로직
    // ─────────────────────────────────────────────────────────────

    private void onBuyItem(Item item, int price) {
        // 골드 부족
        if (player.getStat().getGold() < price) {
            Toast.makeText(getContext(), "골드가 부족합니다!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 인벤토리 풀
        if (player.getInventory().isFullItem()) {
            Toast.makeText(getContext(), "인벤토리가 가득 찼습니다!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 구매 처리
        player.getStat().setGold(player.getStat().getGold() - price);
        player.getInventory().addItem(item);

        updateGoldDisplay();
        Toast.makeText(getContext(),
                item.getName() + "을(를) 구매했습니다!",
                Toast.LENGTH_SHORT).show();
    }

    private void updateGoldDisplay() {
        goldText.setText("💰 보유 골드: " + player.getStat().getGold() + " G");
    }

    // ─────────────────────────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────────────────────────

    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}