package com.pj.jumioassignment.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.pj.jumioassignment.databinding.FragmentPreviewBinding
import java.io.IOException
import java.io.InputStream


private const val KEY_IMAGE_URI = "image.uri"
private val TAG = PreviewFragment::class.simpleName

class PreviewFragment : Fragment() {

    lateinit var binding: FragmentPreviewBinding
    var imageUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageUri = it.getString(KEY_IMAGE_URI)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btRetry.setOnClickListener {
            activity?.onBackPressed()
        }
        imageUri?.let {
            runTextRecognition(Uri.parse(it))
        }
    }

    companion object {
        fun newInstance(uri: Uri) =
            PreviewFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_IMAGE_URI, uri.toString())
                }
            }
    }

    private fun runTextRecognition(uri: Uri) {
        val imageBitmap = handleSamplingAndRotationBitmap(uri)
        imageBitmap?.let {
            val image =
                InputImage.fromBitmap(it, 0)
            binding.ivPreview.setImageBitmap(imageBitmap)
            val recognizer: TextRecognizer = TextRecognition.getClient()
            recognizer.process(image)
                .addOnSuccessListener(
                    OnSuccessListener<Any?> { texts ->
                        processTextRecognitionResult(texts as Text)
                    })
                .addOnFailureListener(
                    OnFailureListener { e -> // Task failed with an exception
                        Log.e(TAG, "TextRecognizer binding failed", e)
                    })
        }
    }

    private fun processTextRecognitionResult(texts: Text) {
        val blocks: List<Text.TextBlock> = texts.getTextBlocks()
        if (blocks.isEmpty()) {
            showToast("No text found")
            return
        }
        binding.tvOverlayDetection.clear()
        for (i in blocks.indices) {
            val lines: List<Text.Line> = blocks[i].getLines()
            for (j in lines.indices) {
                val elements: List<Text.Element> = lines[j].getElements()
                for (k in elements.indices) {
                    val graphicGraphic: GraphicOverlay.Graphic =
                        TextGraphic(binding.tvOverlayDetection, elements[k])
                    binding.tvOverlayDetection.add(graphicGraphic)
                }
            }
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(
            context,
            text,
            Toast.LENGTH_SHORT
        ).show()
    }

    @Throws(IOException::class)
    fun handleSamplingAndRotationBitmap(selectedImage: Uri): Bitmap? {
        val MAX_HEIGHT = 1024
        val MAX_WIDTH = 1024

        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var imageStream: InputStream? =
            activity?.contentResolver?.openInputStream(selectedImage)
        BitmapFactory.decodeStream(imageStream, null, options)
        imageStream?.close()

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        imageStream = activity?.contentResolver?.openInputStream(selectedImage)
        var img = BitmapFactory.decodeStream(imageStream, null, options)
        img = img?.let { rotateImageIfRequired(it, selectedImage) }
        return img
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int, reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
            val totalPixels = (width * height).toFloat()
            val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()
            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++
            }
        }
        return inSampleSize
    }


    @Throws(IOException::class)
    private fun rotateImageIfRequired(img: Bitmap, selectedImage: Uri): Bitmap? {
        val ei = selectedImage.path?.let { ExifInterface(it) }
        ei?.let {
            val orientation: Int =
                ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
                else -> img
            }
        }
        return img
    }

    private fun rotateImage(img: Bitmap, degree: Int): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }
}