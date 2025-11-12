package com.fptu.prm392.mad.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

    private RecyclerView rvJoinRequests;
    private LinearLayout emptyState;
    private JoinRequestAdapter adapter;

    private ProjectJoinRequestRepository requestRepository;
    private ProjectRepository projectRepository;
    private FirebaseAuth auth;

    // Realtime listener
    private ListenerRegistration requestsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        // Initialize repositories
        requestRepository = new ProjectJoinRequestRepository();
        projectRepository = new ProjectRepository();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        rvJoinRequests = view.findViewById(R.id.rvJoinRequests);
        emptyState = view.findViewById(R.id.emptyState);

        // Setup RecyclerView
        rvJoinRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new JoinRequestAdapter(new JoinRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(ProjectJoinRequest request, int position) {
                handleAcceptRequest(request, position);
            }

            @Override
            public void onReject(ProjectJoinRequest request, int position) {
                handleRejectRequest(request, position);
            }
        });
        rvJoinRequests.setAdapter(adapter);

        // Start listening to realtime updates
        startListeningToRequests();

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
    }

    private void startListeningToRequests() {
        if (auth.getCurrentUser() == null) {
            showEmptyState();
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();

        // Setup realtime listener
        requestsListener = requestRepository.listenToPendingRequests(currentUserId,
            requests -> {
                if (requests.isEmpty()) {
                    showEmptyState();
                } else {
                    showRequests(requests);
                }
            },
            e -> {
                Toast.makeText(getContext(), "Error loading requests: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        );
    }

    private void stopListeningToRequests() {
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
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
                        adapter.removeItem(position);

                        // Check if empty
                        if (adapter.getItemCount() == 0) {
                            showEmptyState();
                        }
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
                                // Find manager ID from project
                                ProjectRepository projectRepo = new ProjectRepository();
                                projectRepo.getProjectById(request.getProjectId(),
                                    project -> {
                                        notificationRepo.createInvitationRejectedNotification(
                                            project.getCreatedBy(), // managerId
                                            user.getFullname(),
                                            request.getProjectName(),
                                            request.getProjectId(),
                                            notifId -> Log.d("NotificationsFragment", "Rejection notification sent"),
                                            e -> Log.e("NotificationsFragment", "Error sending notification", e)
                                        );
                                    },
                                    e -> Log.e("NotificationsFragment", "Error getting project", e)
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
                        notifId -> Log.d("NotificationsFragment", "Rejection notification sent"),
                        e -> Log.e("NotificationsFragment", "Error sending notification", e)
                    );
                }

                Toast.makeText(getContext(), "Request rejected", Toast.LENGTH_SHORT).show();
                // Remove from list
                adapter.removeItem(position);

                // Check if empty
                if (adapter.getItemCount() == 0) {
                    showEmptyState();
                }
            },
            e -> {
                Toast.makeText(getContext(), "Error rejecting request: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void showRequests(List<ProjectJoinRequest> requests) {
        adapter.setRequests(requests);
        rvJoinRequests.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        rvJoinRequests.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }
}

