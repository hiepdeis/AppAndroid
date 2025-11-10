package com.fptu.prm392.mad.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.adapters.MessageAdapter;
import com.fptu.prm392.mad.repositories.ChatRepository;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

public class ChatDetailFragment extends Fragment {

    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;
    private EditText etMessageInput;
    private ImageView btnSendMessage, btnBackToChats;
    private TextView tvChatDetailProjectName;

    private String currentChatId;
    private String currentChatProjectName;

    private ChatRepository chatRepository;
    private FirebaseAuth mAuth;
    private ListenerRegistration messagesListener;

    private OnBackToChatsListener backToChatsListener;

    public interface OnBackToChatsListener {
        void onBackToChats();
    }

    public static ChatDetailFragment newInstance(String chatId, String projectName) {
        ChatDetailFragment fragment = new ChatDetailFragment();
        Bundle args = new Bundle();
        args.putString("CHAT_ID", chatId);
        args.putString("PROJECT_NAME", projectName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        chatRepository = new ChatRepository();

        if (getArguments() != null) {
            currentChatId = getArguments().getString("CHAT_ID");
            currentChatProjectName = getArguments().getString("PROJECT_NAME");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_detail, container, false);

        // Initialize views
        recyclerViewMessages = view.findViewById(R.id.recyclerViewMessages);
        etMessageInput = view.findViewById(R.id.etMessageInput);
        btnSendMessage = view.findViewById(R.id.btnSendMessage);
        btnBackToChats = view.findViewById(R.id.btnBackToChats);
        tvChatDetailProjectName = view.findViewById(R.id.tvChatDetailProjectName);

        // Set project name
        tvChatDetailProjectName.setText(currentChatProjectName);

        // Setup RecyclerView for Messages
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        messageAdapter = new MessageAdapter();
        recyclerViewMessages.setAdapter(messageAdapter);

        // Back button
        btnBackToChats.setOnClickListener(v -> {
            if (backToChatsListener != null) {
                backToChatsListener.onBackToChats();
            }
        });

        // Send message button
        btnSendMessage.setOnClickListener(v -> sendMessage());

        // Load messages
        loadMessages();

        return view;
    }

    private void loadMessages() {
        if (messagesListener != null) {
            messagesListener.remove();
        }

        messagesListener = chatRepository.getChatMessages(
                currentChatId,
                messages -> {
                    messageAdapter.setMessages(messages);
                    if (!messages.isEmpty()) {
                        recyclerViewMessages.scrollToPosition(messages.size() - 1);
                    }
                },
                e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading messages: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void sendMessage() {
        String messageText = etMessageInput.getText().toString().trim();

        if (messageText.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String currentUserId = currentUser.getUid();

        UserRepository userRepository = new UserRepository();
        userRepository.getUserById(currentUserId,
                user -> {
                    String senderName = user.getFullname() != null && !user.getFullname().isEmpty()
                            ? user.getFullname()
                            : user.getEmail();

                    chatRepository.sendMessage(
                            currentChatId,
                            currentUserId,
                            senderName,
                            messageText,
                            aVoid -> etMessageInput.setText(""),
                            e -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Error sending message: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                },
                e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error getting user info", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    public void setOnBackToChatsListener(OnBackToChatsListener listener) {
        this.backToChatsListener = listener;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (messagesListener != null) {
            messagesListener.remove();
        }
    }
}

