package com.ketonax.jukebox.Playback;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import com.ketonax.jukebox.Activities.MainActivity;
import com.ketonax.jukebox.R;

import java.io.IOException;

public class PlayMusicService extends Service {

    public static String SONG_NAME = "song name";
    public static String PATH_TO_SONG = "path to song";
    public static String TRACK__POSITION = "track position";
    private MediaPlayer mediaPlayer;
    private boolean isPlaying;
    private static int classID = 579;

    public PlayMusicService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String songName = intent.getStringExtra(SONG_NAME);
        String songPath = intent.getStringExtra(PATH_TO_SONG);
        int trackPosition = intent.getIntExtra(TRACK__POSITION, 0);
        if(songName != null && songPath != null)
            play(songName, songPath, trackPosition);

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        stop();
    }

    private void play(String songName, String path, int trackPosition) {
        /** Start playing music */

        isPlaying = true;

        /* Set up notification */
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent notificationIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new Notification.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Now playing " + songName)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(notificationIntent).build();

        /* Prepare media player */
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.seekTo(trackPosition);
            mediaPlayer.start();

            /* Remove notification if music stops */
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    stopForeground(true);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* Show notification */
        startForeground(classID, notification);
    }

    private void stop() {

        if(isPlaying){
            isPlaying = false;
            if(mediaPlayer != null){
                mediaPlayer.release();
                mediaPlayer = null;
            }
            stopForeground(true);
        }
    }
}
