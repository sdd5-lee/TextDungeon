package com.textdungeon.activity_control;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// 💡 Glide 클래스 임포트 추가
import com.bumptech.glide.Glide;
import com.example.textdungeon.R;
import com.textdungeon.data.CollectionData;

import java.util.List;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.ViewHolder> {

    private final Context context;
    private final List<CollectionData> collectionList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(CollectionData data);
    }

    public CollectionAdapter(Context context, List<CollectionData> collectionList, OnItemClickListener listener) {
        this.context = context;
        this.collectionList = collectionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_collection_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CollectionData data = collectionList.get(position);

        if (data.isDiscovered()) {
            Glide.with(context)
                    .load(data.getImgResId())
                    .into(holder.image);

            holder.image.setAlpha(1.0f);
        } else {
            Glide.with(context)
                    .load(android.R.drawable.ic_menu_help)
                    .into(holder.image);

            holder.image.setAlpha(0.3f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(data);
            }
        });
    }

    @Override
    public int getItemCount() { return collectionList.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.grid_image);
        }
    }
}