package com.example.PalaTos;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RestaurantDetailActivity extends AppCompatActivity {

    private LinearLayout menuItemsContainer;
    private String restaurantKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.restaurant_detail);

        // Initialize views
        menuItemsContainer = findViewById(R.id.menuItemsContainer);
        ImageView backButton = findViewById(R.id.backButton); // Back Button

        // Handle back button click
        backButton.setOnClickListener(v -> navigateToChooseLocation());

        // Retrieve restaurant key from Intent
        restaurantKey = getIntent().getStringExtra("restaurantKey");
        if (restaurantKey != null) {
            fetchRestaurantDetails(restaurantKey);
        } else {
            showErrorAndExit("Restaurant key not found.");
        }
    }

    /**
     * Fetch restaurant details and menu sections from Firebase.
     */
    private void fetchRestaurantDetails(String restaurantKey) {
        DatabaseReference restaurantRef = FirebaseDatabase.getInstance()
                .getReference("restaurants")
                .child(restaurantKey);

        restaurantRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String description = snapshot.child("description").getValue(String.class);
                    String imageUrl = snapshot.child("image").getValue(String.class);

                    if (name != null && description != null && imageUrl != null) {
                        displayRestaurantDetails(name, description, imageUrl);
                        populateMenuSections(snapshot.child("menu"));
                    } else {
                        showErrorAndExit("Incomplete restaurant details.");
                    }
                } else {
                    showErrorAndExit("Restaurant details not found.");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showErrorAndExit("Failed to fetch restaurant details: " + error.getMessage());
            }
        });
    }

    /**
     * Display restaurant name, description, and image.
     */
    private void displayRestaurantDetails(String name, String description, String imageUrl) {
        TextView nameView = findViewById(R.id.restaurantName);
        TextView descriptionView = findViewById(R.id.restaurantDescription);
        ImageView imageView = findViewById(R.id.restaurantImage);

        nameView.setText(name);
        descriptionView.setText(description);


        Glide.with(this)
                .load(imageUrl)
                .into(imageView);
    }

    /**
     * Populate menu sections with buttons.
     */
    private void populateMenuSections(DataSnapshot menuSnapshot) {
        menuItemsContainer.removeAllViews(); // Clear previous buttons

        if (!menuSnapshot.exists()) {
            Log.e("RestaurantDetail", "No menu sections found.");
            showErrorAndExit("No menu sections available.");
            return;
        }

        for (DataSnapshot sectionSnapshot : menuSnapshot.getChildren()) {
            String sectionName = sectionSnapshot.getKey();
            if (sectionName != null) {
                createMenuButton(sectionName);
            } else {
                Log.e("RestaurantDetail", "Invalid menu section name.");
            }
        }
    }

    /**
     * Create a button dynamically for each menu section.
     */
    private void createMenuButton(String sectionName) {
        Button button = new Button(this);
        button.setText(sectionName);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setTextColor(Color.BLACK);
        button.setTypeface(null, Typeface.BOLD);
        button.setPadding(16, 16, 16, 16);

        button.setOnClickListener(v -> navigateToMenuSection(sectionName));
        menuItemsContainer.addView(button);
    }

    /**
     * Navigate to ChooseLocationActivity.
     */
    private void navigateToChooseLocation() {
        Intent intent = new Intent(RestaurantDetailActivity.this, ChooseLocationActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Navigate to MainActivity and load HomeFragment with the selected section.
     */
    private void navigateToMenuSection(String sectionName) {
        if (restaurantKey == null) {
            showErrorAndExit("Restaurant key is missing.");
            return;
        }

        Intent intent = new Intent(RestaurantDetailActivity.this, MainActivity.class);
        intent.putExtra("restaurantKey", restaurantKey);
        intent.putExtra("menuSection", sectionName);
        startActivity(intent);

        finish();
    }

    /**
     * Handle back button press to go to ChooseLocationActivity.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateToChooseLocation();
    }

    /**
     * Show error message and exit the activity.
     */
    private void showErrorAndExit(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}
