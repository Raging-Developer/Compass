package my.compass

import android.os.Handler
import android.os.Looper
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.net.URL
import java.util.concurrent.Executor
import java.util.concurrent.Executors


/**
 * Get the current altitude from the Evil ones at Google.
 * They won't like it because I am not using it with their maps, so tough.
 *
 * Successfully replacing a java class, because functions are the future.
 */
fun get_height_new(compass: Compass_activity, params: String) {
    val executor: Executor = Executors.newSingleThreadExecutor()
    var handler: Handler = Handler(Looper.getMainLooper())
    var result: StringBuilder = StringBuilder()
    var error: Exception? = null

    var weak_ref: WeakReference<Compass_activity> = WeakReference<Compass_activity>(compass)

    executor.execute {
        val query = String.format(
            "https://maps.googleapis.com/maps/api/elevation/json?"
                    + "locations=%s&key=AIzaSyD1bfNKg0AaATvlWFW0VXINLKcMR4PXw6g", params
        )

        try {
            val url = URL(query)
            val conn = url.openConnection()
            val input = conn.getInputStream()
            val reader = BufferedReader(InputStreamReader(input))
            var line: String?

            while ((reader.readLine().also { line = it }) != null) {
                result.append(line)
            }
        } catch (e: Exception) {
            error = e
        }
    }

    handler.postDelayed({
            val weak_compass: Compass_activity? = weak_ref.get()

            if (result == null && error != null) {
                weak_compass?.got_height_fail(error)
            }

        try {
            //Json array, not an object, just to confuse me.
            val data = JSONObject(result.toString())
            val q_result = data.optJSONArray("results")
            val this_height = q_result.getJSONObject(0).getString("elevation")

            weak_compass!!.got_height(this_height)
        }
        catch (e: JSONException) {
                weak_compass!!.got_height_fail(e)
        }
    }, 1000)
}
