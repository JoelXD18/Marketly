package com.ramos.marketly.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.ramos.marketly.R
import com.ramos.marketly.controller.RateActivity
import com.ramos.marketly.model.Message
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MessageAdapter(
    private val messages: MutableList<Message>,
    private val currentUserId: String,
    private val userRoles: Map<String, String> = emptyMap(),
    private val usernames: Map<String, String> = emptyMap(),
    private val sellerId: String = "",
    private val buyerId: String = ""
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var view: View? = null
    private var message: Message? = null
    private var time: String? = null
    private var isFile: Boolean? = null
    private var isOrderCompleted: Boolean? = null
    private var isIncidenceResolved: Boolean? = null
    private var isIncidenceOpened: Boolean? = null
    private var isOrderReminder: Boolean? = null
    private var orderIdFromMsg: String? = null
    private var deadline: String? = null
    private var instant: Instant? = null
    private var senderUsername: String? = null
    private var senderRole: String? = null
    private var result: String? = null
    private var parts: List<String>? = null
    private var intent: Intent? = null


    private val deadlineFormatter = DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault())

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val llReceived: LinearLayout = itemView.findViewById(R.id.llReceived)
        val llSent: LinearLayout = itemView.findViewById(R.id.llSent)
        val tvReceivedSender: TextView = itemView.findViewById(R.id.tvReceivedSender)
        val tvReceivedMessage: TextView = itemView.findViewById(R.id.tvReceivedMessage)
        val tvReceivedTime: TextView = itemView.findViewById(R.id.tvReceivedTime)
        val tvSentMessage: TextView = itemView.findViewById(R.id.tvSentMessage)
        val tvSentTime: TextView = itemView.findViewById(R.id.tvSentTime)
        val llFileReceived: LinearLayout = itemView.findViewById(R.id.llFileReceived)
        val llFileSent: LinearLayout = itemView.findViewById(R.id.llFileSent)
        val tvFileNameReceived: TextView = itemView.findViewById(R.id.tvFileNameReceived)
        val tvFileNameSent: TextView = itemView.findViewById(R.id.tvFileNameSent)
        val ivSenderAdminBadge: ImageView = itemView.findViewById(R.id.ivSenderAdminBadge)
        val ivSenderModBadge: ImageView = itemView.findViewById(R.id.ivSenderModBadge)
        val cardSpecial: CardView = itemView.findViewById(R.id.cardSpecial)
        val tvSpecialTitle: TextView = itemView.findViewById(R.id.tvSpecialTitle)
        val tvSpecialDescription: TextView = itemView.findViewById(R.id.tvSpecialDescription)
        val btnRateUser1: MaterialButton = itemView.findViewById(R.id.btnRateUser1)
        val btnRateUser2: MaterialButton = itemView.findViewById(R.id.btnRateUser2)
        val cardIncidenceOpened: CardView = itemView.findViewById(R.id.cardIncidenceOpened)
        val tvIncidenceDescription: TextView = itemView.findViewById(R.id.tvIncidenceDescription)
        val cardOrderReminder: CardView = itemView.findViewById(R.id.cardOrderReminder)
        val tvDeadline: TextView = itemView.findViewById(R.id.tvDeadline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view!!)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        message = messages[position]
        time = formatTime(message!!.createdAt)
        isFile = message!!.messageType == "file"
        isOrderCompleted = message!!.messageType == "order_completed"
        isIncidenceResolved = message!!.messageType == "incidence_resolved"
        isIncidenceOpened = message!!.messageType == "incidence_opened"
        isOrderReminder = message!!.messageType == "order_reminder"
        orderIdFromMsg = message!!.orderId ?: ""

        // Ocultar todas las vistas por defecto antes de decidir cual mostrar
        holder.llSent.visibility = View.GONE
        holder.llReceived.visibility = View.GONE
        holder.cardSpecial.visibility = View.GONE
        holder.cardIncidenceOpened.visibility = View.GONE
        holder.cardOrderReminder.visibility = View.GONE

        if (isOrderReminder!!) {
            bindOrderReminder(holder, message!!)
        } else if (isIncidenceOpened!!) {
            bindIncidenceOpened(holder, message!!)
        } else if (isOrderCompleted!! || isIncidenceResolved!!) {
            bindSpecialMessage(holder, message!!, isOrderCompleted!!, orderIdFromMsg!!)
        } else if (message!!.senderId == currentUserId) {
            bindSentMessage(holder, message!!, isFile!!, time!!)
        } else {
            bindReceivedMessage(holder, message!!, isFile!!, time!!)
        }
    }

    // Muestra el recuadro azul con el plazo de 48h y la fecha limite formateada
    private fun bindOrderReminder(holder: MessageViewHolder, message: Message) {
        deadline = message.extraData ?: ""
        holder.cardOrderReminder.visibility = View.VISIBLE
        if (deadline!!.isNotEmpty()) {
            try {
                instant = Instant.parse(deadline)
                holder.tvDeadline.text = "Plazo maximo: ${deadlineFormatter.format(instant)}"
            } catch (e: Exception) {
                holder.tvDeadline.text = "Plazo maximo: $deadline"
            }
        }
    }

    // Muestra el recuadro amarillo con la descripcion de la incidencia abierta
    private fun bindIncidenceOpened(holder: MessageViewHolder, message: Message) {
        holder.cardIncidenceOpened.visibility = View.VISIBLE
        holder.tvIncidenceDescription.text = message.extraData ?: ""
    }

    // Muestra el recuadro verde o amarillo segun si el pedido fue completado o la incidencia resuelta
    private fun bindSpecialMessage(
        holder: MessageViewHolder,
        message: Message,
        isOrderCompleted: Boolean,
        orderIdFromMsg: String
    ) {
        holder.cardSpecial.visibility = View.VISIBLE

        if (isOrderCompleted) {
            holder.tvSpecialTitle.text = "Pedido completado"
            holder.tvSpecialDescription.visibility = View.GONE
            holder.cardSpecial.setCardBackgroundColor(0xFFF0FDF4.toInt())
            holder.tvSpecialTitle.setTextColor(0xFF065F46.toInt())
        } else {
            holder.tvSpecialTitle.text = "Incidencia resuelta"
            holder.tvSpecialDescription.visibility = View.VISIBLE
            holder.tvSpecialDescription.text = message.extraData ?: ""
            holder.cardSpecial.setCardBackgroundColor(0xFFFEF3C7.toInt())
            holder.tvSpecialTitle.setTextColor(0xFF92400E.toInt())
            holder.tvSpecialDescription.setTextColor(0xFF92400E.toInt())
        }

        // Mostrar boton de valorar al vendedor solo si el usuario actual es el comprador
        if (currentUserId == buyerId && sellerId.isNotEmpty()) {
            holder.btnRateUser1.visibility = View.VISIBLE
            holder.btnRateUser1.text = "Valorar vendedor"
            holder.btnRateUser1.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    intent = Intent(holder.itemView.context, RateActivity::class.java)
                    intent!!.putExtra("order_id", orderIdFromMsg)
                    intent!!.putExtra("reviewed_id", sellerId)
                    intent!!.putExtra("reviewed_label", "vendedor")
                    holder.itemView.context.startActivity(intent)
                    holder.btnRateUser1.visibility = View.GONE
                }
            })
        } else {
            holder.btnRateUser1.visibility = View.GONE
        }

        // Mostrar boton de valorar al comprador solo si el usuario actual es el vendedor
        if (currentUserId == sellerId && buyerId.isNotEmpty()) {
            holder.btnRateUser2.visibility = View.VISIBLE
            holder.btnRateUser2.text = "Valorar comprador"
            holder.btnRateUser2.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    intent = Intent(holder.itemView.context, RateActivity::class.java)
                    intent!!.putExtra("order_id", orderIdFromMsg)
                    intent!!.putExtra("reviewed_id", buyerId)
                    intent!!.putExtra("reviewed_label", "comprador")
                    holder.itemView.context.startActivity(intent)
                    holder.btnRateUser2.visibility = View.GONE
                }
            })
        } else {
            holder.btnRateUser2.visibility = View.GONE
        }
    }

    private fun bindSentMessage(
        holder: MessageViewHolder,
        message: Message,
        isFile: Boolean,
        time: String
    ) {
        holder.llSent.visibility = View.VISIBLE
        if (isFile) {
            holder.tvSentMessage.visibility = View.GONE
            holder.llFileSent.visibility = View.VISIBLE
            holder.tvFileNameSent.text = "Descargar archivo"
            holder.llFileSent.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.message))
                    holder.itemView.context.startActivity(intent)
                }
            })
        } else {
            holder.tvSentMessage.visibility = View.VISIBLE
            holder.llFileSent.visibility = View.GONE
            holder.tvSentMessage.text = message.message
        }
        holder.tvSentTime.text = time
    }

    private fun bindReceivedMessage(
        holder: MessageViewHolder,
        message: Message,
        isFile: Boolean,
        time: String
    ) {
        senderUsername = usernames[message.senderId] ?: ""
        senderRole = userRoles[message.senderId] ?: "cliente"

        holder.llReceived.visibility = View.VISIBLE
        holder.tvReceivedSender.text = senderUsername

        if (senderRole == "administrador") {
            holder.ivSenderAdminBadge.visibility = View.VISIBLE
            holder.ivSenderModBadge.visibility = View.GONE
        } else if (senderRole == "moderador") {
            holder.ivSenderModBadge.visibility = View.VISIBLE
            holder.ivSenderAdminBadge.visibility = View.GONE
        } else {
            holder.ivSenderAdminBadge.visibility = View.GONE
            holder.ivSenderModBadge.visibility = View.GONE
        }

        if (isFile) {
            holder.tvReceivedMessage.visibility = View.GONE
            holder.llFileReceived.visibility = View.VISIBLE
            holder.tvFileNameReceived.text = "Descargar archivo"
            holder.llFileReceived.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.message))
                    holder.itemView.context.startActivity(intent)
                }
            })
        } else {
            holder.tvReceivedMessage.visibility = View.VISIBLE
            holder.llFileReceived.visibility = View.GONE
            holder.tvReceivedMessage.text = message.message
        }
        holder.tvReceivedTime.text = time
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun setMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    private fun formatTime(createdAt: String?): String {
        if (createdAt == null) {
            result = ""
        } else {
            try {
                parts = createdAt.split("T")
                result = if (parts!!.size >= 2) parts!![1].substring(0, 5) else ""
            } catch (e: Exception) {
                return ""
            }
        }
        return result!!
    }
}