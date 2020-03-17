package com.funbridge.server.message.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Element to indicate the date of read notifGroup by a player and notif parameters
 * @author ldelbarre
 *
 */
public class MessageNotifGroupReadWithParameters extends MessageNotifGroupRead {
	public Map<String, String> templateParameters = new HashMap<>();
}
