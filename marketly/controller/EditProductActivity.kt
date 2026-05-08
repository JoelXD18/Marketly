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
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.ramos.marketly.R
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

class EditProductActivity : AppCompatActivity() {

    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnAddImage: androidx.cardview.widget.CardView
    private lateinit var btnAddFile: androidx.cardview.widget.CardView
    private lateinit var tvFileName: TextView
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var llImageContainer: LinearLayout

    private val selectedImages = mutableListOf<Uri>()
    private var selectedFileUri: Uri? = null
    private var existingImageUrls = mutableListOf<String>()
    private var existingFileUrl: String? = null
    private var productId: String? = null

    private var categoryAdapter: ArrayAdapter<String>? = null
    private var intentObj: Intent? = null
    private var titleStr: String? = null
    private var descriptionStr: String? = null
    private var priceStr: String? = null
    private var product: ProductData? = null
    private var categoryIndex: Int? = null
    private var uri: Uri? = null
    private var imageView: ImageView? = null
    private var params: LinearLayout.LayoutParams? = null
    private var nameStr: String? = null
    private var cursor: android.database.Cursor? = null
    private var index: Int? = null
    private var finalImageUrls: MutableList<String>? = null
    private var fileName: String? = null
    private var bytes: ByteArray? = null
    private var url: String? = null
    private var finalFileUrl: String? = null
    private var fileBytes: ByteArray? = null

    private val REQUEST_IMAGE = 1001
    private val REQUEST_FILE = 1002

    private val categories = listOf("Software", "E-Books", "Cursos", "Plantillas", "Gráficos", "Música", "Videojuegos", "Otros")

    @Serializable
    data class ProductData(
        val id: String,
        val title: String,
        val description: String,
        val price: Double,
        val product_type: String,
        val file_url: String? = null,
        val image_urls: List<String>? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etPrice = findViewById(R.id.etPrice)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        btnAddImage = findViewById(R.id.btnAddImage)
        btnAddFile = findViewById(R.id.btnAddFile)
        tvFileName = findViewById(R.id.tvFileName)
        btnCancel = findViewById(R.id.btnCancel)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)
        llImageContainer = findViewById(R.id.llImageContainer)

        productId = intent.getStringExtra("product_id")
            ?: run { finish(); return }

        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        btnBack.setOnClickListener { finish() }
        btnCancel.setOnClickListener { finish() }

        btnAddImage.setOnClickListener {
            if (existingImageUrls.size + selectedImages.size >= 5) {
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

        btnSave.setOnClickListener {
            titleStr = etTitle.text.toString().trim()
            descriptionStr = etDescription.text.toString().trim()
            priceStr = etPrice.text.toString().trim()

            if (titleStr!!.isEmpty()) {
                Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (descriptionStr!!.isEmpty()) {
                Toast.makeText(this, "La descripción es obligatoria", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (priceStr!!.isEmpty()) {
                Toast.makeText(this, "El precio es obligatorio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                saveProduct(titleStr!!, descriptionStr!!, priceStr!!.toDouble(), spinnerCategory.selectedItem.toString())
            }
        }

        lifecycleScope.launch {
            loadProduct(productId!!)
        }
    }

    private suspend fun loadProduct(productId: String) {
        try {
            product = SupabaseClient.client
                .from("products")
                .select(Columns.raw("id, title, description, price, product_type, file_url, image_urls")) {
                    filter {
                        eq("id", productId)
                    }
                }
                .decodeSingle<ProductData>()

            runOnUiThread {
                etTitle.setText(product!!.title)
                etDescription.setText(product!!.description)
                etPrice.setText(product!!.price.toString())

                // Seleccionar categoría en el spinner
                categoryIndex = categories.indexOf(product!!.product_type)
                if (categoryIndex!! >= 0) spinnerCategory.setSelection(categoryIndex!!)

                // Cargar imágenes existentes
                existingImageUrls = (product!!.image_urls ?: emptyList()).toMutableList()
                existingFileUrl = product!!.file_url

                for (url in existingImageUrls) {
                    addImagePreviewFromUrl(url)
                }

                if (!product!!.file_url.isNullOrEmpty()) {
                    tvFileName.text = "Archivo actual cargado"
                }
            }

        } catch (e: Exception) {
            Log.e("EditProduct", " Error cargando producto: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE) {
                uri = data.data ?: return
                selectedImages.add(uri!!)
                addImagePreviewFromUri(uri!!)
            } else if (requestCode == REQUEST_FILE) {
                selectedFileUri = data.data
                tvFileName.text = getFileName(selectedFileUri!!)
            }
        }
    }

    private fun addImagePreviewFromUrl(url: String) {
        imageView = ImageView(this)
        params = LinearLayout.LayoutParams(80.dpToPx(), 80.dpToPx())
        params!!.marginEnd = 8.dpToPx()
        imageView!!.layoutParams = params
        imageView!!.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(this).load(url).centerCrop().into(imageView!!)
        llImageContainer.addView(imageView, llImageContainer.childCount - 1)
    }

    private fun addImagePreviewFromUri(uri: Uri) {
        imageView = ImageView(this)
        params = LinearLayout.LayoutParams(80.dpToPx(), 80.dpToPx())
        params!!.marginEnd = 8.dpToPx()
        imageView!!.layoutParams = params
        imageView!!.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(this).load(uri).centerCrop().into(imageView!!)
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

    @Serializable
    data class ProductUpdate(
        val title: String,
        val description: String,
        val price: Double,
        val product_type: String,
        val image_urls: List<String>,
        val file_url: String? = null
    )

    private suspend fun saveProduct(title: String, description: String, price: Double, category: String) {
        try {
            btnSave.isEnabled = false
            Toast.makeText(this, "Guardando...", Toast.LENGTH_SHORT).show()

            // 1. Subir nuevas imágenes si las hay
            finalImageUrls = existingImageUrls.toMutableList()
            for (imageUri in selectedImages) {
                fileName = "${UUID.randomUUID()}.jpg"
                bytes = contentResolver.openInputStream(imageUri)?.readBytes() ?: continue
                SupabaseClient.client.storage.from("product-images").upload(fileName!!, bytes!!)
                url = SupabaseClient.client.storage.from("product-images").publicUrl(fileName!!)
                finalImageUrls!!.add(url!!)
            }

            // 2. Subir nuevo archivo si se seleccionó uno
            finalFileUrl = if (selectedFileUri != null) {
                fileName = "${UUID.randomUUID()}_${getFileName(selectedFileUri!!)}"
                fileBytes = contentResolver.openInputStream(selectedFileUri!!)?.readBytes()
                    ?: throw Exception("No se pudo leer el archivo")
                SupabaseClient.client.storage.from("product-files").upload(fileName!!, fileBytes!!)
                SupabaseClient.client.storage.from("product-files").publicUrl(fileName!!)
            } else {
                existingFileUrl
            }

            // 3. Actualizar producto en BD
            SupabaseClient.client.from("products").update(
                ProductUpdate(
                    title = title,
                    description = description,
                    price = price,
                    product_type = category,
                    image_urls = finalImageUrls!!,
                    file_url = finalFileUrl
                )
            ) {
                filter {
                    eq("id", productId!!)
                }
            }

            Log.d("EditProduct", " Producto actualizado")
            runOnUiThread {
                Toast.makeText(this, "¡Producto actualizado!", Toast.LENGTH_SHORT).show()
                finish()
            }

        } catch (e: Exception) {
            Log.e("EditProduct", " Error: ${e.message}")
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                btnSave.isEnabled = true
            }
        }
    }
}