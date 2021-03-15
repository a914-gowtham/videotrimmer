package com.videotrimmer.videotrimmerdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.widget.EditText
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cocosw.bottomsheet.BottomSheet
import com.videotrimmer.videotrimmerdemo.databinding.ActivityMainBinding
import com.videotrimmer.videotrimmerdemo.trimmer.utils.LogMessage
import com.videotrimmer.videotrimmerdemo.trimmer.utils.TrimType
import com.videotrimmer.videotrimmerdemo.trimmer.utils.TrimVideo
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var mediaController: MediaController

    private var trimType = 0

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_TAKE_VIDEO = 552
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mediaController = MediaController(this)
        binding.apply {
            btnDefaultTrim.setOnClickListener {
                onDefaultTrimClicked()
            }
            btnFixedGap.setOnClickListener {
                onFixedTrimClicked()
            }
            btnMinGap.setOnClickListener {
                onMinGapTrimClicked()
            }
            btnMinMaxGap.setOnClickListener {
                onMinToMaxTrimClicked()
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)


    }


    private fun onDefaultTrimClicked() {
        trimType = 0
        if (checkCamStoragePer())
            showVideoOptions()
    }

    private fun onFixedTrimClicked() {
        trimType = 1
        if (isEdtTxtEmpty(binding.edtFixedGap))
            Toast.makeText(this, "Enter fixed gap duration", Toast.LENGTH_SHORT).show()
        else if (checkCamStoragePer())
            showVideoOptions()

    }

    private fun onMinGapTrimClicked() {
        trimType = 2
        if (isEdtTxtEmpty(binding.edtMinGap))
            Toast.makeText(this, "Enter min gap duration", Toast.LENGTH_SHORT).show()
        else if (checkCamStoragePer())
            showVideoOptions()
    }

    private fun onMinToMaxTrimClicked() {
        trimType = 3
        when {
            isEdtTxtEmpty(binding.edtMinFrom) -> Toast.makeText(
                this,
                "Enter min gap duration",
                Toast.LENGTH_SHORT
            ).show()
            isEdtTxtEmpty(binding.edtMaxTo) -> Toast.makeText(
                this,
                "Enter max gap duration",
                Toast.LENGTH_SHORT
            ).show()
            checkCamStoragePer() -> showVideoOptions()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == TrimVideo.VIDEO_TRIMMER_REQ_CODE && data != null) {
                val uri: Uri = Uri.parse(TrimVideo.getTrimmedVideoPath(data))
                Log.d("Tag", "Trimmed path:: $uri")
                binding.videoView.apply {
                    setMediaController(mediaController)
                    setVideoURI(uri)
                    requestFocus()
                    start()
                    setOnPreparedListener {
                        mediaController.setAnchorView(this)
                    }
                }
                val filepath: String = java.lang.String.valueOf(uri)
                val file = File(filepath)
                val length: Long = file.length()
                Log.d("TAG", "Video size:: " + length / 1024)
            } else if (requestCode == REQUEST_TAKE_VIDEO && resultCode == RESULT_OK) {
                /*    //check video duration if needed
                if (TrimmerUtils.getVideoDuration(this,data.getData())<=30){
                    Toast.makeText(this,"Video should be larger than 30 sec",Toast.LENGTH_SHORT).show()
                    return
                }*/
                if (data!!.data != null) {
                    LogMessage.v("Video path:: " + data.data)
                    openTrimActivity(data.data.toString())
                } else {
                    Toast.makeText(this, "video uri is null", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (isPermissionOk(*grantResults))
            showVideoOptions()
    }


    private fun openVideo() {
        try {
            val intent = Intent()
            intent.type = "video/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_TAKE_VIDEO)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun captureVideo() {
        try {
            val intent = Intent("android.media.action.VIDEO_CAPTURE")
            intent.putExtra("android.intent.extra.durationLimit", 30)
            startActivityForResult(intent, REQUEST_TAKE_VIDEO)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun showVideoOptions() {
        try {
            val builder: BottomSheet.Builder = BottomSheet.Builder(this).title("Select a option")
            builder.sheet(R.menu.menu_video)
            builder.listener { item: MenuItem? ->
                if (item != null && R.id.action_take == item.itemId)
                    captureVideo()
                else
                    openVideo()
                false
            }
            builder.show()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    private fun openTrimActivity(data: String) {
        if (trimType == 0) {
            TrimVideo.activity(data)
                //.setCompressOption(new CompressOption()) //pass empty constructor for default compress option
                .setDestination("/storage/emulated/0/DCIM/TESTFOLDER")
                .start(this)
        } else if (trimType == 1) {
            TrimVideo.activity(data)
                .setTrimType(TrimType.FIXED_DURATION)
                .setFixedDuration(getEdtValueLong(binding.edtFixedGap))
                .start(this)
        } else if (trimType == 2) {
            TrimVideo.activity(data)
                .setTrimType(TrimType.MIN_DURATION)
                .setMinDuration(getEdtValueLong(binding.edtMinGap))
                .start(this)
        } else {
            TrimVideo.activity(data)
                .setTrimType(TrimType.MIN_MAX_DURATION)
                .setMinToMax(
                    getEdtValueLong(binding.edtMinFrom),
                    getEdtValueLong(binding.edtMaxTo)
                )
                .start(this)
        }
    }

    private fun isEdtTxtEmpty(editText: EditText): Boolean {
        return editText.text.toString().trim().isEmpty()
    }

    private fun getEdtValueLong(editText: EditText): Long {
        return editText.text.toString().trim().toLong()
    }

    private fun checkCamStoragePer(): Boolean {
        return checkPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA
        )
    }


    private fun checkPermission(vararg permissions: String): Boolean {
        var allPermitted = false
        for (permission in permissions) {
            allPermitted = (ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED)
            LogMessage.v("granted $permission is granted $allPermitted")
            if (!allPermitted) break
        }
        if (allPermitted) return true
        ActivityCompat.requestPermissions(
            this, permissions,
            220
        )
        return false
    }

    private fun isPermissionOk(vararg results: Int): Boolean {
        var isAllGranted = true
        for (result in results) {
            if (PackageManager.PERMISSION_GRANTED != result) {
                isAllGranted = false
                break
            }
        }
        return isAllGranted
    }

}