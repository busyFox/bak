package com.funbridge.server.ws;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class FBWSExceptionRest {
	public String message;
	public String type;
    public String localizedMessage;

	public FBWSExceptionRest(FBExceptionType type) {
		this.type = type.toString();
		this.message = type.getMessage();
	}
	
	public FBWSExceptionRest(FBExceptionType type, String localizedMessage) {
		this.type = type.toString();
		this.message = type.getMessage();
        this.localizedMessage = localizedMessage;
	}

    public FBWSExceptionRest(FBWSException fbe) {
        this.type = fbe.type.toString();
        this.message = fbe.type.getMessage();
        if (fbe.localizedMessage != null && fbe.localizedMessage.length() > 0) {
            this.localizedMessage = fbe.localizedMessage;
        }
    }
	
//	public FBExceptionBean getFaultInfo() {
//		FBExceptionBean beanInfo = new FBExceptionBean();
//		beanInfo.setMsg(message);
//		beanInfo.setType(type.name());
//		return beanInfo;
//	}
}
