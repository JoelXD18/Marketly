package com.ramos.marketly.controller

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.ramos.marketly.R
import com.ramos.marketly.model.Product
import com.ramos.marketly.utils.SessionManager
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.util.UUID

class PublishProductActivity : AppCompatActivity() {

    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnAddImage: CardView
    private lateinit var btnAddFile: CardView
    private lateinit var tvFileName: TextView
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnPublish: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var llImageContainer: LinearLayout

    private val selectedImages = mutableListOf<Uri>()
    private var selectedFileUri: Uri? = null

    private var categoriesList: List<String>? = null
    private var categoryAdapter: ArrayAdapter<String>? = null
    private var intentObj: Intent? = null
    private var titleStr: String? = null
    private var descriptionStr: String? = null
    private var priceStr: String? = null
    private var categoryStr: String? = null
    private var uriObj: Uri? = null
    private var fileNameStr: String? = null
    private var imageView: ImageView? = null
    private var params: LinearLayout.LayoutParams? = null
    private var nameStr: String? = null
    private var cursor: android.database.Cursor? = null
    private var index: Int? = null
    private var sellerIdStr: String? = null
    private var imageUrlsList: MutableList<String>? = null
    private var bytes: ByteArray? = null
    private var urlStr: String? = null
    private var fileUriObj: Uri? = null
    private var fileBytes: ByteArray? = null
    private var fileUrlStr: String? = null

    private val REQUEST_IMAGE = 1001
    private val REQUEST_FILE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publish_product)

        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etPrice = findViewById(R.id.etPrice)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        btnAddImage = findViewById(R.id.btnAddImage)
        btnAddFile = findViewById(R.id.btnAddFile)
        tvFileName = findViewById(R.id.tvFileName)
        btnCancel = findViewById(R.id.btnCancel)
        btnPublish = findViewById(R.id.btnPublish)
        btnBack = findViewById(R.id.btnBack)
        llImageContainer = findViewById(R.id.llImageContainer)

        categoriesList = listOf("Software", "E-Books", "Cursos", "Plantillas", "Gráficos", "Música", "Videojuegos", "Otros")
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoriesList!!)
        categoryAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        btnBack.setOnClickListener {
            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnAddImage.setOnClickListener {
            if (selectedImages.size >= 5) {
                Toast.makeText(this, "Máximo 5 imágenes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            intentObj = Intent(Intent.ACTION_PICK)
            intentObj!!.type = "image/*"
            startActivityForResult(intentObj!!, REQUEST_IMAGE)
        }

        btnAddFile.setOnClickListener {
            intentObj = Intent(Intent.ACTION_GET_CONTENT)
            intentObj!!.type = "*/*"
            startActivityForResult(intentObj!!, REQUEST_FILE)
        }

        btnPublish.setOnClickListener {
            titleStr = etTitle.text.toString().trim()
            descriptionStr = etDescription.text.toString().trim()
            priceStr = etPrice.text.toString().trim()
            categoryStr = spinnerCategory.selectedItem.toString()

            if (validateInputs(titleStr!!, descriptionStr!!, priceStr!!)) {
                lifecycleScope.launch {
                    publishProduct(titleStr!!, descriptionStr!!, priceStr!!.toDouble(), categoryStr!!)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE) {
                uriObj = data.data ?: return
                selectedImages.add(uriObj!!)
                addImagePreview(uriObj!!)
            } else if (requestCode == REQUEST_FILE) {
                selectedFileUri = data.data
                fileNameStr = getFileName(selectedFileUri!!)
                tvFileName.text = fileNameStr
            }
        }
    }

    private fun addImagePreview(uri: Uri) {
        imageView = ImageView(this)
        params = LinearLayout.LayoutParams(80.dpToPx(), 80.dpToPx())
        params!!.marginEnd = 8.dpToPx()
        imageView!!.layoutParams = params
        imageView!!.scaleType = ImageView.ScaleType.CENTER_CROP

        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(imageView!!)

        // Insertar antes del botón de añadir
        llImageContainer.addView(imageView, llImageContainer.childCount - 1)
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun getFileName(uri: Uri): String {
        nameStr = "archivo"
        cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor != null && cursor!!.moveToFirst()) {
            index = cursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index!! >= 0) nameStr = cursor!!.getString(index!!)
            cursor!!.close()
        }
        return nameStr!!
    }

    private fun validateInputs(title: String, description: String, price: String): Boolean {
        if (title.isEmpty()) {
            Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
            return false
        }
        if (description.isEmpty()) {
            Toast.makeText(this, "La descripción es obligatoria", Toast.LENGTH_SHORT).show()
            return false
        }
        if (price.isEmpty()) {
            Toast.makeText(this, "El precio es obligatorio", Toast.LENGTH_SHORT).show()
            return false
        }
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Añade al menos una imagen", Toast.LENGTH_SHORT).show()
            return false
        }
        if (selectedFileUri == null) {
            Toast.makeText(this, "Añade el archivo digital", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private suspend fun publishProduct(title: String, description: String, price: Double, category: String) {
        try {
            btnPublish.isEnabled = false
            Toast.makeText(this, "Publicando...", Toast.LENGTH_SHORT).show()

            sellerIdStr = SessionManager.getUser()?.id
                ?: throw Exception("No hay usuario autenticado")

            // 1. Subir imágenes
            imageUrlsList = mutableListOf<String>()
            for (imageUri in selectedImages) {
                fileNameStr = "${UUID.randomUUID()}.jpg"
                bytes = contentResolver.openInputStream(imageUri)?.readBytes()
                    ?: continue
                SupabaseClient.client.storage
                    .from("product-images")
                    .upload(fileNameStr!!, bytes!!)
                urlStr = SupabaseClient.client.storage
                    .from("product-images")
                    .publicUrl(fileNameStr!!)
                imageUrlsList!!.add(urlStr!!)
            }

            // 2. Subir archivo digital
            fileUriObj = selectedFileUri!!
            fileNameStr = "${UUID.randomUUID()}_${getFileName(fileUriObj!!)}"
            fileBytes = contentResolver.openInputStream(fileUriObj!!)?.readBytes()
                ?: throw Exception("No se pudo leer el archivo")
            SupabaseClient.client.storage
                .from("product-files")
                .upload(fileNameStr!!, fileBytes!!)
            fileUrlStr = SupabaseClient.client.storage
                .from("product-files")
                .publicUrl(fileNameStr!!)

            // 3. Guardar producto en BD
            SupabaseClient.client.from("products").insert(
                Product(
                    id = UUID.randomUUID().toString(),
                    sellerId = sellerIdStr!!,
                    title = title,
                    description = description,
                    price = price,
                    productType = category,
                    fileUrl = fileUrlStr,
                    imageUrls = imageUrlsList,
                    status = "activo"
                )
            )

            Log.d("PublishProduct", " Producto publicado correctamente")
            runOnUiThread {
                Toast.makeText(this, "¡Producto publicado!", Toast.LENGTH_SHORT).show()
                finish()
            }

        } catch (e: Exception) {
            Log.e("PublishProduct", " Error: ${e.message}")
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                btnPublish.isEnabled = true
            }
        }
    }
}