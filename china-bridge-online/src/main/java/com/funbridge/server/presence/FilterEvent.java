package com.funbridge.server.presence;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.ws.event.Event;

public class FilterEvent {
	public long senderID, receiverID;
	public String category;
	
	public String toString() {
		return "{receiverID="+receiverID+" - senderID="+senderID+" - category="+category+"}";
	}
	
	public boolean matchesEvent(Event event) {
		boolean result = false;
		// check receiver
		if (receiverID == Constantes.EVENT_RECEIVER_ALL || receiverID == event.receiverID) {
			result = true;
		}
		// check category
		if (result && category != null && category.length() > 0) {
			if (event.getCategory() != null) {
				result = category.equals(event.getCategory());
			} else {
				result = false;
			}
		}
		return result;
	}
}
