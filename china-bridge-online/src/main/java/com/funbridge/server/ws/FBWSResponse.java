package com.funbridge.server.ws;

public class FBWSResponse {
	private FBWSExceptionRest exception;
	private Object data;
	public FBWSExceptionRest getException() {
		return exception;
	}
	public void setException(FBWSExceptionRest exception) {
		this.exception = exception;
	}
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
}
