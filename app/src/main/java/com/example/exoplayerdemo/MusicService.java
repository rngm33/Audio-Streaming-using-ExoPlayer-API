package com.example.exoplayerdemo;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MusicService extends Service {

    private static String TAG = "MusicService";
    private SimpleExoPlayer player;
    private LocalBroadcastManager broadcaster;
    private final Binder mBinder = new MusicBinder();
    private PlayerNotificationManager playerNotificationManager;
    final static String buffering="STATE_BUFFERING";
    final static String playing="STATE_PLAYING";
    final static String paused="STATE_PAUSED";
    final static String ended="STATE_ENDED";

    @Override
    public void onCreate() {
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        AdaptiveTrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);
        player = ExoPlayerFactory.newSimpleInstance(getApplicationContext(), trackSelector);
        broadcaster = LocalBroadcastManager.getInstance(this);

        player.addListener(new SimpleExoPlayer.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == ExoPlayer.STATE_BUFFERING) {
                    Intent intent = new Intent("com.example.exoplayerdemo.PLAYER_STATUS");
                    intent.putExtra("state",buffering);
                    broadcaster.sendBroadcast(intent);
                } else if (playbackState == ExoPlayer.STATE_READY) {
                    Intent intent = new Intent("com.example.exoplayerdemo.PLAYER_STATUS");
                    if (playWhenReady) {
                        intent.putExtra("state", playing);
                    } else {
                        intent.putExtra("state", paused);
                    }

                    broadcaster.sendBroadcast(intent);
                }
                else if(playbackState== ExoPlayer.STATE_ENDED){
                    Intent intent = new Intent("com.example.exoplayerdemo.PLAYER_STATUS");
                    intent.putExtra("state", ended);
                    broadcaster.sendBroadcast(intent);
                }
            }
        });

        playerNotificationManager = new PlayerNotificationManager(
                this,
                "radio_playback_channel",
                100, new DescriptionAdapter(this));
        playerNotificationManager.setPlayer(player);
        // omit skip previous and next actions
        playerNotificationManager.setUseNavigationActions(false);
        // omit fast forward action by setting the increment to zero
        playerNotificationManager.setFastForwardIncrementMs(0);
        // omit rewind action by setting the increment to zero
        playerNotificationManager.setRewindIncrementMs(0);
        // omit the stop action
        playerNotificationManager.setUseStopAction(false);

        playerNotificationManager.setColor(Color.BLACK);
        playerNotificationManager.setColorized(true);
        playerNotificationManager.setUseChronometer(true);
        playerNotificationManager.setSmallIcon(R.drawable.ic_stat_radio);
        playerNotificationManager.setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE);

        super.onCreate();
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        playerNotificationManager.setPlayer(null);
        return super.onUnbind(intent);
    }

    public void play(String channelUrl) {
//        if(player.getCurrentPosition()!=0){
//            player.seekTo(player.getCurrentPosition());
//        }
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getApplicationContext(), "ExoPlayerDemo");
            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
            Handler mainHandler = new Handler();
            MediaSource mediaSource = new ExtractorMediaSource(Uri.parse(channelUrl), dataSourceFactory, extractorsFactory, mainHandler, null);
            player.prepare(mediaSource);
            player.setPlayWhenReady(true);

    }

    public void stop() {
        player.setPlayWhenReady(false);
        player.stop();
    }

    public boolean isPlaying() {
        return player.getPlaybackState() == Player.STATE_READY;
    }
}
