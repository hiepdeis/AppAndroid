package com.fptu.prm392.mad.Domains.Projects.Adapters;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ImageButton;
import com.fptu.prm392.mad.Domains.Projects.Dtos.UserDto;
import com.fptu.prm392.mad.Domains.Projects.Models.User;
import com.fptu.prm392.mad.R;
import java.util.List;
import com.google.android.material.chip.Chip;
import com.bumptech.glide.Glide;


public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder>{
    public interface OnMemberActionListener {
        void onRemoveClicked(UserDto member);
    }

    private final List<UserDto> memberList;
    private final OnMemberActionListener listener;
    public MemberAdapter(List<UserDto> memberList,OnMemberActionListener listener) {
        this.memberList = memberList;
        this.listener = listener;
    }







    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_member, parent, false);

        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        if (memberList == null || position >= memberList.size()) return;
        UserDto member  = memberList.get(position);

        // ✅ Gán fullname
        holder.tvFullName.setText(member.getFullname() != null ? member.getFullname() : "(Không có tên)");

        // ✅ Gán email
        holder.tvEmail.setText(member.getEmail() != null ? member.getEmail() : "(Không có email)");

        // ✅ Gán role
        holder.chipRole.setText(member.getRole() != null ? member.getRole() : "N/A");

        // ✅ Load avatar bằng Glide
        if (member.getAvatar() != null && !member.getAvatar().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(member.getAvatar())
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_person);
        }


        // Wire remove/ban action
        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveClicked(member);
            }
        });

    }

    @Override
    public int getItemCount() {
        return memberList != null ? memberList.size() : 0;
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvFullName, tvEmail;
        ImageView imgAvatar;
        Chip chipRole;
        ImageButton btnRemove;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            chipRole = itemView.findViewById(R.id.chipRole);
            btnRemove = itemView.findViewById(R.id.btnRemoveMember);
        }
    }
}
