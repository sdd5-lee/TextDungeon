package com.textdungeon.activity_control;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.textdungeon.model.Achievement;

import java.util.List;

public class AchieveAdapter extends BaseAdapter {

    private final Context context;
    private final List<Achievement> achieveList;

    public AchieveAdapter(Context context, List<Achievement> achieveList) {
        this.context = context;
        this.achieveList = achieveList;
    }

    @Override
    public int getCount() { return achieveList.size(); }

    @Override
    public Object getItem(int position) { return achieveList.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.row_achievement, parent, false);
        }

        Achievement ach = achieveList.get(position);
        ImageView icon = convertView.findViewById(R.id.ach_icon);
        TextView name = convertView.findViewById(R.id.ach_name);
        TextView desc = convertView.findViewById(R.id.ach_desc);
        TextView progress = convertView.findViewById(R.id.ach_progress_text);
        TextView date = convertView.findViewById(R.id.ach_date);

        progress.setText("진행도: " + ach.getCurrentValue() + " / " + ach.getTargetValue());

        if (ach.isUnlocked()) {
            icon.setImageResource(android.R.drawable.btn_star_big_on);
            icon.setAlpha(1.0f);
            name.setText(ach.getName());
            name.setTextColor(Color.parseColor("#E9C176"));
            desc.setText(ach.getDescription());
            date.setVisibility(View.VISIBLE);
            date.setText("달성일: " + ach.getAchievedDate());
        } else {
            icon.setImageResource(android.R.drawable.ic_secure);
            icon.setAlpha(0.4f);
            name.setText("???");
            name.setTextColor(Color.parseColor("#A0A0A0"));
            desc.setText("아직 달성하지 못한 업적입니다.");
            date.setVisibility(View.GONE);
        }
        return convertView;
    }
}