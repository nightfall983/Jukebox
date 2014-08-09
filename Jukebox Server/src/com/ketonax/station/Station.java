package com.ketonax.station;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.ketonax.constants.Networking;
import com.ketonax.networking.MessageBuilder;

public class Station implements Runnable {

	private String stationName;
	private DatagramSocket udpServerSocket = null;
	private Queue<SocketAddress> userList = null;
	private Queue<String> songQueue = null;
	private Queue<String> songsPlayedQueue = null;
	private Map<String, Integer> songLengthMap = null;
	private Map<String, SocketAddress> songSourceMap = null;
	private Map<String, Integer> songDownloadedMap = null;
	private Map<String, Integer> latencyMap = null;
	private InetAddress groupAddress = null;

	private boolean isPlaying = false;
	private String currentSong = null;
	private int trackPosition = 0;
	private int playSongTimeout = 30; // Seconds
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
		songDownloadedMap = new ConcurrentHashMap<String, Integer>();
		latencyMap = new HashMap<String, Integer>();

		try {
			log("Station is running.");
			log("User at " + userSocketAddres + " has created station: "
					+ stationName);
			addUser(userSocketAddres);
		} catch (StationException e) {
			System.err.println(e.getMessage());
		}
	}

	public void run() {
		/** Station controls the flow of the playlist for its users. */

		stopRunning = false;
		while (stopRunning == false) {

			if (userList.isEmpty()) {
				halt();
			}

			if (isPlaying == false && !songQueue.isEmpty()) {

				currentSong = songQueue.element();
				try {
					removeSong(currentSong);
				} catch (StationException e) {
					e.printStackTrace();
				}
				try {

					/* Wait for 1 second, then play the next song */
					Thread.sleep(1000);

					/*
					 * Wait for notification from users that song has been
					 * downloaded or timeout has occured.
					 */
					preparePlayback();
					playSong(currentSong);

				} catch (StationException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				songsPlayedQueue.add(currentSong);
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

	public void addUser(SocketAddress userSocketAddress)
			throws StationException {
		/** This function adds a user to the station user list. */

		if (!userList.contains(userSocketAddress)) {
			userList.add(userSocketAddress);
			log("User at \"" + userSocketAddress + "\" has joined."
					+ " User list size = " + userList.size());

			try {
				sendPlaylist(userSocketAddress);
				sendUserList(userSocketAddress);
			} catch (IOException e) {
				e.printStackTrace();
			}

			String notification = Networking.buildUserAddedNotifier(
					stationName, userSocketAddress.toString());
			// sendMulticastMessage(notification);
			sendToAll(notification);

			/*
			 * If user is added while the current song is playing tell song
			 * holder to send the song to the user.
			 */
			if (isPlaying) {
				SocketAddress songHolder = getSongSource(currentSong);
				if (!songHolder.toString().equals(userSocketAddress.toString())) {
					String downloadSongCommand = Networking
							.buildSendSongToUserCommand(stationName,
									currentSong, userSocketAddress);
					sendToUser(downloadSongCommand, songHolder);
				} else {
					try {
						playSongCatchUp(currentSong, userSocketAddress);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		} else
			throw new StationException("User is already on the list");
	}

	public void removeUser(SocketAddress userSocketAddress)
			throws StationException {
		/**
		 * This function removes a user from userList. Note that the user might
		 * still be responsible for streaming songs to other devices in the
		 * background.
		 */

		if (!userList.contains(userSocketAddress))
			throw new StationException("User (Address: " + userSocketAddress
					+ ") is not on station (Station Name: " + stationName
					+ ") list.");
		userList.remove(userSocketAddress);
		latencyMap.remove(userSocketAddress);

		String[] elements = { Networking.USER_REMOVED_NOTIFIER, stationName,
				userSocketAddress.toString() };
		String notification = MessageBuilder.buildMessage(elements,
				Networking.SEPARATOR);
		// sendMulticastMessage(notification);
		sendToAll(notification);
		log("User at " + userSocketAddress + " has been removed."
				+ " User list size = " + userList.size());
	}

	public void addSong(SocketAddress userSocketAddress, String songName,
			int songLength) throws StationException {
		/**
		 * This station adds a song name to the stations song queue. It also
		 * keeps a map of the song source and song length with respect to the
		 * song name.
		 */

		if (!userList.contains(userSocketAddress))
			throw new StationException("Cannot add music. User (Address: "
					+ userSocketAddress
					+ ") is not part of this station (Station Name: "
					+ stationName + ").");

		if (songQueue.contains(songName))
			throw new StationException("Song (Song Name: " + songName
					+ ") is already on this station (Station Name: "
					+ stationName + ") playlist.");

		songQueue.add(songName);
		songSourceMap.put(songName, userSocketAddress);
		songLengthMap.put(songName, songLength);
		songDownloadedMap.put(songName, 0);

		String notification = Networking.SONG_ADDED_NOTIFIER + ","
				+ stationName + "," + songName;
		// sendMulticastMessage(notification);
		sendToAll(notification);
		log("\"" + songName + "\" has been added to the station by user at "
				+ userSocketAddress + "." + " Songs on queue = "
				+ songQueue.size());

		/* Tell the user to send the song to other devices */
		String command = Networking.buildSendSongCommand(stationName, songName);
		sendToUser(command, userSocketAddress);

		log("Instructed user at address \"" + userSocketAddress
				+ "\" to send \"" + currentSong + "\" to station users.");
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
		// songSourceMap.remove(songName);
		// songLengthMap.remove(songName);

		String notification = Networking.buildSongRemovedNotifier(stationName,
				songName);
		// sendMulticastMessage(notification);
		sendToAll(notification);
		log(songName + " has been removed from the station."
				+ " Songs on queue = " + songQueue.size());
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
			throw new StationException("In function getSongLength(), \""
					+ songName + "\" is not on songLengthMap.");

		return songLengthMap.get(songName);
	}

	public void sendPlaylist(SocketAddress userSocketAddress)
			throws IOException, StationException {
		/**
		 * This function sends each song on the playlist to a user. This
		 * function should only be used if the playlist is explicitly requested
		 * by a user. Station should default to notifying a device each time a
		 * song is added.
		 */

		String data = null;
		for (String songName : songQueue) {
			data = Networking.buildSongOnListResponse(stationName, songName);
			sendToUser(data, userSocketAddress);

			/*
			 * If user is added while the current song is playing tell song
			 * holder to send the song to the user.
			 */
			if (isPlaying) {
				if (songName.equals(currentSong))
					continue;

				SocketAddress songHolder = getSongSource(songName);
				if (!songHolder.toString().equals(userSocketAddress.toString())) {
					String downloadSongCommand = Networking
							.buildSendSongToUserCommand(stationName,
									songName, userSocketAddress);
					sendToUser(downloadSongCommand, songHolder);
				}
			}

			log("Songs on station queue have been sent to the user at "
					+ userSocketAddress);
		}
	}

	public void notifyDownloaded(SocketAddress userSocketAddress,
			String songName) {

		log("User at \"" + userSocketAddress + "\" has finished downloading "
				+ songName + ".");
		try {
			if (isPlaying == true && songName.equals(currentSong)) {
				playSongCatchUp(currentSong, userSocketAddress);
			} else {
				int currentDownloadCount = songDownloadedMap.get(songName);
				songDownloadedMap.put(songName, ++currentDownloadCount);
			}
		} catch (StationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void latencyUpdate(SocketAddress userSocketAddress, int latency) {
		latencyMap.put(userSocketAddress.toString(), latency);
		// log(Integer.toString(latency)); // TODO test
	}

	private void preparePlayback() {
		/**
		 * This method checks to see that the current song is ready to play. It
		 * checks to see that the number of times the song has been downloaded
		 * matches the userList size.
		 */

		// log("readyToPlay started. Checking for " + currentSong); // TODO test

		if (isPlaying == false) {

			int i = 0;
			while (i < playSongTimeout) {
				try {
					int downloadCount = songDownloadedMap.get(currentSong);
					// log(Integer.toString(downloadCount)); //TODO Test
					if (downloadCount == userList.size() - 1)
						break;
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				i++;
			}
			// log("readyToPlay stopped."); // TODO test
		}
	}

	public boolean songIsPlaying() {
		return isPlaying;
	}

	public void resetTrackPosition() {
		trackPosition = 0;
	}

	public void startPlaybackTimer() throws StationException,
			InterruptedException {

		// log("playback timer started."); //TODO

		if (!songLengthMap.containsKey(currentSong))
			throw new StationException(
					"No song length information for the current song \""
							+ currentSong + "\"");

		resetTrackPosition();
		final int songLength = getSongLength(currentSong);
		isPlaying = true;

		final Timer timer = new Timer();
		log("Playback timer started.");
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				++trackPosition;
				if (trackPosition == songLength) {
					timer.cancel();
					isPlaying = false;
					log("playback timer stopped");
				}
			}
		}, 0, 1);
		// removeSong(currentSong); //TODO
		// log("playback timer stopped.");
	}

	/* Networking dependent functions */
	public void userAddedNotifier(SocketAddress addedUserSocketAddress) {
		/**
		 * This function notifies all devices that a new user has been added. It
		 * sends the socket address of the user to the devices.
		 */

		String notification = Networking.buildUserAddedNotifier(stationName,
				addedUserSocketAddress.toString());
		// sendMulticastMessage(notification);
		sendToAll(notification);
		log("User at " + addedUserSocketAddress
				+ " has been added to the station");
	}

	public void songAddedNotifier(String songName) {
		/**
		 * This function notifies all devices that a new song has been added to
		 * the queue.
		 */

		String notification = Networking.buildSongAddedNotifier(stationName,
				songName);
		// sendMulticastMessage(notification);
		sendToAll(notification);
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
			data = Networking.buildUserOnListResponse(stationName,
					user.toString());
			sendToUser(data, userSocketAddress);
		}

		log("Socket address of users on station user list have been sent to the user at "
				+ userSocketAddress);
	}

	private void sendToUser(String message, SocketAddress userSocketAddress) {
		/** Sends a message to a specified device. */

		String userIPString = Networking.getIPString(userSocketAddress);
		int userPort = Networking.getPort(userSocketAddress);

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

	@SuppressWarnings("unused")
	private void sendMulticastMessage(String message) {
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

	private void sendToAll(String message) {
		/** This function sends message to all devices. */

		for (SocketAddress userSocketAddress : userList)
			sendToUser(message, userSocketAddress);
	}

	private void playSong(String songName) throws StationException,
			IOException, InterruptedException {
		/**
		 * Establish a socket connection and tell all users to play song with
		 * String songName from the beginning
		 */

		// Check to see if song is on songSourceMap
		if (!songSourceMap.containsKey(songName))
			throw new StationException("In function playSong(), \"" + songName
					+ "\" is not on songSourceMap.");

		notifyCurrentlyPlaying();
		/* Display station queue status */
		log("Instructed station users " + "to play \"" + songName
				+ "\". Song length = " + songLengthMap.get(songName)
				+ "ms. Songs played = " + songsPlayedQueue.size()
				+ ". Songs on queue = " + songQueue.size());
		startPlaybackTimer();
	}

	private void notifyCurrentlyPlaying() {
		SocketAddress songSource = songSourceMap.get(currentSong);

		for (SocketAddress user : userList) {

			int userLatency = 0;
			if (latencyMap.containsKey(user))
				userLatency = latencyMap.get(user);
			int startPosition = 0;

			/* Send notification to all devices of current song playing */
			String notification = Networking.buildCurrentlyPlayingNotifier(
					stationName, currentSong, songSource.toString(),
					Integer.toString(startPosition + userLatency));
			sendToUser(notification, user);
		}
	}

	@SuppressWarnings("unused")
	private void playSongCatchUp(String songName,
			SocketAddress userSocketAddress) throws StationException,
			IOException, InterruptedException {
		/**
		 * Establish a socket connection and tells a specific user to play song
		 * at the current trackPosition
		 */

		// Check to see if song is on songSourceMap
		if (!songSourceMap.containsKey(songName))
			throw new StationException("In function playSongCatchUp(), \""
					+ songName + "\" is not on songSourceMap.");

		int songLength = getSongLength(songName);
		SocketAddress songSource = getSongSource(songName);

		/* Send notification to a devices about current song playing */
		int userLatency = 0;
		if (latencyMap.containsKey(userSocketAddress.toString()))
			userLatency = latencyMap.get(userSocketAddress.toString());
		else
			log("Latency map doesn't contain address: " + userSocketAddress);

		int startPosition = trackPosition;
		String notification = Networking.buildCurrentlyPlayingNotifier(
				stationName, songName, songSource.toString(),
				Integer.toString(startPosition + userLatency));
		// sendMulticastMessage(notification);
		sendToUser(notification, userSocketAddress);

		/* Display station queue status */
		log("Instructed user at address \"" + userSocketAddress
				+ "\" to play \"" + songName + "\". Start position = "
				+ startPosition + ". Latency factor = " + userLatency
				+ ". Song length = " + songLengthMap.get(songName)
				+ "ms. Songs played = " + songsPlayedQueue.size()
				+ ". Songs on queue = " + songQueue.size());
	}

	private void log(String message) {
		/** This function displays log messages on the console. */

		String logMessage = "[" + stationName + " station log]: " + message;
		System.out.println(logMessage);
	}
}
