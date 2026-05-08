package com.ramos.marketly.controller

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.ramos.marketly.R
import com.ramos.marketly.model.Rating
import com.ramos.marketly.utils.SessionManager
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.util.UUID
import io.github.jan.supabase.postgrest.query.Columns

class RateActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvRateSubtitle: TextView
    private lateinit var tvRatingLabel: TextView
    private lateinit var tvStar1: TextView
    private lateinit var tvStar2: TextView
    private lateinit var tvStar3: TextView
    private lateinit var tvStar4: TextView
    private lateinit var tvStar5: TextView
    private lateinit var etComment: TextInputEditText
    private lateinit var btnSubmit: MaterialButton

    private var selectedRating = 0
    private var orderId: String? = null
    private var reviewedId: String? = null
    private var reviewedLabel: String = "usuario"

    private var starsList: List<TextView>? = null
    private var currentUserIdStr: String? = null
    private var oid: String? = null
    private var rid: String? = null
    private var existingList: List<Rating>? = null
    private var commentStr: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rate)

        btnBack = findViewById(R.id.btnBack)
        tvRateSubtitle = findViewById(R.id.tvRateSubtitle)
        tvRatingLabel = findViewById(R.id.tvRatingLabel)
        tvStar1 = findViewById(R.id.tvStar1)
        tvStar2 = findViewById(R.id.tvStar2)
        tvStar3 = findViewById(R.id.tvStar3)
        tvStar4 = findViewById(R.id.tvStar4)
        tvStar5 = findViewById(R.id.tvStar5)
        etComment = findViewById(R.id.etComment)
        btnSubmit = findViewById(R.id.btnSubmit)

        orderId = intent.getStringExtra("order_id")
        reviewedId = intent.getStringExtra("reviewed_id")
        reviewedLabel = intent.getStringExtra("reviewed_label") ?: "usuario"

        tvRateSubtitle.text = "Valora al $reviewedLabel"

        btnBack.setOnClickListener { finish() }

        starsList = listOf(tvStar1, tvStar2, tvStar3, tvStar4, tvStar5)

        for (i in 0 until starsList!!.size) {
            val star = starsList!![i]
            star.setOnClickListener(object : android.view.View.OnClickListener {
                override fun onClick(v: android.view.View?) {
                    selectedRating = i + 1
                    updateStars(starsList!!, selectedRating)
                    if (selectedRating == 1) {
                        tvRatingLabel.text = "Muy malo"
                    } else if (selectedRating == 2) {
                        tvRatingLabel.text = "Malo"
                    } else if (selectedRating == 3) {
                        tvRatingLabel.text = "Regular"
                    } else if (selectedRating == 4) {
                        tvRatingLabel.text = "Bueno"
                    } else if (selectedRating == 5) {
                        tvRatingLabel.text = "Excelente"
                    } else {
                        tvRatingLabel.text = "Selecciona una puntuación"
                    }
                }
            })
        }

        btnSubmit.setOnClickListener(object : android.view.View.OnClickListener {
            override fun onClick(v: android.view.View?) {
                if (selectedRating == 0) {
                    Toast.makeText(this@RateActivity, "Selecciona una puntuación", Toast.LENGTH_SHORT).show()
                    return
                }
                lifecycleScope.launch {
                    submitRating()
                }
            }
        })
    }

    private fun updateStars(stars: List<TextView>, rating: Int) {
        for (i in 0 until stars.size) {
            val star = stars[i]
            if (i < rating) {
                star.text = ""
            } else {
                star.text = ""
            }
        }
    }

    private suspend fun submitRating() {
        try {
            btnSubmit.isEnabled = false

            currentUserIdStr = SessionManager.getUser()?.id
                ?: throw Exception("No hay usuario autenticado")
            oid = orderId ?: throw Exception("No hay orden")
            rid = reviewedId ?: throw Exception("No hay usuario a valorar")

            // Comprobar si ya valoró
            existingList = SupabaseClient.client
                .from("ratings")
                .select(Columns.raw("id, order_id, reviewer_id, reviewed_id, rating")) {
                    filter {
                        eq("order_id", oid!!)
                        eq("reviewer_id", currentUserIdStr!!)
                        eq("reviewed_id", rid!!)
                    }
                }
                .decodeList<Rating>()

            if (existingList!!.isNotEmpty()) {
                runOnUiThread {
                    Toast.makeText(this, "Ya has valorado a este usuario", Toast.LENGTH_SHORT).show()
                    btnSubmit.isEnabled = true
                }
                return
            }

            commentStr = etComment.text.toString().trim()

            SupabaseClient.client.from("ratings").insert(
                Rating(
                    id = UUID.randomUUID().toString(),
                    orderId = oid!!,
                    reviewerId = currentUserIdStr!!,
                    reviewedId = rid!!,
                    rating = selectedRating,
                    comment = if (commentStr!!.isEmpty()) null else commentStr
                )
            )

            Log.d("RateActivity", " Valoración enviada")
            runOnUiThread {
                Toast.makeText(this, "¡Valoración enviada!", Toast.LENGTH_SHORT).show()
                finish()
            }

        } catch (e: Exception) {
            Log.e("RateActivity", " Error: ${e.message}")
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                btnSubmit.isEnabled = true
            }
        }
    }
}