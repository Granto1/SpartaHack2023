package com.example.stickapp;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F934FB");
    TextView txtIsConnected = findViewById(R.id.txtConnected);
    TextView txtData = findViewById(R.id.txtConnected);
    OutputStream outputStream;
    InputStream inputStream;
    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice hc05 = btAdapter.getRemoteDevice("00:21:13:02:B6:5B");
    BluetoothSocket btSocket = null;
    private ReadInput mReadThread = null;
    boolean mIsBluetoothConnected = false;

    //TTS Stuff
    TextToSpeech mTTS;
    String[] speech = new String[]{"Watch out!", "Way too close", "Too Close", "Ok Distance", "No Danger"};

    //Vibrate service
    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    //USB Camera feed?


    final static String on="1984";//on
    final static String off="551";//off
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTTS = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                mTTS.setLanguage(Locale.ENGLISH);
            }
        });
    }

    @Override
    protected void onPause() {
        if (btSocket != null && mIsBluetoothConnected) {
            new DisConnectBT().execute();
        }
        Log.d(TAG, "Paused");
        super.onPause();
    }

    //After create, app continues
    @Override
    protected void onResume() {
        if (btSocket == null || !mIsBluetoothConnected) { //if either are true, something's wrong
            new ConnectBT().execute(); //search for bluetooth device
        }
        //Log.d(TAG, "Resumed");
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Stopped");
        super.onStop();
    } //When app stop

    @SuppressLint("StaticFieldLeak")
    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean mConnectSuccessful = true; //if connection is established

        @Override
        protected void onPreExecute() {}
        @SuppressLint("MissingPermission")
        @Override
        protected Void doInBackground(Void... devices) {
            //look for bluetooth device
            try {
                if (btSocket == null || !mIsBluetoothConnected) { //if either are true, it's a problem
                    btSocket = hc05.createInsecureRfcommSocketToServiceRecord(mUUID); //search for bluetooth
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                // Unable to connect to device`
                // e.printStackTrace();
                mConnectSuccessful = false;
            }
            return null;
        } //Look for bluetooth device and connect
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if (!mConnectSuccessful) { //if not successful, continue on
                finish();
            } //Unsuccessful connect
            else { //start the input cycle
                mIsBluetoothConnected = true;
                //Start the arduino sequence
                try {
                    outputStream = btSocket.getOutputStream();
                    btSocket.getOutputStream().write(on.getBytes()); //Write the on sequence
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mReadThread = new ReadInput(); // Kick off input reader
                txtIsConnected.setText("Connected!!!!");
            } //Successful connect
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class DisConnectBT extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (mReadThread != null) {
                mReadThread.stop();
                while(mReadThread.isRunning()) {
                    ;
                }
                mReadThread = null;
            }
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();}
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mIsBluetoothConnected = false;
//            if (mIsUserInitiatedDisconnect) {
//                finish();
//            }
        }
    }

    private class ReadInput implements Runnable {
        private boolean bStop = false;
        private Thread t;
        public ReadInput() {
            t = new Thread(this, "Input Thread");
            t.start();
        }
        public boolean isRunning() {
            return t.isAlive();
        }
        @SuppressLint("SetTextI18n")
        @Override
        public void run() {
            int reading = -1;
            try {
                inputStream = btSocket.getInputStream();
                while(!bStop) {
                    byte[] buffer = new byte[256];
                    if (inputStream.available() > 0) {
                        DataInputStream mmInStream = new DataInputStream(inputStream);
                        int bytes = mmInStream.read(buffer);
                        //inputStream.read(buffer);
                        //int i = 0;
                        //while (i < buffer.length && buffer[i] != 0) {i++;}
                        String strInput = new String(buffer, 0, bytes); //Need to define String structure....
                        String str = new String(buffer, StandardCharsets.UTF_8);
                        txtData.setText(strInput + "\n" + str); //Throw the text onto the phone app
                        reading = Integer.parseInt(strInput); //reading from Arduino
                    }
                    response(reading);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //TextToSpeech sequence and vibration reaction
        public void response(int distance) {
            if (distance < 4) { //if we reach a dangerous position value
                mTTS.speak(speech[distance - 1], TextToSpeech.QUEUE_FLUSH, null);
                //A little bit of trolling
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(100 * distance, distance));
                }
                else {
                    v.vibrate(100 * distance);
                }
            }
            else {
                mTTS.speak(speech[4], TextToSpeech.QUEUE_FLUSH, null);
                v.vibrate(500);
            }
        }
        public void stop() {
            bStop = true;
        }
    }
}