package com.textdungeon.activity_control;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.textdungeon.R;
import com.google.android.material.snackbar.Snackbar;
import com.textdungeon.model.Achievement;
import com.textdungeon.model.Job;
import com.textdungeon.model.Trait;
import com.textdungeon.system.ShopSystem;
import com.textdungeon.system.SoundManager;
import com.textdungeon.system.UserRecord;

import java.util.List;

public class ShopUnlockAdapter extends RecyclerView.Adapter<ShopUnlockAdapter.ViewHolder> {

    private List<?> itemList;
    private Context context;
    private ShopSystem shopSystem;
    private UserRecord record;
    private Runnable onSuccessCallback;

    public ShopUnlockAdapter(Context context, List<?> itemList, ShopSystem shopSystem, UserRecord record, Runnable onSuccessCallback) {
        this.context = context;
        this.itemList = itemList;
        this.shopSystem = shopSystem;
        this.record = record;
        this.onSuccessCallback = onSuccessCallback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.main_shop_character_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = itemList.get(position);

        String name = "";
        String desc = "";
        int price = 0;
        boolean isUnlocked = false;

        // 1. 직업(Job) 처리
        if (item instanceof Job) {
            Job job = (Job) item;
            name = job.name;
            desc = job.description;
            price = job.price;
            isUnlocked = record.isUnlockJob(job.name);

            holder.unlockImg.setVisibility(View.VISIBLE);
            int imgResId = context.getResources().getIdentifier(job.img, "drawable", context.getPackageName());
            if (imgResId != 0) holder.unlockImg.setImageResource(imgResId);

            holder.unlockButton.setOnClickListener(view -> {
                SoundManager.getInstance(context).playButtonSfx();
                String resultMsg = shopSystem.buyJob(job.name());
                showBar(view, resultMsg);
                if (resultMsg.contains("해금되었습니다")){
                    List<Achievement> unlocked = com.textdungeon.data.DataControlTower.getInstance(context).getAchievementManager().updateProgress("shop_unlock", 1, true);
                    if (context instanceof com.textdungeon.activity_control.BaseActivity) {
                        ((com.textdungeon.activity_control.BaseActivity) context).showAchievementNotification(unlocked);
                    }
                    onSuccessCallback.run();
                }
            });
        }
        // 2. 특성(Trait) 처리
        else if (item instanceof Trait) {
            Trait trait = (Trait) item;
            name = trait.displayName;
            desc = trait.description;
            price = trait.price;
            isUnlocked = record.isUnlockTrait(trait.name());

            holder.unlockImg.setVisibility(View.GONE);

            holder.unlockButton.setOnClickListener(view -> {
                SoundManager.getInstance(context).playButtonSfx();
                String resultMsg = shopSystem.buyTrait(trait.name());
                showBar(view, resultMsg);
                if (resultMsg.contains("해금되었습니다")){
                    List<Achievement> unlocked = com.textdungeon.data.DataControlTower.getInstance(context).getAchievementManager().updateProgress("shop_unlock", 1, true);
                    if (context instanceof com.textdungeon.activity_control.BaseActivity) {
                        ((com.textdungeon.activity_control.BaseActivity) context).showAchievementNotification(unlocked);
                    }
                    onSuccessCallback.run();
                }
            });
        }

        // 3. 공통 UI 업데이트
        holder.unlockName.setText(name);
        holder.unlockDesc.setText(desc);
        holder.unlockPrice.setText(String.valueOf(price));

        if (isUnlocked) {
            holder.unlockButton.setVisibility(View.INVISIBLE);
            holder.unlockStatus.setText("이미 해금 완료");
        } else {
            holder.unlockButton.setVisibility(View.VISIBLE);
            holder.unlockStatus.setText("해금하기");
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView unlockImg;
        TextView unlockName, unlockDesc, unlockPrice, unlockStatus;
        LinearLayout unlockButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            unlockImg = itemView.findViewById(R.id.char_image);
            unlockName = itemView.findViewById(R.id.char_name);
            unlockDesc = itemView.findViewById(R.id.char_desc);
            unlockPrice = itemView.findViewById(R.id.char_price);
            unlockButton = itemView.findViewById(R.id.btn_unlock);
            unlockStatus = itemView.findViewById(R.id.unlock_status);
        }
    }
    private void showBar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);

        snackbar.setBackgroundTint(Color.parseColor("#901418"));
        snackbar.setTextColor(Color.parseColor("#E5E2E1"));

        snackbar.show();
    }
}