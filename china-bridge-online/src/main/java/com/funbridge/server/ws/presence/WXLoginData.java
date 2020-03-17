package com.funbridge.server.ws.presence;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="wxloginData")
public class WXLoginData {
    public String code;
    public boolean guest = false;
    public String clientVersion;
    public String deviceID;
    public String deviceInfo;
    public String deviceType;
    public String lang;
    public String displayLang;
    public String country;
    public String protocol;
    @JsonIgnore
    public boolean isValid() {
        if (code == null || code.length() == 0) {
            return false;
        }
        if (deviceID == null || deviceID.length() == 0) {
            return false;
        }
        return deviceType != null && deviceType.length() != 0;
    }
    @JsonIgnore
    public String toString() {
        return "code="+code  +" - clientVersion="+clientVersion+" - deviceID="+deviceID+" - deviceInfo="+deviceInfo+" - devicetype="+deviceType+" - lang="+lang+" - displayLang="+displayLang+" - country="+country+" - protocol="+protocol;
    }
}
