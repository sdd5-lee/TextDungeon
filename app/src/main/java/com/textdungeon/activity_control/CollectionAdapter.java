package com.textdungeon.activity_control;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.textdungeon.R;
import com.textdungeon.data.CollectionData;

import java.util.List;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.ViewHolder> {

    private final Context context;
    private final List<CollectionData> collectionList;

    public CollectionAdapter(Context context, List<CollectionData> collectionList) {
        this.context = context;
        this.collectionList = collectionList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_collection_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CollectionData data = collectionList.get(position);

        if (data.isDiscovered()) {
            holder.name.setText(data.getName());
            holder.desc.setText(data.getDesc());
            holder.status.setText("기록됨");
            holder.status.setTextColor(Color.parseColor("#E9C176"));

            int resId = context.getResources().getIdentifier(data.getImgId(), "drawable", context.getPackageName());
            if (resId != 0) holder.image.setImageResource(resId);
            else holder.image.setImageResource(R.drawable.mon_goblin);
        } else {
            holder.name.setText("???");
            holder.desc.setText("아직 발견하지 못한 대상입니다.");
            holder.status.setText("미발견");
            holder.status.setTextColor(Color.parseColor("#A0A0A0"));
            holder.image.setImageResource(android.R.drawable.ic_menu_help);
        }
    }

    @Override
    public int getItemCount() { return collectionList.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, desc, status;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.char_image);
            name = itemView.findViewById(R.id.char_name);
            desc = itemView.findViewById(R.id.char_desc);
            status = itemView.findViewById(R.id.unlock_status);
        }
    }
}