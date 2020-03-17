package com.funbridge.server.tournament.federation.cbo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.tournament.federation.TourFederationGenerateSettings;

/**
 * Created by ldelbarre on 21/12/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourCBOGenerateSettings extends TourFederationGenerateSettings<TourCBOGenerateSettingsElement> {

}
