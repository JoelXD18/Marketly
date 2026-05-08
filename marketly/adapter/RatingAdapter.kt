package com.ramos.marketly.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ramos.marketly.R
import com.ramos.marketly.model.RatingItem

class RatingAdapter(private val ratings: MutableList<RatingItem>) :
    RecyclerView.Adapter<RatingAdapter.RatingViewHolder>() {

    private var view: View? = null
    private var ratingItem: RatingItem? = null

    class RatingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvReviewer: TextView = itemView.findViewById(R.id.tvReviewer)
        val tvStars: TextView = itemView.findViewById(R.id.tvStars)
        val tvComment: TextView = itemView.findViewById(R.id.tvComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RatingViewHolder {
        view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rating, parent, false)
        return RatingViewHolder(view!!)
    }

    override fun onBindViewHolder(holder: RatingViewHolder, position: Int) {
        ratingItem = ratings[position]

        holder.tvReviewer.text = ratingItem!!.reviewerUsername
        holder.tvStars.text = "".repeat(ratingItem!!.rating) + "".repeat(5 - ratingItem!!.rating)

        if (!ratingItem!!.comment.isNullOrEmpty()) {
            holder.tvComment.visibility = View.VISIBLE
            holder.tvComment.text = ratingItem!!.comment
        } else {
            holder.tvComment.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = ratings.size

    fun addRatings(newRatings: List<RatingItem>) {
        ratings.addAll(newRatings)
        notifyDataSetChanged()
    }

    fun setRatings(newRatings: List<RatingItem>) {
        ratings.clear()
        ratings.addAll(newRatings)
        notifyDataSetChanged()
    }
}