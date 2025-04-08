package org.apache.nifi.util.hpe;

/**
 * Interface used to isolate Hadoop Configuration and it's libraries from the root NiFi classloader
 */
public interface HpeProperties {
    /**
     * Wrap around org.apache.hadoop.conf.Configuration#get(String name)
     *
     * @param name property name
     * @return the value of the <code>name</code> or its replacing property,
     * or null if no such property exists.
     */
    String get(String name);

    /**
     * Wrap around org.apache.hadoop.conf.Configuration#getPassword(String name)
     *
     * @param name property name
     * @return password
     */
    char[] getPassword(String name);

    /**
     * Wrap around org.apache.hadoop.conf.Configuration#addResource(Path file).
     * Resolves a child path against a parent path: org.apache.hadoop.fs.Path(String parent, String child)
     *
     * @param parent parent path
     * @param child child path
     */
    void addResource(String parent, String child);

    /**
     * Retrieve the OIDC discovery url configured in the environment.
     *
     * @return empty string if SSO is not enabled in the environment, regardless of whether the OIDC discovery URL is set or not.
     */
    String getOidcDiscoveryUrl();

    /**
     * Retrieve the OIDC client ID configured in the environment.
     *
     * @return empty string if SSO is not enabled in the environment, regardless of whether the OIDC client ID is set or not.
     */
    String getOidcClientId();

    /**
     * Retrieve the OIDC client secret configured in the environment.
     *
     * @return empty string if SSO is not enabled in the environment, regardless of whether the OIDC client secret is set or not.
     */
    String getOidcClientSecret();

    /**
     * Retrieve the OIDC claim that identifies the authenticated user.
     *
     * @return empty string if SSO is not enabled in the environment, regardless of whether the OIDC client secret is set or not.
     */
    String getUserClaim();
}
