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

import com.ketonax.constants.Networking;
import com.ketonax.station.Station;
import com.ketonax.station.StationException;

public class Server {

	/* Variables */
	static ArrayList<SocketAddress> allUsers = new ArrayList<SocketAddress>();
	static Queue<Station> stationList = null;
	static Map<String, Station> stationMap = null;
	static Map<SocketAddress, Station> currentStationMap = null;

	/* Networking */
	static InetAddress groupAddress = null;
	static DatagramSocket udpServerSocket = null;

	public static void main(String[] args) {
		System.out.println("Jukebox server has started.");

		stationList = new LinkedList<Station>();
		stationMap = new HashMap<String, Station>();
		currentStationMap = new HashMap<SocketAddress, Station>();

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

						try {
							createStation(stationName, userSocketAddress);
						} catch (ServerException e) {
							System.err.println(e.getMessage());
						}
					} else if (messageArray[0].equals(Networking.ADD_SONG_CMD)) {
						String stationName = messageArray[1];
						String songName = messageArray[2];
						int songLength = Integer.parseInt(messageArray[3]);

						try {
							addSongToStation(userSocketAddress, stationName,
									songName, songLength);
						} catch (ServerException e) {
							System.out.println(e.getMessage());
						}
					} else if (messageArray[0]
							.equals(Networking.LEAVE_STATION_CMD)) {

						String stationName = messageArray[1];
						leaveStation(userSocketAddress, stationName);
					} else if (messageArray[0]
							.equals(Networking.JOIN_STATION_CMD)) {

						String stationName = messageArray[1];
						try {
							joinStation(userSocketAddress, stationName);
						} catch (ServerException e) {
							e.printStackTrace();
						}
					} else if (messageArray[0]
							.equals(Networking.GET_PLAYLIST_CMD)) {

						String stationName = messageArray[1];
						Station targetStation = stationMap.get(stationName);
						targetStation.sendPlaylist(userSocketAddress);
					} else {
						System.err.println("Unrecognized message \""
								+ userMessage + "\" passed to the server from"
								+ userSocketAddress);
					}
				} else if (userMessage.equals(Networking.EXIT_JUKEBOX_NOTIFIER)) {

					if (allUsers.contains(userSocketAddress))
						allUsers.remove(userSocketAddress);
					if (currentStationMap.containsKey(userSocketAddress)) {
						Station targetStation = currentStationMap
								.get(userSocketAddress);

						if (targetStation.hasUser(userSocketAddress))
							try {
								/* Remove user from their current station */
								targetStation.removeUser(userSocketAddress);
							} catch (StationException e) {
								e.printStackTrace();
							}
						currentStationMap.remove(userSocketAddress);
					}
				} else if (userMessage
						.equals(Networking.STATION_LIST_REQUEST_CMD)) {

					if (!allUsers.contains(userSocketAddress))
						allUsers.add(userSocketAddress);
					
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
		} finally {
			udpServerSocket.close();
		}
	}

	private static void createStation(String stationName,
			SocketAddress userAddress) throws ServerException {
		/** Creates a new Station and runs it in a new thread. */

		if (currentStationMap.containsKey(userAddress)) {
			Station oldStation = currentStationMap.get(userAddress);

			try {
				oldStation.removeUser(userAddress);
			} catch (StationException e) {
				System.err.println(e.getMessage());
			}

			currentStationMap.remove(userAddress);
		}

		// Add user to ALL_USERS list
		if (!allUsers.contains(userAddress))
			allUsers.add(userAddress);

		if (!stationMap.containsKey(stationName)) {
			Station station = new Station(stationName, userAddress,
					udpServerSocket);
			stationList.add(station);
			stationMap.put(station.getName(), station);
			currentStationMap.put(userAddress, station);
			
			/* Start new station thread */
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
					+ " attempted to add a song to nonexistent station : "
					+ stationName);
		} else {
			try {
				targetStation.addSong(userSocketAddress, songName, songLength);
			} catch (StationException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	@SuppressWarnings("unused")
	private static void sendStationList() throws IOException {
		/** Sends the a list of the station names to all users */
		for (SocketAddress sa : allUsers)
			sendStationList(sa);
	}

	private static void sendStationList(SocketAddress userSocketAddress) {
		/** Sends the a list of the station names to user */

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
			System.err.println(e.getMessage());
		}

		String data = null;
		byte[] sendData = null;

		DatagramPacket sendPacket = null;

		for (Station s : stationList) {
			String name = s.getName();

			// Create data string and convert it to bytes
			String[] elements = { Networking.STATION_LIST_REQUEST_RESPONSE,
					name };
			data = MessageBuilder.buildMessage(elements, Networking.SEPERATOR);
			sendData = data.getBytes();

			// Send data packet
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

		String[] elements = { Networking.STATION_KILLED_NOTIFIER,
				station.getName() };
		String message = MessageBuilder.buildMessage(elements,
				Networking.SEPERATOR);
		// sendMulticastMessage(message);
		sendToAll(message);
		log("Notified all users that " + station.getName() + " has terminated.");
	}

	private static void sendStationAddedNotifier(Station station) {
		/**
		 * Sends a notifier to all devices that a station has been added. It
		 * includes the station name.
		 */

		String message = Networking.STATION_ADDED_NOTIFIER + ","
				+ station.getName();
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

	@SuppressWarnings("unused")
	private static void sendToAll(String message) {
		/** Send message to all devices in allUsers */

		for (SocketAddress sa : allUsers) {

			// Parse user socket address (sa)
			String userAddress[] = sa.toString().split(":");
			String userIPString = userAddress[0];
			int userPort = Integer.parseInt(userAddress[1]);

			// Check to see if userIP contains '/' and remove it
			if (userIPString.contains("/"))
				userIPString = userIPString.replace("/", "");
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

		String logMessage = "Server log: " + message;
		System.out.println(logMessage);
	}
}
