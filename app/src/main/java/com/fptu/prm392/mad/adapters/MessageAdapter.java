package com.fptu.prm392.mad.adapters;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.models.Message;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages;
    private String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public MessageAdapter() {
        this.messages = new ArrayList<>();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout messageContainer;
        TextView tvMessage, tvSenderName, tvTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        public void bind(Message message) {
            tvMessage.setText(message.getContent());
            tvSenderName.setText(message.getSenderName());
            tvTime.setText(timeFormat.format(message.getTimestamp().toDate()));

            // Align message based on sender
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) messageContainer.getLayoutParams();

            if (message.getSenderId().equals(currentUserId)) {
                // My message - align right with blue background and white text
                params.gravity = Gravity.END;
                messageContainer.setBackgroundResource(R.drawable.bg_message_sent);
                tvSenderName.setVisibility(View.GONE);
                tvMessage.setTextColor(0xFFFFFFFF); // White text
                tvTime.setTextColor(0xFFE3F2FD); // Light blue text
            } else {
                // Other's message - align left with light blue background and dark text
                params.gravity = Gravity.START;
                messageContainer.setBackgroundResource(R.drawable.bg_message_received);
                tvSenderName.setVisibility(View.VISIBLE);
                tvMessage.setTextColor(0xFF1A1A1A); // Dark text
                tvTime.setTextColor(0xFF757575); // Gray text
            }

            messageContainer.setLayoutParams(params);
        }
    }
}

