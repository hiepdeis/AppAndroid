package com.fptu.prm392.mad;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.adapters.ChatListAdapter;
import com.fptu.prm392.mad.models.Chat;
import com.fptu.prm392.mad.repositories.ChatRepository;
import com.google.firebase.firestore.ListenerRegistration;

public class ChatListActivity extends AppCompatActivity {

    private ImageView btnBack;
    private RecyclerView recyclerViewChats;
    private LinearLayout emptyState;
    private ChatListAdapter chatListAdapter;
    private ChatRepository chatRepository;
    private ListenerRegistration chatsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        chatRepository = new ChatRepository();

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        recyclerViewChats = findViewById(R.id.recyclerViewChats);
        emptyState = findViewById(R.id.emptyState);

        // Setup RecyclerView
        recyclerViewChats.setLayoutManager(new LinearLayoutManager(this));
        chatListAdapter = new ChatListAdapter(this::openChat);
        recyclerViewChats.setAdapter(chatListAdapter);

        // Setup listeners
        btnBack.setOnClickListener(v -> finish());

        // Load chats with real-time updates
        loadChats();
    }

    private void loadChats() {
        chatsListener = chatRepository.getUserChats(
                chats -> {
                    if (chats.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerViewChats.setVisibility(View.GONE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                        recyclerViewChats.setVisibility(View.VISIBLE);
                        chatListAdapter.setChats(chats);
                    }
                },
                e -> {
                    Toast.makeText(this, "Error loading chats: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void openChat(Chat chat) {
        Intent intent = new Intent(this, ChatDetailActivity.class);
        intent.putExtra("CHAT_ID", chat.getChatId());
        intent.putExtra("PROJECT_NAME", chat.getProjectName());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Detach listener when activity is destroyed
        if (chatsListener != null) {
            chatsListener.remove();
        }
    }
}

