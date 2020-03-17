package com.gotogames.bridge.engineserver.ws;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="exception")
public class WSException {
	/**
	 * 
	 */
	private String message;
	private ServiceExceptionType type;

	public WSException(ServiceExceptionType type) {
		this.type = type;
		message = type.getMessage();
	}
	
	public WSException(ServiceExceptionType type, String additionalMessage) {
		this.type = type;
		message = type.getMessage();
		if (additionalMessage != null && additionalMessage.length() > 0) {
			message += " - " + additionalMessage;
		}
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public ServiceExceptionType getType() {
		return type;
	}
	
	public void setType(ServiceExceptionType type) {
		this.type = type;
	}
	
	@JsonIgnore
	public String toString() {
		return "type="+type+" - message="+message;
	}
}
