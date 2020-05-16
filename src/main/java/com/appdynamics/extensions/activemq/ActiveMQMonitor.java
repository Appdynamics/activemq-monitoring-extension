/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.activemq;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.AssertUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.activemq.utils.Constants.*;

public class ActiveMQMonitor extends ABaseMonitor {


    private static final Logger logger = ExtensionsLoggerFactory.getLogger(ActiveMQMonitor.class);

    @Override
    protected String getDefaultMetricPrefix() {
        return CUSTOMMETRICS + METRICS_SEPARATOR + MONITORNAME;
    }

    @Override
    public String getMonitorName() {
        return MONITORNAME;
    }

    @Override
    protected void doRun(TasksExecutionServiceProvider taskExecutor) {
        try {
            Map<String, ?> config = getContextConfiguration().getConfigYml();
            if (config != null) {
                List<Map<String, ?>> servers = getServers();
                if (!servers.isEmpty()) {
                    for (Map server : servers) {
                        AssertUtils.assertNotNull(server.get(DISPLAY_NAME), DISPLAY_NAME + " can not be null in the config_old.yml");

                        ActiveMQMonitorTask task = new ActiveMQMonitorTask(taskExecutor.getMetricWriteHelper(), server, getContextConfiguration());
                        taskExecutor.submit((String) server.get(DISPLAY_NAME), task);
                    }
                } else {
                    logger.error("There are no servers configured");
                }
            } else {
                logger.error("The config_old.yml is not loaded due to previous errors.The task will not run");
            }
        } catch (Exception e) {
            logger.error("JMX Extension can not proceed due to errors in the config.", e);
        }
    }

    protected List<Map<String, ?>> getServers() {
        List<Map<String, ?>> servers = (List<Map<String, ?>>) getContextConfiguration().getConfigYml().get(SERVERS);
        AssertUtils.assertNotNull(servers, "The 'servers' section in config_old.yml is not initialised");
        return servers;
    }
}
