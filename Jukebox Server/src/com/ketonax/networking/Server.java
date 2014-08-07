package com.ketonax.networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.ketonax.constants.Networking;
import com.ketonax.station.Station;
import com.ketonax.station.StationException;

public class Server {

	/* Variables */
	private static Queue<SocketAddress> allUsers = new ConcurrentLinkedQueue<SocketAddress>();
	private static Queue<Station> stationList = null;
	private static Map<String, Station> stationMap = null;
	private static Map<SocketAddress, Station> currentStationMap = null;
	private static Map<SocketAddress, Boolean> pingResponseMap = null;
	private static Map<SocketAddress, Integer> latencyMap = null;
	private static Map<SocketAddress, Integer> pingCountMap = null;
	private static Map<SocketAddress, ArrayList<Integer>> latencySampleSizeMap = null;
	private static int totalPingSampleSize = 100;
	private static int latencyTime = 0; // Milliseconds
	private static boolean pingStarted = false;

	/* Networking */
	static InetAddress groupAddress = null;
	static DatagramSocket udpServerSocket = null;

	public static void main(String[] args) {
		System.out.println("Jukebox server has started.\n");

		stationList = new LinkedList<Station>();
		stationMap = new HashMap<String, Station>();
		currentStationMap = new HashMap<SocketAddress, Station>();
		pingResponseMap = new ConcurrentHashMap<SocketAddress, Boolean>();
		latencyMap = new ConcurrentHashMap<SocketAddress, Integer>();
		pingCountMap = new ConcurrentHashMap<SocketAddress, Integer>();
		latencySampleSizeMap = new ConcurrentHashMap<SocketAddress, ArrayList<Integer>>();

		startServer();
	}

	private static void startServer() {
		try {
			udpServerSocket = new DatagramSocket(Networking.SERVER_PORT);
			groupAddress = Networking.getGroupAddress();

			while (true) {
				/* Listen for incoming commands */

				byte[] receiveData = new byte[Networking.DATA_LIMIT_SIZE];
				DatagramPacket receivePacket = new DatagramPacket(receiveData,
						receiveData.length);
				udpServerSocket.receive(receivePacket);

				String userMessage = new String(receivePacket.getData()).trim();

				// Get the address of the connected device
				SocketAddress userSocketAddress = receivePacket
						.getSocketAddress();

				// Determine what to do with received message
				if (userMessage.contains(",")) {
					String messageArray[] = userMessage.split(",");

					if (messageArray[0].equals(Networking.CREATE_STATION_CMD)) {
						String stationName = messageArray[1];
						createStation(stationName, userSocketAddress);

					} else if (messageArray[0].equals(Networking.ADD_SONG_CMD)) {
						String stationName = messageArray[1];
						String songName = messageArray[2];
						int songLength = Integer.parseInt(messageArray[3]);
						addSongToStation(userSocketAddress, stationName,
								songName, songLength);

					} else if (messageArray[0]
							.equals(Networking.LEAVE_STATION_CMD)) {

						String stationName = messageArray[1];
						leaveStation(userSocketAddress, stationName);
					} else if (messageArray[0]
							.equals(Networking.JOIN_STATION_CMD)) {

						String stationName = messageArray[1];
						joinStation(userSocketAddress, stationName);

					} else if (messageArray[0]
							.equals(Networking.GET_PLAYLIST_CMD)) {

						String stationName = messageArray[1];
						Station targetStation = stationMap.get(stationName);
						targetStation.sendPlaylist(userSocketAddress);
					} else if (messageArray[0]
							.equals(Networking.SONG_DOWNLOADED_NOTIFIER)) {

						log("Received SONG_DOWNLOADED_NOTIFIER from user at "
								+ userSocketAddress);

						String stationName = messageArray[1];
						songDownloadedNotifier(userSocketAddress, stationName);

					} else {
						System.err.println("Unrecognized message \""
								+ userMessage + "\" passed to the server from"
								+ userSocketAddress);
					}
				} else if (userMessage.equals(Networking.PING_RESPONSE)) {
					pingResponseMap.put(userSocketAddress, true);
				} else if (userMessage.equals(Networking.EXIT_JUKEBOX_NOTIFIER)) {

					removeUserFromServer(userSocketAddress);
				} else if (userMessage
						.equals(Networking.STATION_LIST_REQUEST_CMD)) {

					/* Add user to ALL_USERS list */
					addUserToServer(userSocketAddress);

					sendStationList(userSocketAddress);
					log("Station list sent to " + userSocketAddress);
				} else {
					System.err.println("Unrecognized message " + userMessage
							+ " passed to the server from" + userSocketAddress);
				}

				/* Check to see if stations are running */
				Iterator<Station> it = stationList.iterator();
				while (it.hasNext()) {
					Station s = it.next();
					if (s.hasStopped()) {
						// Send updated station list to all users
						sendStationKilledNotifier(s);
						stationMap.remove(s.getName());
						it.remove();

						if (stationList.size() == 0)
							log("There are no stations available.");
					}
				}
			}
		} catch (SocketException e) {
			System.err.println(e.getMessage());
		} catch (IOException e) {
			System.err.println(e.getMessage());
		} catch (ServerException e) {
			e.printStackTrace();
		} catch (StationException e) {
			e.printStackTrace();
		} finally {
			udpServerSocket.close();
		}
	}

	private static void addUserToServer(SocketAddress userSocketAddress) {

		/* Add user to ALL_USERS list */
		if (!allUsers.contains(userSocketAddress))
			allUsers.add(userSocketAddress);

		if (!latencyMap.containsKey(userSocketAddress))
			latencyMap.put(userSocketAddress, 0);

		if (!pingCountMap.containsKey(userSocketAddress))
			pingCountMap.put(userSocketAddress, 0);

		if (!pingResponseMap.containsKey(userSocketAddress))
			pingResponseMap.put(userSocketAddress, false);

		if (!latencySampleSizeMap.containsKey(userSocketAddress)) {
			ArrayList<Integer> pingValuesList = new ArrayList<Integer>(
					totalPingSampleSize);
			initPingArray(pingValuesList);
			latencySampleSizeMap.put(userSocketAddress, pingValuesList);
		}

		/* Start pinging devices. */
		if (pingStarted == false)
			startPinging();
	}

	private static void initPingArray(ArrayList<Integer> pingValuesList) {
		for (int i = 0; i < totalPingSampleSize; i++)
			pingValuesList.add(i, 0);
	}

	private static void removeUserFromServer(SocketAddress userSocketAddress)
			throws StationException {

		if (allUsers.contains(userSocketAddress))
			allUsers.remove(userSocketAddress);

		if (pingCountMap.containsKey(userSocketAddress))
			pingCountMap.remove(userSocketAddress);

		if (pingResponseMap.containsKey(userSocketAddress))
			pingResponseMap.remove(userSocketAddress);

		if (latencyMap.containsKey(userSocketAddress))
			latencyMap.remove(userSocketAddress);

		if (latencySampleSizeMap.containsKey(userSocketAddress))
			latencySampleSizeMap.remove(userSocketAddress);

		if (currentStationMap.containsKey(userSocketAddress)) {
			Station targetStation = currentStationMap.get(userSocketAddress);

			if (targetStation.hasUser(userSocketAddress))
				/* Remove user from their current station */
				targetStation.removeUser(userSocketAddress);

			currentStationMap.remove(userSocketAddress);
		}

		log("User at " + userSocketAddress + " has disconnected.");
	}

	private static void startPinging() {

		log("Pinging started.");
		pingStarted = true;

		Thread pingerThread = new Thread() {
			Iterator<SocketAddress> it;
			SocketAddress user;

			public void run() {
				while (true) {
					it = allUsers.iterator();
					while (it.hasNext()) {
						String pingMessage = Networking.buildPingMessage();
						user = (SocketAddress) it.next();
						if (allUsers.contains(user))
							sendToUser(pingMessage, user);
						startLatencyTimer(user);

						try {
							sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		};

		pingerThread.start();
	}

	private static void createStation(String stationName,
			SocketAddress userSocketAddress) throws ServerException {
		/** Creates a new Station and runs it in a new thread. */

		/* Add user if the user is not on the allUsers list */
		if (!allUsers.contains(userSocketAddress))
			addUserToServer(userSocketAddress);

		/*
		 * Check if the creator is currently in a different station. If so,
		 * remove creator from that station.
		 */
		if (currentStationMap.containsKey(userSocketAddress)) {
			Station oldStation = currentStationMap.get(userSocketAddress);

			if (stationName.equals(oldStation.getName()))
				throw new ServerException(stationName
						+ " station already on stationList.");

			try {
				if (oldStation.hasUser(userSocketAddress))
					oldStation.removeUser(userSocketAddress);
			} catch (StationException e) {
				System.err.println(e.getMessage());
			}

			/* Remove map to the old station */
			currentStationMap.remove(userSocketAddress);
		}

		if (!stationMap.containsKey(stationName)) {
			Station station = new Station(stationName, userSocketAddress,
					udpServerSocket);
			int userLatency = latencyMap.get(userSocketAddress);
			station.latencyUpdate(userSocketAddress, userLatency);
			stationList.add(station);
			stationMap.put(station.getName(), station);
			currentStationMap.put(userSocketAddress, station);

			/* Start new station thread and notify user of station creation */
			new Thread(station).start();
			sendStationAddedNotifier(station);
		} else
			throw new ServerException(stationName
					+ " station already on stationList.");
	}

	public static void joinStation(SocketAddress userSocketAddress,
			String stationName) throws ServerException {

		if (!stationMap.containsKey(stationName)) {
			throw new ServerException("User at " + userSocketAddress
					+ " attempted to join nonexistent station: " + stationName);
		} else {
			/* Add user if the user is not on the allUsers list */
			if (!allUsers.contains(userSocketAddress))
				addUserToServer(userSocketAddress);

			Station targetStation = stationMap.get(stationName);

			/* Check for user's old station */
			if (currentStationMap.containsKey(userSocketAddress)) {
				Station oldStation = currentStationMap.get(userSocketAddress);
				try {
					oldStation.removeUser(userSocketAddress);

					// Remove old pairing
					currentStationMap.remove(userSocketAddress);
				} catch (StationException e) {
					e.printStackTrace();
				}
			}
			currentStationMap.put(userSocketAddress, targetStation);
			try {
				targetStation.addUser(userSocketAddress);
				int userLatency = latencyMap.get(userSocketAddress);
				targetStation.latencyUpdate(userSocketAddress, userLatency);
			} catch (StationException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	public static void leaveStation(SocketAddress userSocketAddress,
			String stationName) {
		Station targetStation = stationMap.get(stationName);

		if (targetStation != null) {
			if (currentStationMap.containsKey(userSocketAddress))
				currentStationMap.remove(userSocketAddress);

			try {
				if (targetStation.hasUser(userSocketAddress))
					targetStation.removeUser(userSocketAddress);
			} catch (StationException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	public static void addSongToStation(SocketAddress userSocketAddress,
			String stationName, String songName, int songLength)
			throws ServerException {
		Station targetStation = stationMap.get(stationName);

		if (targetStation == null) {
			throw new ServerException("User at " + userSocketAddress
					+ " attempted to add a song to nonexistent station: "
					+ stationName);
		} else {
			try {
				targetStation.addSong(userSocketAddress, songName, songLength);
			} catch (StationException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	public static void songDownloadedNotifier(SocketAddress userSocketAddress,
			String stationName) throws ServerException {
		/**
		 * This method increments a variable in the station that checks whether
		 * the a song has been downloaded.
		 */

		Station targetStation = stationMap.get(stationName);
		if (targetStation != null) {
			if (!targetStation.songIsPlaying())
				targetStation.incrementSongDownloaded();
			else {
				int latency = latencyMap.get(userSocketAddress);
				targetStation.latencyUpdate(userSocketAddress, latency);
				targetStation.notifyLateDownload(userSocketAddress);
			}
		} else {
			throw new ServerException("User at " + userSocketAddress
					+ " sent notification to nonexistent station \""
					+ stationName + "\"");
		}
	}

	private static void startLatencyTimer(final SocketAddress userSocketAddress) {

		for (latencyTime = 0; latencyTime < 10000; latencyTime++) {
			if (pingResponseMap.containsKey(userSocketAddress)) {
				if (pingResponseMap.get(userSocketAddress) == true) {
					int pingCount = pingCountMap.get(userSocketAddress);
					pingCount = pingCount % totalPingSampleSize;

					pingCount++;
					pingCountMap.put(userSocketAddress, pingCount);

					ArrayList<Integer> pingValuesList = latencySampleSizeMap
							.get(userSocketAddress);
					pingValuesList.remove(pingCount - 1); // Remove old
					pingValuesList.add(pingCount - 1, latencyTime);

					// log(Integer.toString(pingValuesList.get(pingCount - 1)));
					// //TODO test

					latencySampleSizeMap.put(userSocketAddress, pingValuesList);
					int averageLatency = getAverageLatency(userSocketAddress);
					latencyMap.put(userSocketAddress, averageLatency);
					break;
				}
			}

			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// log(Integer.toString(latencyMap.get(userSocketAddress))); // TODO
		// Test
		/* Reset the ping response boolean for this user */
		pingResponseMap.put(userSocketAddress, false);
	}

	private static int getAverageLatency(SocketAddress userSocketAddress) {

		int averageLatency = 0;
		ArrayList<Integer> pingValuesList = latencySampleSizeMap
				.get(userSocketAddress);
		int pingSize = pingCountMap.get(userSocketAddress);
		for (int i = 0; i < pingSize; i++)
			averageLatency += pingValuesList.get(i);

		averageLatency /= pingSize;
		// log("Average latency for user at \"" + userSocketAddress + "\" is: "
		// + Integer.toString(averageLatency)); // TODO test
		return averageLatency;
	}

	@SuppressWarnings("unused")
	private static void sendStationList() throws IOException {
		/** Sends the a list of the station names to all users */
		for (SocketAddress sa : allUsers)
			sendStationList(sa);
	}

	private static void sendStationList(SocketAddress userSocketAddress) {
		/** Sends the a list of the station names to user */

		String userIPString = Networking.getIPString(userSocketAddress);
		int userPort = Networking.getPort(userSocketAddress);

		InetAddress userIP = null;
		try {
			userIP = InetAddress.getByName(userIPString);
		} catch (UnknownHostException e) {
			System.err.println(e.getMessage());
		}

		String data = null;
		byte[] sendData = null;
		DatagramPacket sendPacket = null;

		for (Station s : stationList) {
			String stationName = s.getName();

			/* Create data string and convert it to bytes */
			data = Networking.buildStationListRequestResponse(stationName);
			sendData = data.getBytes();

			/* Send data packet */
			sendPacket = new DatagramPacket(sendData, sendData.length, userIP,
					userPort);
			try {
				udpServerSocket.send(sendPacket);
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	private static void sendStationKilledNotifier(Station station) {
		/** Sends a notifier that a station has been killed */

		String message = Networking.buildStationKilledNotifier(station
				.getName());
		// sendMulticastMessage(message);
		sendToAll(message);
		log("Notified all users that " + station.getName() + " has terminated.");
	}

	private static void sendStationAddedNotifier(Station station) {
		/**
		 * Sends a notifier to all devices that a station has been added. It
		 * includes the station name.
		 */

		String message = Networking
				.buildStationAddedNotifier(station.getName());
		// sendMulticastMessage(message);
		sendToAll(message);
	}

	@SuppressWarnings("unused")
	private static void sendMulticastMessage(String message) {
		/** Send message to all devices in allUsers */

		byte[] sendData = message.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData,
				sendData.length, groupAddress, Networking.GROUP_PORT);

		try {
			udpServerSocket.send(sendPacket);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

	private static void sendToUser(String message,
			SocketAddress userSocketAddress) {
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

	private static void sendToAll(String message) {
		/** Send message to all devices in allUsers */

		for (SocketAddress sa : allUsers) {

			String userIPString = Networking.getIPString(sa);
			int userPort = Networking.getPort(sa);

			InetAddress userIP = null;
			try {
				userIP = InetAddress.getByName(userIPString);
			} catch (UnknownHostException e) {
				System.err.println(e.getMessage());
			}

			byte[] sendData = message.getBytes();

			DatagramPacket sendPacket = new DatagramPacket(sendData,
					sendData.length, userIP, userPort);

			try {
				udpServerSocket.send(sendPacket);
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	private static void log(String message) {
		/** This function displays log messages on the console. */

		String logMessage = "[Server log]: " + message;
		System.out.println(logMessage);
	}
}
