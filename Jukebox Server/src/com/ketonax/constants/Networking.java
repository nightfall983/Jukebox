package com.ketonax.constants;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import com.ketonax.networking.MessageBuilder;

public final class Networking {
	/* Commands To Devices */
	public static final String SEND_SONG_CMD = "/send_song";
	public static final String SEND_SONG_TO_USER_CMD = "/send_song_to_user";

	/* Commands from devices */
	public static final String JOIN_STATION_CMD = "/join_station";
	public static final String LEAVE_STATION_CMD = "/leave_station";
	public static final String ADD_SONG_CMD = "/add_song";
	public static final String GET_PLAYLIST_CMD = "/get_playlist";

	/* Commands To Server */
	public static final String CREATE_STATION_CMD = "/new_station";
	public static final String STATION_LIST_REQUEST_CMD = "/request_station_list";

	/* Notifications to devices */
	public static final String STATION_LIST_REQUEST_RESPONSE = "/station_list_response";
	public static final String STATION_KILLED_NOTIFIER = "/station_terminated";
	public static final String STATION_ADDED_NOTIFIER = "/station_added";
	public static final String SONG_ADDED_NOTIFIER = "/song_added";
	public static final String SONG_REMOVED_NOTIFIER = "/song_removed";
	public static final String USER_ADDED_NOTIFIER = "/user_added";
	public static final String USER_REMOVED_NOTIFIER = "/user_removed";
	public static final String CURRENTLY_PLAYING_NOTIFIER = "/currently_playing";

	/* Notifications to server */
	public static final String EXIT_JUKEBOX_NOTIFIER = "/jukebox_user_exit";
	public static final String SONG_DOWNLOADED_NOTIFIER = "/song_downloaded";

	/* Response to devices */
	public static final String SONG_ON_LIST_RESPONSE = "/song_on_list";
	public static final String USER_ON_LIST_RESPONSE = "/user_on_list";

	/* Ping Communication */
	public static final String PING = "/ping";
	public static final String PING_RESPONSE = "/ping_response";

	/* Constants */
	public static final int DATA_LIMIT_SIZE = 1024;

	/* Separator string */
	public static final String SEPARATOR = ",";

	/* Message identifier */
	public static final String JUKEBOX_MESSAGE_IDENTIFIER = "/";

	/* Server IP address */
	public static final String SERVER_IP_STRING = "localhost";

	/* Server port */
	public static final int SERVER_PORT = 61001;

	/* Multicast */
	public static final String MULTICAST_IP_STRING = "225.4.5.6";
	public static final int GROUP_PORT = 61002;

	public static InetAddress getGroupAddress() {
		/* Returns InetAddress for multicast */

		InetAddress groupAddress = null;
		try {
			groupAddress = InetAddress.getByName(MULTICAST_IP_STRING);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return groupAddress;
	}

	public static String getIPString(SocketAddress socketAddress) {
		String ipString = null;
		
		/* Parse userSocketAddress */
		if(socketAddress != null){
			String address[] = socketAddress.toString().split(":");
			ipString = address[0];

			/* Check to see if userIP contains '/' and remove it */
			if (ipString.startsWith("/"))
				ipString = ipString.replaceFirst("/", "");
		}
		return ipString;
	}

	public static int getPort(SocketAddress socketAddress) {

		String address[] = socketAddress.toString().split(":");
		int userPort = Integer.parseInt(address[1]);
		return userPort;
	}

	public static String buildSendSongCommand(String stationName,
			String songName) {

		String[] elements = { SEND_SONG_CMD, stationName, songName };
		MessageBuilder.buildMessage(elements, SEPARATOR);
		String message = MessageBuilder.buildMessage(elements, SEPARATOR);
		return message;
	}

	public static String buildSendSongToUserCommand(String stationName,
			String songName, SocketAddress userSocketAddress) {

		String destIP = getIPString(userSocketAddress);
		String[] elements = { SEND_SONG_TO_USER_CMD, stationName, songName,
				destIP };
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

	public static String buildAddSongCommand(String stationName,
			String songName, String songLength) {

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

		String[] elements = { SONG_REMOVED_NOTIFIER, stationName, songName };
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

	public static String buildPingResponse(String stationName,
			SocketAddress userSocketAddress) {

		String[] elements = { PING_RESPONSE, stationName,
				userSocketAddress.toString() };

		String message = MessageBuilder.buildMessage(elements, SEPARATOR);
		return message;
	}
}
