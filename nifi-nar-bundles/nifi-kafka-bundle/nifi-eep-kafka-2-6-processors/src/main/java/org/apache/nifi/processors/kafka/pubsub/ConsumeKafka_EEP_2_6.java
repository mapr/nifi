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
import org.apache.nifi.annotation.documentation.Tags;

@Tags({"EEP", "MapR", "Kafka"  , "Get", "Ingest", "Ingress", "Topic", "PubSub", "Consume", "2.6"})
@CapabilityDescription("Consumes messages from EEP Kafka specifically built against the Kafka 2.6 Consumer API. "
        + "The complementary NiFi processor for sending messages is PublishKafka_EEP_2_6.")
public class ConsumeKafka_EEP_2_6 extends ConsumeKafka_2_6 {
}