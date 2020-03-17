package com.funbridge.server.ws;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.funbridge.server.ws.engine.EngineService;
import com.funbridge.server.ws.event.EventServiceRestImpl;
import com.funbridge.server.ws.game.GameServiceRestImpl;
import com.funbridge.server.ws.message.MessageService;
import com.funbridge.server.ws.notification.NotificationService;
import com.funbridge.server.ws.player.PlayerServiceRestImpl;
import com.funbridge.server.ws.presence.PresenceServiceRestImpl;
import com.funbridge.server.ws.result.ResultServiceRestImpl;
import com.funbridge.server.ws.store.StoreServiceRestImpl;
import com.funbridge.server.ws.support.SupportServiceImpl;
import com.funbridge.server.ws.team.TeamService;
import com.funbridge.server.ws.tournament.TournamentServiceRestImpl;
import com.funbridge.server.ws.pay.OrderServiceRestImpl;
import com.gotogames.common.bridge.BridgeGame;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.File;

import static com.gotogames.common.bridge.PBNConvertion.PBNToGame;

/**
 * Set configuration for REST Configuration
 */
public class ApplicationConfig extends ResourceConfig {
    public ApplicationConfig() {
        // use JSON provider to support application/json request
        register(JacksonJsonProvider.class);
        // use specific mapper provider
        register(JacksonMapperProvider.class);

        // declare all services
        register(EngineService.class);
        register(EventServiceRestImpl.class);
        register(GameServiceRestImpl.class);
        register(NotificationService.class);
        register(PlayerServiceRestImpl.class);
        register(PresenceServiceRestImpl.class);
        register(ResultServiceRestImpl.class);
        register(StoreServiceRestImpl.class);
        register(SupportServiceImpl.class);
        register(TeamService.class);
        register(MessageService.class);
        register(TournamentServiceRestImpl.class);
        register(OrderServiceRestImpl.class);
    }
}
