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
	static ArrayList<SocketAddress> allUSers = new ArrayList<SocketAddress>();
	static Queue<Station> stationList = null;
	static Map<String, Station> stationMap = null;
	static Map<SocketAddress, Station> creatorMap = null;

	static DatagramSocket udpServerSocket = null;

	public static void main(String[] args) {

		System.out.println("Jukebox server has started.");

		stationList = new LinkedList<Station>();
		stationMap = new HashMap<String, Station>();
		creatorMap = new HashMap<SocketAddress, Station>();

		try {
			int serverPort = AssigningService.assignPortNumber();
			udpServerSocket = new DatagramSocket(serverPort);

			while (true) {
				/* Listen for incoming commands */
				// Receive packet
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

						Station targetStation = stationMap.get(stationName);

						try {
							targetStation.addSong(userSocketAddress, songName,
									songLength);
						} catch (StationException e) {
							System.err.println(e.getMessage());
						}
					} else if (messageArray[0].equals(Networking.LEAVE_STATION_CMD)) {

						String stationName = messageArray[1];
						Station targetStation = stationMap.get(stationName);

						try {
							targetStation.removeUser(userSocketAddress);
						} catch (StationException e) {
							System.err.println(e.getMessage());
						}
					} else if (messageArray[0].equals(Networking.JOIN_STATION_CMD)) {

						String stationName = messageArray[1];
						Station targetStation = stationMap.get(stationName);

						try {
							targetStation.addUser(userSocketAddress);
						} catch (StationException e) {
							System.err.println(e.getMessage());
						}
					} else if (messageArray[0].equals(Networking.GET_PLAYLIST_CMD)) {

						String stationName = messageArray[1];
						Station targetStation = stationMap.get(stationName);
						targetStation.sendPlaylist(userSocketAddress);
					} else {
						System.err.println("Unrecognized message \""
								+ userMessage + "\" passed to the server from"
								+ userSocketAddress);
					}
				} else if (userMessage.equals(Networking.EXIT_JUKEBOX_NOTIFIER)) {
					allUSers.remove(userSocketAddress);
				} else if (userMessage.equals(Networking.STATION_LIST_REQUEST_CMD)) {

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
		} catch (PortException e) {
			System.err.println(e.getMessage());
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

		if(creatorMap.containsKey(userAddress)){
			Station oldStation = creatorMap.get(userAddress);
			log("Test");
			
			try {
				oldStation.removeUser(userAddress);
			} catch (StationException e) {
				System.err.println(e.getMessage());
			}
			
			creatorMap.remove(userAddress);
		}
		
		Station station = new Station(stationName, userAddress, udpServerSocket);

		// Add user to ALL_USERS list
		if (!allUSers.contains(userAddress))
			allUSers.add(userAddress);

		if (!stationList.contains(station)
				&& !stationMap.containsKey(station.getName())) {
			stationList.add(station);
			stationMap.put(station.getName(), station);
			new Thread(station).start();// Start new station thread
		} else
			throw new ServerException(station
					+ " station already on stationList.");
		
		creatorMap.put(userAddress, station);
		sendStationAddedNotifier(station);
	}

	@SuppressWarnings("unused")
	private static void sendStationList() throws IOException {
		/** Sends the a list of the station names to all users */
		for (SocketAddress sa : allUSers)
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
			data = Networking.STATION_LIST_REQUEST_RESPONSE + "," + name;
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

		String[] elements = {Networking.STATION_KILLED_NOTIFIER, station.getName()};
		String message = MessageBuilder.buildMessage(elements);
		sendNotification(message);
	}

	private static void sendStationAddedNotifier(Station station) {
		/**
		 * Sends a notifier to all devices that a station has been added. It
		 * includes the station name.
		 */

		String data = Networking.STATION_ADDED_NOTIFIER + "," + station.getName();
		sendNotification(data);
	}

	private static void sendNotification(String message) {
		/** Send message to all devices in allUsers */
		
		for (SocketAddress sa : allUSers) {

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