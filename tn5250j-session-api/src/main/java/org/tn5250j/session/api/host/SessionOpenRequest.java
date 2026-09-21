package org.tn5250j.session.api.host;

import java.util.Collections;
import java.util.Map;

public final class SessionOpenRequest {

    private final String sessionName;
    private final String configurationResource;
    private final Map<String, String> properties;

    public SessionOpenRequest(String sessionName, String configurationResource,
                                Map<String, String> properties) {
        this.sessionName = sessionName;
        this.configurationResource = configurationResource;
        this.properties = properties == null ? Collections.emptyMap() : properties;
    }

    public String getSessionName() {
        return sessionName;
    }

    public String getConfigurationResource() {
        return configurationResource;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
