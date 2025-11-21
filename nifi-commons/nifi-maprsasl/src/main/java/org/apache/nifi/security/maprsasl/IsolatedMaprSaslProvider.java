package org.apache.nifi.security.maprsasl;

import org.apache.nifi.util.hpe.HpePropertiesURLClassLoader;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;
import java.lang.reflect.Method;
import java.security.Provider;
import java.util.Map;

/**
 * A security Provider that registers the MapR SASL client mechanism
 * ("MAPR-SECURITY") while isolating all MapR dependencies inside a dedicated
 * class loader.
 */
public class IsolatedMaprSaslProvider extends Provider {
    static {
        
    }

    public IsolatedMaprSaslProvider() {
        super("MaprSasl", "1.0", "Dependency isolated Mapr SASL provider");
        put("SaslClientFactory." + "MAPR-SECURITY", IsolatedMaprSaslClientFactory.class.getName());
    }

    private static Class<?> loadClassFromHpeDeps(String className) throws ClassNotFoundException {
        HpePropertiesURLClassLoader hpePropertiesURLClassLoader = new HpePropertiesURLClassLoader();
        return hpePropertiesURLClassLoader.loadClass(className);
    }

    public static class IsolatedMaprSaslClientFactory implements SaslClientFactory {
        private static final String MAPR_SASL_CLIENT_CLASS_NAME = "com.mapr.security.maprsasl.MaprSaslClient$SaslMaprClientFactory";

        private volatile Object factoryInstance;
        private volatile Method getMechanismNamesMethod;
        private volatile Method createSaslClientMethod;

        private void initFactory() throws Exception {
            if (factoryInstance != null) {
                return;
            }
            synchronized (this) {
                if (factoryInstance == null) {
                    Class<?> maprSaslClientFactoryClass = loadClassFromHpeDeps(MAPR_SASL_CLIENT_CLASS_NAME);
                    factoryInstance = maprSaslClientFactoryClass.getConstructor().newInstance();

                    getMechanismNamesMethod = maprSaslClientFactoryClass.getMethod("getMechanismNames", Map.class);

                    createSaslClientMethod = maprSaslClientFactoryClass.getMethod("createSaslClient",
                            String[].class,
                            String.class,
                            String.class,
                            String.class,
                            Map.class,
                            CallbackHandler.class
                    );
                }
            }
        }

        @Override
        public String[] getMechanismNames(Map<String, ?> props) {
            try {
                initFactory();
                return (String[]) getMechanismNamesMethod.invoke(factoryInstance, props);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public SaslClient createSaslClient(String[] mechanisms,
                                           String authorizationId, String protocol, String serverName,
                                           Map<String, ?> props, CallbackHandler cbh) throws SaslException {
            try {
                initFactory();
                return (SaslClient) createSaslClientMethod.invoke(
                        factoryInstance,
                        mechanisms,
                        authorizationId,
                        protocol,
                        serverName,
                        props,
                        cbh
                );
            } catch (Exception e) {
                throw new SaslException("MapR SASL client creation failed", e);
            }
        }
    }
}
