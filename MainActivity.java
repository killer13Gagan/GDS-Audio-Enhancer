package com.example.gdsaudioenhanceser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private static final float AMPLIFICATION_FACTOR = 1.5f; // Adjust the amplification factor as needed

    private NoiseSuppressor noiseSuppressor;
    private AudioRecord audioRecord;
    int Count = 0, PCount = 0;

    FloatingActionButton floatingActionButtonRecord, floatingActionButtonPlay;
    Button btn;
    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;
    private MediaRecorder recorder = null;
    private MediaPlayer player = null;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Switch aSwitch = findViewById(R.id.switch1);
        floatingActionButtonRecord = findViewById(R.id.floatingActionButtonRecord);
        floatingActionButtonPlay = findViewById(R.id.floatingActionButtonPlay);
        btn = findViewById(R.id.button);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Switch switchBluetooth = findViewById(R.id.switch1);

        switchBluetooth.setChecked(bluetoothAdapter.isEnabled());

        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/audiorecordtest.3gp";

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);


        switchBluetooth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Toggle Bluetooth based on the Switch state
                if (isChecked) {
                    enableBluetooth();
                } else {
                    disableBluetooth();
                }
            }
        });

        initializeNoiseSuppressor();
    }

    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            Toast.makeText(this, "Turning On Bluetooth", Toast.LENGTH_SHORT).show();
            startActivityForResult(enableBtIntent, 1);
        }
    }

    private void disableBluetooth() {
        if (bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            Toast.makeText(this, "Turning off Bluetooth", Toast.LENGTH_SHORT).show();
            bluetoothAdapter.disable();
        }
    }

    private void initializeNoiseSuppressor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int audioSessionId = AudioRecord.getMinBufferSize(
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            );

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            audioRecord = new AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(44100)
                            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                            .build())
                    .setBufferSizeInBytes(audioSessionId)
                    .build();

            noiseSuppressor = NoiseSuppressor.create(audioRecord.getAudioSessionId());
            if (noiseSuppressor != null) {
                noiseSuppressor.setEnabled(true);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecordAccepted) finish();

    }

    public void startPlaying(View view) {
        if (PCount == 0) {
            btn.setText("STOP");
//            floatingActionButtonPlay.setImageDrawable(getDrawable(R.drawable.baseline_pause_24));
            player = new MediaPlayer();
            try {
                player.setDataSource(fileName);
                player.prepare();
                player.setVolume(AMPLIFICATION_FACTOR, AMPLIFICATION_FACTOR);
                player.start();
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
            }
            PCount = 1;
        } else {
            btn.setText("Amplify and Play");
//            floatingActionButtonPlay.setImageDrawable(getDrawable(R.drawable.baseline_play_arrow_24)); // stops listening
            player.release();
            player = null;
            PCount = 0;
        }
    }

    public void startRecording(View view) {
        if (Count == 0) {
            floatingActionButtonRecord.setImageDrawable(getDrawable(R.drawable.baseline_mic_24));
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(fileName);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            try {
                recorder.prepare();
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
            }
            recorder.start();

            if (noiseSuppressor != null) {
                noiseSuppressor.setEnabled(true);
            }
            Count = 1;
        } else {
            floatingActionButtonRecord.setImageDrawable(getDrawable(R.drawable.baseline_mic_off_24)); // stops listening
            recorder.stop();
            recorder.release();
            recorder = null;

            if (noiseSuppressor != null) {
                noiseSuppressor.setEnabled(false);
                noiseSuppressor.release();
            }
            Count = 0;

        }
    }

    public void amp(View view){
        amplifyAudio(fileName);
    }
    private void amplifyAudio(String filePath){
        try{
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();

            mediaPlayer.setVolume(AMPLIFICATION_FACTOR, AMPLIFICATION_FACTOR);

            mediaPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error amplifying audio");
        }
    }

}