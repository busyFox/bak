package test.gotogames.common.tools;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="engineRestResponse")
public class EngineRestResponse<E> {
	public EngineRestException exception;
	public E data;
	
	public boolean isException() {
		return exception != null;
	}
	
	public String getExceptionString() {
		if (exception != null) {
			return "type="+exception.type+" - message="+exception.message;
		}
		return null;
	}
	
	public String getExceptionType() {
		if (exception != null) {
			return exception.type;
		}
		return null;
	}
	
	public String toString() {
		return "exception="+exception+" - data="+data;
	}
}
