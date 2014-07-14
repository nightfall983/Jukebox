package com.ketonax.networking;

public class MessageBuilder {
    /* Separator string */
    private static final String SEPARATOR_STRING = ",";
    
	public static String buildMessage(String[] elements) {
		String message = "";
		int separatorCount = elements.length - 1;

		for (String s : elements) {
			message += s;

			/* Add separator */
			if (separatorCount > 0) {
				message += SEPARATOR_STRING;
				separatorCount--;
			}
		}

		return message;
	}
}
