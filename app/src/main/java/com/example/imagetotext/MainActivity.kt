package com.example.imagetotext

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : AppCompatActivity() {


    private lateinit var btnCapture: Button
    private lateinit var btnSelect: Button
    private lateinit var btnCopyText: Button
    private lateinit var ivCapturedImage: ImageView
    private lateinit var tvExtractedText: TextView
    private lateinit var btnShare: Button

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_SELECT = 2
    private val REQUEST_PERMISSION_CODE = 100


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCapture = findViewById(R.id.btnCapture)
        btnSelect = findViewById(R.id.btnSelect)
        btnCopyText = findViewById(R.id.btnCopyText)
        ivCapturedImage = findViewById(R.id.ivCapturedImage)
        tvExtractedText = findViewById(R.id.tvExtractedText)
        btnShare = findViewById(R.id.btnShare)

        btnCapture.setOnClickListener {
            dispatchTakePictureIntent()
        }

        btnSelect.setOnClickListener {
            openGallery()
        }

        btnShare.setOnClickListener {
            shareText(tvExtractedText.text.toString())
        }

        btnCopyText.setOnClickListener {
            copyTextToClipboard(tvExtractedText.text.toString())
        }

    }


    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun openGallery() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also { galleryIntent ->
            startActivityForResult(galleryIntent, REQUEST_IMAGE_SELECT)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    ivCapturedImage.setImageBitmap(imageBitmap)
                    recognizeTextFromImage(imageBitmap)
                }
                REQUEST_IMAGE_SELECT -> {
                    val imageUri = data?.data
                    imageUri?.let { uri ->
                        val imageBitmap = uriToBitmap(uri)
                        ivCapturedImage.setImageBitmap(imageBitmap)
                        recognizeTextFromImage(imageBitmap)
                    }
                }
            }
        }
        /*btnShare.visibility = Button.VISIBLE
        btnCopyText.visibility = Button.VISIBLE*/
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }

    private fun recognizeTextFromImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                tvExtractedText.text = visionText.text
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
        btnShare.visibility = Button.VISIBLE
        btnCopyText.visibility = Button.VISIBLE
    }

    private fun copyTextToClipboard(text: String) {
        if (TextUtils.isEmpty(text)) return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = android.content.ClipData.newPlainText("Extracted Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this@MainActivity,"Text Copied To Clipboard",Toast.LENGTH_SHORT).show();
    }

    private fun shareText(text: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }


}