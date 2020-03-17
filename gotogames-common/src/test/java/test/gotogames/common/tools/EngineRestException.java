package test.gotogames.common.tools;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="engineRestException")
public class EngineRestException {
	public String message;
	public String type;
	
	public String toString() {
		return "type="+type+" - message="+message;
	}
}
