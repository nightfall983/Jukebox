package com.ketonax.Constants;

/**
 * Created by nightfall on 7/14/14.
 */
public final class AppConstants {
    /* Constants */
    public final static String APP_TAG = "com.ketonax.jukebox";
    public final static String SERVER_MESSAGE = "server message";

    /* Broadcast Actions */
    public final static String ACTION_SERVER_MESSAGE_RECEIVED = "server message received action";

    /* Message Integer Constants */
    public final static int MSG_REGISTER_CLIENT = 1;
    public final static int MSG_UNREGISTER_CLIENT = 2;

    /* Commands To Devices */
    public static final int PLAY_SONG_CMD = 3;

    /* Commands to server */
    public static final int CREATE_STATION_CMD = 4;
    public static final int STATION_LIST_REQUEST_CMD = 5;
    public static final int JOIN_STATION_CMD = 6;
    public static final int LEAVE_STATION_CMD = 7;
    public static final int ADD_SONG_CMD = 8;
    public static final int GET_PLAYLIST_CMD = 9;

    /* Notifications to server */
    public static final int EXIT_JUKEBOX_NOTIFIER = 10;

    /* Notifications from server */
    public static final int STATION_LIST_NOTIFIER = 11;
    public static final int STATION_KILLED_NOTIFIER = 12;
    public static final int STATION_ADDED_NOTIFIER = 13;
    public static final int SONG_ADDED_NOTIFIER = 14;
    public static final int SONG_REMOVED_NOTIFIER = 15;
    public static final int USER_ADDED_NOTIFIER = 16;
    public static final int USER_REMOVED_NOTIFIER = 17;
    public static final int CURRENTLY_PLAYING_NOTIFIER = 18;

    /* Response to devices */
    public static final int SONG_ON_LIST_RESPONSE = 19;
    public static final int USER_ON_LIST_RESPONSE = 20;
}
