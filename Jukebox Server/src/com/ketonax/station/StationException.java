package com.ketonax.station;

public class StationException extends Exception{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static String Error_Header = "Station exception: ";

	public StationException() {
		super(Error_Header + "Fatal error.");
	}
	
	public StationException(String message){
		super(Error_Header + message);
	}

}
