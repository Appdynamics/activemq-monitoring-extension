/**
 * Copyright 2013 AppDynamics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appdynamics.extensions.activemq;

import com.appdynamics.TaskInputArgs;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.activemq.config.Configuration;
import com.appdynamics.extensions.conf.MonitorConfiguration;

import com.appdynamics.extensions.activemq.config.Server;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.file.FileLoader;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.appdynamics.extensions.yml.YmlReader;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXServiceURL;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.appdynamics.TaskInputArgs.ENCRYPTION_KEY;
import static com.appdynamics.TaskInputArgs.PASSWORD_ENCRYPTED;
import static com.appdynamics.extensions.activemq.ActiveMQUtil.convertToString;


public class ActiveMQMonitor extends AManagedMonitor{

    private static final Logger logger = LoggerFactory.getLogger(ActiveMQMonitor.class);
    //public static final String CONFIG_ARG = "config-file";

    private MonitorConfiguration configuration;

    private static String getImplementationVersion() {
        return ActiveMQMonitor.class.getPackage().getImplementationVersion();
    }

    private String logVersion() {
        String msg = "Using ActiveMQ Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        return msg;
    }

    public ActiveMQMonitor(){
        logger.info(logVersion());
        System.out.print(logVersion());
    }

    private ActiveMQMonitorTask createTask(Map server) throws IOException{

        String serviceUrl = convertToString(server.get("serviceUrl"),"");
        String host = convertToString(server.get("host"),"");
        String portStr = convertToString(server.get("port"),"");
        int    port = (portStr == null || portStr == "") ? -1 : Integer.parseInt(portStr);
        String username = convertToString(server.get("username"), "");
        String password = getPassword(server);

        JMXConnectionAdapter adapter = JMXConnectionAdapter.create(serviceUrl, host, port, username, password);
        return new ActiveMQMonitorTask.Builder().metricPrefix(
                configuration.getMetricPrefix()).metricWriter(
                        configuration.getMetricWriter()).jmxConnectionAdapter(adapter).server(server).mbeans(
                                (List<Map>) configuration.getConfigYml().get("mbeans")).build();
    }

    private String getPassword (Map server) {
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


    private class TaskRunner implements Runnable{

        public void run(){
            Map<String, ?> config = configuration.getConfigYml();
            if(config != null){
                List<Map> servers = (List) config.get("servers");
                if (servers != null && !servers.isEmpty()){
                    for(Map server: servers){
                        try{
                            ActiveMQMonitorTask task = createTask(server);
                            configuration.getExecutorService().execute(task);
                        } catch (IOException e){
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
    }

    private void initialize(Map<String, String > taskArgs){

        if(configuration == null){
            MetricWriteHelper metricWriter = MetricWriteHelperFactory.create(this);
            MonitorConfiguration conf = new MonitorConfiguration("Custom Metrics|ActiveMQ|", new TaskRunner(),metricWriter);
            final String configFilePath = taskArgs.get("config-file");
            conf.setConfigYml(configFilePath);
            conf.checkIfInitialized(MonitorConfiguration.ConfItem.METRIC_PREFIX, MonitorConfiguration.ConfItem
                    .CONFIG_YML, MonitorConfiguration.ConfItem.HTTP_CLIENT, MonitorConfiguration.ConfItem
                    .EXECUTOR_SERVICE);
            this.configuration = conf;

        }
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext out) throws TaskExecutionException{

        logVersion();
        logger.debug("The raw arguments are: ", taskArgs);

        try{
            initialize(taskArgs);
            configuration.executeTask();
        }
        catch (Exception ex){
            if(configuration != null && configuration.getMetricWriter() != null){
                configuration.getMetricWriter().registerError(ex.getMessage(),ex);
            }
        }

        return null;
    }

    public static void main (String[] args) throws TaskExecutionException {
        ActiveMQMonitor activeMQMonitor = new ActiveMQMonitor();

        Map<String, String> argsMap = new HashMap<String, String>();
        argsMap.put("config-file", "/Users/bhuvnesh.kumar/repos/appdynamics/extensions/activemq-monitoring-extension" +
                "" + "" + "/src/main/resources/conf/config.yml");
        activeMQMonitor.execute(argsMap, null);
    }

}
