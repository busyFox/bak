package com.gotogames.bridge.engineserver.ws;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.gotogames.bridge.engineserver.ws.compute.ComputeServiceImpl;
import com.gotogames.bridge.engineserver.ws.request.RequestServiceImpl;
import com.gotogames.bridge.engineserver.ws.session.SessionServiceImpl;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Created by pserent on 21/12/2015.
 */
public class ApplicationConfig extends ResourceConfig {
    public ApplicationConfig() {
        // use JSON provider to support application/json request
        register(JacksonJsonProvider.class);
        // use specific mapper provider
        register(JacksonMapperProvider.class);

        // declare all services
        register(ComputeServiceImpl.class);
        register(RequestServiceImpl.class);
        register(SessionServiceImpl.class);
    }
}
