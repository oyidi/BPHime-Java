package com.windworkshop.bphime;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class DanmuListAdapter extends RecyclerView.Adapter<DanmuListAdapter.DanmuViewHolder> {
    ArrayList<DanmuItem> danmus;
    Context context;
    public DanmuListAdapter(Context context, ArrayList<DanmuItem> danmus) {
        this.danmus = danmus;
        this.context = context;
    }
    @NonNull
    @Override
    public DanmuViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(context).inflate(R.layout.danmu_item, viewGroup, false);

        return new DanmuViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DanmuViewHolder danmuViewHolder, int i) {
        danmuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        danmuViewHolder.itemView.setTag(i);
        DanmuItem danmu = danmus.get(i);
        if(danmu.cmd.equals("DANMU_MSG")){
            SpannableString snString = new SpannableString(danmu.userName+" : "+danmu.danmuText);
            snString.setSpan(new ForegroundColorSpan(Color.parseColor("#42b7e8")),0, danmu.userName.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            danmuViewHolder.danmuTextView.setText(snString);
        } else if(danmu.cmd.equals("SEND_GIFT")){
            SpannableString snString = new SpannableString(danmu.giftUserName + " 赠送 " + danmu.giftNum + " 个" + danmu.giftName);
            snString.setSpan(new ForegroundColorSpan(Color.parseColor("#42b7e8")),0, danmu.giftUserName.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            danmuViewHolder.danmuTextView.setText(snString);
        } else if(danmu.cmd.equals("WELCOME")){
        }

    }
    public void addDanmu(DanmuItem danmu) {
        danmus.add(danmu);
    }
    public void clear() {
        danmus.clear();
    }
    @Override
    public int getItemCount() {
        return danmus.size();
    }

    class DanmuViewHolder extends RecyclerView.ViewHolder {
        TextView danmuTextView;
        public DanmuViewHolder(View itemView) {
            super(itemView);
            danmuTextView = itemView.findViewById(R.id.danmu_item_danmutext);
        }
    }
}
