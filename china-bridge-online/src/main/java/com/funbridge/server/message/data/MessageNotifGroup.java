package com.funbridge.server.message.data;

import com.funbridge.server.player.data.Player;
import com.funbridge.server.ws.event.WSMessage;

public class MessageNotifGroup extends MessageNotifBase {
	public static final String MSG_TYPE = "notif_group";
	
	public MessageNotifGroup() {
		super(MSG_TYPE);
	}
	
	public MessageNotifGroup(String template) {
		super(MSG_TYPE, template);
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
