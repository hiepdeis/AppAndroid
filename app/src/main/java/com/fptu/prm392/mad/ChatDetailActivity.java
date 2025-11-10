package com.fptu.prm392.mad;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.adapters.MessageAdapter;
import com.fptu.prm392.mad.repositories.ChatRepository;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

public class ChatDetailActivity extends AppCompatActivity {

    private ImageView btnBack, btnSend;
    private TextView tvProjectName;
    private EditText etMessage;
    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;

    private ChatRepository chatRepository;
    private UserRepository userRepository;
    private FirebaseAuth auth;

    private String chatId;
    private String projectName;
    private String currentUserId;
    private String currentUserName;
    private ListenerRegistration messagesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        chatRepository = new ChatRepository();
        userRepository = new UserRepository();
        auth = FirebaseAuth.getInstance();

        // Get data from intent
        chatId = getIntent().getStringExtra("CHAT_ID");
        projectName = getIntent().getStringExtra("PROJECT_NAME");

        if (chatId == null) {
            Toast.makeText(this, "Error: Chat not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = auth.getCurrentUser().getUid();

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        btnSend = findViewById(R.id.btnSend);
        tvProjectName = findViewById(R.id.tvProjectName);
        etMessage = findViewById(R.id.etMessage);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);

        // Setup UI
        tvProjectName.setText(projectName);

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        recyclerViewMessages.setLayoutManager(layoutManager);
        messageAdapter = new MessageAdapter();
        recyclerViewMessages.setAdapter(messageAdapter);

        // Setup listeners
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());

        // Load user name and then load messages
        loadUserName();
        loadMessages();
    }

    private void loadUserName() {
        userRepository.getUserById(currentUserId,
                user -> {
                    currentUserName = user.getFullname() != null && !user.getFullname().isEmpty()
                            ? user.getFullname()
                            : user.getEmail();
                },
                e -> {
                    currentUserName = "User";
                }
        );
    }

    private void loadMessages() {
        messagesListener = chatRepository.getChatMessages(chatId,
                messages -> {
                    messageAdapter.setMessages(messages);
                    // Scroll to bottom when new messages arrive
                    if (!messages.isEmpty()) {
                        recyclerViewMessages.smoothScrollToPosition(messages.size() - 1);
                    }
                },
                e -> {
                    Toast.makeText(this, "Error loading messages: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();

        if (content.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable send button while sending
        btnSend.setEnabled(false);

        chatRepository.sendMessage(chatId, currentUserId, currentUserName, content,
                aVoid -> {
                    // Clear input and re-enable button
                    etMessage.setText("");
                    btnSend.setEnabled(true);
                },
                e -> {
                    Toast.makeText(this, "Error sending message: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    btnSend.setEnabled(true);
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Detach listener when activity is destroyed
        if (messagesListener != null) {
            messagesListener.remove();
        }
    }
}

