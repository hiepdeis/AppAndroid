package com.fptu.prm392.mad.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.ImageView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.Chat;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.fptu.prm392.mad.utils.AvatarLoader;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private List<Chat> chats;
    private OnChatClickListener listener;
    private UserRepository userRepository;
    private String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    public ChatListAdapter(OnChatClickListener listener) {
        this.chats = new ArrayList<>();
        this.listener = listener;
        this.userRepository = new UserRepository();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private boolean isOneOnOneChat(Chat chat) {
        // Check if chat is 1-1:
        // - participantIds size == 2
        // - projectId == null (not a project/task chat)
        return chat.getParticipantIds() != null
                && chat.getParticipantIds().size() == 2
                && chat.getProjectId() == null;
    }

    public void setChats(List<Chat> chats) {
        this.chats = chats;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chats.get(position);
        holder.bind(chat);
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView ivChatIcon;
        TextView tvProjectName, tvLastMessage, tvTime, tvUnreadCount;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivChatIcon = itemView.findViewById(R.id.ivChatIcon);
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }

        public void bind(Chat chat) {
            // Set icon based on chat type
            if (isOneOnOneChat(chat)) {
                // Solo 1-1 chat: Load avatar of other user
                String otherUserId = getOtherUserId(chat);
                if (otherUserId != null) {
                    loadUserAvatar(otherUserId);
                } else {
                    ivChatIcon.setImageResource(R.drawable.profile);
                }
            } else {
                // Group chat: Use group chat icon
                ivChatIcon.setImageResource(R.drawable.group_chat_avatar);
            }

            tvProjectName.setText(chat.getProjectName());

            // Display last message
            if (chat.getLastMessage() != null && !chat.getLastMessage().isEmpty()) {
                tvLastMessage.setText(chat.getLastMessage());
            } else {
                tvLastMessage.setText("No messages yet");
            }

            // Display time
            if (chat.getLastMessageTime() != null) {
                long now = System.currentTimeMillis();
                long messageTime = chat.getLastMessageTime().toDate().getTime();
                long diff = now - messageTime;

                // If today, show time; otherwise show date
                if (diff < 24 * 60 * 60 * 1000) {
                    tvTime.setText(timeFormat.format(chat.getLastMessageTime().toDate()));
                } else {
                    tvTime.setText(dateFormat.format(chat.getLastMessageTime().toDate()));
                }
            } else {
                tvTime.setText("");
            }

            // Display unread count
            if (chat.getUnreadCount() > 0) {
                tvUnreadCount.setVisibility(View.VISIBLE);
                tvUnreadCount.setText(String.valueOf(chat.getUnreadCount()));
            } else {
                tvUnreadCount.setVisibility(View.GONE);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatClick(chat);
                }
            });
        }

        private String getOtherUserId(Chat chat) {
            // Get the other user's ID in 1-1 chat
            if (chat.getParticipantIds() != null && chat.getParticipantIds().size() == 2) {
                for (String userId : chat.getParticipantIds()) {
                    if (!userId.equals(currentUserId)) {
                        return userId;
                    }
                }
            }
            return null;
        }

        private void loadUserAvatar(String userId) {
            // Load avatar from User collection
            userRepository.getUserById(userId,
                user -> {
                    // Load avatar using AvatarLoader with circular crop
                    AvatarLoader.loadAvatar(itemView.getContext(), user.getAvatar(), ivChatIcon);
                },
                e -> {
                    // On error, show default avatar
                    ivChatIcon.setImageResource(R.drawable.profile);
                }
            );
        }
    }
}

