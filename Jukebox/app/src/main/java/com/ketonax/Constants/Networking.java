package com.ketonax.Constants;

import com.ketonax.Networking.MessageBuilder;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Created by nightfall on 7/13/14.
 */
public final class Networking {
    /* Commands from server */
    public static final String SEND_SONG_CMD = "/send_song";
    public static final String SEND_SONG_TO_USER_CMD = "/send_song_to_user";

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
    public static final String SONG_DOWNLOADED_NOTIFIER = "/song_downloaded";
    public static final String EXIT_JUKEBOX_NOTIFIER = "/jukebox_user_exit";

    /* Response to devices  */
    public static final String STATION_LIST_REQUEST_RESPONSE = "/station_list_response";
    public static final String USER_ON_LIST_RESPONSE = "/user_on_list";
    public static final String SONG_ON_LIST_RESPONSE = "/song_on_list";

    /* Ping Communication */
    public static final String PING = "/ping";
    public static final String PING_RESPONSE = "/ping_response";

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

    /* TCP PORT */
    public static final int TCP_PORT_NUMBER = 5000;


    /* Multicast */
    public static final String MULTICAST_IP_STRING = "225.4.5.6";
    public static final int GROUP_PORT = 61002;
    private static MulticastSocket groupSocket = null;

    private static DatagramSocket udpSocket = null;

    public static String getLocalIP(){
        /** Returns devices IP address */

        String ip = null;
        Enumeration<NetworkInterface> interfaces = null;

        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while (interfaces.hasMoreElements()) {
            NetworkInterface current = interfaces.nextElement();

            try {
                if (!current.isUp() || current.isLoopback()
                        || current.isVirtual())
                    continue;
            } catch (SocketException e) {
                e.printStackTrace();
            }

            Enumeration<InetAddress> addresses = current.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress current_addr = addresses.nextElement();
                if (current_addr instanceof Inet4Address)
                    ip = current_addr.getHostAddress();
            }
        }

        return ip;
    }

    public static DatagramSocket getSocket(){
        /** Returns a UDP socket */

        if(udpSocket == null){
            try {
                udpSocket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        return udpSocket;
    }

    public static MulticastSocket getMulticastSocket(){
        /** Returns Multicast socket for group messages */

        if(groupSocket == null){
            try {
                groupSocket = new MulticastSocket(GROUP_PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return groupSocket;
    }

    public static InetAddress getGroupAddress() {
        /** Returns InetAddress for multicast */

        InetAddress groupAddress = null;
        try {
            groupAddress = InetAddress.getByName(MULTICAST_IP_STRING);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return groupAddress;
    }

    public static String getIPString(SocketAddress socketAddress) {

		/* Parse userSocketAddress */
        String address[] = socketAddress.toString().split(":");
        String ipString = address[0];

		/* Check to see if userIP contains '/' and remove it */
        if (ipString.startsWith("/"))
            ipString = ipString.replaceFirst("/", "");

        return ipString;
    }

    public static int getPort(SocketAddress socketAddress) {

        String address[] = socketAddress.toString().split(":");
        int userPort = Integer.parseInt(address[1]);

        return userPort;
    }

    public static String buildSendSongCommand(String stationName, String songName) {

        String[] elements = { SEND_SONG_CMD, stationName, songName };
        MessageBuilder.buildMessage(elements, SEPARATOR);
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildSendSongToUserCommand(String stationName, String songName, SocketAddress userSocketAddress){

        String destIP = getIPString(userSocketAddress);
        String[] elements = {SEND_SONG_TO_USER_CMD, stationName, songName, destIP};
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);
        return message;
    }

    public static String buildJoinStationCommand(String stationName) {

        String[] elements = { JOIN_STATION_CMD, stationName };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildLeaveStationCommand(String stationName) {

        String[] elements = { LEAVE_STATION_CMD, stationName };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildAddSongCommand(String stationName, String songName,
                                             String songLength) {

        String[] elements = { ADD_SONG_CMD, stationName, songName, songLength };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildGetPlaylistCommand(String stationName) {

        String[] elements = { GET_PLAYLIST_CMD, stationName };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildCreateStationCommand(String stationName) {

        String[] elements = { CREATE_STATION_CMD, stationName };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildStationListRequestCommand() {

        String[] elements = { STATION_LIST_REQUEST_CMD };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildStationListRequestResponse(String stationName) {

        String[] elements = { STATION_LIST_REQUEST_RESPONSE, stationName };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildStationKilledNotifier(String stationName) {

        String[] elements = { STATION_KILLED_NOTIFIER, stationName };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildStationAddedNotifier(String stationName) {

        String[] elements = { STATION_ADDED_NOTIFIER, stationName };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildSongAddedNotifier(String stationName,
                                                String songName) {

        String[] elements = { SONG_ADDED_NOTIFIER, stationName, songName };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildSongRemovedNotifier(String stationName,
                                                  String songName) {

        String[] elements = { SONG_REMOVED_NOTIFIER, stationName };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildUserAddedNotifier(String stationName,
                                                String userSocketAddress) {

        String[] elements = { USER_ADDED_NOTIFIER, stationName,
                userSocketAddress };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildUserRemovedNotifier(String stationName,
                                                  String userSocketAddress) {

        String[] elements = { USER_REMOVED_NOTIFIER, stationName,
                userSocketAddress };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildCurrentlyPlayingNotifier(String stationName,
                                                       String songName, String holderSocketAddress, String songPosition) {

        String[] elements = { CURRENTLY_PLAYING_NOTIFIER, stationName,
                songName, holderSocketAddress, songPosition };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildExitJukeboxNotifier() {

        String[] elements = { EXIT_JUKEBOX_NOTIFIER };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildSongDownloadedNotifier(String stationName, String songName) {

        String[] elements = { SONG_DOWNLOADED_NOTIFIER, stationName, songName };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildSongOnListResponse(String stationName,
                                                 String songName) {

        String[] elements = { SONG_ON_LIST_RESPONSE, stationName, songName };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildUserOnListResponse(String stationName,
                                                 String userSocketAddress) {

        String[] elements = { USER_ON_LIST_RESPONSE, stationName,
                userSocketAddress };
        String message = MessageBuilder.buildMessage(elements, SEPARATOR);

        return message;
    }

    public static String buildPingMessage() {
        return PING;
    }

    public static String buildPingResponse() {

        String[] elements = { PING_RESPONSE };

        String message = MessageBuilder.buildMessage(elements, SEPARATOR);
        return message;
    }
}
