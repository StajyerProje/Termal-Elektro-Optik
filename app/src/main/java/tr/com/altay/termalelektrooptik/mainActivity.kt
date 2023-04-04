package tr.com.altay.termalelektrooptik

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import dev.bmcreations.scrcast.ScrCast
import tr.com.altay.termalelektrooptik.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import dev.bmcreations.scrcast.config.NotificationConfig
import dev.bmcreations.scrcast.config.Options
import dev.bmcreations.scrcast.config.StorageConfig
import dev.bmcreations.scrcast.config.VideoConfig




class mainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var displayMetrics: DisplayMetrics
    private lateinit var recorder: ScrCast

    private var isStorage1Permission = false
    private var isStoragePermissionGranted = false
    private var isCameraPermissionGranted = false
    private var isAudioPermissionGranted = false


    private var isStarted = false
    private lateinit var videoFileName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        this.recorder = ScrCast.use(this).apply {
            options{
                video { maxLengthSecs=360 }
                storage { directoryName="altay" }
                moveTaskToBack=false
                stopOnScreenOff=true
                startDelayMs=1_000
            }
        }


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            isStorage1Permission = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?:isStorage1Permission
            isStoragePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?:isStoragePermissionGranted
            isAudioPermissionGranted = permissions[Manifest.permission.RECORD_AUDIO] ?:isAudioPermissionGranted
            isCameraPermissionGranted = permissions[Manifest.permission.CAMERA] ?:isCameraPermissionGranted
        }


        val videoFrame = binding.include.videoView

        val offlineUri = Uri.parse("android.resource://tr.com.altay.termalelektrooptik/${R.raw.termal}")
        videoFrame.setVideoPath(offlineUri.toString())
        videoFrame.focusable
        videoFrame.start()

        requestPermission()

        binding.photoButton.setOnClickListener {
            if(isStoragePermissionGranted)
                getImage(videoFrame) {bitmap: Bitmap? -> saveImage(bitmap)}
            else
                requestPermission()
        }


        binding.videoButton.setOnClickListener {
            requestPermission()

            if(isStoragePermissionGranted && isAudioPermissionGranted && isCameraPermissionGranted){
                when(isStarted){
                    true -> stopProjection()
                    false -> startProjection()
                }
            }else{
                Log.v("Error","setOnClickListener_SDK_INT < S")
                requestPermission()
            }
        }

    }


    /**
     * Captures SurfaceView and it's children using PixelCopy
     */
    private fun getImage(videoView: SurfaceView, callback: (Bitmap?) -> Unit) {
        val bitmap: Bitmap = Bitmap.createBitmap(
            videoView.width,
            videoView.height,
            Bitmap.Config.ARGB_8888
        )
        try {
            val handlerThread = HandlerThread("PixelCopier")
            handlerThread.start()

            PixelCopy.request(videoView, bitmap,
                PixelCopy.OnPixelCopyFinishedListener { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        callback(bitmap)
                    }
                    handlerThread.quitSafely();
                },
                Handler(handlerThread.looper)
            )
        } catch (e: IllegalArgumentException) {
            callback(null)
            Log.e("Image Capture Error","Couldn't take screenshot",e)
        }
    }

    /**
     * Saves captured bitmap to android device's gallery using FileOutputStream and ContentValues
     */
    private fun saveImage(bitmap: Bitmap?) {
        val filename = "${getFileName()}.jpeg"
        var fos: OutputStream? = null

        if(bitmap == null)
            return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }

        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            Log.v("Final","Entered finally")
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            fos?.flush()
            fos?.close()
        }

        Toast.makeText(this , "Galeriye kaydedildi" , Toast.LENGTH_SHORT).show()
    }

    private fun getFileName(): String{
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")

        return currentTime.format(formatter).toString()
    }

    private fun startProjection(){
        recorder.record()
        isStarted = true
    }

    private fun stopProjection(){
        recorder.stopRecording()
        isStarted = false
        Log.e("stopProjection","stoppedProjection")
    }


    private fun requestPermission(){
        isStorage1Permission = (ContextCompat.checkSelfPermission(this@mainActivity,
            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)

        isStoragePermissionGranted = (ContextCompat.checkSelfPermission(this@mainActivity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)

        isAudioPermissionGranted = (ContextCompat.checkSelfPermission(this@mainActivity,
        Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)

        isCameraPermissionGranted = (ContextCompat.checkSelfPermission(this@mainActivity,
            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)

        val permissionRequest: MutableList<String> = ArrayList()

        if(!isStorage1Permission){
            permissionRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            Log.e("Not Granted","Storage_Read")
        }

        if(!isStoragePermissionGranted){
            permissionRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            Log.e("Not Granted","Storage_Write")
        }

        if(!isAudioPermissionGranted){
            permissionRequest.add(Manifest.permission.RECORD_AUDIO)
            Log.e("Not Granted","Audio")

        }

        if(!isCameraPermissionGranted){
            permissionRequest.add(Manifest.permission.CAMERA)
            Log.e("Not Granted","Camera")
        }

        if(permissionRequest.isNotEmpty())
            permissionLauncher.launch(permissionRequest.toTypedArray())
    }

}