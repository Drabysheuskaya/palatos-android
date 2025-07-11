package com.example.PalaTos.Fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.PalaTos.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FeedbackFragment extends Fragment {

    private TextView nameTextView;
    private Spinner orderNumberSpinner;
    private EditText feedbackEditText;
    private Button sendFeedbackButton;
    private ImageView backButton;
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private String restaurantKey;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_feedback, container, false);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        nameTextView = rootView.findViewById(R.id.nameTextView);
        orderNumberSpinner = rootView.findViewById(R.id.orderNumberSpinner);
        feedbackEditText = rootView.findViewById(R.id.feedbackEditText);
        sendFeedbackButton = rootView.findViewById(R.id.sendFeedbackButton);
        backButton = rootView.findViewById(R.id.backButton);

        if (getArguments() != null) {
            restaurantKey = getArguments().getString("restaurantKey");
        }

        loadUserData();
        listenForNameUpdates();

        backButton.setOnClickListener(v -> navigateToHistoryFragment());

        sendFeedbackButton.setOnClickListener(v -> submitFeedback());

        feedbackEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 1000) {
                    feedbackEditText.setError("Feedback must be less than 1000 characters.");
                    sendFeedbackButton.setEnabled(false); // Disable button if limit exceeded
                } else {
                    feedbackEditText.setError(null);
                    sendFeedbackButton.setEnabled(true); // Enable button if within limit
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return rootView;
    }

    private void loadUserData() {
        if (auth.getCurrentUser() == null || restaurantKey == null) return;

        database.getReference("users").child(auth.getCurrentUser().getUid())
                .child("restaurants").child(restaurantKey).child("name")
                .get().addOnSuccessListener(snapshot -> {
                    String name = snapshot.getValue(String.class);
                    nameTextView.setText(name != null ? name : "Unknown");
                }).addOnFailureListener(e -> Log.e("FeedbackFragment", "Failed to load user name: " + e.getMessage()));

        database.getReference("restaurants").child(restaurantKey).child("orders")
                .orderByChild("userId").equalTo(auth.getCurrentUser().getUid())
                .get().addOnSuccessListener(snapshot -> {
                    List<String> orderList = new ArrayList<>();
                    for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                        String status = orderSnapshot.child("status").getValue(String.class);
                        if ("Paid".equals(status)) {
                            String orderId = orderSnapshot.child("orderId").getValue(String.class);
                            if (orderId != null) {
                                orderList.add(orderId);
                            }
                        }
                    }

                    if (orderList.isEmpty()) {
                        Toast.makeText(requireContext(), "No paid orders found for feedback", Toast.LENGTH_SHORT).show();
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, orderList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    orderNumberSpinner.setAdapter(adapter);
                }).addOnFailureListener(e -> Log.e("FeedbackFragment", "Failed to load orders: " + e.getMessage()));
    }

    private void listenForNameUpdates() {
        if (auth.getCurrentUser() == null || restaurantKey == null) return;

        database.getReference("users").child(auth.getCurrentUser().getUid())
                .child("restaurants").child(restaurantKey).child("name")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String updatedName = snapshot.getValue(String.class);
                        nameTextView.setText(updatedName != null ? updatedName : "Unknown");
                        Log.d("FeedbackFragment", "Name updated in real-time: " + updatedName);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FeedbackFragment", "Failed to listen for name updates: " + error.getMessage());
                    }
                });
    }

    private void submitFeedback() {
        String name = nameTextView.getText().toString().trim();
        String orderId = (String) orderNumberSpinner.getSelectedItem();
        String feedback = feedbackEditText.getText().toString().trim();

        if (feedback.isEmpty()) {
            Toast.makeText(requireContext(), "Feedback cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (feedback.length() > 1000) {
            Toast.makeText(requireContext(), "Feedback must be less than 1000 characters.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (restaurantKey == null) {
            Toast.makeText(requireContext(), "Restaurant key is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        FeedbackData feedbackData = new FeedbackData(name, orderId, feedback, auth.getCurrentUser().getUid());

        database.getReference("restaurants").child(restaurantKey).child("feedbacks")
                .push().setValue(feedbackData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Feedback submitted successfully", Toast.LENGTH_SHORT).show();
                    navigateToHistoryFragment();
                })
                .addOnFailureListener(e -> {
                    Log.e("FeedbackFragment", "Failed to submit feedback: " + e.getMessage());
                    Toast.makeText(requireContext(), "Failed to submit feedback", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToHistoryFragment() {
        HistoryFragment historyFragment = new HistoryFragment();
        Bundle bundle = new Bundle();
        bundle.putString("restaurantKey", restaurantKey);
        historyFragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, historyFragment)
                .addToBackStack(null)
                .commit();
    }

    public static class FeedbackData {
        public String name;
        public String orderId;
        public String feedback;
        public String userId;

        public FeedbackData(String name, String orderId, String feedback, String userId) {
            this.name = name;
            this.orderId = orderId;
            this.feedback = feedback;
            this.userId = userId;
        }

        public FeedbackData() {

        }
    }
}
