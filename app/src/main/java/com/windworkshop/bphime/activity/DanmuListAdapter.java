package com.windworkshop.bphime.activity;

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

import com.windworkshop.bphime.object.DanmuItem;
import com.windworkshop.bphime.R;

import java.util.ArrayList;

public class DanmuListAdapter extends RecyclerView.Adapter<DanmuListAdapter.DanmuViewHolder> {
    ArrayList<DanmuItem> danmus;
    boolean isShowSendingTime = false;
    boolean isShowMemberIn = false;
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
    public void setShowSendingTime(boolean isShowSendingTime) {
        this.isShowSendingTime = isShowSendingTime;
    }
    public void setShowMemberIn(boolean isShowMemberIn) {
        this.isShowMemberIn = isShowMemberIn;
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
        SpannableString snString;
        if(danmu.getCmd().equals("DANMU_MSG")){
            if(isShowSendingTime) {
                String receiveTimeString = "["+danmu.getReceiveTimeString() + "] ";
                snString = new SpannableString( receiveTimeString + danmu.getUserName()+" : "+danmu.getDanmuText());
                snString.setSpan(new ForegroundColorSpan(danmuViewHolder.itemView.getContext().getResources().getColor(R.color.showTime)),0, receiveTimeString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                snString.setSpan(new ForegroundColorSpan(danmuViewHolder.itemView.getContext().getResources().getColor(R.color.danmuUsername)),receiveTimeString.length(), receiveTimeString.length() + danmu.getUserName().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                snString = new SpannableString(danmu.getUserName()+" : "+danmu.getDanmuText());
                snString.setSpan(new ForegroundColorSpan(Color.parseColor("#42b7e8")),0, danmu.getUserName().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            danmuViewHolder.danmuTextView.setText(snString);
        } else if(danmu.getCmd().equals("SEND_GIFT")){
            if(isShowSendingTime) {
                String receiveTimeString = "[" + danmu.getReceiveTimeString() + "] ";
                snString = new SpannableString(receiveTimeString + danmu.getGiftUserName() + " 赠送 " + danmu.getGiftNum() + " 个" + danmu.getGiftName());
                snString.setSpan(new ForegroundColorSpan(danmuViewHolder.itemView.getContext().getResources().getColor(R.color.showTime)),0, receiveTimeString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                snString.setSpan(new ForegroundColorSpan(danmuViewHolder.itemView.getContext().getResources().getColor(R.color.danmuUsername)),receiveTimeString.length(), receiveTimeString.length() + danmu.getGiftUserName().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                snString = new SpannableString(danmu.getGiftUserName() + " 赠送 " + danmu.getGiftNum() + " 个" + danmu.getGiftName());
                snString.setSpan(new ForegroundColorSpan(danmuViewHolder.itemView.getContext().getResources().getColor(R.color.danmuUsername)),0, danmu.getGiftUserName().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            danmuViewHolder.danmuTextView.setText(snString);
        } else if(danmu.getCmd().equals("INTERACT_WORD")){
            if(isShowMemberIn) {
                if(isShowSendingTime) {
                    String receiveTimeString = "[" + danmu.getReceiveTimeString() + "] ";
                    snString = new SpannableString(receiveTimeString + danmu.getUserName() + "  进入直播间");
                    snString.setSpan(new ForegroundColorSpan(danmuViewHolder.itemView.getContext().getResources().getColor(R.color.showTime)),0, receiveTimeString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    snString.setSpan(new ForegroundColorSpan(danmuViewHolder.itemView.getContext().getResources().getColor(R.color.showInteract)),receiveTimeString.length(), receiveTimeString.length() + danmu.getUserName().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                } else {
                    snString = new SpannableString(danmu.getUserName() + "  进入直播间");
                    snString.setSpan(new ForegroundColorSpan(danmuViewHolder.itemView.getContext().getResources().getColor(R.color.showInteract)),0, danmu.getUserName().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
                danmuViewHolder.danmuTextView.setText(snString);
                //LogUtils.i("show member in:" + danmu.userName);
            }
        } else if(danmu.getCmd().equals("log")){
            snString = new SpannableString("日志：" + danmu.getDanmuText() + " 时间：" +  danmu.getUserName());
            snString.setSpan(new ForegroundColorSpan(Color.parseColor("#e60012")),0, 3, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            danmuViewHolder.danmuTextView.setText(snString);
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
