package org.apache.nifi.security.maprsasl;

import org.apache.nifi.util.hpe.HpePropertiesURLClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * A wrapper around {@code com.mapr.security.maprsasl.MaprSecurityLoginModule}
 * to isolate all dependencies into a dedicated class loader.
 */
public class IsolatedMaprSecurityLoginModule implements LoginModule {
    private static final Logger logger = LoggerFactory.getLogger(IsolatedMaprSecurityLoginModule.class);

    private final LoginModule originLoginModule;

    public IsolatedMaprSecurityLoginModule() {
        try {
            String moduleName = getMaprSecurityLoginModuleName();

            logger.debug("Initializing MapR Security LoginModule using class: {}", moduleName);

            HpePropertiesURLClassLoader hpeClassLoader = new HpePropertiesURLClassLoader();
            Class<?> moduleClass = hpeClassLoader.loadClass(moduleName);
            originLoginModule = (LoginModule) moduleClass.getConstructor().newInstance();

            logger.debug("Successfully instantiated MapR Security LoginModule: {}", moduleName);
        } catch (ClassNotFoundException e) {
            logger.error("MapR Security LoginModule class not found", e);
            throw new RuntimeException("Unable to load MapR Security LoginModule class", e);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 InstantiationException e) {
            logger.error("Failed to instantiate MapR Security LoginModule", e);
            throw new RuntimeException("Unable to instantiate MapR Security LoginModule", e);
        }
    }

    private static String getMaprSecurityLoginModuleName() {
        final String commonLoginModuleClassNme = "com.mapr.security.maprsasl.MaprSecurityLoginModule";
        final String defaultZookeeperClientConfiguration = "Client";

        logger.debug("Looking up MapR Security LoginModule in JAAS entry '{}'", defaultZookeeperClientConfiguration);
        AppConfigurationEntry[] zookeeperClientConfiguration = Configuration.getConfiguration().getAppConfigurationEntry("Client");
        if (zookeeperClientConfiguration != null) {
            if (zookeeperClientConfiguration.length > 1) {
                logger.debug("JAAS entry {} has more that 1 login module", defaultZookeeperClientConfiguration);
            } else {
                return zookeeperClientConfiguration[0].getLoginModuleName();
            }
        }
        logger.debug("No suitable LoginModule found in JAAS entry '{}'; falling back to default MapR class name: {}", defaultZookeeperClientConfiguration, commonLoginModuleClassNme);
        return commonLoginModuleClassNme;
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        originLoginModule.initialize(subject, callbackHandler, sharedState, options);
    }

    @Override
    public boolean login() throws LoginException {
        return originLoginModule.login();
    }

    @Override
    public boolean commit() throws LoginException {
        return originLoginModule.commit();
    }

    @Override
    public boolean abort() throws LoginException {
        return originLoginModule.abort();
    }

    @Override
    public boolean logout() throws LoginException {
        return originLoginModule.logout();
    }
}
