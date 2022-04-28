/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.nifi.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.nifi.util.mapr.MapRComponentsUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for providing property's values from Hadoop
 */
public final class MapRPropertiesUtils {

    /**
     * Value to notify that this property should be found in Hadoop
     */
    public static final String MAPR_HADOOP_PROPERTY_PROVIDER = "$HadoopProvider";

    /**
     * TrustStore properties
     */
    public static final String SERVER_TRUSTSTORE_PASSWORD = "ssl.server.truststore.password";
    public static final String SERVER_TRUSTSTORE_LOCATION = "ssl.server.truststore.location";
    public static final String SERVER_TRUSTSTORE_TYPE = "ssl.server.truststore.type";

    /**
     * KeyStore properties
     */
    public static final String SERVER_KEYSTORE_KEY_PASSWORD = "ssl.server.keystore.keypassword";
    public static final String SERVER_KEYSTORE_PASSWORD = "ssl.server.keystore.password";
    public static final String SERVER_KEYSTORE_LOCATION = "ssl.server.keystore.location";
    public static final String SERVER_KEYSTORE_TYPE = "ssl.server.keystore.type";

    private static final String PASSWORD_SUFFIX = "password";
    private static final String HADOOP_COMPONENT_NAME = "hadoop";

    private static final String HADOOP_CONF = getHadoopConfFolder();
    private static final String HADOOP_CONF_INTERNAL_PATH = "etc/hadoop";
    private static final String HADOOP_CONF_SSL_SERVER_XML = "ssl-server.xml";
    private static final String HADOOP_CONF_CORE_SITE_XML = "core-site.xml";

    private static Configuration hadoopConf = null;

    private static final Map<String, String> mapNifiToHadoopProperties = new HashMap<>();

    static {
        //keystore
        mapNifiToHadoopProperties.put(NiFiProperties.SECURITY_KEYSTORE, SERVER_KEYSTORE_LOCATION);
        mapNifiToHadoopProperties.put(NiFiProperties.SECURITY_KEYSTORE_TYPE, SERVER_KEYSTORE_TYPE);
        mapNifiToHadoopProperties.put(NiFiProperties.SECURITY_KEYSTORE_PASSWD, SERVER_KEYSTORE_PASSWORD);
        mapNifiToHadoopProperties.put(NiFiProperties.SECURITY_KEY_PASSWD, SERVER_KEYSTORE_KEY_PASSWORD);

        //truststore
        mapNifiToHadoopProperties.put(NiFiProperties.SECURITY_TRUSTSTORE, SERVER_TRUSTSTORE_LOCATION);
        mapNifiToHadoopProperties.put(NiFiProperties.SECURITY_TRUSTSTORE_TYPE, SERVER_TRUSTSTORE_TYPE);
        mapNifiToHadoopProperties.put(NiFiProperties.SECURITY_TRUSTSTORE_PASSWD, SERVER_TRUSTSTORE_PASSWORD);
    }

    /**
     * Returns hadoop properties for both passwords and simple values
     *
     * @param key property name
     * @return Hadoop value
     */
    public static String getHadoopProperty(final String key) {
        String hadoopKey = mapNifiToHadoopProperties.get(key);

        if (hadoopKey == null) {
            throw new RuntimeException(String.format("No mapping for property '%s'", key));
        }

        if (key.endsWith(PASSWORD_SUFFIX)) {
            try {
                return getPassword(key);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Failed to get property '%s'", hadoopKey), e);
            }
        } else {
            return getProperty(hadoopKey);
        }
    }

    /**
     * Return property from Hadoop
     *
     * @param property name
     * @return property value
     */
    public static String getProperty(final String property) {
        return getHadoopConf().get(property);
    }

    /**
     * Returns password from Hadoop
     *
     * @param property name
     * @return password
     * @throws IOException if fails to get password value
     */
    public static String getPassword(final String property) throws IOException {
        return new String(getHadoopConf().getPassword(property));
    }

    private static Configuration getHadoopConf() {
        if (hadoopConf == null) {
            hadoopConf = new Configuration();
            hadoopConf.addResource(new Path(HADOOP_CONF, HADOOP_CONF_SSL_SERVER_XML));
            hadoopConf.addResource(new Path(HADOOP_CONF, HADOOP_CONF_CORE_SITE_XML));
        }

        return hadoopConf;
    }

    private static String getHadoopConfFolder() {
        String hadoopFolder;
        try {
            hadoopFolder = MapRComponentsUtils.getComponentFolder(HADOOP_COMPONENT_NAME).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Paths.get(hadoopFolder, HADOOP_CONF_INTERNAL_PATH).toString();
    }
}
