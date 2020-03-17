package com.funbridge.server.message.data;

import com.funbridge.server.player.data.Player;
import com.funbridge.server.ws.event.WSMessage;

/**
 * Message sent by FunBridge for all player
 * @author pserent
 *
 */
public class MessageNotifAll extends MessageNotifBase{
	
	public static final String MSG_TYPE = "notif_all";
	
	public MessageNotifAll() {
		super(MSG_TYPE);
	}
	
	/**
	 * Transform NotifAll to WSMessage. IMPORTANT : Flag read is not set !
	 * @param p
	 * @return
	 */
	public WSMessage toWSMessage(Player p) {
		return super.toWSMessage(p);
	}
	
	/**
	 * Check msgType is MSG_TYPE
	 * @return
	 */
	public boolean checkType() {
		return msgType.equals(MSG_TYPE);
	}
}
