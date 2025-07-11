package com.example.PalaTos.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.PalaTos.Model.Order
import com.example.PalaTos.R

class OrderAdapter(private val orders: List<Order>, private val onPayClick: (Order) -> Unit) :
    RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.order_item, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val orderIdTextView: TextView = itemView.findViewById(R.id.orderIdTextView)
        private val tableNumberTextView: TextView = itemView.findViewById(R.id.tableNumberTextView)
        private val waiterNameTextView: TextView = itemView.findViewById(R.id.waiterNameTextView)
        private val totalPriceTextView: TextView = itemView.findViewById(R.id.totalPriceTextView)
        private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        private val itemsTextView: TextView = itemView.findViewById(R.id.itemsTextView)
        private val payButton: Button = itemView.findViewById(R.id.payButton)
        private val timeOfOrderTextView: TextView = itemView.findViewById(R.id.timeOfOrderTextView)
        private val subtotalTextView: TextView = itemView.findViewById(R.id.subtotalTextView)
        private val serviceTextView: TextView = itemView.findViewById(R.id.serviceTextView)
        private val estimatedTimeTextView: TextView = itemView.findViewById(R.id.estimatedTimeTextView)

        fun bind(order: Order) {
            orderIdTextView.text = "Order ID: ${order.orderId}"
            tableNumberTextView.text = "Table: ${order.tableNumber}"
            waiterNameTextView.text = "Waiter: ${order.waiter}"
            subtotalTextView.text = "Subtotal: %.2f PLN".format(order.subtotal.toDoubleOrNull() ?: 0.0)
            serviceTextView.text = "Service: %.2f PLN".format(order.service.toDoubleOrNull() ?: 0.0)
            totalPriceTextView.text = "Total: %.2f PLN".format(order.total.toDoubleOrNull() ?: 0.0)
            statusTextView.text = "Status: ${order.status}"
            timeOfOrderTextView.text = "Time: ${order.timeOfOrder}"
            estimatedTimeTextView.text = "Estimated Time: ${order.estimatedTime} min"

            val itemsDescription = order.items.joinToString("\n") { "${it.name} x ${it.quantity} - ${it.price}" }
            itemsTextView.text = itemsDescription

            payButton.apply {
                visibility = if (order.status == "Pending") View.VISIBLE else View.GONE
                setOnClickListener {
                    order.status = "Paid"
                    notifyItemChanged(adapterPosition)

                    onPayClick(order)
                }
            }
        }
    }
}
