package com.ketonax.networking;

public class MessageBuilder {
	
	public static String buildMessage(String[] elementsToBuild, String separator) {
		String message = "";
		int separatorCount = elementsToBuild.length - 1;

		for (String s : elementsToBuild) {
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
