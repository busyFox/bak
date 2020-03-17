package com.gotogames.bridge.engineserver.ws;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class WSResponse {
	private WSException exception;
	private Object data;
	public WSException getException() {
		return exception;
	}
	public void setException(WSException exception) {
		this.exception = exception;
	}
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	@JsonIgnore
	public String toString() {
		return "exception="+exception+" - data="+data;
	}
}
