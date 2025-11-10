package com.fptu.prm392.mad.Domains.Projects.Adapters;

import android.widget.Filterable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;


import android.widget.TextView;

import com.fptu.prm392.mad.Domains.Projects.Dtos.UserAdd;

import java.util.ArrayList;
import java.util.List;
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> implements Filterable {
    private List<UserAdd> original;
    private List<UserAdd> filtered;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(UserAdd user);
    }

    public UserAdapter(List<UserAdd> users, OnUserClickListener listener) {
        this.original = users;
        this.filtered = new ArrayList<>(users);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        UserAdd u = filtered.get(position);
        holder.tv1.setText(u.getFullname());
        holder.tv2.setText(u.getEmail());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(u);
        });
    }

    @Override
    public int getItemCount() {
        return filtered.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override protected FilterResults performFiltering(CharSequence constraint) {
                String q = constraint == null ? "" : constraint.toString().trim().toLowerCase();
                FilterResults res = new FilterResults();
                if (q.isEmpty()) {
                    res.values = new ArrayList<>(original);
                    res.count = original.size();
                } else {
                    List<UserAdd> out = new ArrayList<>();
                    for (UserAdd u : original) {
                        if (u.getFullname() != null && u.getFullname().toLowerCase().contains(q)) {
                            out.add(u);
                        }
                    }
                    res.values = out;
                    res.count = out.size();
                }
                return res;
            }
            @Override protected void publishResults(CharSequence constraint, FilterResults results) {
                filtered = (List<UserAdd>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv1, tv2;
        VH(@NonNull View itemView) {
            super(itemView);
            tv1 = itemView.findViewById(android.R.id.text1);
            tv2 = itemView.findViewById(android.R.id.text2);
        }
    }

}
