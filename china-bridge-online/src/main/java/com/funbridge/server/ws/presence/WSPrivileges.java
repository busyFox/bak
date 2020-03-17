package com.funbridge.server.ws.presence;

import com.funbridge.server.player.data.Privileges;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Created by pserent on 21/10/2015.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSPrivileges {
    public boolean serverChange = false;

    public String toString() {
        return "serverChange="+serverChange;
    }

    public WSPrivileges(){}

    public WSPrivileges(Privileges privileges) {
        this.serverChange = privileges.serverChange;
    }
}
