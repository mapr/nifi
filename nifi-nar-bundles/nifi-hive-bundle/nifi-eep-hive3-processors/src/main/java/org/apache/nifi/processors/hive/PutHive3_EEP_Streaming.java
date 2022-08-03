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

package org.apache.nifi.processors.hive;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.resource.ResourceCardinality;
import org.apache.nifi.components.resource.ResourceType;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.util.mapr.MapRComponentsUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Tags({"hive", "eep", "mapr", "streaming", "put", "database", "store"})
@CapabilityDescription("This processor uses Hive Streaming to send flow file records to an EEP Hive 3.0+ table. If 'Static Partition Values' is not set, then "
        + "the partition values are expected to be the 'last' fields of each record, so if the table is partitioned on column A for example, then the last field in "
        + "each record should be field A. If 'Static Partition Values' is set, those values will be used as the partition values, and any record fields corresponding to "
        + "partition columns will be ignored.")
public class PutHive3_EEP_Streaming extends PutHive3Streaming {
    private static final String HIVE_COMPONENT_NAME = "hive";
    private static final String HIVE_CONF_CONF = "conf/hive-site.xml";

    private static final String HIVE_CONFIG_PATH = getHiveConfPath();

    static final PropertyDescriptor HIVE_CONFIGURATION_RESOURCES_EEP = new PropertyDescriptor.Builder()
            .name("hive3-config-resources")
            .displayName("Hive Configuration Resources")
            .description("A file or comma separated list of files which contains the Hive configuration (hive-site.xml, e.g.). Without this, Hadoop "
                    + "will search the classpath for a 'hive-site.xml' file or will revert to a default configuration. Note that to enable authentication "
                    + "with Kerberos e.g., the appropriate properties must be set in the configuration files. Also note that if Max Concurrent Tasks is set "
                    + "to a number greater than one, the 'hcatalog.hive.client.cache.disabled' property will be forced to 'true' to avoid concurrency issues. "
                    + "Please see the Hive documentation for more details.")
            .required(false)
            .defaultValue(HIVE_CONFIG_PATH)
            .identifiesExternalResource(ResourceCardinality.MULTIPLE, ResourceType.FILE)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    private List<PropertyDescriptor> propertyDescriptorsEEP;
    private Set<Relationship> relationshipsEEP;

    @Override
    protected void init(ProcessorInitializationContext context) {
        List<PropertyDescriptor> props = new ArrayList<>();
        props.add(RECORD_READER);
        props.add(METASTORE_URI);
        props.add(HIVE_CONFIGURATION_RESOURCES_EEP);
        props.add(DB_NAME);
        props.add(TABLE_NAME);
        props.add(STATIC_PARTITION_VALUES);
        props.add(RECORDS_PER_TXN);
        props.add(TXNS_PER_BATCH);
        props.add(CALL_TIMEOUT);
        props.add(DISABLE_STREAMING_OPTIMIZATIONS);
        props.add(ROLLBACK_ON_FAILURE);
        props.add(KERBEROS_CREDENTIALS_SERVICE);
        props.add(KERBEROS_PRINCIPAL);
        props.add(KERBEROS_PASSWORD);

        propertyDescriptorsEEP = Collections.unmodifiableList(props);

        Set<Relationship> _relationships = new HashSet<>();
        _relationships.add(REL_SUCCESS);
        _relationships.add(REL_FAILURE);
        _relationships.add(REL_RETRY);
        relationshipsEEP = Collections.unmodifiableSet(_relationships);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return propertyDescriptorsEEP;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationshipsEEP;
    }

    private static String getHiveConfPath() {
        Path hiveFolder;
        try {
            hiveFolder = MapRComponentsUtils.getComponentFolder(HIVE_COMPONENT_NAME);
        } catch (IOException e) {
            return "";
        }

        return hiveFolder.resolve(HIVE_CONF_CONF).toString();
    }
}