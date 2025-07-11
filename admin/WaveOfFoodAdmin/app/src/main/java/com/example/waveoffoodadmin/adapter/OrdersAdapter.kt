package com.example.waveoffoodadmin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.waveoffoodadmin.R
import com.example.waveoffoodadmin.model.OrderModel


class OrdersAdapter(
    private val orders: List<OrderModel>,
    private val onAcceptClick: (String) -> Unit,
    private val fetchUserName: (String, (String) -> Unit) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.order_item_layout, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        fetchUserName(order.userId) { userName ->
            holder.bind(order, userName, onAcceptClick)
        }
    }

    override fun getItemCount(): Int = orders.size

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderTime: TextView = itemView.findViewById(R.id.tvOrderTime)
        private val tvOrderDetails: TextView = itemView.findViewById(R.id.tvOrderDetails)
        private val tvItems: TextView = itemView.findViewById(R.id.tvItems)
        private val tvSubtotal: TextView = itemView.findViewById(R.id.tvSubtotal)
        private val tvService: TextView = itemView.findViewById(R.id.tvService)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)

        fun bind(order: OrderModel, userName: String, onAcceptClick: (String) -> Unit) {
            tvOrderTime.text = order.timeOfOrder // Display the time of order
            tvOrderDetails.text = "Order #: ${order.orderId}, Table: ${order.tableNumber}, Waiter: ${order.waiter}"
            tvItems.text = order.items.joinToString("\n") { "${it.name} x ${it.quantity} - ${it.price}" }
            tvSubtotal.text = "Sub-Total: ${order.subtotal} PLN"
            tvService.text = "Service: ${order.service} PLN"
            tvTotal.text = "Total: ${order.total} PLN"
            tvUserName.text = "User: $userName"

            btnAccept.setOnClickListener {
                onAcceptClick(order.orderId)
            }
        }
    }
}
