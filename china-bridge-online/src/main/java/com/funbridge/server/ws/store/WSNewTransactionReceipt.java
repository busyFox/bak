package com.funbridge.server.ws.store;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class WSNewTransactionReceipt {
    public boolean transactionValid = false;
    public int creditAccount = 0;
    public long accountExpirationDate = 0;
    public boolean replayEnabled = true;
    public boolean freemium = false;
    public long subscriptionTimeLeft = 0;

    @JsonIgnore
    public String toString() {
        return "transactionValid="+transactionValid+" - creditAccount="+creditAccount;
    }
}
