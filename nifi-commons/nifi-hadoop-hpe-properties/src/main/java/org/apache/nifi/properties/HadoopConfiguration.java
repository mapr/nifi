package org.apache.nifi.properties;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.authentication.util.SsoConfigurationUtil;
import org.apache.nifi.util.hpe.HpeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

public class HadoopConfiguration implements HpeProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(HadoopConfiguration.class);

    private final Configuration configuration;

    public HadoopConfiguration() {
        configuration = new Configuration();
    }

    @Override
    public String get(String name) {
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            return configuration.get(name);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public char[] getPassword(String name) {
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            return configuration.getPassword(name);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to get property '%s'", name), e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public void addResource(String parent, String child) {
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            File resource = new File(parent, child);
            if (resource.exists() && resource.canRead()) {
                configuration.addResource(new Path(parent, child));
            } else {
                LOGGER.info("Hadoop resource '{}' is not accessible; skipping load.", resource);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public String getOidcDiscoveryUrl() {
        String clientIssuer = SsoConfigurationUtil.getInstance().getClientIssuer();
        if (!clientIssuer.isBlank()) {
            clientIssuer = clientIssuer + "/.well-known/openid-configuration";
        }
        return clientIssuer;
    }

    @Override
    public String getOidcClientId() {
        return SsoConfigurationUtil.getInstance().getClientId();
    }

    @Override
    public String getOidcClientSecret() {
        return SsoConfigurationUtil.getInstance().getClientSecret();
    }

    @Override
    public String getUserClaim() {
        return SsoConfigurationUtil.getInstance().getUserAttrName();
    }
}
