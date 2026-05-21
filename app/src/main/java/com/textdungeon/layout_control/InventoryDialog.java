package com.textdungeon.layout_control;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.textdungeon.R;
import com.textdungeon.data.DataControl;
import com.textdungeon.model.Item;
import com.textdungeon.player.Equipment;
import com.textdungeon.player.Player;

import java.util.Map;

public class InventoryDialog extends Dialog {
    Player player;
    DataControl<Item> itemLibrary;
    Runnable onUpdateCallback;

    public InventoryDialog(Context context, Player player, DataControl<Item> itemLibrary,Runnable onUpdateCallback) {
        super(context);
        this.player = player;
        this.itemLibrary = itemLibrary;
        this.onUpdateCallback = onUpdateCallback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inventory_dialog);

        // 배경 투명하게 처리 및 다이얼로그 가로 크기를 화면 너비의 95%로 확장
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
            int width = (int) (displayMetrics.widthPixels * 0.95);
            getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView btnClose = findViewById(R.id.btn_inventory_close);
        btnClose.setOnClickListener(v -> dismiss());

        itemList();
        updateEquipmentSlots();
        updateStatsUI();

        findViewById(R.id.slot_weapon).setOnClickListener(v -> {
            Item item = player.getEquipment().getWeapon();
            if (item != null) showEquippedItemDialog(item, "weapon", 0);
        });

        findViewById(R.id.slot_armor).setOnClickListener(v -> {
            Item item = player.getEquipment().getArmor();
            if (item != null) showEquippedItemDialog(item, "armor", 0);
        });

        findViewById(R.id.slot_atf_1).setOnClickListener(v -> {
            Item item = player.getEquipment().getArtifact()[0];
            if (item != null) showEquippedItemDialog(item, "artifact", 0);
        });

        findViewById(R.id.slot_atf_2).setOnClickListener(v -> {
            Item item = player.getEquipment().getArtifact()[1];
            if (item != null) showEquippedItemDialog(item, "artifact", 1);
        });
    }
    private void refreshAllUI() {
        GridLayout bagGrid = findViewById(R.id.bag_grid);
        if (bagGrid != null) {
            bagGrid.removeAllViews();
            itemList();
        }
        updateEquipmentSlots();
        updateStatsUI();
        if (onUpdateCallback != null) {
            onUpdateCallback.run();
        }
    }
    private void updateStatsUI() {
        TextView tvAtk = findViewById(R.id.tv_final_atk);
        TextView tvHp = findViewById(R.id.tv_final_hp);

        if (tvAtk != null && tvHp != null) {
            tvAtk.setText("⚔️ 공격력: " + player.getFinalAtk());
            tvHp.setText("❤️ 최대 체력: " + player.getMaxHp());
        }
    }

    private void itemList() {
        GridLayout bagGrid = findViewById(R.id.bag_grid);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        Map<String, Integer> itemMap = player.getInventory().getItemMap();
        for (String itemId : itemMap.keySet()) {
            View itemSlot = inflater.inflate(R.layout.item_slot, bagGrid, false);

            ImageView actionIcon = itemSlot.findViewById(R.id.action_icon);

            Item item = itemLibrary.spawn(itemId);
            String iconName = getIconNameByType(item.getType());

            int imgResId = getContext().getResources().getIdentifier(iconName, "drawable", getContext().getPackageName());
            if (imgResId != 0){
                actionIcon.setImageResource(imgResId);
            }

            itemSlot.setOnClickListener(v -> showItemActionDialog(item, itemMap.get(itemId)));

            bagGrid.addView(itemSlot);
        }

        for(int i = 0; i < 30-itemMap.size(); i++){

            View itemSlot = inflater.inflate(R.layout.item_slot, bagGrid, false);
            ImageView actionIcon = itemSlot.findViewById(R.id.action_icon);
            actionIcon.setImageDrawable(null);

            itemSlot.setOnClickListener(v -> { });

            bagGrid.addView(itemSlot);
        }
    }

    private void showItemActionDialog(Item item, int count) {
        Dialog itemDialog = new Dialog(getContext());
        itemDialog.setContentView(R.layout.item_action_dialog);

        if (itemDialog.getWindow() != null) {
            itemDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView icon = itemDialog.findViewById(R.id.dialog_item_icon);
        TextView name = itemDialog.findViewById(R.id.dialog_item_name);
        TextView desc = itemDialog.findViewById(R.id.dialog_item_desc);
        TextView btnClose = itemDialog.findViewById(R.id.btn_dialog_close);
        TextView btnDiscard = itemDialog.findViewById(R.id.btn_discard);
        TextView btnMainAction = itemDialog.findViewById(R.id.btn_main_action);

        name.setText(item.getName() + " :  "+ count +"개");
        desc.setText(item.getDescription() + "\n\n 공격력 : " + item.getAtk()+"\n체력 : " + item.getHp());
        getItemIcon(item, icon);

        if (item.getType().equalsIgnoreCase("consumables")) {
            btnMainAction.setText("사용하기");
            btnMainAction.setOnClickListener(v -> {
                player.consumablesItem(item);
                itemDialog.dismiss();
                refreshAllUI();
                Toast.makeText(getContext(), item.getName()+"를 사용하였습니다", Toast.LENGTH_SHORT).show();
            });
        } else {
            btnMainAction.setText("장착하기");
            btnMainAction.setOnClickListener(v -> {
                if (item.getType().equalsIgnoreCase("artifact")) {
                    showArtifactSlotDialog(item, itemDialog);
                } else {
                    player.equipItem(item);
                    itemDialog.dismiss();
                    refreshAllUI();
                }
                Toast.makeText(getContext(), item.getName()+"를 장착하였습니다", Toast.LENGTH_SHORT).show();
            });
        }

        btnDiscard.setOnClickListener(v -> {
            itemDialog.dismiss();
            refreshInventory();
        });

        btnClose.setOnClickListener(v -> itemDialog.dismiss());

        itemDialog.show();
    }
    private void showEquippedItemDialog(Item item, String type, int slotIndex) {
        Dialog itemDialog = new Dialog(getContext());
        itemDialog.setContentView(R.layout.item_action_dialog);

        if (itemDialog.getWindow() != null) {
            itemDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView icon = itemDialog.findViewById(R.id.dialog_item_icon);
        TextView name = itemDialog.findViewById(R.id.dialog_item_name);
        TextView desc = itemDialog.findViewById(R.id.dialog_item_desc);
        TextView btnClose = itemDialog.findViewById(R.id.btn_dialog_close);
        TextView btnDiscard = itemDialog.findViewById(R.id.btn_discard);
        TextView btnMainAction = itemDialog.findViewById(R.id.btn_main_action);

        name.setText(item.getName() + " (장착 중)");
        desc.setText(item.getDescription());
        getItemIcon(item, icon);

        btnDiscard.setVisibility(View.GONE);

        btnMainAction.setText("장착 해제");
        btnMainAction.setOnClickListener(v -> {
            if (player.getInventory().isFullItem()) {
                android.widget.Toast.makeText(getContext(), "가방이 꽉 차서 장착을 해제할 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            player.unequipItem(type, slotIndex);
            itemDialog.dismiss();
            refreshAllUI();
        });

        btnClose.setOnClickListener(v -> itemDialog.dismiss());
        itemDialog.show();
    }
    private void updateEquipmentSlots() {
        Equipment eq = player.getEquipment();

        updateSingleSlot(R.id.slot_weapon, R.id.slot_weapon_img, eq.getWeapon(), "ic_weapon");
        updateSingleSlot(R.id.slot_armor, R.id.slot_armor_img, eq.getArmor(), "ic_armor");
        updateSingleSlot(R.id.slot_atf_1, R.id.slot_atf_1_img, eq.getArtifact()[0], "ic_artifact");
        updateSingleSlot(R.id.slot_atf_2, R.id.slot_atf_2_img, eq.getArtifact()[1], "ic_artifact");
    }
    private void updateSingleSlot(int layoutId, int iconId, Item item, String defaultIconName) {
        FrameLayout slotLayout = findViewById(layoutId);
        ImageView slotIcon = slotLayout.findViewById(iconId);

        if (item != null) {
            getItemIcon(item, slotIcon);
            slotIcon.setAlpha(1.0f);
            slotLayout.setBackgroundColor(Color.parseColor("#44E9C176"));
        } else {
            int defaultResId = getContext().getResources().getIdentifier(defaultIconName, "drawable", getContext().getPackageName());
            if (defaultResId != 0) slotIcon.setImageResource(defaultResId);

            slotIcon.setAlpha(0.3f);
            slotLayout.setBackgroundResource(R.drawable.bg_event_panel);
        }
    }
    private void showArtifactSlotDialog(Item item, Dialog parentDialog) {
        String[] slots = {"장신구 슬롯 1", "장신구 슬롯 2"};

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("장착할 슬롯을 선택하세요")
                .setItems(slots, (dialog, which) -> {

                    player.equipArtifact(which, item);

                    parentDialog.dismiss();
                    refreshAllUI();
                })
                .setNegativeButton("취소", null)
                .show();
    }


    private void getItemIcon(Item item,ImageView icon){
        String imgName = getIconNameByType(item.getType());
        int imgResId = getContext().getResources().getIdentifier(imgName, "drawable", getContext().getPackageName());
        if (imgResId != 0) icon.setImageResource(imgResId);
    }
    private String getIconNameByType(String type) {
        switch (type.toLowerCase()) {
            case "weapon": return "ic_weapon";
            case "armor": return "ic_armor";
            case "consumables": return "ic_potion";
            case "artifact": return "ic_artifact";
            default: return "ic_default";
        }
    }
    private void refreshInventory() {
        GridLayout bagGrid = findViewById(R.id.bag_grid);
        if (bagGrid != null) {
            bagGrid.removeAllViews();
            itemList();
        }
    }


}