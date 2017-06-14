package my.compass;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Get the current altitude from the Evil ones at Google.
 * They won't like it because I am not using it with their maps, so tough.
 * Created by Christopher D. Harte on 06/02/2017.
 */

class Get_height
{
    private Compass_activity compass;
    private Exception error;
    /**
     * Run in background to get the altitude from the google api
     * according to the evil google I cannot use this value except on
     * a google map. Let us see if they notice. They appear to be watching
     * everything else in the world.
     * @param compass Compass_activity
     *
     */
    Get_height(Compass_activity compass)
    {
        super();
        this.compass = compass;
    }

    /**
     * Get the altitude of your current location
     * @param  lat_and_long String
     */
    void altitude(String lat_and_long)
    {
        new AsyncTask<String, Void, String>()
        {
            @Override protected String doInBackground(String... params)
            {
                //53.518051,-2.268018 without quotes and my api key stuck on the end
                String query = String.format("https://maps.googleapis.com/maps/api/elevation/json?"
                        + "locations=%s&key=removed", params[0]);


                try
                {
                    URL url    = new URL(query);
                    URLConnection conn   = url.openConnection();
                    InputStream input  = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    StringBuilder  result = new StringBuilder();
                    String         line;

                    while ((line = reader.readLine()) != null)
                    {
                        result.append(line);

                    }

                    return result.toString();
                }
                catch (Exception e)
                {
                    error = e;
                    e.printStackTrace();
                }


                return null;
            }

            @Override protected void onPostExecute(String result)
            {
                super.onPostExecute(result);

                if (result == null && error != null)
                {
                    compass.got_height_fail(error);
                }

                try
                {
                    //Json array, not an object, just to confuse me.
                    JSONObject data        = new JSONObject (result);
                    JSONArray  q_result    = data.optJSONArray("results");
                    String     this_height = q_result.getJSONObject(0).getString("elevation");

                    compass.got_height(this_height);
                }
                catch (JSONException e)
                {
                    compass.got_height_fail(e);
                }
            }

        }.execute(lat_and_long);

    }


}
