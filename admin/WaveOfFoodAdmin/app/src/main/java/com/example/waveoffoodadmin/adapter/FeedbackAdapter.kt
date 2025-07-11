package com.example.waveoffoodadmin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.waveoffoodadmin.R
import com.example.waveoffoodadmin.model.Feedback

class FeedbackAdapter(private val context: Context, private val feedbackList: List<Feedback>) :
    ArrayAdapter<Feedback>(context, 0, feedbackList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_feedback, parent, false)
        val feedback = feedbackList[position]


        val nameTextView = view.findViewById<TextView>(R.id.nameTextView)
        val orderIdTextView = view.findViewById<TextView>(R.id.orderIdTextView)
        val feedbackTextView = view.findViewById<TextView>(R.id.feedbackTextView)


        nameTextView.text = feedback.name ?: "Anonymous"
        orderIdTextView.text = "Order №${feedback.orderId}"
        feedbackTextView.text = feedback.feedback ?: "No feedback provided"

        return view
    }
}
