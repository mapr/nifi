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

package org.apache.nifi.spring;

import org.apache.nifi.controller.leader.election.CuratorLeaderElectionManager;
import org.apache.nifi.controller.leader.election.LeaderElectionManager;
import org.apache.nifi.controller.leader.election.StandaloneLeaderElectionManager;
import org.apache.nifi.security.util.JaasConfigWrapper;
import org.apache.nifi.util.NiFiProperties;
import org.apache.nifi.util.StringUtils;
import org.apache.zookeeper.client.ZKClientConfig;
import org.springframework.beans.factory.FactoryBean;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

public class LeaderElectionManagerFactoryBean implements FactoryBean<LeaderElectionManager> {
    private int numThreads;
    private NiFiProperties properties;

    @Override
    public LeaderElectionManager getObject() throws Exception {
        final boolean isNode = properties.isNode();
        if (isNode) {
            addZookeeperLoginContextIfMissing();
            return new CuratorLeaderElectionManager(numThreads, properties);
        } else {
            return new StandaloneLeaderElectionManager();
        }
    }

    /**
     * Ensures that a JAAS login context for Zookeeper exists.
     * <p>
     * If the login context specified in NiFi properties is missing in the current system configuration,
     * this method wraps the default Zookeeper client entry using a wrapper login module, preserving the same control
     * flag and options, and adds it under the configured name.
     * <p>
     * We used it to add an isolated MaprSasl login module.
     */
    private void addZookeeperLoginContextIfMissing() {
        String loginContextName = properties.getProperty(NiFiProperties.ZOOKEEPER_LOGIN_CONTEXT_NAME);
        if (StringUtils.isNotEmpty(loginContextName)) {
            AppConfigurationEntry[] zookeeperLoginContext = Configuration.getConfiguration().getAppConfigurationEntry(loginContextName);
            if (zookeeperLoginContext == null) {
                JaasConfigWrapper.wrapDefaultConfig(ZKClientConfig.LOGIN_CONTEXT_NAME_KEY_DEFAULT, loginContextName);
            }
        }
    }

    @Override
    public Class<?> getObjectType() {
        return LeaderElectionManager.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void setNumThreads(final int numThreads) {
        this.numThreads = numThreads;
    }

    public void setProperties(final NiFiProperties properties) {
        this.properties = properties;
    }
}
