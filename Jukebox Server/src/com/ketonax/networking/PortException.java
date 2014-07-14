package com.ketonax.networking;

public class PortException extends Exception{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static String ERROR_HEADER = "Station error: ";
	
	public PortException() {
		super(ERROR_HEADER + "Fatal error.");	
	}
	
	public PortException(String message){
		super(ERROR_HEADER + message);
	}

}
