package tr.com.altay.termalelektrooptik

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
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
import tr.com.altay.termalelektrooptik.databinding.ActivityMainBinding
import tr.com.altay.termalelektrooptik.databinding.ContentMainBinding
import tr.com.altay.termalelektrooptik.databinding.FragmentFirstBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class mainActivity : AppCompatActivity() {

    //private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var displayMetrics: DisplayMetrics
    private lateinit var mediaRecorder: MediaRecorder

    private var isStoragePermissionGranted = false
    private var isCameraPermissionGranted = false
    private var isAudioPermissionGranted = false
    private var isRecordPermissionGranted = false

    private var isStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            isStoragePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?:isStoragePermissionGranted
            isAudioPermissionGranted = permissions[Manifest.permission.RECORD_AUDIO] ?:isAudioPermissionGranted
            isCameraPermissionGranted = permissions[Manifest.permission.CAMERA] ?:isCameraPermissionGranted
        }

        requestPermission()

        val videoFrame = binding.include.videoView

        val offlineUri = Uri.parse("android.resource://tr.com.altay.termalelektrooptik/${R.raw.termal}")
        videoFrame.setVideoPath(offlineUri.toString())
        videoFrame.focusable
        videoFrame.start()

        binding.photoButton.setOnClickListener {
            if(isStoragePermissionGranted)
                getImage(videoFrame) {bitmap: Bitmap? -> saveImage(bitmap)}
            else
                requestPermission()
        }

        /**
         * Bilinen sikintilar: Android api 29 sonrasi exception atiyor
         */
        binding.videoButton.setOnClickListener {
            requestPermission()

            if(isStoragePermissionGranted && isAudioPermissionGranted && isCameraPermissionGranted){
                /*when(isStarted){
                    true -> stopProjection()
                    false -> startProjection()
                }*/
                startProjection()
            }else{
                Log.v("Error","setOnClickListener_SDK_INT < S")
                requestPermission()
            }
        }

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == RESULT_OK){
                try{
                    if(result.data != null){
                        mediaProjection = mediaProjectionManager.getMediaProjection(result.resultCode,
                            result.data!!) as MediaProjection
                    }else{
                        throw Exception()
                    }

                    initRecorder()

                    virtualDisplay = mediaProjection.createVirtualDisplay(
                        "Display",
                        displayMetrics.widthPixels,
                        displayMetrics.heightPixels,
                        displayMetrics.densityDpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        mediaRecorder.surface,
                        null,
                        null
                    )

                    mediaRecorder.start()
                }catch(e: Exception){
                    Log.e("Error","resultLauncher",e)
                }
            }
        }

        //requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

        //setSupportActionBar(binding.toolbar)
        //val navController = findNavController(R.id.nav_host_fragment_content_main)
        //appBarConfiguration = AppBarConfiguration(navController.graph)
        //setupActionBarWithNavController(navController, appBarConfiguration)
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
        try{
            mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent = mediaProjectionManager.createScreenCaptureIntent()
            val intent2 = Intent(this,ScreenRecordService::class.java)


            if (mediaProjectionManager != null) {
                if(!isStarted){
                    startForegroundService(intent2.apply {
                        putExtra("resultCode",Activity.RESULT_OK)
                        putExtra("data",intent)
                    })

                    isStarted = true
                }else{
                    startForegroundService(intent2)
                    isStarted = false
                }
            }


        }catch(e: Exception){
            Log.e("Error","startProjection",e)
        }
    }

    private fun stopProjection(){
        try{
            /*mediaRecorder.stop()
            mediaRecorder.reset()
            virtualDisplay.release()
            mediaProjection.stop()
            */

            isStarted = false

            Log.v("Ended","Ended Projection")
        }catch(e: Exception){
            Log.e("Error","stopProjection",e)
        }
    }

    private fun initRecorder(){
        try{
            val fileDir: String

            displayMetrics = binding.include.videoView.resources.displayMetrics

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                fileDir = "${externalCacheDir?.absolutePath}/${getFileName()}.3gp"
                mediaRecorder = MediaRecorder(this@mainActivity)
            }else{
                fileDir = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}/${getFileName()}.3gp"
                mediaRecorder = MediaRecorder()
            }

            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)

            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
            mediaRecorder.setOutputFile(fileDir)
            mediaRecorder.setVideoFrameRate(30)
            mediaRecorder.setVideoSize(displayMetrics.widthPixels,displayMetrics.heightPixels)

            mediaRecorder.prepare()
        }catch(e: Exception){
            Log.e("Error","initRecorder",e)
        }
    }

    private fun requestPermission(){
        isStoragePermissionGranted = (ContextCompat.checkSelfPermission(this@mainActivity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)

        isAudioPermissionGranted = (ContextCompat.checkSelfPermission(this@mainActivity,
        Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)

        isCameraPermissionGranted = (ContextCompat.checkSelfPermission(this@mainActivity,
            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)

        val permissionRequest: MutableList<String> = ArrayList()

        if(!isStoragePermissionGranted){
            permissionRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            Log.e("Not Granted","Storage")
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

    /*
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }*/
}