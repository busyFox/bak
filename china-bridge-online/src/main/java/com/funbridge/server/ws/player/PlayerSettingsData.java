package com.funbridge.server.ws.player;

/**
 * Object send in JSON format to save player settings
 *
 * @author pascal
 */
public class PlayerSettingsData {
    public int convention;
    public String conventionFreeProfile = null;
    public String conventionBidsFreeProfile = null;
    public int conventionCards;
    public String conventionCardsFreeProfile = null;
    public PlayerNotificationsSettingsData notifications = null;
}
