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
import org.apache.nifi.util.mapr.MapRComponentsUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private static final Path HADOOP_CONF_PATH = getHadoopConfFolder();
    private static final String HADOOP_HOME_PROPERTY = "hadoop.home.dir";
    private static final String HADOOP_CONF_INTERNAL_PATH = "etc/hadoop";
    private static final String HADOOP_CONF_CORE_SITE_XML = "core-site.xml";
    private static final String HADOOP_CONF_SSL_SERVER_XML = "ssl-server.xml";
    private static final String HADOOP_CONF_SSL_CLIENT_XML = "ssl-client.xml";
    private static final String HADOOP_CONF_HDFS_SITE_XML = "hdfs-site.xml";

    private static volatile Configuration hadoopConf = null;

    private static final Map<String, String> mapNifiToHadoopProperties = new HashMap<>();
    private static final List<String> hadoopClientConfigs = new ArrayList<>();

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

        //hadoop conf
        hadoopClientConfigs.add(HADOOP_CONF_PATH.resolve(HADOOP_CONF_HDFS_SITE_XML).toString());
        hadoopClientConfigs.add(HADOOP_CONF_PATH.resolve(HADOOP_CONF_SSL_CLIENT_XML).toString());
    }

    /**
     * Method to get list of Hadoop client configs
     *
     * @return list with configs paths
     */
    public static List<String> getHadoopClientConfigs() {
        return new ArrayList<>(hadoopClientConfigs);
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

        if (hadoopKey.toLowerCase().endsWith(PASSWORD_SUFFIX)) {
            try {
                return getPassword(hadoopKey);
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
        char[] data = getHadoopConf().getPassword(property);

        if (data == null) {
            return null;
        }

        return new String(data);
    }

    private static synchronized Configuration getHadoopConf() {
        if (hadoopConf == null) {
            hadoopConf = new Configuration();
            hadoopConf.addResource(new org.apache.hadoop.fs.Path(
                    HADOOP_CONF_PATH.toString(),
                    HADOOP_CONF_CORE_SITE_XML)
            );
            hadoopConf.addResource(new org.apache.hadoop.fs.Path(
                    HADOOP_CONF_PATH.toString(),
                    HADOOP_CONF_SSL_SERVER_XML)
            );
        }

        return hadoopConf;
    }

    private static Path getHadoopConfFolder() {
        Path hadoopFolder;

        try {
            hadoopFolder = MapRComponentsUtils.getComponentFolder(HADOOP_COMPONENT_NAME);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        setHadoopHomeIfNotSet(hadoopFolder.toString());

        return hadoopFolder.resolve(HADOOP_CONF_INTERNAL_PATH);
    }

    private static void setHadoopHomeIfNotSet(String hadoopFolder) {
        String defaultHadoopHome = System.getProperty(HADOOP_HOME_PROPERTY, "");

        if (defaultHadoopHome.isEmpty()) {
            System.setProperty(HADOOP_HOME_PROPERTY, hadoopFolder);
        }
    }
}
