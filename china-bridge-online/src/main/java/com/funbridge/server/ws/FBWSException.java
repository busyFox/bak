package com.funbridge.server.ws;

import com.fasterxml.jackson.annotation.JsonAutoDetect;


@SuppressWarnings("serial")
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class FBWSException extends Exception{
	
	public String message;
	public FBExceptionType type;
	public String localizedMessage;
    public String alertMessage;
    public boolean alert = false;

	public FBWSException(FBExceptionType type) {
		this.type = type;
		this.message = type.getMessage();
	}

    public FBWSException(FBExceptionType type, boolean alert) {
        this.type = type;
        this.message = type.getMessage();
        this.alert = alert;
    }
	
	public FBWSException(FBExceptionType type, String localizedMessage) {
        this.type = type;
        this.message = type.getMessage();
        this.localizedMessage = localizedMessage;
    }

    public FBWSException(FBExceptionType type, boolean alert, String alertMessage) {
        this.type = type;
        this.message = type.getMessage();
        this.alertMessage = alertMessage;
        this.alert = alert;
    }
	
	public String getMessage() {
		return message + " alert="+alert+" - alertMessage="+alertMessage;
	}

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setMessage(String message) {
		this.message = message;
	}
	
	public FBExceptionType getType() {
		return type;
	}
	
	public void setType(FBExceptionType type) {
		this.type = type;
	}

    public boolean isAlert() {
        return alert;
    }

    public void setAlert(boolean alert) {
        this.alert = alert;
    }
}
