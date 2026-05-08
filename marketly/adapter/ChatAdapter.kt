package com.ramos.marketly.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ramos.marketly.R
import com.ramos.marketly.controller.ChatActivity
import com.ramos.marketly.model.ChatPreview

class ChatAdapter(private val chats: MutableList<ChatPreview>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private var view: View? = null
    private var chat: ChatPreview? = null
    private var intent: Intent? = null

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        val tvChatUsername: TextView = itemView.findViewById(R.id.tvChatUsername)
        val ivChatAdminBadge: ImageView = itemView.findViewById(R.id.ivChatAdminBadge)
        val ivChatModBadge: ImageView = itemView.findViewById(R.id.ivChatModBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view!!)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        chat = chats[position]

        holder.tvProductName.text = chat!!.productTitle
        holder.tvLastMessage.text = chat!!.lastMessage
        holder.tvChatUsername.text = chat!!.otherUsername

        if (chat!!.otherUserRole == "administrador") {
            holder.ivChatAdminBadge.visibility = View.VISIBLE
            holder.ivChatModBadge.visibility = View.GONE
        } else if (chat!!.otherUserRole == "moderador") {
            holder.ivChatModBadge.visibility = View.VISIBLE
            holder.ivChatAdminBadge.visibility = View.GONE
        } else {
            holder.ivChatAdminBadge.visibility = View.GONE
            holder.ivChatModBadge.visibility = View.GONE
        }

        if (chat!!.productImageUrl != null) {
            Glide.with(holder.itemView.context)
                .load(chat!!.productImageUrl)
                .centerCrop()
                .into(holder.ivProductImage)
        } else {
            holder.ivProductImage.setImageResource(R.drawable.marketly)
        }

        holder.itemView.setOnClickListener {
            intent = Intent(holder.itemView.context, ChatActivity::class.java)
            intent!!.putExtra("product_id", chat!!.productId)
            intent!!.putExtra("seller_id", chat!!.sellerId)
            intent!!.putExtra("product_title", chat!!.productTitle)
            intent!!.putExtra("product_seller", chat!!.sellerUsername)
            intent!!.putExtra("product_image", chat!!.productImageUrl)
            intent!!.putExtra("chat_id", chat!!.chatId)
            intent!!.putExtra("other_user_id", chat!!.otherUserId)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = chats.size

    fun setChats(newChats: List<ChatPreview>) {
        chats.clear()
        chats.addAll(newChats)
        notifyDataSetChanged()
    }
}