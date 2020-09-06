package com.example.exoplayerdemo;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";
    private ImageView playStopBtn, gifImageView;
    private DrawableImageViewTarget imageViewTarget;
    private boolean mBound = false;
    private MusicService musicService;
    private String streamUrl = "https://download.mp3-direct.xyz/o/The-Weeknd-The-Hills.mp3";
    //  private String streamUrl = "https://download.mp3-direct.xyz/s/Quando-Rondo-Safest.mp3";
    private static final int READ_PHONE_STATE_REQUEST_CODE = 22;
    private BroadcastReceiver broadcastReceiver;
    private Snackbar snackbar;
    private AudioManager audio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initRegBroadcast();
        processPhoneListenerPermission();
        createNotificationChannel();

        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        snackbar = Snackbar.make(findViewById(R.id.myCoordinatorLayout), "No internet connection", Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("DISMISS", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
            }
        });

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
                if (tm != null) {
                    if (tm.getCallState() == TelephonyManager.CALL_STATE_RINGING) {
                        if (musicService.isPlaying()) {
                            musicService.stop();
                            playStopBtn.setImageResource(R.drawable.ic_play);
                        }
                    }
                }

                if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                    if (networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                        if (snackbar.isShown()) {
                            snackbar.dismiss();
                        }
                        playStopBtn.setEnabled(true);
                    } else if (networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTED) {
                        playStopBtn.setEnabled(false);
                        snackbar.show();
                    }
                }

                String playerState = intent.getStringExtra("state");
                if (playerState == MusicService.buffering) {
                    Glide.with(MainActivity.this).load(R.drawable.not_playing).into(imageViewTarget);
                    playStopBtn.setImageResource(R.drawable.ic_pause);
                } else if (playerState == MusicService.playing) {
                    playStopBtn.setImageResource(R.drawable.ic_pause);
                    Glide.with(MainActivity.this).load(R.drawable.playing).into(imageViewTarget);
                } else if (playerState == MusicService.paused) {
                    playStopBtn.setImageResource(R.drawable.ic_play);
                    Glide.with(MainActivity.this).load(R.drawable.not_playing).into(imageViewTarget);
                } else if (playerState == MusicService.ended) {
                    playStopBtn.setImageResource(R.drawable.ic_play);
                    Glide.with(MainActivity.this).load(R.drawable.not_playing).into(imageViewTarget);
                }

            }
        };

    }

    private void initRegBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(broadcastReceiver, filter);
    }

    private void initView() {
        playStopBtn = findViewById(R.id.playStopBtn);
        gifImageView = findViewById(R.id.gifImageView);
        imageViewTarget = new DrawableImageViewTarget(gifImageView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(MainActivity.this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver((broadcastReceiver),
                new IntentFilter("com.example.exoplayerdemo.PLAYER_STATUS")
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (musicService != null && musicService.isPlaying()) {
            playStopBtn.setImageResource(R.drawable.ic_pause);
            Glide.with(MainActivity.this).load(R.drawable.playing).into(imageViewTarget);
        }
    }

    @Override
    protected void onDestroy() {
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
        super.onDestroy();
    }

    public void playStop(View view) {
        if (!musicService.isPlaying()) {
            musicService.play(streamUrl);
        } else {
            musicService.stop();
        }
        Animation animFadein = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);
        playStopBtn.startAnimation(animFadein);
    }

    private void processPhoneListenerPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, READ_PHONE_STATE_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == READ_PHONE_STATE_REQUEST_CODE) {
            if (!(grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(getApplicationContext(),
                        R.string.permision_msg,
                        Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Streaming Radio";
            String description = "test channel for streaming radio";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("radio_playback_channel", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    //ServiceConnection for establishing communication betwn. activity and service

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            MusicService.MusicBinder mServiceBinder = (MusicService.MusicBinder) iBinder;
            musicService = mServiceBinder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            System.exit(0);
        }
    };

}
