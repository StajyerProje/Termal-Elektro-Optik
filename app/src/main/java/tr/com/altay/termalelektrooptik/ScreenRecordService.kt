package tr.com.altay.termalelektrooptik

import android.app.*
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore.Audio.Media
import android.util.DisplayMetrics
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import tr.com.altay.termalelektrooptik.databinding.ActivityMainBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScreenRecordService: Service() {

    private val CHANNEL_ID = "Screen Record"
    private val NOTIFICATION_ID = 1

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private lateinit var binding: ActivityMainBinding


    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager


    }

    fun getBinding(b: ActivityMainBinding){
        this.binding = b
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        var fileDir: String

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            fileDir = "${externalCacheDir?.absolutePath}/${getFileName()}.3gp"
            mediaRecorder = MediaRecorder(this)
        }else{
            fileDir = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}/${getFileName()}.3gp"
            mediaRecorder = MediaRecorder()
        }

        val resultCode = intent?.getIntExtra("resultCode",Activity.RESULT_CANCELED)
        val data = intent?.getParcelableExtra<Intent>("data")

        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode!!,data!!)

        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode!!, data!!)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenRecording",
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels,
            resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface,
            null,
            null
        )

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setOutputFile(fileDir)
            setVideoFrameRate(30)
            setVideoSize(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
            prepare()
        }
        mediaRecorder?.start()
        startForeground(NOTIFICATION_ID,createNotification())

        return START_NOT_STICKY
        //return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotification(): Notification{
        val notifyChannel = NotificationChannel("$CHANNEL_ID","Screen Record", NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(notifyChannel)

        val notification = NotificationCompat.Builder(this,"$CHANNEL_ID")
            .setSmallIcon(10)
            .setContentTitle("Screen Record")
            .setContentText("Recording")
            .setPriority(NotificationManager.IMPORTANCE_MAX)
            .setAutoCancel(true)
            .build()


        return notification
    }

    override fun onDestroy(){
        super.onDestroy()

        mediaProjection?.stop()
        mediaRecorder?.stop()
        mediaRecorder?.release()
        virtualDisplay?.release()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun getFileName(): String{
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")

        return currentTime.format(formatter).toString()
    }
}