package com.fptu.prm392.mad.repositories;

import android.util.Log;

import com.fptu.prm392.mad.models.Chat;
import com.fptu.prm392.mad.models.Message;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {
    private static final String TAG = "ChatRepository";
    private static final String COLLECTION_CHATS = "chats";
    private static final String COLLECTION_MESSAGES = "messages";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ChatRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    // CREATE group chat
    public void createGroupChat(String groupName, List<String> participantIds,
                               OnSuccessListener<Chat> onSuccess, OnFailureListener onFailure) {
        String chatId = db.collection(COLLECTION_CHATS).document().getId();

        // Create chat with null projectId (indicating it's a group chat, not project chat)
        Chat newChat = new Chat(chatId, null, groupName, participantIds);
        // Set lastMessageTime to now so it appears in the list
        newChat.setLastMessageTime(Timestamp.now());

        db.collection(COLLECTION_CHATS)
            .document(chatId)
            .set(newChat)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Group chat created: " + chatId);
                onSuccess.onSuccess(newChat);
            })
            .addOnFailureListener(onFailure);
    }

    // GET or CREATE one-on-one chat
    public void getOrCreateOneOnOneChat(String otherUserId, String otherUserName,
                                       OnSuccessListener<Chat> onSuccess, OnFailureListener onFailure) {
        String currentUserId = auth.getCurrentUser().getUid();

        // Check if chat already exists between these 2 users
        db.collection(COLLECTION_CHATS)
            .whereArrayContains("participantIds", currentUserId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                // Find existing 1-1 chat
                for (DocumentSnapshot doc : querySnapshot) {
                    Chat chat = doc.toObject(Chat.class);
                    if (chat != null && chat.getParticipantIds() != null
                            && chat.getParticipantIds().size() == 2
                            && chat.getParticipantIds().contains(otherUserId)
                            && chat.getProjectId() == null) {
                        // Found existing 1-1 chat
                        chat.setChatId(doc.getId());
                        Log.d(TAG, "Found existing 1-1 chat: " + chat.getChatId());
                        onSuccess.onSuccess(chat);
                        return;
                    }
                }

                // No existing chat, create new one
                String chatId = db.collection(COLLECTION_CHATS).document().getId();
                List<String> participantIds = new ArrayList<>();
                participantIds.add(currentUserId);
                participantIds.add(otherUserId);

                Chat newChat = new Chat(chatId, null, otherUserName, participantIds);
                newChat.setLastMessageTime(Timestamp.now());

                db.collection(COLLECTION_CHATS)
                    .document(chatId)
                    .set(newChat)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "1-1 chat created: " + chatId);
                        onSuccess.onSuccess(newChat);
                    })
                    .addOnFailureListener(onFailure);
            })
            .addOnFailureListener(onFailure);
    }

    // CREATE hoặc GET chat cho project
    public void getOrCreateProjectChat(String projectId, String projectName, List<String> participantIds,
                                      OnSuccessListener<Chat> onSuccess, OnFailureListener onFailure) {
        // Check if chat already exists for this project
        db.collection(COLLECTION_CHATS)
            .whereEqualTo("projectId", projectId)
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    // Chat already exists
                    DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                    Chat chat = doc.toObject(Chat.class);
                    if (chat != null) {
                        chat.setChatId(doc.getId());
                        Log.d(TAG, "Chat found: " + chat.getChatId());
                        onSuccess.onSuccess(chat);
                    }
                } else {
                    // Create new chat
                    String chatId = db.collection(COLLECTION_CHATS).document().getId();
                    Chat newChat = new Chat(chatId, projectId, projectName, participantIds);

                    db.collection(COLLECTION_CHATS)
                        .document(chatId)
                        .set(newChat)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Chat created: " + chatId);
                            onSuccess.onSuccess(newChat);
                        })
                        .addOnFailureListener(onFailure);
                }
            })
            .addOnFailureListener(onFailure);
    }

    // GET: Lấy tất cả chats của user hiện tại (real-time)
    public ListenerRegistration getUserChats(OnSuccessListener<List<Chat>> onSuccess, OnFailureListener onFailure) {
        String currentUserId = auth.getCurrentUser().getUid();

        return db.collection(COLLECTION_CHATS)
            .whereArrayContains("participantIds", currentUserId)
            // Không dùng orderBy để tránh lỗi index, sẽ sort ở client side
            .addSnapshotListener((querySnapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error getting chats", error);
                    onFailure.onFailure(error);
                    return;
                }

                if (querySnapshot != null) {
                    List<Chat> chats = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Chat chat = doc.toObject(Chat.class);
                        if (chat != null) {
                            chat.setChatId(doc.getId());
                            chats.add(chat);
                        }
                    }

                    // Sort by lastMessageTime ở client side (newest first)
                    chats.sort((c1, c2) -> {
                        if (c1.getLastMessageTime() == null && c2.getLastMessageTime() == null) return 0;
                        if (c1.getLastMessageTime() == null) return 1;
                        if (c2.getLastMessageTime() == null) return -1;
                        return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
                    });

                    Log.d(TAG, "Found " + chats.size() + " chats");
                    onSuccess.onSuccess(chats);
                }
            });
    }

    // SEND message
    public void sendMessage(String chatId, String senderId, String senderName, String content,
                           OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        String messageId = db.collection(COLLECTION_MESSAGES).document().getId();
        Message message = new Message(messageId, chatId, senderId, senderName, content);

        // Add message to messages collection
        db.collection(COLLECTION_MESSAGES)
            .document(messageId)
            .set(message)
            .addOnSuccessListener(aVoid -> {
                // Update chat with last message info
                Map<String, Object> updates = new HashMap<>();
                updates.put("lastMessage", content);
                updates.put("lastMessageSenderId", senderId);
                updates.put("lastMessageTime", Timestamp.now());

                db.collection(COLLECTION_CHATS)
                    .document(chatId)
                    .update(updates)
                    .addOnSuccessListener(aVoid2 -> {
                        Log.d(TAG, "Message sent: " + messageId);
                        onSuccess.onSuccess(aVoid2);
                    })
                    .addOnFailureListener(onFailure);
            })
            .addOnFailureListener(onFailure);
    }

    // GET: Lấy messages của một chat (real-time)
    public ListenerRegistration getChatMessages(String chatId, OnSuccessListener<List<Message>> onSuccess,
                                               OnFailureListener onFailure) {
        return db.collection(COLLECTION_MESSAGES)
            .whereEqualTo("chatId", chatId)
            // Không dùng orderBy để tránh lỗi index, sẽ sort ở client side
            .addSnapshotListener((querySnapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error getting messages", error);
                    onFailure.onFailure(error);
                    return;
                }

                if (querySnapshot != null) {
                    List<Message> messages = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Message message = doc.toObject(Message.class);
                        if (message != null) {
                            message.setMessageId(doc.getId());
                            messages.add(message);
                        }
                    }

                    // Sort by timestamp ở client side (oldest first)
                    messages.sort((m1, m2) -> {
                        if (m1.getTimestamp() == null && m2.getTimestamp() == null) return 0;
                        if (m1.getTimestamp() == null) return 1;
                        if (m2.getTimestamp() == null) return -1;
                        return m1.getTimestamp().compareTo(m2.getTimestamp());
                    });

                    Log.d(TAG, "Found " + messages.size() + " messages");
                    onSuccess.onSuccess(messages);
                }
            });
    }

    // DELETE: Xóa message
    public void deleteMessage(String messageId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(COLLECTION_MESSAGES)
            .document(messageId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Message deleted: " + messageId);
                onSuccess.onSuccess(aVoid);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting message", e);
                onFailure.onFailure(e);
            });
    }
}

