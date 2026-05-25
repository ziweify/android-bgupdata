package com.bgupdata.lottery.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bgupdata.lottery.R;
import com.bgupdata.lottery.model.AddressItem;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onEdit(int position, AddressItem item);
        void onDelete(int position, AddressItem item);
        void onToggle(int position, AddressItem item, boolean enabled);
    }

    private List<AddressItem> dataList = new ArrayList<>();
    private OnItemActionListener listener;

    public void setListener(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<AddressItem> data) {
        this.dataList = data != null ? new ArrayList<>(data) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<AddressItem> getData() {
        return dataList;
    }

    public void addItem(AddressItem item) {
        dataList.add(item);
        notifyItemInserted(dataList.size() - 1);
    }

    public void updateItem(int position, AddressItem item) {
        if (position >= 0 && position < dataList.size()) {
            dataList.set(position, item);
            notifyItemChanged(position);
        }
    }

    public void removeItem(int position) {
        if (position >= 0 && position < dataList.size()) {
            dataList.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_address, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AddressItem item = dataList.get(position);

        holder.tvType.setText(item.getSiteType().getValue());
        holder.tvUrl.setText(item.getUrl());
        holder.switchEnabled.setChecked(item.isEnabled());

        holder.tvUrl.setAlpha(item.isEnabled() ? 1f : 0.4f);
        holder.tvType.setAlpha(item.isEnabled() ? 1f : 0.4f);

        holder.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            dataList.get(pos).setEnabled(isChecked);
            holder.tvUrl.setAlpha(isChecked ? 1f : 0.4f);
            holder.tvType.setAlpha(isChecked ? 1f : 0.4f);
            if (listener != null) listener.onToggle(pos, dataList.get(pos), isChecked);
        });

        holder.btnEdit.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onEdit(pos, dataList.get(pos));
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onDelete(pos, dataList.get(pos));
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        SwitchMaterial switchEnabled;
        TextView tvType, tvUrl;
        ImageButton btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            switchEnabled = itemView.findViewById(R.id.switch_enabled);
            tvType = itemView.findViewById(R.id.tv_type);
            tvUrl = itemView.findViewById(R.id.tv_url);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
