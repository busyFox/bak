package com.gotogames.bridge.engineserver.user;

import java.util.concurrent.atomic.AtomicLong;

public class UserVirtualSimpleRequest extends UserVirtual {
    private AtomicLong nbRequest = new AtomicLong(0);

    public UserVirtualSimpleRequest(String login, String loginPrefix, String password, long id) {
        super(login, loginPrefix, password, id);
    }

    public String toString() {
        return super.toString()+" - nbRequest="+getNbRequest();
    }

    @Override
    public boolean isEngine() {
        return false;
    }

    public long incrementNbRequest() {
        return this.nbRequest.incrementAndGet();
    }

    public long getNbRequest() {
        return this.nbRequest.get();
    }

}
