package com.fptu.prm392.mad.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.User;

import java.util.ArrayList;
import java.util.List;

public class MemberSelectAdapter extends RecyclerView.Adapter<MemberSelectAdapter.MemberViewHolder> {

    private Context context;
    private List<User> memberList;
    private List<String> selectedMemberIds;

    public MemberSelectAdapter(Context context, List<User> memberList, List<String> preSelectedIds) {
        this.context = context;
        this.memberList = memberList;
        this.selectedMemberIds = new ArrayList<>(preSelectedIds);
    }

    public List<String> getSelectedMemberIds() {
        return selectedMemberIds;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_member_selectable, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User member = memberList.get(position);

        holder.tvMemberName.setText(member.getFullname());
        holder.tvMemberEmail.setText(member.getEmail());

        // Set checkbox state
        holder.cbMember.setChecked(selectedMemberIds.contains(member.getUserId()));

        // Handle checkbox click
        holder.cbMember.setOnCheckedChangeListener(null); // Clear old listener
        holder.cbMember.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedMemberIds.contains(member.getUserId())) {
                    selectedMemberIds.add(member.getUserId());
                }
            } else {
                selectedMemberIds.remove(member.getUserId());
            }
        });

        // Handle item click
        holder.itemView.setOnClickListener(v -> holder.cbMember.toggle());
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbMember;
        TextView tvMemberName, tvMemberEmail;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            cbMember = itemView.findViewById(R.id.cbMember);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberEmail = itemView.findViewById(R.id.tvMemberEmail);
        }
    }
}

