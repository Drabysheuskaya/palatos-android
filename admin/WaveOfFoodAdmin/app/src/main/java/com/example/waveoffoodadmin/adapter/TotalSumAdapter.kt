package com.example.waveoffoodadmin.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.waveoffoodadmin.R
import com.example.waveoffoodadmin.model.OrderModel

class TotalSumAdapter(
    private val orders: List<OrderModel>,
    private val fetchUserName: (String, (String) -> Unit) -> Unit,
    private val fetchWaiterName: (String, (String) -> Unit) -> Unit,
    private val restaurantKey: String
) : RecyclerView.Adapter<TotalSumAdapter.TotalSumViewHolder>() {

    companion object {
        private const val TAG = "TotalSumAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TotalSumViewHolder {
        Log.d(TAG, "onCreateViewHolder called")
        val view = LayoutInflater.from(parent.context).inflate(R.layout._sum_item_layout, parent, false)
        return TotalSumViewHolder(view)
    }

    override fun onBindViewHolder(holder: TotalSumViewHolder, position: Int) {
        val order = orders[position]
        Log.d(TAG, "onBindViewHolder called for position $position with orderId: ${order.orderId}")

        fetchUserName(order.userId) { userName ->
            Log.d(TAG, "Fetched userName: $userName for userId: ${order.userId}")
            val tableNumberStr = order.tableNumber?.toString() ?: "Unknown Table"
            fetchWaiterName(tableNumberStr) { waiterName ->
                Log.d(TAG, "Fetched waiterName: $waiterName for tableNumber: $tableNumberStr")
                holder.bind(order, userName, waiterName)
            }
        }
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount called, total items: ${orders.size}")
        return orders.size
    }

    inner class TotalSumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderTime: TextView = itemView.findViewById(R.id.tvOrderTime)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvOrderDetails: TextView = itemView.findViewById(R.id.tvOrderDetails)
        private val tvItems: TextView = itemView.findViewById(R.id.tvItems)
        private val tvSubtotal: TextView = itemView.findViewById(R.id.tvSubtotal)
        private val tvService: TextView = itemView.findViewById(R.id.tvService)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)
        private val tvProgressStatus: TextView = itemView.findViewById(R.id.tvProgressStatus)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(order: OrderModel, userName: String, waiterName: String) {
            Log.d(TAG, "Binding data for orderId: ${order.orderId}")
            tvUserName.text = "User: $userName"
            tvOrderTime.text = "Order Time: ${order.timeOfOrder}"
            tvOrderDetails.text = "Order ID: ${order.orderId} | Table: ${order.tableNumber} | Waiter: $waiterName"
            tvItems.text = order.items.joinToString("\n") { "• ${it.name} x ${it.quantity} - ${it.price}" }
            tvSubtotal.text = "Subtotal: ${order.subtotal} PLN"
            tvService.text = "Service: ${order.service} PLN"
            tvTotal.text = "Total: ${order.total} PLN"
            tvProgressStatus.text = "Progress: ${order.progress_status}"
            tvStatus.text = "Status: ${order.status}"
        }
    }
}
