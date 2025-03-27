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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.nifi.kafka.shared.property.SaslMechanism;
import org.apache.nifi.kafka.shared.property.SecurityProtocol;
import org.apache.nifi.kerberos.KerberosCredentialsService;
import org.apache.nifi.kerberos.KerberosUserService;
import org.apache.nifi.kerberos.SelfContainedKerberosUserService;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestConsumeKafka_3 {

    ConsumerLease mockLease = null;
    ConsumerPool mockConsumerPool = null;

    @BeforeEach
    public void setup() {
        mockLease = mock(ConsumerLease.class);
        mockConsumerPool = mock(ConsumerPool.class);
    }

    @Test
    public void validateNoLimitToTopicCount() {
        final int expectedCount = 101;
        final String topics = String.join(",", Collections.nCopies(expectedCount, "foo"));
        final ConsumeKafka_3 consumeKafka = new ConsumeKafka_3() {
            protected ConsumerPool createConsumerPool(final ProcessContext context, final ComponentLog log) {
                final ConsumerPool consumerPool = super.createConsumerPool(context, log);
                try {
                    final Field topicsField = ConsumerPool.class.getDeclaredField("topics");
                    topicsField.setAccessible(true);
                    final Object o = topicsField.get(consumerPool);
                    final List<?> list = assertInstanceOf(List.class, o);
                    assertEquals(expectedCount, list.size());
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                return consumerPool;
            }
        };

        TestRunner runner = TestRunners.newTestRunner(consumeKafka);
        runner.setValidateExpressionUsage(false);
        runner.setProperty(ConsumeKafka_3.BOOTSTRAP_SERVERS, "localhost:1234");
        runner.setProperty(ConsumeKafka_3.TOPICS, topics);
        runner.setProperty(ConsumeKafka_3.GROUP_ID, "foo");
        runner.setProperty(ConsumeKafka_3.AUTO_OFFSET_RESET, ConsumeKafka_3.OFFSET_EARLIEST);
        runner.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        runner.run();
    }

    @Test
    public void validateCustomValidatorSettings() {
        ConsumeKafka_3 consumeKafka = new ConsumeKafka_3();
        TestRunner runner = TestRunners.newTestRunner(consumeKafka);
        runner.setProperty(ConsumeKafka_3.BOOTSTRAP_SERVERS, "okeydokey:1234");
        runner.setProperty(ConsumeKafka_3.TOPICS, "foo");
        runner.setProperty(ConsumeKafka_3.GROUP_ID, "foo");
        runner.setProperty(ConsumeKafka_3.AUTO_OFFSET_RESET, ConsumeKafka_3.OFFSET_EARLIEST);
        runner.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        runner.assertValid();
        runner.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        runner.assertValid();
    }

    @Test
    public void validatePropertiesValidation() {
        ConsumeKafka_3 consumeKafka = new ConsumeKafka_3();
        TestRunner runner = TestRunners.newTestRunner(consumeKafka);
        runner.setProperty(ConsumeKafka_3.BOOTSTRAP_SERVERS, "okeydokey:1234");
        runner.setProperty(ConsumeKafka_3.TOPICS, "foo");
        runner.setProperty(ConsumeKafka_3.GROUP_ID, "foo");
        runner.setProperty(ConsumeKafka_3.AUTO_OFFSET_RESET, ConsumeKafka_3.OFFSET_EARLIEST);

        runner.removeProperty(ConsumeKafka_3.GROUP_ID);

        AssertionError e = assertThrows(AssertionError.class, runner::assertValid);
        assertTrue(e.getMessage().contains("invalid because Group ID is required"));

        runner.setProperty(ConsumeKafka_3.GROUP_ID, "");

        e = assertThrows(AssertionError.class, runner::assertValid);
        assertTrue(e.getMessage().contains("must contain at least one character that is not white space"));

        runner.setProperty(ConsumeKafka_3.GROUP_ID, "  ");

        e = assertThrows(AssertionError.class, runner::assertValid);
        assertTrue(e.getMessage().contains("must contain at least one character that is not white space"));
    }

    @Test
    public void testJaasGssApiConfiguration() throws Exception {
        ConsumeKafka_3 consumeKafka = new ConsumeKafka_3();
        TestRunner runner = TestRunners.newTestRunner(consumeKafka);
        runner.setProperty(ConsumeKafka_3.BOOTSTRAP_SERVERS, "okeydokey:1234");
        runner.setProperty(ConsumeKafka_3.TOPICS, "foo");
        runner.setProperty(ConsumeKafka_3.GROUP_ID, "foo");
        runner.setProperty(ConsumeKafka_3.AUTO_OFFSET_RESET, ConsumeKafka_3.OFFSET_EARLIEST);

        runner.setProperty(ConsumeKafka_3.SECURITY_PROTOCOL, SecurityProtocol.SASL_PLAINTEXT.name());
        runner.setProperty(ConsumeKafka_3.SASL_MECHANISM, SaslMechanism.GSSAPI.getValue());
        runner.assertNotValid();

        runner.setProperty(ConsumeKafka_3.KERBEROS_SERVICE_NAME, "kafka");
        runner.assertNotValid();

        runner.setProperty(ConsumeKafka_3.KERBEROS_PRINCIPAL, "nifi@APACHE.COM");
        runner.assertNotValid();

        runner.setProperty(ConsumeKafka_3.KERBEROS_KEYTAB, "not.A.File");
        runner.assertNotValid();

        runner.setProperty(ConsumeKafka_3.KERBEROS_KEYTAB, "src/test/resources/server.properties");
        runner.assertValid();

        runner.setVariable("keytab", "src/test/resources/server.properties");
        runner.setVariable("principal", "nifi@APACHE.COM");
        runner.setVariable("service", "kafka");
        runner.setProperty(ConsumeKafka_3.KERBEROS_PRINCIPAL, "${principal}");
        runner.setProperty(ConsumeKafka_3.KERBEROS_KEYTAB, "${keytab}");
        runner.setProperty(ConsumeKafka_3.KERBEROS_SERVICE_NAME, "${service}");
        runner.assertValid();

        final KerberosUserService kerberosUserService = enableKerberosUserService(runner);
        runner.setProperty(ConsumeKafka_3.SELF_CONTAINED_KERBEROS_USER_SERVICE, kerberosUserService.getIdentifier());
        runner.assertNotValid();

        runner.removeProperty(ConsumeKafka_3.KERBEROS_PRINCIPAL);
        runner.removeProperty(ConsumeKafka_3.KERBEROS_KEYTAB);
        runner.assertValid();

        final KerberosCredentialsService kerberosCredentialsService = enabledKerberosCredentialsService(runner);
        runner.setProperty(ConsumeKafka_3.KERBEROS_CREDENTIALS_SERVICE, kerberosCredentialsService.getIdentifier());
        runner.assertNotValid();

        runner.removeProperty(ConsumeKafka_3.SELF_CONTAINED_KERBEROS_USER_SERVICE);
        runner.assertValid();
    }

    private SelfContainedKerberosUserService enableKerberosUserService(final TestRunner runner) throws InitializationException {
        final SelfContainedKerberosUserService kerberosUserService = mock(SelfContainedKerberosUserService.class);
        when(kerberosUserService.getIdentifier()).thenReturn("userService1");
        runner.addControllerService(kerberosUserService.getIdentifier(), kerberosUserService);
        runner.enableControllerService(kerberosUserService);
        return kerberosUserService;
    }

    private KerberosCredentialsService enabledKerberosCredentialsService(final TestRunner runner) throws InitializationException {
        final KerberosCredentialsService credentialsService = mock(KerberosCredentialsService.class);
        when(credentialsService.getIdentifier()).thenReturn("credsService1");
        when(credentialsService.getPrincipal()).thenReturn("principal1");
        when(credentialsService.getKeytab()).thenReturn("keytab1");

        runner.addControllerService(credentialsService.getIdentifier(), credentialsService);
        runner.enableControllerService(credentialsService);
        return credentialsService;
    }

}
