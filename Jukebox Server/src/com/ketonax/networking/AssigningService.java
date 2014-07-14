package com.ketonax.networking;

import java.util.ArrayList;
import java.util.List;

public class AssigningService {

	private static int availablePort = 61001; //49152 or 60000
	private static List<Integer> portList = new ArrayList<Integer>();

	public AssigningService() {

	}

	public static int assignPortNumber() throws PortException {
		if (availablePort > 65535)
			throw new PortException("No more ports available.");

		int port;

		if (!portList.isEmpty()) {
			port = portList.remove(0);
		} else {
			port = availablePort;
			availablePort++;
		}

		return port;
	}

	public static void freePort(int portNum) {
		portList.add(portNum);
	}

}
