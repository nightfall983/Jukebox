package com.ketonax.Networking;

public class MessageBuilder {
    
	public static String buildMessage(String[] elements, String separator) {
		String message = "";
		int separatorCount = elements.length - 1;

		for (String s : elements) {
			message += s;

			/* Add separator */
			if (separatorCount > 0) {
				message += separator;
				separatorCount--;
			}
		}

		return message;
	}
}
