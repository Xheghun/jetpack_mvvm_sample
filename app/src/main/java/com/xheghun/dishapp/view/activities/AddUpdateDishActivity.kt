package com.xheghun.dishapp.view.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import com.xheghun.dishapp.R
import com.xheghun.dishapp.application.FavDishApplication
import com.xheghun.dishapp.databinding.ActivityAddUpdateDishBinding
import com.xheghun.dishapp.databinding.DialogCustomImageSelectionBinding
import com.xheghun.dishapp.databinding.DialogCustomListBinding
import com.xheghun.dishapp.models.entities.FavDish
import com.xheghun.dishapp.utils.Constants
import com.xheghun.dishapp.utils.showToast
import com.xheghun.dishapp.view.adapters.CustomListItemAdapter
import com.xheghun.dishapp.viewmodel.FavDishViewModel
import com.xheghun.dishapp.viewmodel.FavDishViewModelFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

class AddUpdateDishActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var mBinding: ActivityAddUpdateDishBinding
    private lateinit var camLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    private var imagePath: String = ""

    private lateinit var mCustomListDialog: Dialog


    private val mFavDishViewModel: FavDishViewModel by viewModels {
        FavDishViewModelFactory((application as FavDishApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityAddUpdateDishBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setupActionBar()

        handleActivityResult()

        mBinding.ivAddDishImage.setOnClickListener(this)
        mBinding.etType.setOnClickListener(this)
        mBinding.etCategory.setOnClickListener(this)
        mBinding.etCookingTime.setOnClickListener(this)

        mBinding.btnAddDish.setOnClickListener(this)

    }

    override fun onClick(v: View) {

        when (v.id) {

            R.id.iv_add_dish_image -> {
                customImageSelectionDialog()

                return
            }
            R.id.et_type -> {
                customItemDialog(
                    resources.getString(R.string.title_select_dish_type),
                    Constants.dishTypes(),
                    Constants.DISH_TYPE
                )
                return
            }

            R.id.et_category -> {
                customItemDialog(
                    resources.getString(R.string.title_select_dish_categoty),
                    Constants.dishCategories(),
                    Constants.DISH_CATEGORY
                )
                return
            }

            R.id.et_cooking_time -> {
                customItemDialog(
                    resources.getString(R.string.title_select_dish_cooking_time),
                    Constants.dishCookingTime(),
                    Constants.DISH_COOKING_TIME
                )
                return
            }

            R.id.btn_add_dish -> {
                val title = mBinding.etTitle.text.toString().trim()
                val type = mBinding.etType.text.toString().trim()
                val category = mBinding.etCategory.text.toString().trim()
                val ingredients = mBinding.etIngredients.text.toString().trim()
                val cookingTime = mBinding.etCookingTime.text.toString().trim()
                val cookingDirection = mBinding.etDirectionToCook.text.toString().trim()

                when {
                    imagePath.isEmpty() -> {
                        showToast(this, resources.getString(R.string.err_msg_dish_image_select))
                    }
                    title.isEmpty() -> {
                        showToast(this, resources.getString(R.string.err_msg_dish_title))
                    }
                    type.isEmpty() -> {
                        showToast(this, resources.getString(R.string.err_msg_dish_type))
                    }
                    category.isEmpty() -> {
                        showToast(this, resources.getString(R.string.err_msg_dish_category))
                    }
                    ingredients.isEmpty() -> {
                        showToast(this, resources.getString(R.string.err_msg_dish_ingredients))
                    }
                    cookingTime.isEmpty() -> {
                        showToast(this, resources.getString(R.string.err_msg_dish_cook_time))
                    }
                    cookingDirection.isEmpty() -> {
                        showToast(this, resources.getString(R.string.err_msg_dish_cook_instruct))
                    }

                    else -> {

                        val favDishDetails = FavDish(
                            imagePath,
                            Constants.DISH_IMAGE_SOURCE_LOCAL,
                            title,
                            type,
                            category,
                            ingredients,
                            cookingTime,
                            cookingDirection,
                            false
                        )

                        mFavDishViewModel.insert(favDishDetails)

                        showToast(this, "All entries are valid")

                        finish()

                    }

                }
            }
        }
    }

    /**
     * A function for ActionBar setup.
     */
    private fun setupActionBar() {
        setSupportActionBar(mBinding.toolbarAddDishActivity)
        mBinding.toolbarAddDishActivity.setNavigationOnClickListener { onBackPressed() }
    }

    private fun changeSelectIcon() {
        mBinding.ivAddDishImage.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.ic_edit)
        )
    }

    fun selectedListItem(item: String, selection: String) {
        mCustomListDialog.dismiss()
        when (selection) {
            Constants.DISH_TYPE -> {
                mBinding.etType.setText(item)
            }
            Constants.DISH_CATEGORY -> {
                mBinding.etCategory.setText(item)
            }
            Constants.DISH_COOKING_TIME -> {
                mBinding.etCookingTime.setText(item)
            }
        }
    }

    private fun handleActivityResult() {
        camLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.extras?.let {
                        val thumbnail: Bitmap = result.data!!.extras!!.get("data") as Bitmap
                        imagePath = saveImageToFileSystem(thumbnail)

                        loadImage(thumbnail, mBinding.ivDishImage)

                        Log.d(SCOPE, "image path is $imagePath")

                        changeSelectIcon()
                    }
                }
            }

        galleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                if (result.resultCode == Activity.RESULT_OK) {
                    val selectedImage = result.data?.data
                    loadImage(selectedImage!!, mBinding.ivDishImage)
                    changeSelectIcon()
                }
            }
    }

    private fun customImageSelectionDialog() {
        val dialog = Dialog(this@AddUpdateDishActivity)

        val binding: DialogCustomImageSelectionBinding =
            DialogCustomImageSelectionBinding.inflate(layoutInflater)

        //Set the screen content from a layout resource.
        //The resource will be inflated, adding all top-level views to the screen.
        dialog.setContentView(binding.root)

        binding.tvCamera.setOnClickListener {
            dialog.dismiss()
            requestCamPermission()
        }

        binding.tvGallery.setOnClickListener {
            dialog.dismiss()
            requestStoragePermission()
        }
        // END

        //Start the dialog and display it on screen.
        dialog.show()
    }

    private fun requestStoragePermission() {

        Dexter.withContext(this).withPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
        ).withListener(object : PermissionListener {
            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                val mIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                galleryLauncher.launch(mIntent)

            }

            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                Toast.makeText(this@AddUpdateDishActivity, "permission denied", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: PermissionRequest?,
                p1: PermissionToken?
            ) {
                showRationaleDialogForPermission()
            }


        }).onSameThread().check()
    }

    private fun requestCamPermission() {
        Dexter.withContext(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                report?.let {
                    if (report.areAllPermissionsGranted()) {
                        val mIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        camLauncher.launch(mIntent)
                    }
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                showRationaleDialogForPermission()
            }


        }).onSameThread().check()
    }

    private fun showRationaleDialogForPermission() {
        AlertDialog.Builder(this)
            .setMessage("Please allow necessary permissions to make app function properly")
            .setPositiveButton("OK") { _, _ ->

                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }.show()

    }

    private fun loadImage(image: Any, view: ImageView) {
        try {
            Glide.with(this).load(image)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e(SCOPE, "Error loading image", e)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        resource?.let {
                            val bitmap = resource.toBitmap()
                            imagePath = saveImageToFileSystem(bitmap)
                        }
                        return false
                    }

                })
                .into(view)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveImageToFileSystem(bitmap: Bitmap): String {
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, ContextWrapper.MODE_PRIVATE)

        file = File(file, "${UUID.randomUUID()}.jpg")

        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return file.absolutePath
    }

    private fun customItemDialog(title: String, itemsList: List<String>, selection: String) {
        mCustomListDialog = Dialog(this)
        val binding: DialogCustomListBinding = DialogCustomListBinding.inflate(layoutInflater)

        mCustomListDialog.setContentView(binding.root)

        binding.tvTitle.text = title
        binding.rvList.layoutManager = LinearLayoutManager(this)

        val adapter = CustomListItemAdapter(this, itemsList, selection)
        binding.rvList.adapter = adapter
        mCustomListDialog.show()
    }

    companion object {
        private const val IMAGE_DIRECTORY = "Images"
        private const val SCOPE = "AddUpdateActivity"
    }
}