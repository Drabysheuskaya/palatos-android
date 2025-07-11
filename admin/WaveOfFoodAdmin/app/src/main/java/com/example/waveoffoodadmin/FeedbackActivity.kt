package com.example.waveoffoodadmin

import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.waveoffoodadmin.adapter.FeedbackAdapter
import com.example.waveoffoodadmin.model.Feedback
import com.example.waveoffoodadmin.databinding.ActivityFeedbackBinding
import com.google.firebase.database.*

class FeedbackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbackBinding
    private lateinit var database: DatabaseReference
    private var restaurantKey: String? = null
    private val feedbackList = mutableListOf<Feedback>()
    private lateinit var feedbackAdapter: FeedbackAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)


        restaurantKey = intent.getStringExtra("RESTAURANT_KEY")

        if (restaurantKey == null) {
            Toast.makeText(this, "Error: Missing restaurant key!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        database = FirebaseDatabase.getInstance("https://palatos-6612b-default-rtdb.firebaseio.com/").reference


        feedbackAdapter = FeedbackAdapter(this, feedbackList)
        binding.feedbackListView.adapter = feedbackAdapter


        fetchFeedback()


        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun fetchFeedback() {
        database.child("restaurants").child(restaurantKey!!).child("feedbacks")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    feedbackList.clear()
                    if (snapshot.exists()) {
                        for (feedbackSnapshot in snapshot.children) {
                            val feedback = feedbackSnapshot.getValue(Feedback::class.java)
                            feedback?.let { feedbackList.add(it) }
                        }
                        feedbackAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@FeedbackActivity, "No feedback found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FeedbackActivity", "Failed to fetch feedback: ${error.message}")
                    Toast.makeText(this@FeedbackActivity, "Failed to load feedback.", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
