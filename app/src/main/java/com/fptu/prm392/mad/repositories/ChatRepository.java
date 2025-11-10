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
import com.google.firebase.firestore.Query;

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
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
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
            .orderBy("timestamp", Query.Direction.ASCENDING)
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

