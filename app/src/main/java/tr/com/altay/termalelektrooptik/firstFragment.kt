package tr.com.altay.termalelektrooptik

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.System.getConfiguration
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import tr.com.altay.termalelektrooptik.databinding.FragmentFirstBinding
import kotlin.math.roundToInt


class firstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Toast.makeText(activity, "Bağlantı kuruluyor", Toast.LENGTH_LONG).show();

        val offlineUri = Uri.parse("android.resource://tr.com.altay.termalelektrooptik/${R.raw.termal}")
        binding.videoplayer.setVideoPath(offlineUri.toString())
        binding.videoplayer.focusable
        binding.videoplayer.start()

        /*
        println(getScreenHeight())
        println(getScreenWidth())

        if (Resources.getSystem().configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            val newWidth = getScreenWidth() * 640 / 480
            println("newWidth px: "+newWidth)

            println("dp: "+convertPixelsToDp(newWidth.toFloat(),context))

            //binding.videobase?.layoutParams?.width = 2506
            //binding.videoplayer.layoutParams.width = newWidth

        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

            val newHeight = getScreenWidth() * 480 / 640
            println("newWidth px: "+newHeight)

            println("dp: "+convertPixelsToDp(newHeight.toFloat(),context))

            //binding.videobase?.layoutParams?.width = 2506
            //binding.videoplayer.layoutParams.width = newHeight

        }
        */
    }

    fun getVideo(): VideoView{
        return binding.videoplayer
    }

    /*
    fun getScreenWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }

    fun getScreenHeight(): Int {
        return Resources.getSystem().displayMetrics.heightPixels
    }

    fun convertDpToPixel(dp: Float, context: Context?): Float {
        return if (context != null) {
            val resources = context.resources
            val metrics = resources.displayMetrics
            dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        } else {
            val metrics = Resources.getSystem().displayMetrics
            dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }
    }

    fun convertPixelsToDp(px: Float, context: Context?): Float {
        return if (context != null) {
            val resources = context.resources
            val metrics = resources.displayMetrics
            px / (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        } else {
            val metrics = Resources.getSystem().displayMetrics
            px / (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }
    }*/

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}