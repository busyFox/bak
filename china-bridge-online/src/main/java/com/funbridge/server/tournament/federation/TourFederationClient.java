package com.funbridge.server.tournament.federation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

/**
 * Created by luke on 30/08/2017.
 */
public abstract class TourFederationClient {

    protected Logger log = LogManager.getLogger(this.getClass());

    protected Client client;
    protected WebTarget target;

    public TourFederationClient() {
        // Jersey client creation
        client = ClientBuilder.newClient();

        // Add timeouts
        client.property(ClientProperties.CONNECT_TIMEOUT, getTimeout());
        client.property(ClientProperties.READ_TIMEOUT, getTimeout());
    }

    protected abstract int getTimeout();

}
