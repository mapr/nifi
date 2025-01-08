package org.apache.nifi.properties;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.authentication.util.SsoConfigurationUtil;
import org.apache.nifi.util.StringUtils;
import org.apache.nifi.util.hpe.HpeProperties;

import java.io.IOException;

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
    public char[] getPassword(String name) throws IOException {
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            return configuration.getPassword(name);
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
        if (isSsoAuthenticationEnabled()) {
            return SsoConfigurationUtil.getInstance().getClientIssuer() + "/.well-known/openid-configuration";
        }
        return StringUtils.EMPTY;
    }

    @Override
    public String getOidcClientId() {
        if (isSsoAuthenticationEnabled()) {
            return SsoConfigurationUtil.getInstance().getClientId();
        }
        return StringUtils.EMPTY;
    }

    @Override
    public String getOidcClientSecret() {
        if (isSsoAuthenticationEnabled()) {
            return SsoConfigurationUtil.getInstance().getClientSecret();
        }
        return StringUtils.EMPTY;
    }

    private boolean isSsoAuthenticationEnabled() {
        String jwtEnabled = get(SsoConfigurationUtil.HADOOP_JWT_ENABLED);
        return Boolean.parseBoolean(jwtEnabled);
    }

}
