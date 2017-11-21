/**
 * Copyright 2013 AppDynamics, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appdynamics.extensions.activemq;

import java.util.HashMap;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TaskInputArgs;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.util.AssertUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.TaskInputArgs.PASSWORD_ENCRYPTED;
import static com.appdynamics.extensions.activemq.ActiveMQUtil.convertToString;


public class ActiveMQMonitor extends ABaseMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ActiveMQMonitor.class);

    //Required for MonitorConfiguration initialisation
    @Override
    protected String getDefaultMetricPrefix() {
        return "Custom Metrics|ActiveMQ";
    }

    //Required for MonitorConfiguration initialisation
    @Override
    public String getMonitorName() {
        return "Vertica Monitor";
    }


    @Override
    protected void doRun(TasksExecutionServiceProvider taskExecutor) {
        Map<String, ?> config = configuration.getConfigYml();
        if (config != null) {
            List<Map> servers = (List) config.get("servers");
            AssertUtils.assertNotNull(servers, "The 'servers' section in config.yml is not initialised");

            if (servers != null && !servers.isEmpty()) {
                for (Map server : servers) {
                    try {
                        ActiveMQMonitorTask task = createTask(server, taskExecutor);
                        taskExecutor.submit((String) server.get("name"), task);
                    } catch (IOException e) {
                        logger.error("Cannot construct JMX uri for {}", convertToString(server.get("displayName"), ""));
                    }
                }
            } else {
                logger.error("There are no servers configured");
            }
        } else {
            logger.error("The config.yml is not loaded due to previous errors.The task will not run");
        }

    }

    @Override
    protected int getTaskCount() {
        List<Map<String, String>> servers = (List<Map<String, String>>) configuration.getConfigYml().get("servers");
        AssertUtils.assertNotNull(servers, "The 'servers' section in config.yml is not initialised");
        return servers.size();
    }


    private ActiveMQMonitorTask createTask(Map server, TasksExecutionServiceProvider taskExecutor) throws IOException {

        String serviceUrl = convertToString(server.get("serviceUrl"), "");
        String host = convertToString(server.get("host"), "");
        String portStr = convertToString(server.get("port"), "");
        int port = (portStr == null || portStr == "") ? -1 : Integer.parseInt(portStr);
        String username = convertToString(server.get("username"), "");
        String password = getPassword(server);

        JMXConnectionAdapter adapter = JMXConnectionAdapter.create(serviceUrl, host, port, username, password);
        return new ActiveMQMonitorTask.Builder().
                metricPrefix(configuration.getMetricPrefix()).
                metricWriter(taskExecutor.getMetricWriteHelper()).
                jmxConnectionAdapter(adapter).server(server).
                mbeans((List<Map>) configuration.getConfigYml().get("mbeans")).build();
    }

    private String getPassword(Map server) {
        String password = convertToString(server.get("password"), "");
        if (!Strings.isNullOrEmpty(password)) {
            return password;
        }
        String encryptionKey = convertToString(configuration.getConfigYml().get("encryptionKey"), "");
        String encryptedPassword = convertToString(server.get("encryptedPassword"), "");
        if (!Strings.isNullOrEmpty(encryptionKey) && !Strings.isNullOrEmpty(encryptedPassword)) {
            java.util.Map<String, String> cryptoMap = Maps.newHashMap();
            cryptoMap.put(PASSWORD_ENCRYPTED, encryptedPassword);
            cryptoMap.put(TaskInputArgs.ENCRYPTION_KEY, encryptionKey);
            return CryptoUtil.getPassword(cryptoMap);
        }
        return null;
    }


    public static void main(String[] args) throws TaskExecutionException {
        ActiveMQMonitor activeMQMonitor = new ActiveMQMonitor();

        Map<String, String> argsMap = new HashMap<String, String>();
        argsMap.put("config-file", "/Users/bhuvnesh.kumar/repos/appdynamics/extensions/activemq-monitoring-extension/src/test/resources/conf/config_for_test.yml");
        activeMQMonitor.execute(argsMap, null);
    }

}
