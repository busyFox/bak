package test.gotogames.common.tools;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="getResultResponse")
public class EngineRestGetResultResponse {
	public String result;
	
	public String toString() {
		return result;
	}
}
