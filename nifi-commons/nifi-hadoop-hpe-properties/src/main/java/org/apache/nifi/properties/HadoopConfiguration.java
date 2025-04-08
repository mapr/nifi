package org.apache.nifi.properties;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.authentication.util.SsoConfigurationUtil;
import org.apache.nifi.util.hpe.HpeProperties;

import java.io.IOException;
import java.io.UncheckedIOException;

public class HadoopConfiguration implements HpeProperties {
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
            configuration.addResource(new Path(parent, child));
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
