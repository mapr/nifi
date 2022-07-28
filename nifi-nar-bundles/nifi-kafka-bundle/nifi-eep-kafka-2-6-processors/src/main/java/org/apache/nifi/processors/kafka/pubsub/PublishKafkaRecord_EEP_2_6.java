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

package org.apache.nifi.processors.kafka.pubsub;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.Relationship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.nifi.expression.ExpressionLanguageScope.NONE;

@Tags({"EEP", "MapR", "Kafka", "Record", "csv", "json", "avro", "logs", "Put", "Send", "Message", "PubSub", "2.6"})
@CapabilityDescription("Sends the contents of a FlowFile as individual records to EEP Kafka using the Kafka 2.6 Producer API. "
        + "The contents of the FlowFile are expected to be record-oriented data that can be read by the configured Record Reader. "
        + "The complementary NiFi processor for fetching messages is ConsumeKafkaRecord_EEP_2_6.")
@SeeAlso({PublishKafka_EEP_2_6.class, ConsumeKafka_EEP_2_6.class, ConsumeKafkaRecord_EEP_2_6.class})
public class PublishKafkaRecord_EEP_2_6 extends PublishKafkaRecord_2_6 {

    //TODO Remove after adding transaction support to EEP Kafka
    static final PropertyDescriptor USE_TRANSACTIONS_EEP = new PropertyDescriptor.Builder()
            .name("use-transactions")
            .displayName("Use Transactions")
            .description("Specifies whether or not NiFi should provide Transactional guarantees when communicating with Kafka. If there is a problem sending data to Kafka, "
                    + "and this property is set to false, then the messages that have already been sent to Kafka will continue on and be delivered to consumers. "
                    + "If this is set to true, then the Kafka transaction will be rolled back so that those messages are not available to consumers. Setting this to true "
                    + "requires that the <Delivery Guarantee> property be set to \"Guarantee Replicated Delivery.\"")
            .expressionLanguageSupported(NONE)
            .allowableValues("true", "false")
            .defaultValue("false")
            .required(true)
            .build();

    private static final List<PropertyDescriptor> PROPERTIES_EEP;
    private static final Set<Relationship> RELATIONSHIPS_EEP;

    static {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(KafkaProcessorUtils.BOOTSTRAP_SERVERS);
        properties.add(TOPIC);
        properties.add(RECORD_READER);
        properties.add(RECORD_WRITER);
        properties.add(USE_TRANSACTIONS_EEP);
        properties.add(TRANSACTIONAL_ID_PREFIX);
        properties.add(KafkaProcessorUtils.FAILURE_STRATEGY);
        properties.add(DELIVERY_GUARANTEE);
        properties.add(ATTRIBUTE_NAME_REGEX);
        properties.add(MESSAGE_HEADER_ENCODING);
        properties.add(KafkaProcessorUtils.SECURITY_PROTOCOL);
        properties.add(KafkaProcessorUtils.SASL_MECHANISM);
        properties.add(KafkaProcessorUtils.KERBEROS_CREDENTIALS_SERVICE);
        properties.add(KafkaProcessorUtils.SELF_CONTAINED_KERBEROS_USER_SERVICE);
        properties.add(KafkaProcessorUtils.JAAS_SERVICE_NAME);
        properties.add(KafkaProcessorUtils.USER_PRINCIPAL);
        properties.add(KafkaProcessorUtils.USER_KEYTAB);
        properties.add(KafkaProcessorUtils.USERNAME);
        properties.add(KafkaProcessorUtils.PASSWORD);
        properties.add(KafkaProcessorUtils.TOKEN_AUTH);
        properties.add(KafkaProcessorUtils.SSL_CONTEXT_SERVICE);
        properties.add(MESSAGE_KEY_FIELD);
        properties.add(MAX_REQUEST_SIZE);
        properties.add(ACK_WAIT_TIME);
        properties.add(METADATA_WAIT_TIME);
        properties.add(PARTITION_CLASS);
        properties.add(PARTITION);
        properties.add(COMPRESSION_CODEC);

        PROPERTIES_EEP = Collections.unmodifiableList(properties);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        RELATIONSHIPS_EEP = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return RELATIONSHIPS_EEP;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return PROPERTIES_EEP;
    }
}
