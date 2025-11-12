package com.fptu.prm392.mad.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptu.prm392.mad.R;
import com.fptu.prm392.mad.adapters.JoinRequestAdapter;
import com.fptu.prm392.mad.models.ProjectJoinRequest;
import com.fptu.prm392.mad.models.ProjectMember;
import com.fptu.prm392.mad.repositories.ProjectJoinRequestRepository;
import com.fptu.prm392.mad.repositories.ProjectRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView rvJoinRequests, rvRejectionNotifications;
    private LinearLayout emptyState;
    private TextView tvEmptyJoinRequests, tvEmptyNotifications;
    private JoinRequestAdapter requestAdapter;
    private com.fptu.prm392.mad.adapters.RejectionNotificationAdapter notificationAdapter;

    private ProjectJoinRequestRepository requestRepository;
    private com.fptu.prm392.mad.repositories.NotificationRepository notificationRepository;
    private ProjectRepository projectRepository;
    private FirebaseAuth auth;

    // Realtime listeners
    private ListenerRegistration requestsListener;
    private ListenerRegistration notificationsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        // Initialize repositories
        requestRepository = new ProjectJoinRequestRepository();
        notificationRepository = new com.fptu.prm392.mad.repositories.NotificationRepository();
        projectRepository = new ProjectRepository();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        rvJoinRequests = view.findViewById(R.id.rvJoinRequests);
        rvRejectionNotifications = view.findViewById(R.id.rvRejectionNotifications);
        emptyState = view.findViewById(R.id.emptyState);
        tvEmptyJoinRequests = view.findViewById(R.id.tvEmptyJoinRequests);
        tvEmptyNotifications = view.findViewById(R.id.tvEmptyNotifications);

        // Setup Join Requests RecyclerView
        rvJoinRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        requestAdapter = new JoinRequestAdapter(new JoinRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(ProjectJoinRequest request, int position) {
                handleAcceptRequest(request, position);
            }

            @Override
            public void onReject(ProjectJoinRequest request, int position) {
                handleRejectRequest(request, position);
            }
        });
        rvJoinRequests.setAdapter(requestAdapter);

        // Setup Rejection Notifications RecyclerView
        rvRejectionNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationAdapter = new com.fptu.prm392.mad.adapters.RejectionNotificationAdapter(
            (notification, position) -> handleDismissNotification(notification, position)
        );
        rvRejectionNotifications.setAdapter(notificationAdapter);

        // Start listening to realtime updates
        startListeningToRequests();
        startListeningToRejectionNotifications();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Realtime listener đã active, không cần reload
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop listening when fragment is destroyed
        stopListeningToRequests();
        stopListeningToRejectionNotifications();
    }

    private void startListeningToRequests() {
        if (auth.getCurrentUser() == null) {
            updateEmptyStates();
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();

        // Setup realtime listener
        requestsListener = requestRepository.listenToPendingRequests(currentUserId,
            requests -> {
                if (requests.isEmpty()) {
                    tvEmptyJoinRequests.setVisibility(View.VISIBLE);
                    rvJoinRequests.setVisibility(View.GONE);
                } else {
                    tvEmptyJoinRequests.setVisibility(View.GONE);
                    rvJoinRequests.setVisibility(View.VISIBLE);
                    requestAdapter.setRequests(requests);
                }
                updateEmptyStates();
            },
            e -> {
                Toast.makeText(getContext(), "Error loading requests: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
                updateEmptyStates();
            }
        );
    }

    private void stopListeningToRequests() {
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }
    }

    private void startListeningToRejectionNotifications() {
        if (auth.getCurrentUser() == null) {
            updateEmptyStates();
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();

        // Setup realtime listener for rejection notifications
        notificationsListener = notificationRepository.listenToRejectionNotifications(currentUserId,
            notifications -> {
                if (notifications.isEmpty()) {
                    tvEmptyNotifications.setVisibility(View.VISIBLE);
                    rvRejectionNotifications.setVisibility(View.GONE);
                } else {
                    tvEmptyNotifications.setVisibility(View.GONE);
                    rvRejectionNotifications.setVisibility(View.VISIBLE);
                    notificationAdapter.setNotifications(notifications);
                }
                updateEmptyStates();
            },
            e -> {
                Toast.makeText(getContext(), "Error loading notifications: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
                updateEmptyStates();
            }
        );
    }

    private void stopListeningToRejectionNotifications() {
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }
    }

    private void updateEmptyStates() {
        boolean hasRequests = requestAdapter != null && requestAdapter.getItemCount() > 0;
        boolean hasNotifications = notificationAdapter != null && notificationAdapter.getItemCount() > 0;

        if (!hasRequests && !hasNotifications) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    // ...existing code...

    private void handleAcceptRequest(ProjectJoinRequest request, int position) {
        // Show loading (optional - add ProgressBar if needed)

        // Step 1: Add user as member to project
        ProjectMember newMember = new ProjectMember(
            request.getProjectId(),
            request.getRequesterId(),
            request.getRequesterName(),
            request.getRequesterEmail(),
            request.getRequesterAvatar(),
            "member"
        );

        projectRepository.addMemberToProject(request.getProjectId(), newMember,
            aVoid -> {
                // Step 2: Update request status to approved
                requestRepository.approveRequest(request.getRequestId(),
                    v -> {
                        Toast.makeText(getContext(), "Request accepted!", Toast.LENGTH_SHORT).show();
                        // Remove from list
                        requestAdapter.removeItem(position);

                        // Check if empty - use updateEmptyStates() instead
                        updateEmptyStates();
                    },
                    e -> {
                        Toast.makeText(getContext(), "Error updating request: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    }
                );
            },
            e -> {
                Toast.makeText(getContext(), "Error adding member: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void handleRejectRequest(ProjectJoinRequest request, int position) {
        // Determine notification recipient based on request type
        boolean isInvitation = "invitation".equals(request.getRequestType());

        requestRepository.rejectRequest(request.getRequestId(),
            aVoid -> {
                // Send notification to the rejected party
                com.fptu.prm392.mad.repositories.NotificationRepository notificationRepo =
                    new com.fptu.prm392.mad.repositories.NotificationRepository();

                if (isInvitation) {
                    // Invitation rejected by user → Notify manager
                    // Get current user (who rejected)
                    if (auth.getCurrentUser() != null) {
                        com.fptu.prm392.mad.repositories.UserRepository userRepo =
                            new com.fptu.prm392.mad.repositories.UserRepository();
                        userRepo.getUserById(auth.getCurrentUser().getUid(),
                            user -> {
                                // Send notification directly to managerId (manager who sent invitation)
                                notificationRepo.createInvitationRejectedNotification(
                                    request.getManagerId(), // managerId from request
                                    user.getFullname(),
                                    request.getProjectName(),
                                    request.getProjectId(),
                                    notifId -> Log.d("NotificationsFragment", "Rejection notification sent to manager: " + request.getManagerId()),
                                    e -> Log.e("NotificationsFragment", "Error sending notification", e)
                                );
                            },
                            e -> Log.e("NotificationsFragment", "Error getting user", e)
                        );
                    }
                } else {
                    // Join request rejected by manager → Notify requester
                    notificationRepo.createRequestRejectedNotification(
                        request.getRequesterId(),
                        request.getProjectName(),
                        request.getProjectId(),
                        notifId -> Log.d("NotificationsFragment", "Rejection notification sent to requester: " + request.getRequesterId()),
                        e -> Log.e("NotificationsFragment", "Error sending notification", e)
                    );
                }

                Toast.makeText(getContext(), "Request rejected", Toast.LENGTH_SHORT).show();
                // Remove from list
                requestAdapter.removeItem(position);

                // Check if empty
                updateEmptyStates();
            },
            e -> {
                Toast.makeText(getContext(), "Error rejecting request: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void handleDismissNotification(com.fptu.prm392.mad.models.Notification notification, int position) {
        // Mark notification as read (will automatically remove from list via listener)
        notificationRepository.markAsRead(notification.getNotificationId(),
            aVoid -> {
                Toast.makeText(getContext(), "Notification dismissed", Toast.LENGTH_SHORT).show();
                // Listener will auto-update the list
            },
            e -> {
                Toast.makeText(getContext(), "Error dismissing notification: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        rvJoinRequests.setVisibility(View.GONE);
        rvRejectionNotifications.setVisibility(View.GONE);
    }

    private void showRequests(List<ProjectJoinRequest> requests) {
        requestAdapter.setRequests(requests);
        rvJoinRequests.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }
}

