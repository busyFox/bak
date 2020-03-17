package com.funbridge.server.tournament.federation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

/**
 * Created by ldelbarre on 02/08/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourFederationGenerateSettings<T extends TourFederationGenerateSettingsElement> {
    public List<T> settings;
}
