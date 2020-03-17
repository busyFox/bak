package com.funbridge.server.ws.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WSMessageNotif extends WSMessage {
	public String richBody;
	public String title;
	public String titleIcon;
	public String titleBackgroundColor;
	public String titleColor;
	public String actionButtonText;
	public String actionText;

	@JsonIgnore
	public String toString() {
		return super.toString()
				+ " - richBody=" + richBody
				+ " - title=" + title
				+ " - titleIcon=" + titleIcon
				+ " - titleBackgroundColor=" + titleBackgroundColor
				+ " - titleColor=" + titleColor
				+ " - actionButtonText=" + actionButtonText
				+ " - actionText=" + actionText;
	}

}
