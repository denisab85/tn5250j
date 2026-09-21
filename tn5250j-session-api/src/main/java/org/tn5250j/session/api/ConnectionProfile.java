package org.tn5250j.session.api;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public final class ConnectionProfile {

    public enum Mode {
        IN_PROCESS,
        REMOTE
    }

    private final Mode mode;
    private final String remoteUrl;
    private final String authToken;
    private final String sessionName;
    private final Properties sessionProperties;
    private final String configurationResource;

    private ConnectionProfile(Mode mode, String remoteUrl, String authToken,
                              String sessionName, Properties sessionProperties,
                              String configurationResource) {
        this.mode = mode;
        this.remoteUrl = remoteUrl;
        this.authToken = authToken;
        this.sessionName = sessionName;
        this.sessionProperties = sessionProperties;
        this.configurationResource = configurationResource;
    }

    public static ConnectionProfile inProcess(String sessionName, Properties props,
                                              String configurationResource) {
        return new ConnectionProfile(Mode.IN_PROCESS, null, null, sessionName, props,
                configurationResource);
    }

    public static ConnectionProfile remote(String url, String authToken, String sessionName,
                                           Map<String, String> props) {
        Properties p = new Properties();
        if (props != null) {
            p.putAll(props);
        }
        return new ConnectionProfile(Mode.REMOTE, url, authToken, sessionName, p, null);
    }

    public Mode getMode() {
        return mode;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getSessionName() {
        return sessionName;
    }

    public Properties getSessionProperties() {
        return sessionProperties == null ? new Properties() : sessionProperties;
    }

    public String getConfigurationResource() {
        return configurationResource;
    }

    public Map<String, String> getSessionPropertiesMap() {
        Properties p = getSessionProperties();
        Map<String, String> map = new java.util.HashMap<>();
        for (String name : p.stringPropertyNames()) {
            map.put(name, p.getProperty(name));
        }
        return Collections.unmodifiableMap(map);
    }
}
