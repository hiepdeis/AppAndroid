package com.fptu.prm392.mad.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.adapters.AddUserAdapter;
import com.fptu.prm392.mad.adapters.ChatListAdapter;
import com.fptu.prm392.mad.adapters.UserChatAdapter;
import com.fptu.prm392.mad.models.Chat;
import com.fptu.prm392.mad.models.User;
import com.fptu.prm392.mad.repositories.ChatRepository;
import com.fptu.prm392.mad.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment {

    private RecyclerView recyclerViewChats;
    private ChatListAdapter chatListAdapter;
    private LinearLayout emptyStateChat;
    private ImageView fabCreateGroupChat;
    private EditText etSearchChat;
    private com.google.android.material.card.MaterialCardView searchOverlay;
    private RecyclerView recyclerViewSearchUsers;
    private UserChatAdapter userChatAdapter;

    private ChatRepository chatRepository;
    private UserRepository userRepository;
    private FirebaseAuth mAuth;
    private ListenerRegistration chatsListener;

    private OnChatClickListener chatClickListener;

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    public static ChatListFragment newInstance() {
        return new ChatListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chatRepository = new ChatRepository();
        userRepository = new UserRepository();
        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        recyclerViewChats = view.findViewById(R.id.recyclerViewChats);
        emptyStateChat = view.findViewById(R.id.emptyStateChat);
        fabCreateGroupChat = view.findViewById(R.id.fabCreateGroupChat);
        etSearchChat = view.findViewById(R.id.etSearchChat);
        searchOverlay = view.findViewById(R.id.searchOverlay);
        recyclerViewSearchUsers = view.findViewById(R.id.recyclerViewSearchUsers);

        // Setup RecyclerView for chats
        recyclerViewChats.setLayoutManager(new LinearLayoutManager(getContext()));
        chatListAdapter = new ChatListAdapter(chat -> {
            if (chatClickListener != null) {
                chatClickListener.onChatClick(chat);
            }
        });
        recyclerViewChats.setAdapter(chatListAdapter);

        // Setup search overlay RecyclerView
        recyclerViewSearchUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        userChatAdapter = new UserChatAdapter(this::createOneOnOneChat);
        recyclerViewSearchUsers.setAdapter(userChatAdapter);

        // Setup search functionality
        setupSearchBar();

        // Setup FAB
        fabCreateGroupChat.setOnClickListener(v -> showCreateGroupChatDialog());

        loadChats();

        return view;
    }

    private void loadChats() {
        if (chatsListener != null) {
            chatsListener.remove();
        }

        chatsListener = chatRepository.getUserChats(
                chats -> {
                    if (chats.isEmpty()) {
                        emptyStateChat.setVisibility(View.VISIBLE);
                        recyclerViewChats.setVisibility(View.GONE);
                    } else {
                        emptyStateChat.setVisibility(View.GONE);
                        recyclerViewChats.setVisibility(View.VISIBLE);
                        chatListAdapter.setChats(chats);
                    }
                },
                e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading chats: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    public void setOnChatClickListener(OnChatClickListener listener) {
        this.chatClickListener = listener;
    }

    private void showCreateGroupChatDialog() {
        if (getContext() == null) return;

        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_group_chat);

        // Set transparent background để bo tròn hiển thị
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Find views
        EditText etGroupName = dialog.findViewById(R.id.etGroupName);
        EditText etSearchUsers = dialog.findViewById(R.id.etSearchUsers);
        RecyclerView recyclerViewUsers = dialog.findViewById(R.id.recyclerViewUsers);
        TextView tvSelectedCount = dialog.findViewById(R.id.tvSelectedCount);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnCreate = dialog.findViewById(R.id.btnCreate);

        // Setup RecyclerView
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        AddUserAdapter userAdapter = new AddUserAdapter((AddUserAdapter.OnSelectionChangedListener) count -> {
            if (count > 0) {
                tvSelectedCount.setText(getResources().getQuantityString(R.plurals.members_selected, count, count));
                btnCreate.setEnabled(!etGroupName.getText().toString().trim().isEmpty());
            } else {
                tvSelectedCount.setText(getString(R.string.no_members_selected));
                btnCreate.setEnabled(false);
            }
        });
        recyclerViewUsers.setAdapter(userAdapter);

        // Load all users
        userRepository.getAllUsers(
                users -> {
                    // Filter out current user
                    String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
                    List<User> filteredUsers = new ArrayList<>();
                    for (User user : users) {
                        if (!user.getUserId().equals(currentUserId)) {
                            filteredUsers.add(user);
                        }
                    }
                    userAdapter.setUsers(filteredUsers);
                },
                e -> Toast.makeText(getContext(), "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );

        // Search functionality
        etSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                userAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Group name change listener
        etGroupName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnCreate.setEnabled(!s.toString().trim().isEmpty() && !userAdapter.getSelectedUserIds().isEmpty());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Create button
        btnCreate.setOnClickListener(v -> {
            String groupName = etGroupName.getText().toString().trim();
            List<String> selectedUserIds = userAdapter.getSelectedUserIds();

            if (groupName.isEmpty()) {
                Toast.makeText(getContext(), "Please enter group name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedUserIds.isEmpty()) {
                Toast.makeText(getContext(), "Please select at least one member", Toast.LENGTH_SHORT).show();
                return;
            }

            createGroupChat(groupName, selectedUserIds);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void createGroupChat(String groupName, List<String> memberIds) {
        if (mAuth.getCurrentUser() == null) return;

        String currentUserId = mAuth.getCurrentUser().getUid();

        // Add current user to members list
        List<String> allMemberIds = new ArrayList<>(memberIds);
        if (!allMemberIds.contains(currentUserId)) {
            allMemberIds.add(currentUserId);
        }

        chatRepository.createGroupChat(
                groupName,
                allMemberIds,
                chat -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Group chat created successfully", Toast.LENGTH_SHORT).show();
                    }
                    // Chat will appear automatically via real-time listener
                },
                e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error creating group chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupSearchBar() {
        etSearchChat.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Show dropdown and load all users
                searchOverlay.setVisibility(View.VISIBLE);
                loadAllUsers();
            } else {
                // Hide dropdown when lost focus and empty
                if (etSearchChat.getText().toString().trim().isEmpty()) {
                    searchOverlay.setVisibility(View.GONE);
                }
            }
        });

        etSearchChat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (userChatAdapter != null) {
                    userChatAdapter.filter(s.toString());
                }

                // Show/hide dropdown based on text
                if (s.length() > 0 || etSearchChat.hasFocus()) {
                    searchOverlay.setVisibility(View.VISIBLE);
                } else {
                    searchOverlay.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadAllUsers() {
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        userRepository.getAllUsers(
                users -> {
                    // Filter out current user
                    List<User> filteredUsers = new ArrayList<>();
                    for (User user : users) {
                        if (!user.getUserId().equals(currentUserId)) {
                            filteredUsers.add(user);
                        }
                    }
                    userChatAdapter.setUsers(filteredUsers);
                },
                e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading users: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void closeSearch() {
        searchOverlay.setVisibility(View.GONE);
        etSearchChat.setText("");
        etSearchChat.clearFocus();

        // Hide keyboard
        if (getActivity() != null) {
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null && getView() != null) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
    }

    private void createOneOnOneChat(User otherUser) {
        String otherUserName = otherUser.getFullname() != null && !otherUser.getFullname().isEmpty()
                ? otherUser.getFullname()
                : otherUser.getEmail();

        chatRepository.getOrCreateOneOnOneChat(
                otherUser.getUserId(),
                otherUserName,
                chat -> {
                    closeSearch();
                    // Open chat detail
                    if (chatClickListener != null) {
                        chatClickListener.onChatClick(chat);
                    }
                },
                e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error creating chat: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        loadChats();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatsListener != null) {
            chatsListener.remove();
        }
    }
}

