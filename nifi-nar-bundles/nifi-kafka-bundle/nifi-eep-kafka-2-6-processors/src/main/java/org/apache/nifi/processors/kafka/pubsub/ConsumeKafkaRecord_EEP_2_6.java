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

@Tags({"EEP", "MapR", "Kafka", "Get", "Record", "csv", "avro", "json", "Ingest", "Ingress", "Topic", "PubSub", "Consume", "2.6"})
@CapabilityDescription("Consumes messages from EEP Kafka specifically built against the EEP Kafka 2.6 Consumer API. "
        + "The complementary NiFi processor for sending messages is PublishKafkaRecord_EEP_2_6. Please note that, at this time, the Processor assumes that "
        + "all records that are retrieved from a given partition have the same schema. If any of the Kafka messages are pulled but cannot be parsed or written with the "
        + "configured Record Reader or Record Writer, the contents of the message will be written to a separate FlowFile, and that FlowFile will be transferred to the "
        + "'parse.failure' relationship. Otherwise, each FlowFile is sent to the 'success' relationship and may contain many individual messages within the single FlowFile. "
        + "A 'record.count' attribute is added to indicate how many messages are contained in the FlowFile. No two Kafka messages will be placed into the same FlowFile if they "
        + "have different schemas, or if they have different values for a message header that is included by the <Headers to Add as Attributes> property.")
@SeeAlso({ConsumeKafka_EEP_2_6.class, PublishKafka_EEP_2_6.class, PublishKafkaRecord_EEP_2_6.class})
public class ConsumeKafkaRecord_EEP_2_6 extends ConsumeKafkaRecord_2_6 {
}
