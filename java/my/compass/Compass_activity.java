package my.compass;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Compass and altimeter.
 * Now using the new google api and still using the permissions incorrectly.
 */

public class Compass_activity extends AppCompatActivity implements SensorEventListener
{
    private TextView       show_elevation;
    private ImageView      rose;
    private SensorManager  my_sensor;
    private Sensor         accel;
    private Sensor         magne;
    private final float[]  last_accel     = new float[3];
    private final float[]  last_magne     = new float[3];
    private boolean        last_accel_set = false;
    private boolean        last_magne_set = false;
    private final float[]  orient         = new float[3];
    private final float[]  rotat          = new float[9];
    private float          current_degree = 0f;
    private AlertDialog    dialog;
    private List<Address>  address_info;
    private final int      REQ_CODE = 111;
    FusedLocationProviderClient fused_client;
    LocationRequest locRequest;
    private LocationCallback callback;

    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        show_elevation = findViewById(R.id.tv_elevation);
        rose           = findViewById(R.id.rose_image);
        my_sensor      = (SensorManager) getSystemService(SENSOR_SERVICE);

        callback = new LocationCallback() {
            @Override public void onLocationResult(LocationResult locationResult){
                if (locationResult == null){
                    return;
                }
            }
        };

        if (my_sensor != null) {
            accel = my_sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magne = my_sensor.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        //Evil google documentation. No mention of the callback and the fact that
        //the manifest is now no longer good enough.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int lookup1 = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

            if (lookup1 != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE}, REQ_CODE);
            }
        }

        // This might take a bit of time
        dialog = new AlertDialog.Builder(this).create();
        dialog.setMessage("Just finding where you are...");
        dialog.show();

        locRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000)
                .setWaitForAccurateLocation(false)
                .build();

        fused_client = LocationServices.getFusedLocationProviderClient(this);
        fused_client.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>()
        {
            @Override public void onSuccess(Location location) {
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    use_lat_and_long();
                }
            }
        });
    }

    private void use_lat_and_long() {
        Get_height get_h = new Get_height(this);

        if(test_connection()) {
            Geocoder geo = new Geocoder(this, Locale.getDefault());
            address_info = null;

            try {
                address_info = geo.getFromLocation(latitude, longitude, 1);
            }
            catch (IOException e) {
                dialog.dismiss();
                e.printStackTrace();
            }

            String alti = latitude + "," + longitude;
            get_h.altitude(alti);
        }
        else {
            dialog.dismiss();
            show_elevation.setText("There is no network, so no altitude");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        my_sensor.unregisterListener(this, accel);
        my_sensor.unregisterListener(this, magne);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
        my_sensor.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        my_sensor.registerListener(this, magne, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fused_client.requestLocationUpdates(locRequest, callback, Looper.getMainLooper());
    }

    @Override
    public void onRequestPermissionsResult(int req_code, String[] perms, int[] grants) {
        if (req_code == REQ_CODE) {
            if (grants[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location access denied!", Toast.LENGTH_LONG).show();
            }
        }
        else {
            super.onRequestPermissionsResult(req_code, perms, grants);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accel) {
            System.arraycopy(event.values, 0, last_accel, 0, event.values.length);
            last_accel_set = true;
        }
        else if (event.sensor == magne) {
            System.arraycopy(event.values, 0, last_magne, 0, event.values.length);
            last_magne_set = true;
        }

        if (last_magne_set && last_accel_set) {
            SensorManager.getRotationMatrix(rotat, null, last_accel, last_magne);
            SensorManager.getOrientation(rotat, orient);

            float azi_radians = orient[0];
            float azi_degrees = (float) (Math.toDegrees(azi_radians) + 360) % 360;

            RotateAnimation rotat_ani = new RotateAnimation(current_degree,
                    -azi_degrees,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);

            rotat_ani.setDuration(5000);
            rotat_ani.setFillAfter(true);

            rose.startAnimation(rotat_ani);
            current_degree = -azi_degrees;
        }
    }

    /**
     * Make sure we have some sort of network connection before trying to get an
     * altitude
     */
    public boolean test_connection() {
        ConnectivityManager conn = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (conn != null) {
            NetworkInfo net_info = conn.getActiveNetworkInfo();

            return net_info.isConnectedOrConnecting();
        }
        return false;
    }

    /**
     * Called from the background task that gets the height from the google api.
     * The height may be a whole interger, so account for that.
     * @param elev String
     */
    public void got_height(String elev) {
        dialog.dismiss();

        String heights;
        int s_leng = elev.length();

        // round it down a bit, but not too much, I might be on K2 one day
        if (s_leng < 8) {
            heights = elev.substring(0, s_leng);
        }
        else {
            heights = elev.substring(0, 8);
        }

        String city    = address_info.get(0).getLocality();
        String town    = address_info.get(0).getSubLocality();
        String street  = address_info.get(0).getThoroughfare();
        String place   = street;

        if (street == null) {
            place = town;
            if (town == null) {
                place = city;
            }
        }

        String outs = "You are at " + place + " at a height of " + heights + " metres.";
        show_elevation.setText(outs);
    }

    public void got_height_fail(Exception e) {
        dialog.dismiss();
        Toast.makeText(this, "Ooops... " + e.getMessage(), Toast.LENGTH_LONG)
                .show();
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy)
    {}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.compass, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int itemId = item.getItemId();
        if (itemId == R.id.about_app)
        {
            Intent a = new Intent("my.compass.ABOUT");
            a.putExtra("title", "A compass");
            a.putExtra("body", "This is a plain compass which points to magnetic, not true, north.\n"
                    + "It relies on the accelerometer, so moving about helps.\n"
                    + "If there is a network connection it tells you where you are, "
                    + "accuracy dependent on signal.\n"
                    + "It also gives your current altitude, as returned by the google location api, "
                    + "accuracy dependent on google.");
            startActivity(a);

            // No home icon as per the new evil rules from googling
        }
        else if (itemId == android.R.id.home) {
            finish();
        }
        return false;
    }
}
