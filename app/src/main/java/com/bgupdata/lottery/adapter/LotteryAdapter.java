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

public class LotteryAdapter extends RecyclerView.Adapter<LotteryAdapter.ViewHolder> {

    private List<LotteryData> dataList = new ArrayList<>();

    public void setData(List<LotteryData> data) {
        this.dataList = data != null ? data : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lottery, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LotteryData data = dataList.get(position);
        holder.tvIssueId.setText(String.valueOf(data.getIssueId()));

        String openData = data.getOpenData();
        if (openData == null || openData.isEmpty()) {
            holder.tvOpenData.setText("等待采集...");
            holder.tvOpenData.setTextColor(0xFF9E9E9E);
        } else if (data.getStatus() == LotteryData.Status.FAILED) {
            holder.tvOpenData.setText("采集失败");
            holder.tvOpenData.setTextColor(0xFFF44336);
        } else {
            holder.tvOpenData.setText(openData);
            holder.tvOpenData.setTextColor(0xFF424242);
        }

        holder.tvAcCount.setText(data.getAcCount() > 0 ? String.valueOf(data.getAcCount()) : "");
        holder.tvAcTime.setText(data.getAcTime() != null ? data.getAcTime() : "");
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIssueId, tvOpenData, tvAcCount, tvAcTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIssueId = itemView.findViewById(R.id.tv_issue_id);
            tvOpenData = itemView.findViewById(R.id.tv_open_data);
            tvAcCount = itemView.findViewById(R.id.tv_ac_count);
            tvAcTime = itemView.findViewById(R.id.tv_ac_time);
        }
    }
}
