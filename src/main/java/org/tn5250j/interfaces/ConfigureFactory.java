package org.tn5250j.interfaces;

import java.util.Properties;

/**
 * An interface defining objects that can create Configure
 * instances.
 */
public abstract class ConfigureFactory {

    static final public String SESSIONS = "sessions";
    static final public String MACROS = "macros";
    static final public String KEYMAP = "keymap";
    private static ConfigureFactory factory;

    /**
     * Default concrete factory used when no system property override is set.
     * Loaded by name so this type does not hard-depend on the Swing desktop
     * configuration implementation.
     */
    private static final String DEFAULT_FACTORY =
            "org.tn5250j.GlobalConfigure";

    /**
     * @return An instance of the Configure.
     */
    public static ConfigureFactory getInstance() {
        ConfigureFactory.setFactory();
        return factory;
    }

    private static void setFactory() {
        if (factory == null) {
            try {
                String className = System.getProperty(ConfigureFactory.class.getName());
                if (className != null) {
                    Class<?> classObject = Class.forName(className);
                    Object object = classObject.newInstance();
                    if (object instanceof ConfigureFactory) {
                        ConfigureFactory.factory = (ConfigureFactory) object;
                    }
                }
            } catch (Exception ex) {
                // fall through to default
            }
            if (ConfigureFactory.factory == null) {
                try {
                    Class<?> classObject = Class.forName(DEFAULT_FACTORY);
                    Object object = classObject.newInstance();
                    if (object instanceof ConfigureFactory) {
                        ConfigureFactory.factory = (ConfigureFactory) object;
                    }
                } catch (Exception ex) {
                    throw new IllegalStateException(
                            "Unable to load configure factory " + DEFAULT_FACTORY, ex);
                }
            }
        }
    }

    abstract public void reloadSettings();

    abstract public void saveSettings();

    abstract public String getProperty(String regKey);

    abstract public String getProperty(String regKey, String defaultValue);

    abstract public void setProperties(String regKey, Properties regProps);

    abstract public void setProperties(String regKey, String fileName, String header);

    abstract public void setProperties(String regKey, String fileName, String header,
                                       boolean createFile);

    abstract public Properties getProperties(String regKey);

    abstract public Properties getProperties(String regKey, String fileName);

    abstract public Properties getProperties(String regKey, String fileName,
                                             boolean createFile, String header);

    abstract public Properties getProperties(String regKey, String fileName,
                                             boolean createFile, String header,
                                             boolean reloadIfLoaded);

    abstract public void saveSettings(String regKey);

    abstract public void saveSettings(String regKey, String header);

    abstract public void saveSettings(String regKey, String fileName, String header);

}
