package com.ketonax.networking;

public class ServerException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static String Error_Header = "Server error: ";

	public ServerException() {
		super(Error_Header + "Fatal error.");
	}

	public ServerException(String message) {
		super(Error_Header + message);
	}
}
