package com.ketonax.station;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.ketonax.constants.Networking;
import com.ketonax.networking.MessageBuilder;

/* TODO implement ping function to check if a device is still connected.
 * If the current song playing belongs to a device that is no longer connected then 
 * the song should be skipped */
public class Station implements Runnable {

	private String stationName;
	private DatagramSocket udpServerSocket = null;
	private Queue<SocketAddress> userList = null;
	private Queue<String> songQueue = null;
	private Queue<String> songsPlayedQueue = null;
	private Map<String, Integer> songLengthMap = null;
	private Map<String, SocketAddress> songSourceMap = null;
	private InetAddress groupAddress = null;

	private boolean stopRunning = false;

	public Station(String stationName, SocketAddress userSocketAddres,
			DatagramSocket udpServerSocket) {

		/* Initialize variables */
		this.stationName = stationName;

		this.udpServerSocket = udpServerSocket;

		/* Group address for multicast messages */
		groupAddress = Networking.getGroupAddress();

		userList = new ConcurrentLinkedQueue<SocketAddress>();
		songQueue = new ConcurrentLinkedQueue<String>();
		songsPlayedQueue = new ConcurrentLinkedQueue<String>();
		songLengthMap = new ConcurrentHashMap<String, Integer>();
		songSourceMap = new ConcurrentHashMap<String, SocketAddress>();

		try {
			log("User at " + userSocketAddres + " has created station: "
					+ stationName);
			addUser(userSocketAddres);
		} catch (StationException e) {
			System.err.println(e.getMessage());
		}
	}

	public void run() {
		/** Station controls the flow of the playlist for its users. */

		log("Station is running.");

		stopRunning = false;
		while (stopRunning == false) {

			if (userList.isEmpty()) {
				halt();
			}

			Iterator<String> it = songQueue.iterator();

			while (it.hasNext() && !userList.isEmpty()) {
				String song = it.next();
				try {
					Thread.sleep(1000); // Not sure why, but fixes songLengthMap
										// // issue.
					playSong(song);
				} catch (StationException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				songsPlayedQueue.add(song);
				songRemovedNotifier(song);
				log("The song \"" + song
						+ "\" has been removed from station queue");
				it.remove();
			}
		}

		log(stationName + " has stopped running.");
	}

	public void halt() {
		stopRunning = true;
	}

	public boolean hasStopped() {
		return stopRunning;
	}

	public boolean isEmpty() {
		return userList.isEmpty();
	}

	public boolean hasUser(SocketAddress userSocketAddress) {
		return userList.contains(userSocketAddress);
	}

	@Override
	public String toString() {
		return stationName;
	}

	public String getName() {
		return stationName;
	}

	public void addUser(SocketAddress userAddress) throws StationException {
		/** This function adds a user to the station user list. */
		if (!userList.contains(userAddress)) {
			userList.add(userAddress);
			log("User at \"" + userAddress + "\" has joined.");

			try {
				sendPlaylist(userAddress);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else
			throw new StationException("User is already on the list");
	}

	public void removeUser(SocketAddress userAddress) throws StationException {
		/**
		 * This function removes a user from userList. Note that the user might
		 * still be responsible for streaming songs to other devices in the
		 * background.
		 */

		if (!userList.contains(userAddress))
			throw new StationException("User (Address: " + userAddress
					+ ") is not on station (Station Name: " + stationName
					+ ") list.");
		userList.remove(userAddress);

		String[] elements = { Networking.USER_REMOVED_NOTIFIER, stationName,
				userAddress.toString() };
		String notification = MessageBuilder.buildMessage(elements,
				Networking.SEPERATOR);
		//sendMulticastMessage(notification);
		sendToAll(notification);
		log("User at " + userAddress + " has been removed.");
	}

	public void addSong(SocketAddress userAddress, String songName,
			int songLength) throws StationException {
		/**
		 * This station adds a song name to the stations song queue. It also
		 * keeps a map of the song source and song length with respect to the
		 * song name.
		 */

		if (!userList.contains(userAddress))
			throw new StationException("Cannot add music. User (Address: "
					+ userAddress
					+ ") is not part of this station (Station Name: "
					+ stationName + ").");

		if (songSourceMap.containsKey(songName))
			throw new StationException("Song (Song Name: " + songName
					+ ") is already on this station (Station Name: "
					+ stationName + ") playlist.");

		songQueue.add(songName);
		songSourceMap.put(songName, userAddress);
		songLengthMap.put(songName, songLength);

		String notification = Networking.SONG_ADDED_NOTIFIER + ","
				+ stationName + "," + songName;
		//sendMulticastMessage(notification);
		sendToAll(notification);
		log(songName + " has been added to the station.");
	}

	public void removeSong(String songName) throws StationException {
		/** This function removes songName from all lists and maps that hold it. */

		if (!songQueue.contains(songName))
			throw new StationException("In removeSong(), " + songName
					+ " is not on songQueue");

		if (!songSourceMap.containsKey(songName))
			throw new StationException("In removeSong(), " + songName
					+ " is not on songSourceMap");

		if (!songLengthMap.containsKey(songName))
			throw new StationException("In removeSong(), " + songName
					+ " is not on songLengthMap");

		songQueue.remove(songName);
		songSourceMap.remove(songName);
		songLengthMap.remove(songName);

		String[] elements = { Networking.SONG_REMOVED_NOTIFIER, stationName,
				songName };
		String notification = MessageBuilder.buildMessage(elements,
				Networking.SEPERATOR);
		//sendMulticastMessage(notification);
		sendToAll(notification);
		log(songName + " has been removed from the station");
	}

	public SocketAddress getSongSource(String songName) throws StationException {
		/** Returns the socket address of the user device holding the given song */

		if (!songSourceMap.containsKey(songName))
			throw new StationException("In function getSongSource(), \""
					+ songName + "\" is not on songSourceMap.");

		return songSourceMap.get(songName);
	}

	public int getSongLength(String songName) throws StationException {
		/** Returns the song length in milliseconds */

		if (!songLengthMap.containsKey(songName))
			throw new StationException("In function getSongSource(), \""
					+ songName + "\" is not on songLengthMap.");

		return songLengthMap.get(songName);
	}

	/* Networking dependent functions */
	public void userAddedNotifier(SocketAddress addedUserSocketAddress) {
		/**
		 * This function notifies all devices that a new user has been added. It
		 * sends the socket address of the user to the devices.
		 */

		String[] elements = { Networking.USER_ADDED_NOTIFIER, stationName,
				addedUserSocketAddress.toString() };
		String notification = MessageBuilder.buildMessage(elements,
				Networking.SEPERATOR);
		//sendMulticastMessage(notification);
		sendToAll(notification);
		log("User at " + addedUserSocketAddress
				+ " has been added to the station");
	}

	public void songAddedNotifier(String songName) {
		/**
		 * This function notifies all devices that a new song has been added to
		 * the queue.
		 */

		String[] elements = { Networking.SONG_ADDED_NOTIFIER, songName };
		String notification = MessageBuilder.buildMessage(elements,
				Networking.SEPERATOR);
		//sendMulticastMessage(notification);
		sendToAll(notification);
	}

	public void songRemovedNotifier(String songName) {
		/**
		 * This function notifies all devices that a new song has been removed
		 * to the queue.
		 */

		String[] elements = { Networking.SONG_REMOVED_NOTIFIER, songName };
		String notification = MessageBuilder.buildMessage(elements,
				Networking.SEPERATOR);
		//sendMulticastMessage(notification);
		sendToAll(notification);
	}

	public void sendPlaylist(SocketAddress userSocketAddress)
			throws IOException {
		/**
		 * This function sends each song on the playlist to a user. This
		 * function should only be used if the playlist is explicitly requested
		 * by a user. Station should default to notifying a device each time a
		 * song is added.
		 */

		String data = null;
		for (String s : songQueue) {
			String[] elements = { Networking.SONG_ON_LIST_RESPONSE,
					stationName, s };
			data = MessageBuilder.buildMessage(elements, Networking.SEPERATOR);
			sendToUser(data, userSocketAddress);
		}

		log("Songs on station queue have been sent to the user at "
				+ userSocketAddress);
	}

	public void sendUserList(SocketAddress userSocketAddress)
			throws IOException {
		/**
		 * This function sends each user on userList to a requesting user. This
		 * function should only be used if the userList is explicitly requested
		 * by a user. Station should default to notifying a device each time a
		 * user is added.
		 */

		String data = null;
		for (SocketAddress user : userList) {
			String[] elements = { Networking.USER_ON_LIST_RESPONSE,
					stationName, user.toString() };
			data = MessageBuilder.buildMessage(elements, Networking.SEPERATOR);
			sendToUser(data, userSocketAddress);
		}

		log("Socket address of users on station user list have been sent to the user at "
				+ userSocketAddress);
	}

	public void sendToUser(String message, SocketAddress userSocketAddress) {
		/** Sends a message to a specified device. */

		// Parse userSocketAddress
		String userAddress[] = userSocketAddress.toString().split(":");
		String userIPString = userAddress[0];
		int userPort = Integer.parseInt(userAddress[1]);

		// Check to see if userIP contains '/' and remove it
		if (userIPString.startsWith("/"))
			userIPString = userIPString.replaceFirst("/", "");
		InetAddress userIP = null;
		try {
			userIP = InetAddress.getByName(userIPString);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		byte[] sendData = null;
		sendData = message.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData,
				sendData.length, userIP, userPort);

		try {
			udpServerSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMulticastMessage(String message) {
		/** This function sends a multicast message to all users in the group */

		byte[] sendData = message.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData,
				sendData.length, groupAddress, Networking.GROUP_PORT);
		
		try {
			udpServerSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendToAll(String message) {
		/** This function sends message to all devices. */

		for (SocketAddress userSocketAddress : userList) 
			sendToUser(message, userSocketAddress);		
	}

	private void playSong(String songName) throws StationException,
			IOException, InterruptedException {
		/* Establish a socket connection and play song with String songName */

		// Check to see if song is on songSourceMap
		if (!songSourceMap.containsKey(songName))
			throw new StationException("In function playSong(), \"" + songName
					+ "\" is not on songSourceMap.");

		int songLength = getSongLength(songName);
		SocketAddress songSource = getSongSource(songName);

		// Parse songSource to get IP address and port
		String sourceArray[] = songSource.toString().split(":");
		String address = sourceArray[0];
		if (address.contains("/"))
			address = address.replaceFirst("/", "");

		/* Send command to device to play song */
		String[] commandElements = { Networking.PLAY_SONG_CMD, songName };
		String command = MessageBuilder.buildMessage(commandElements,
				Networking.SEPERATOR);
		sendToUser(command, songSource);

		/* Send notification to all devices of current song playing */
		String[] notificationElements = {
				Networking.CURRENTLY_PLAYING_NOTIFIER, stationName, songName,
				songSource.toString() };
		String notification = MessageBuilder.buildMessage(notificationElements,
				Networking.SEPERATOR);
		//sendMulticastMessage(notification);
		sendToAll(notification);

		/* Display station queue status */
		log("Instructed user at \"" + address + "\" to play \"" + songName
				+ "\". Song length = " + songLengthMap.get(songName)
				+ "ms. Songs played = " + songsPlayedQueue.size()
				+ ". Songs on queue = " + songQueue.size());

		Thread.sleep(songLength);
	}

	private void log(String message) {
		/** This function displays log messages on the console. */

		String logMessage = "[" + stationName + "] station log: " + message;
		System.out.println(logMessage);
	}
}
