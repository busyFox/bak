package com.funbridge.server.ws.player;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "profile")
public class WSProfile {
    public String firstName;
    public String lastName;
    public int sex; // (0 not set, 1 woman, 2 man)
    public String countryCode;
    public String town;
    public String description;
    public long birthdate;
    public boolean avatar = false;
    public long dateCreation = 0;
}
