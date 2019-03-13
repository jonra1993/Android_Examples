package com.e.jona.sonidos;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicInteger;


// tutorial https://www.youtube.com/watch?v=Gfm_nDm6Ues
//https://dzone.com/articles/playing-sounds-android sound
//sensores https://developer.android.com/guide/topics/sensors/sensors_overview
//https://www.youtube.com/user/josedlujan1/search?query=navigation
//brujul http://agamboadev.esy.es/como-crear-un-brujula-en-android/


public class MainActivity extends AppCompatActivity implements SensorEventListener{

    ImageButton ibtnMP, ibtnSP;
    ImageView flecha, co;
    TextView texto,ms;
    MediaPlayer mp;
    SoundPool sp;
    int song=0;
    Sensor magnetometro;
    Sensor acelerometro;
    SensorManager sm;


    boolean x=false;
    boolean y=false;
    float leftv=(float)1;
    float rightv=(float)1;

    float[] mGravity;
    float[] mGeomagnetic;
    private float currentDegree = 0f;
    // Los angulos del movimiento de la flecha que señala al norte
    float degree;
    float volumen_normal;
    // Guarda el valor del azimut
    float azimut;
    PID pid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //inicialización del PID
        pid = new PID(1,0,0);
        pid.setOutputLimits(-100,100);
        pid.setSetpoint(0);
        pid.setOutputFilter(0.1);


        volumen_normal=0f;

        co=findViewById(R.id.imageViewer1);
        ms=findViewById(R.id.tvtitulo);
        texto=findViewById(R.id.texto);
        flecha=findViewById(R.id.flecha);
        ibtnMP=findViewById(R.id.ibtnMP);
        ibtnSP=findViewById(R.id.ibtnSP);
        mp=MediaPlayer.create(this,R.raw.cascada);
        sp=new SoundPool(1, AudioManager.STREAM_MUSIC,1);
        song=sp.load(this,R.raw.cascada,1);
        sm=(SensorManager) getSystemService(SENSOR_SERVICE);
        magnetometro=sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        acelerometro=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this,magnetometro,SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this,acelerometro,SensorManager.SENSOR_DELAY_NORMAL);

        if (magnetometro != null){
            // Success! There's a magnetometer.
        }
        else {
            // Failure! No magnetometer.
        }


        ibtnMP.setOnClickListener(new View.OnClickListener() {
            double leydecontrol=pid.getOutput(10);

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
            }
        });

        ibtnSP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (y==false) {
                    //sp.play(song,leftv,rightv,1,0,1);
                   // sp.play(song,leftv,rightv,1,-1,1);//por siempre
                    volumen_normal=0.3f;
                    y=true;
                }
                else{
                   //sp.autoPause();
                    //sp.pause(song);
                    //sp.stop(song);
                    volumen_normal=0f;
                    y=false;
                }
            }
        });
        



    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType())
        {
            case Sensor.TYPE_ACCELEROMETER:
                mGravity = sensorEvent.values;
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                mGeomagnetic = sensorEvent.values;
                break;
        }
        //cuando varia un dato del magnetometro
        if ((mGravity != null) && (mGeomagnetic != null)) {
            float RotationMatrix[] = new float[16];
            boolean success = SensorManager.getRotationMatrix(RotationMatrix,                                                             null, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(RotationMatrix, orientation);
                azimut = orientation[0] * (180 / (float) Math.PI);
            }
        }
        degree = azimut;
        texto.setText("Ángulo: " + Float.toString(degree) + " degrees");
        // se crea la animacion de la rottacion (se revierte el giro en grados, negativo)

        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        // el tiempo durante el cual la animación se llevará a cabo
        ra.setDuration(200);
        // establecer la animación después del final de la estado de reserva
        ra.setFillAfter(true);
        // Inicio de la animacion
        flecha.startAnimation(ra);
        currentDegree = -degree;

       /* if (y==true) {
            //float[]temp=funcion_sonido(degree, (float)0.3);
            //sp.setVolume(song, temp[0],temp[1]);
        }*/

        if (x==true) {

            double ley=pid.getOutput((double) currentDegree);
            float error= (float) pid.getError();
            //Log.d("Ley de control", String.valueOf(ley));
            //float[]temp=funcion_sonido(degree,volumen_normal);
            float[] temp = funcion_sonido_pid((float) ley, volumen_normal * 100,error, -20,20);
            // Log.d("Volumen l", String.valueOf(temp[0]));
            // Log.d("Volumen r", String.valueOf(temp[1]));
            mp.setVolume(temp[0], temp[1]);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public  static  float[] funcion_sonido(float deg, float maxV)
    {
        float tempo;
        if (deg<0)
        {
            tempo=deg*-1 / -180;
        }
        else
        {
            tempo=deg*1 / 180;
        }
        float r = maxV-tempo;
        if(r>=1) r=1;
        else if (r<0) r=0;

        float l= maxV + tempo;
        if(l>=1) l=1;
        else if (l<0) l=0;

        return new float[] {l,r};
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
