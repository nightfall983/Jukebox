package com.ketonax.Constants;

/**
 * Created by nightfall on 7/14/14.
 */
public final class AppConstants {
    /* Constants */
    public final static String APP_TAG = "com.ketonax.jukebox";
    public final static String SERVICE_CONNECTED_STATUS = "service connected status";
    public final static String STATION_LIST_KEY = "station list";
    public final static String CURRENT_STATION_KEY = "current station";
    public final static String STATION_NAME_KEY = "station name";
    public final static String SONG_NAME_KEY = "song name";
    public final static String SONG_LENGTH_KEY = "song length";
    public final static String TRACK_POSITION_KEY = "track position";
    public final static String SONG_PATH_KEY = "song path ";
    public final static String USER_IP_KEY = "user ip";
    public final static String USER_UDP_PORT_KEY = "udp port";
    public final static String PING_RESPONSE_KEY = "ping response";

    /* The fragment argument representing the section number for a fragment. */
    public final static String ARG_SECTION_NUMBER = "section_number";

    /* Message Integer Constants */
    public final static int MSG_REGISTER_CLIENT = 1;
    public final static int MSG_UNREGISTER_CLIENT = 2;

    /* Commands To Devices */
    public static final int SEND_SONG_CMD = 3;
    public static final int SEND_SONG_TO_USER_CMD = 4;

    /* Commands to server */
    public static final int CREATE_STATION_CMD = 5;
    public static final int STATION_LIST_REQUEST_CMD = 6;
    public static final int JOIN_STATION_CMD = 7;
    public static final int LEAVE_STATION_CMD = 8;
    public static final int ADD_SONG_CMD = 9;
    public static final int GET_PLAYLIST_CMD = 10;

    /* Notifications to server */
    public static final int EXIT_JUKEBOX_NOTIFIER = 11;
    public static final int SONG_DOWNLOADED_NOTIFIER = 12;

    /* Notifications from server */
    public static final int STATION_KILLED_NOTIFIER = 13;
    public static final int STATION_ADDED_NOTIFIER = 14;
    public static final int SONG_ADDED_NOTIFIER = 15;
    public static final int SONG_REMOVED_NOTIFIER = 16;
    public static final int USER_ADDED_NOTIFIER = 17;
    public static final int USER_REMOVED_NOTIFIER = 18;
    public static final int CURRENTLY_PLAYING_NOTIFIER = 19;

    /* Response to devices */
    public static final int STATION_LIST_REQUEST_RESPONSE = 20;
    public static final int USER_ON_LIST_RESPONSE = 21;
    public static final int SONG_ON_LIST_RESPONSE = 22;

    /* TCP Receiver notification */
    public static final int SONG_DOWNLOADED = 23;

    /* Ping Communication */
    public static final int PING = 24;
    public static final int PING_RESPONSE = 25;
}
