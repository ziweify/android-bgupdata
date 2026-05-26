package com.bgupdata.lottery.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bgupdata.lottery.R;
import com.bgupdata.lottery.model.LotteryData;

import java.util.ArrayList;
import java.util.List;

public class CompletedLotteryAdapter extends RecyclerView.Adapter<CompletedLotteryAdapter.ViewHolder> {

    public interface OnPostClickListener {
        void onPostClick(int position, LotteryData data);
    }

    private List<LotteryData> dataList = new ArrayList<>();
    private OnPostClickListener postClickListener;

    public void setData(List<LotteryData> data) {
        this.dataList = data != null ? data : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setPostClickListener(OnPostClickListener listener) {
        this.postClickListener = listener;
    }

    public List<LotteryData> getData() {
        return dataList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lottery_completed, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LotteryData data = dataList.get(position);
        holder.tvIssueId.setText(String.valueOf(data.getIssueId()));

        String openData = data.getOpenData();
        if (openData != null && !openData.isEmpty()) {
            holder.tvOpenData.setText(openData);
            holder.tvOpenData.setTextColor(0xFF424242);
        } else {
            holder.tvOpenData.setText("--");
            holder.tvOpenData.setTextColor(0xFF9E9E9E);
        }

        holder.tvAcTime.setText(data.getAcTime() != null ? data.getAcTime() : "");

        holder.btnPostItem.setOnClickListener(v -> {
            if (postClickListener != null) {
                postClickListener.onPostClick(position, data);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIssueId, tvOpenData, tvAcTime, btnPostItem;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIssueId = itemView.findViewById(R.id.tv_issue_id);
            tvOpenData = itemView.findViewById(R.id.tv_open_data);
            tvAcTime = itemView.findViewById(R.id.tv_ac_time);
            btnPostItem = itemView.findViewById(R.id.btn_post_item);
        }
    }
}
