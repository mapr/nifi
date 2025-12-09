package org.apache.nifi.security.util;

import org.apache.nifi.security.maprsasl.IsolatedMaprSecurityLoginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.util.HashMap;
import java.util.Map;

/**
 * JAAS Configuration wrapper that allows creating a new login entry
 * by replicating an existing target entry's options and control flag.
 */
public class JaasConfigWrapper extends Configuration {
    private static final Logger logger = LoggerFactory.getLogger(JaasConfigWrapper.class);
    private final Configuration baseConfig;
    private final String targetEntryName;
    private final String wrapperEntryName;

    private AppConfigurationEntry[] wrappedEntry;

    public JaasConfigWrapper(Configuration baseConfig, String targetEntryName, String wrapperEntryName) {
        this.baseConfig = baseConfig;
        this.targetEntryName = targetEntryName;
        this.wrapperEntryName = wrapperEntryName;
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        // If the requested entity is target entry, build it
        if (wrapperEntryName.equals(name)) {
            if (wrappedEntry == null) {
                synchronized (this) {
                    if (wrappedEntry == null) {
                        wrappedEntry = buildWrapperEntry();
                    }
                }
            }
            return wrappedEntry.clone();
        }

        // Otherwise delegate to the base configuration
        return baseConfig.getAppConfigurationEntry(name);
    }

    private AppConfigurationEntry[] buildWrapperEntry() {
        // Get the target configuration
        AppConfigurationEntry[] clientEntries = baseConfig.getAppConfigurationEntry(targetEntryName);
        if (clientEntries == null || clientEntries.length == 0) {
            throw new IllegalStateException(String.format("'%s' configuration not found in JAAS config",  targetEntryName));
        }

        // Replicate options from the target entity
        Map<String, Object> options = new HashMap<>(clientEntries[0].getOptions());

        return new AppConfigurationEntry[] {
                new AppConfigurationEntry(
                        IsolatedMaprSecurityLoginModule.class.getName(),
                        clientEntries[0].getControlFlag(),
                        options
                )
        };
    }

    /**
     * Utility method to set this wrapper as the global JAAS config
     */
    public static void setAsJaasConfig(Configuration baseConfig, String targetEntryName, String wrapperEntryName) {
        Configuration.setConfiguration(new JaasConfigWrapper(baseConfig, targetEntryName, wrapperEntryName));
    }

    /**
     * Utility method to set this wrapper as the global JAAS config around the default Configuration
     */
    public static synchronized void wrapDefaultConfig(String targetEntryName, String wrapperEntryName) {
        if (!Configuration.getConfiguration().getClass().equals(JaasConfigWrapper.class)) {
            setAsJaasConfig(Configuration.getConfiguration(), targetEntryName, wrapperEntryName);
        } else {
            logger.debug("Configuration instance is already a JaasConfigWrapper. Will not wrap it");
        }
    }
}
