package com.yarnspace.app.core.util

import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File

class PhotoPickerHelper(
    private val fragment: Fragment,
    private val onImagePicked: (Uri?) -> Unit
) {

    private var currentPhotoUri: Uri? = null

    private val takePictureLauncher = fragment.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhotoUri != null) {
            onImagePicked(currentPhotoUri)
        } else {
            currentPhotoUri = null
            onImagePicked(null)
        }
    }

    private val pickGalleryLauncher = fragment.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        onImagePicked(uri)
    }


    fun openCamera() {
        val context = fragment.requireContext()
        val photoFile = File(context.cacheDir, "camera_temp_${System.currentTimeMillis()}.jpg")

        currentPhotoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )

        takePictureLauncher.launch(currentPhotoUri)
    }

    fun openGallery() {
        pickGalleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
}