package org.apache.nifi.util.hpe;

import java.io.IOException;
/**
 * Interface used to isolate Hadoop Configuration and it's libraries from the root NiFi classloader
 */
public interface HpeProperties {
    /**
     * Wrap around org.apache.hadoop.conf.Configuration#get(String name)
     * @param name property name
     * @return the value of the <code>name</code> or its replacing property,
     *         or null if no such property exists.
     */
    String get(String name);

    /**
     * Wrap around org.apache.hadoop.conf.Configuration#getPassword(String name)
     *
     * @param name property name
     * @return password
     * @throws IOException
     */
    char[] getPassword(String name) throws IOException;

    /**
     * Wrap around org.apache.hadoop.conf.Configuration#addResource(Path file).
     * Resolves a child path against a parent path: org.apache.hadoop.fs.Path(String parent, String child)
     * @param parent parent path
     * @param child child path
     */
    void addResource(String parent, String child);
}
