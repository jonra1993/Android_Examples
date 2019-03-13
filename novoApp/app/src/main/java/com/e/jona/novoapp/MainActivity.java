//tutorial 1 https://www.youtube.com/watch?v=XOF8aFU03ew  https://www.youtube.com/watch?v=1C9mZnxaN7I
//https://www.youtube.com/watch?v=0CCFd612UnE
//https://developer.android.com/guide/topics/location/strategies
//https://developer.android.com/reference/android/location/Location

//layouts https://www.youtube.com/watch?v=Qhie_NDM_DQ

package com.e.jona.novoapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.e.jona.novoapp.OsmAndHelper.*;
import net.osmand.aidl.gpx.ImportGpxParams;
import net.osmand.aidl.gpx.StartGpxRecordingParams;
import net.osmand.aidl.gpx.StopGpxRecordingParams;
import net.osmand.aidl.map.ALatLon;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

//https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html

public class MainActivity extends AppCompatActivity {
    long counter = 1;

   Button botonGPS;
   TextView latitud, longitud;
    TextView velocidad, hora, distancia, referencia_tv, bearing_tv;

    static double distanceInMeters=0;
    static Location lastLocation = null;
    boolean una_vez=false;

    private Timer myTimer;

    MediaPlayer mp;
    private float currentDegree = 0f;
    float volumen_normal;
    PID pid;
    boolean x=false;
    float bearing=500;
    static Location lo=null;
    static Location dest=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitud= findViewById(R.id.latitud);
        longitud= findViewById(R.id.longitud);
        velocidad= findViewById(R.id.velocidad);
        hora= findViewById(R.id.hora);
        botonGPS = findViewById(R.id.boton);
        distancia=findViewById(R.id.distancia);

        referencia_tv=findViewById(R.id.referencia);
        bearing_tv=findViewById(R.id.bearing);


        lo=new Location("");
        lo.setLatitude(-0.07643581d);
        lo.setLongitude(-78.46171552d);


        dest=new Location("");
        dest.setLatitude(-0.0770185d);
        dest.setLongitude(-78.4617583d);

        float re=lo.bearingTo(dest);
        if (re<0) re=360+re;
        //inicialización del PID
        pid = new PID(1,0,0);
        pid.setOutputLimits(-100,100);
        //pid.setSetpoint(re);
        pid.setSetpoint(0);
        pid.setOutputFilter(0.1);



        referencia_tv.setText(""+re);

        volumen_normal=0.3f;
        mp=MediaPlayer.create(this,R.raw.cascada);



        botonGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (x==false) {
                    mp.start();
                    mp.setLooping(true);
                    x=true;
                }
                else{
                    mp.pause();
                    x=false;
                }

                if(una_vez==false){
                    una_vez=true;
                    // Acquire a reference to the system Location Manager
                    LocationManager locationManager = (LocationManager) MainActivity.this.getSystemService(Context.LOCATION_SERVICE); //le indicamos que vamos a sacar un servicio de localización
                    // Define a listener that responds to location updates
                    LocationListener locationListener = new LocationListener() {
                        public void onLocationChanged(Location location) {
                            if (lastLocation == null) {
                                lastLocation = location;
                            }
                            if (location.hasBearing()){
                                distanceInMeters += location.distanceTo(lastLocation);
                                lastLocation=location;
                                bearing = location.getBearing();
                                bearing_tv.setText(""+location.getBearing());
                            }
                            else
                            {
                                lastLocation=null;
                                bearing=500;
                            }
                            // Called when a new location is found by the network location provider.
                            latitud.setText(""+location.getLatitude());
                            longitud.setText(""+location.getLongitude());
                            velocidad.setText(""+location.getSpeed());
                           // hora.setText(""+location.getTime());  	//getAltitude()
                           // distancia.setText(""+distanceInMeters);

                        }
                        public void onStatusChanged(String provider, int status, Bundle extras) {}

                        public void onProviderEnabled(String provider) {}

                        public void onProviderDisabled(String provider) {}
                    };
                    int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
                    // Register the listener with the Location Manager to receive location updates
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
                    // locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

                }
            }
        });

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck==PackageManager.PERMISSION_DENIED)
        {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
            }
        }

        Thread t = new Thread(){
            @Override
            public void run(){
                try{
                    while(!isInterrupted()){
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                long date = System.currentTimeMillis();
                                //SimpleDateFormat sdf= new SimpleDateFormat("MMM dd yyyy\nhh-mm-ss a");
                                SimpleDateFormat sdf= new SimpleDateFormat("HH-mm-ss");
                                String date_string = sdf.format(date);
                                hora.setText(date_string);  	//getAltitude()
                            }
                        });
                    }
                }
                catch (InterruptedException e){
                }
            }
        };

        t.start();


        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                float copy=bearing;
                if(copy!=500&&x==true)
                {
                    double ley=pid.getOutput((double) copy);
                    float error= (float) pid.getError();
                    Log.d("Ley de control", String.valueOf(ley));
                    float[] temp = funcion_sonido_pid((float) ley, volumen_normal * 100,error, -20,20);
                    Log.d("Volumen l", String.valueOf(temp[0]));
                    Log.d("Volumen r", String.valueOf(temp[1]));
                    mp.setVolume(temp[0], temp[1]);
                }

                else{
                    mp.setVolume((float)0.0, (float)0.0);
                }
            }
        };
        myTimer = new Timer();
        myTimer.scheduleAtFixedRate(tt,0,200);

    }


    public  static  float[] funcion_sonido_pid(float ley, float maxV,float error, float hi, float hd)
    {
        float r;
        float l;
        if (error<hi||error>hd)
        {
            r = maxV - ley;
            if (r >= 100) r = 100;
            else if (r < 0) r = 0;

            l = maxV + ley;
            if (l >= 100) l = 100;
            else if (l < 0) l = 0;
        }
        else
        {
            r=maxV;
            l=maxV;
        }

        r= (float) (r/100.0);
        l= (float) (l/100.0);

        return new float[] {l,r};
    }
}
