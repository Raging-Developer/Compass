package my.compass;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;


import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Compass and altimeter.
 * Now using the new google api and still using the permissions incorrectly.
 * Created by Christopher D. Harte on 06/02/2017.
 */

public class Compass_activity extends Activity implements SensorEventListener,
                                                          ConnectionCallbacks,
                                                          OnConnectionFailedListener
{
    private TextView show_elevation;
    private ImageView rose;
    private SensorManager my_sensor;
    private Sensor         accel;
    private Sensor         magne;
    private float[]        last_accel     = new float[3];
    private float[]        last_magne     = new float[3];
    private boolean        last_accel_set = false;
    private boolean        last_magne_set = false;
    private float[]        orient         = new float[3];
    private float[]        rotat          = new float[9];
    private float          current_degree = 0f;
    private ProgressDialog dialog;
    private List<Address> address_info;
    private final int REQ_CODE = 111;
    private GoogleApiClient api_client;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        show_elevation = (TextView) findViewById(R.id.tv_elevation);
        rose           = (ImageView) findViewById(R.id.rose_image);
        my_sensor      = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel          = my_sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magne          = my_sensor.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // This might take a bit of time
        dialog = new ProgressDialog(this);
        dialog.setMessage("Just finding where you are...");
        dialog.show();

        api_client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override protected void onStart()
    {
        super.onStart();
        api_client.connect();
    }

    @Override protected void onStop()
    {
        super.onStop();
        api_client.disconnect();
    }

    @Override public void onConnected(Bundle con_hint)
    {
        //Evil google documentation. No mention of the callback and the fact that
        //the manifest is now no longer good enough.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int lookup1 = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

            if (lookup1 != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE}, REQ_CODE);
            }
        }

        Get_height get_h = new Get_height(this);

        if (test_connection())
        {
            Location location = LocationServices.FusedLocationApi.getLastLocation(api_client);
            Geocoder geo = new Geocoder(this, Locale.getDefault());

            double latitude = 53.5333;
            double longitude = -2.2833;

            if (location != null)
            {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }

            address_info = null;

            try
            {
                address_info = geo.getFromLocation(latitude, longitude, 1);
            }
            catch (IOException e)
            {
                dialog.dismiss();
                e.printStackTrace();
            }

            String alti = String.valueOf(latitude) + ","
                    + String.valueOf(longitude);

            get_h.altitude(alti);
        }
        else
        {
            dialog.dismiss();
            show_elevation.setText("There is no network, so no altitude");
        }
    }

    @Override public void onConnectionFailed (ConnectionResult provider)
    {
        Toast.makeText(getBaseContext(), "There is no gps available " + provider,
                Toast.LENGTH_LONG)
                .show();
    }

    @Override public void onRequestPermissionsResult(int req_code, String[] perms, int[] grants)
    {
        switch (req_code)
        {
            case REQ_CODE:
                if (grants[0] != PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(this, "Location access denied!", Toast.LENGTH_LONG).show();
                }
                break;

            default:
                super.onRequestPermissionsResult(req_code, perms, grants);
        }
    }

    @Override public void onConnectionSuspended (int arg0)
    {
        api_client.connect();
    }

    @Override protected void onResume()
    {
        super.onResume();

        my_sensor.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        my_sensor.registerListener(this, magne, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override protected void onPause()
    {
        super.onPause();

        my_sensor.unregisterListener(this, accel);
        my_sensor.unregisterListener(this, magne);
    }

    @Override public void onSensorChanged(SensorEvent event)
    {
        if (event.sensor == accel)
        {
            System.arraycopy(event.values, 0, last_accel, 0, event.values.length);
            last_accel_set = true;
        }
        else if (event.sensor == magne)
        {
            System.arraycopy(event.values, 0, last_magne, 0, event.values.length);
            last_magne_set = true;
        }

        if (last_magne_set && last_accel_set)
        {
            SensorManager.getRotationMatrix(rotat, null, last_accel, last_magne);
            SensorManager.getOrientation(rotat, orient);

            float azi_radians = orient[0];
            float azi_degrees = (float) (Math.toDegrees(azi_radians) + 360) % 360;

            RotateAnimation rotat_ani = new RotateAnimation(current_degree,
                    -azi_degrees,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);

            rotat_ani.setDuration(250);
            rotat_ani.setFillAfter(true);

            rose.startAnimation(rotat_ani);
            current_degree = -azi_degrees;
        }
    }

    /**
     * Make sure we have some sort of network connection before trying to get an
     * altitude
     *
     * @return true or false
     */
    public boolean test_connection()
    {
        ConnectivityManager conn = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net_info     = conn.getActiveNetworkInfo();

        if (net_info != null
            && net_info.isConnectedOrConnecting())
        {
            return true;
        }
        return false;
    }


    /**
     * Called from the background task that gets the height from the google api.
     * The height may be a whole interger, so account for that.
     * @param elev String
     */
    public void got_height(String elev)
    {
        dialog.dismiss();

        String heights;
        int s_leng = elev.length();

        // round it down a bit, but not too much, I might be on K2 one day
        if (s_leng < 8)
        {
            heights = elev.substring(0, s_leng);
        }
        else
        {
            heights = elev.substring(0, 8);
        }

        String city    = address_info.get(0).getLocality();
        String town    = address_info.get(0).getSubLocality();
        String street  = address_info.get(0).getThoroughfare();
        String place   = street;

        if (street == null)
        {
            place = town;

            if (town == null)
            {
                place = city;
            }
        }

        String outs = "You are at " + place + " at a height of " + heights + " metres.";

        show_elevation.setText(outs);
    }

    public void got_height_fail(Exception e)
    {
        dialog.dismiss();

        Toast.makeText(this, "Ooops... " + e.getMessage(), Toast.LENGTH_LONG)
                .show();
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy)
    {}

    @Override public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.compass, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId())
        {
            case R.id.about_app :
                Intent a = new Intent("my.compass.ABOUT");
                a.putExtra("title", "A compass");
                a.putExtra("body", "This is a plain compass which points to magnetic, not true, north.\n"
                        + "It relies on the accelerometer, so moving about helps.\n"
                        + "If there is a network connection it tells you where you are, "
                        + "accuracy dependent on signal.\n"
                        + "It also gives your current altitude, as returned by the google location api, "
                        + "accuracy dependent on google.");
                startActivity(a);

                break;

            // No home icon as per the new evil rules from googling
            case android.R.id.home :
                finish();
                break;

        }
        return false;
    }
}
