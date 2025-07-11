package com.example.waveoffoodadmin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.waveoffoodadmin.R
import com.example.waveoffoodadmin.model.OrderModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class OrderInfoAdapter(
    private val orders: MutableList<OrderModel> = mutableListOf(),
    private val fetchUserName: (String, (String) -> Unit) -> Unit,
    private val updateOrder: (OrderModel) -> Unit,
    private val restaurantKey: String
) : RecyclerView.Adapter<OrderInfoAdapter.OrderInfoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderInfoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.order_info_item_layout, parent, false)
        return OrderInfoViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderInfoViewHolder, position: Int) {
        val order = orders[position]
        fetchUserName(order.userId) { userName ->
            holder.bind(order, userName, updateOrder, restaurantKey)
        }
    }

    override fun getItemCount(): Int = orders.size

    class OrderInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderTime: TextView = itemView.findViewById(R.id.tvOrderTime)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvOrderDetails: TextView = itemView.findViewById(R.id.tvOrderDetails)
        private val tvItems: TextView = itemView.findViewById(R.id.tvItems)
        private val tvSubtotal: TextView = itemView.findViewById(R.id.tvSubtotal)
        private val tvService: TextView = itemView.findViewById(R.id.tvService)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)
        private val spinnerPaymentStatus: Spinner = itemView.findViewById(R.id.spinnerPaymentStatus)
        private val spinnerStatusProcess: Spinner = itemView.findViewById(R.id.spinnerStatusProcess)
        private val btnSave: Button = itemView.findViewById(R.id.btnSave)

        private var paymentStatus: String = ""
        private var progressStatus: String = ""

        fun bind(
            order: OrderModel,
            userName: String,
            updateOrder: (OrderModel) -> Unit,
            restaurantKey: String
        ) {
            tvUserName.text = "User: $userName"
            tvOrderTime.text = order.timeOfOrder
            tvOrderDetails.text = "Order #: ${order.orderId}\nTable: ${order.tableNumber}\nWaiter: ${order.waiter}"
            tvItems.text = order.items.joinToString("\n") { "• ${it.name} x ${it.quantity} - ${it.price}" }
            tvSubtotal.text = "Sub-Total: ${order.subtotal}"
            tvService.text = "Service: ${order.service}"
            tvTotal.text = "Total: ${order.total}"


            val paymentOptions = listOf("Not Received", "Received")
            val paymentAdapter = ArrayAdapter(itemView.context, android.R.layout.simple_spinner_item, paymentOptions)
            spinnerPaymentStatus.adapter = paymentAdapter
            val paymentIndex = paymentOptions.indexOf(order.status)
            spinnerPaymentStatus.setSelection(if (paymentIndex != -1) paymentIndex else 0)
            spinnerPaymentStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    paymentStatus = paymentOptions[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }


            val progressOptions = listOf("In Progress", "Delivered")
            val progressAdapter = ArrayAdapter(itemView.context, android.R.layout.simple_spinner_item, progressOptions)
            spinnerStatusProcess.adapter = progressAdapter
            val progressIndex = progressOptions.indexOf(order.progress_status)
            spinnerStatusProcess.setSelection(if (progressIndex != -1) progressIndex else 0)
            spinnerStatusProcess.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    progressStatus = progressOptions[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }


            val orderRef = FirebaseDatabase.getInstance()
                .getReference("restaurants/$restaurantKey/orders/${order.orderId}")
            orderRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val updatedProgressStatus = snapshot.child("progress_status").getValue(String::class.java)
                    if (updatedProgressStatus != null && updatedProgressStatus != order.progress_status) {
                        order.progress_status = updatedProgressStatus
                        val newProgressIndex = progressOptions.indexOf(updatedProgressStatus)
                        if (newProgressIndex != -1) {
                            spinnerStatusProcess.setSelection(newProgressIndex)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(itemView.context, "Failed to fetch updates: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })


            btnSave.setOnClickListener {
                if (paymentStatus != order.status || progressStatus != order.progress_status) {
                    order.status = paymentStatus
                    order.progress_status = progressStatus
                    updateOrder(order)
                }
            }
        }
    }
}
