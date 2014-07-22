package com.ketonax.Constants;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by nightfall on 7/13/14.
 */
public final class Networking {
    /* Commands from server */
    public static final String PLAY_SONG_CMD = "/play_song";

    /* Commands to server */
    public static final String CREATE_STATION_CMD = "/new_station";
    public static final String STATION_LIST_REQUEST_CMD = "/request_station_list";
    public static final String JOIN_STATION_CMD = "/join_station";
    public static final String LEAVE_STATION_CMD = "/leave_station";
    public static final String ADD_SONG_CMD = "/add_song";
    public static final String GET_PLAYLIST_CMD = "/get_playlist";

    /* Notifications from server */
    public static final String STATION_KILLED_NOTIFIER = "/station_terminated";
    public static final String STATION_ADDED_NOTIFIER = "/station_added";
    public static final String SONG_ADDED_NOTIFIER = "/song_added";
    public static final String SONG_REMOVED_NOTIFIER = "/song_removed";
    public static final String USER_ADDED_NOTIFIER = "/user_added";
    public static final String USER_REMOVED_NOTIFIER = "/user_removed";
    public static final String CURRENTLY_PLAYING_NOTIFIER = "/currently_playing";

    /* Notifications to server */
    public static final String EXIT_JUKEBOX_NOTIFIER = "/jukebox_user_exit";

    /* Response to devices  */
    public static final String STATION_LIST_REQUEST_RESPONSE = "/station_list_response";
    public static final String USER_ON_LIST_RESPONSE = "/user_on_list";

    /* Separator string */
    public static final String SEPARATOR = ",";

    /* Message identifier */
    public static final String JUKEBOX_MESSAGE_IDENTIFIER = "/";

    /* Maximum size for data */
    public static final int DATA_LIMIT_SIZE = 1024;

    /* Server IP address */
    public static final String SERVER_IP_STRING = "192.168.206.87";

    /* Server port */
    public static final int SERVER_PORT = 61001;

    /* Multicast */
    public static final String MULTICAST_IP_STRING = "225.4.5.6";
    public static final int GROUP_PORT = 61002;
    private static MulticastSocket groupSocket = null;

    private static DatagramSocket udpSocket = null;

    public static DatagramSocket getSocket(){

        if(udpSocket == null){
            try {
                udpSocket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        return udpSocket;
    }

    /** Returns Multicast socket for group messages */
    public static MulticastSocket getMulticastSocket(){

        if(groupSocket == null){
            try {
                groupSocket = new MulticastSocket(GROUP_PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return groupSocket;
    }

    /** Returns InetAddress for multicast */
    public static InetAddress getGroupAddress() {

        InetAddress groupAddress = null;
        try {
            groupAddress = InetAddress.getByName(MULTICAST_IP_STRING);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return groupAddress;
    }
}
