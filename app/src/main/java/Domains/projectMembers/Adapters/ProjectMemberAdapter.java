package Domains.projectMembers.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.fptu.prm392.mad.R;
import java.util.List;

import Domains.projectMembers.Dtos.ProjectMember;

public class ProjectMemberAdapter extends RecyclerView.Adapter<ProjectMemberAdapter.ViewHolder> {

    private List<ProjectMember> memberList;

    public ProjectMemberAdapter(List<ProjectMember> memberList) {
        this.memberList = memberList;
    }

    // ✅ Thêm constructor có listener
    public ProjectMemberAdapter(List<ProjectMember> memberList, OnMemberClickListener listener) {
        this.memberList = memberList;
        this.listener = listener;
    }
    private OnMemberClickListener listener;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_project_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProjectMember member = memberList.get(position);
        holder.tvID.setText(String.valueOf(member.getUserId()));
        holder.tvFullname.setText(member.getFullname());
        holder.tvEmail.setText(member.getEmail());
        holder.tvRole.setText( member.getProjectRole());
        holder.tvStatus.setText( member.getStatus());

        // ✅ Bắt sự kiện click item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onMemberClick(member);
        });
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvID, tvFullname, tvEmail, tvRole, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvID = itemView.findViewById(R.id.tvID);
            tvFullname = itemView.findViewById(R.id.tvFullname);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }

    public void updateData(List<ProjectMember> newList) {
        this.memberList = newList;
        notifyDataSetChanged();
    }
}
