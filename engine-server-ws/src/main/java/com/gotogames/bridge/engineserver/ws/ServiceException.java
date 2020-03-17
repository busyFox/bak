package com.gotogames.bridge.engineserver.ws;

import javax.xml.ws.WebFault;

@SuppressWarnings("serial")
@WebFault(name="FBWSException", faultBean="com.gotogames.bridge.engineserver.ws.ServiceExceptionBean")
public class ServiceException extends Exception {
	private String message;
	private ServiceExceptionType type;

	public ServiceException(ServiceExceptionType type) {
		this.type = type;
		message = type.getMessage();
	}
	
	public ServiceException(ServiceExceptionType type, String additionalMessage) {
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
	
	public ServiceExceptionBean getFaultInfo() {
		ServiceExceptionBean beanInfo = new ServiceExceptionBean();
		beanInfo.setMsg(message);
		beanInfo.setType(type.name());
		return beanInfo;
	}
}
