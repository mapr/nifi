package org.apache.nifi.util.hpe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Loader for HPE Properties
 */
public class HpePropertiesLoader {
    private static final Logger logger = LoggerFactory.getLogger(HpePropertiesLoader.class);

    public static HpeProperties getHpeProperties() {
        final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            HpePropertiesURLClassLoader hpePropertiesClassloader = new HpePropertiesURLClassLoader(originalContextClassLoader);
            Thread.currentThread().setContextClassLoader(hpePropertiesClassloader);

            if (logger.isDebugEnabled()) {
                logger.debug("Seeking for HpeProperties implementation by the following paths: {}",
                        Arrays.toString(hpePropertiesClassloader.getURLs()));
            }

            ServiceLoader<HpeProperties> hpePropertiesLoader = ServiceLoader.load(HpeProperties.class);
            Optional<HpeProperties> hpePropertiesOptional = hpePropertiesLoader.findFirst();
            if (hpePropertiesOptional.isPresent()) {
                return hpePropertiesOptional.get();
            } else {
                throw new HpePropertiesException(String.format("No implementations found [%s]", HpeProperties.class.getName()));
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }
}
