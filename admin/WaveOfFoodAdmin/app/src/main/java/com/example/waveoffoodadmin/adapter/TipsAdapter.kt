package com.example.waveoffoodadmin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.waveoffoodadmin.databinding.ItemTipBinding
import com.example.waveoffoodadmin.model.Tip

class TipsAdapter(
    private val context: Context,
    private val tipsList: List<Tip>
) : RecyclerView.Adapter<TipsAdapter.TipViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val binding = ItemTipBinding.inflate(LayoutInflater.from(context), parent, false)
        return TipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        holder.bind(tipsList[position])
    }

    override fun getItemCount(): Int = tipsList.size

    inner class TipViewHolder(private val binding: ItemTipBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tip: Tip) {
            binding.userName.text = "User: ${tip.userName}"
            binding.orderId.text = "Order #${tip.orderId}"
            binding.totalPrice.text = "Total: ${tip.totalPrice} PLN"
            binding.tableNumber.text = "Table: ${tip.tableNumber}"
            binding.waiterName.text = "Waiter: ${tip.waiterName}"
            binding.timeOfOrder.text = "Time: ${tip.timeOfOrder}"
            binding.tipText.text = "Tip:"
            binding.tipAmount.text = "${tip.amount} PLN"
        }
    }
}
