package com.textdungeon.layout_control;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.textdungeon.R;
import com.textdungeon.model.Job;
import com.textdungeon.system.GameSave;
import com.textdungeon.system.ShopSystem;
import com.textdungeon.system.UserRecord;

import java.util.List;

public class ShopJobAdapter extends RecyclerView.Adapter<ShopJobAdapter.JobViewHolder> {

    private List<Job> jobList;
    private Context context;
    private ShopSystem shopSystem;
    private UserRecord record;
    private Runnable onSuccessCallback;

    public ShopJobAdapter(Context context, List<Job> jobList, ShopSystem shopSystem, UserRecord record, Runnable onSuccessCallback) {
        this.context = context;
        this.jobList = jobList;
        this.shopSystem = shopSystem;
        this.record = record;
        this.onSuccessCallback = onSuccessCallback;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.shop_character_card, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = jobList.get(position);

        int imgResId = context.getResources().getIdentifier(job.img, "drawable", context.getPackageName());
        if (imgResId != 0){
            holder.unlockImg.setImageResource(imgResId);
        }

        holder.unlockName.setText(job.name);
        holder.unlockDesc.setText(job.description);
        holder.unlockPrice.setText(String.valueOf(job.price));

        if (record.isUnlockJob(job.name)){
            holder.unlockButton.setVisibility(View.INVISIBLE);
            holder.unlockStatus.setText("이미 해금된 직업입니다.");
        } else {
            holder.unlockButton.setVisibility(View.VISIBLE);
            holder.unlockStatus.setText("해금하기");
            holder.unlockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String resultMsg = shopSystem.buyJob(job.name());
                    Toast.makeText(context, resultMsg, Toast.LENGTH_SHORT).show();
                    if (resultMsg.contains("해금되었습니다")) {
                        onSuccessCallback.run();
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return jobList.size();
    }

    public static class JobViewHolder extends RecyclerView.ViewHolder {
        ImageView unlockImg;
        TextView unlockName, unlockDesc, unlockPrice, unlockStatus;
        LinearLayout unlockButton;

        public JobViewHolder(@NonNull View itemView) {
            super(itemView);
            unlockImg = itemView.findViewById(R.id.char_image);
            unlockName = itemView.findViewById(R.id.char_name);
            unlockDesc = itemView.findViewById(R.id.char_desc);
            unlockPrice = itemView.findViewById(R.id.char_price);
            unlockButton = itemView.findViewById(R.id.btn_unlock);
            unlockStatus = itemView.findViewById(R.id.unlock_status);
        }
    }
}