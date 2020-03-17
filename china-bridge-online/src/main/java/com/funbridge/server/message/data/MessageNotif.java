package com.funbridge.server.message.data;

import org.springframework.data.mongodb.core.index.Indexed;

import com.funbridge.server.player.data.Player;
import com.funbridge.server.ws.event.WSMessage;


/**
 * Message sent by FunBridge for only one player. Displayed in the notification window
 * @author pserent
 *
 */
public class MessageNotif extends MessageNotifBase {
	@Indexed
	public long recipientID; // recipient of the message
	public boolean read = false; // flag to indicate if message is read by recipient
	public long dateRead = 0;
	
	public static final String MSG_TYPE = "notif";
	
	public MessageNotif() {
		super(MSG_TYPE);
	}
	
	public MessageNotif(String template) {
		super(MSG_TYPE, template);
	}
	
	public String toString() {
		return super.toString()+" -  recipientID="+recipientID+" - read="+read;
	}
	
	public boolean isReadByPlayer() {
		if (read) {
			return true;
		}
        return dateRead > 0;
    }
	
	/**
	 * Transform MessageNotif to complete WSMessage
	 * @param lang
	 * @return
	 */
	public WSMessage toWSMessage(Player p) {
		WSMessage wmsg = super.toWSMessage(p);
		wmsg.read = isReadByPlayer();
		return wmsg;
	}
	
	/**
	 * Check msgType is MSG_TYPE
	 * @return
	 */
	public boolean checkType() {
		return msgType.equals(MSG_TYPE);
	}
}
