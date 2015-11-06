package com.appdynamics.extensions.activemq;


import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.util.Map;

public class ActiveMQMonitorTest {

    public static final String CONFIG_ARG = "config-file";

    @Test
    public void testActiveMQMonitorExtension() throws TaskExecutionException {
        ActiveMQMonitor monitor = new ActiveMQMonitor();
        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put(CONFIG_ARG, "src/test/resources/conf/config.yml");
        monitor.execute(taskArgs, null);
    }

    @Test
    public void testActiveMQMonitorExtensionWithExclude() throws TaskExecutionException {
        ActiveMQMonitor monitor = new ActiveMQMonitor();
        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put(CONFIG_ARG, "src/test/resources/conf/config.yml.withExclude");
        monitor.execute(taskArgs, null);
    }

    @Test
    public void testActiveMQMonitorExtensionWithIncludeExclude() throws TaskExecutionException {
        ActiveMQMonitor monitor = new ActiveMQMonitor();
        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put(CONFIG_ARG, "src/test/resources/conf/config.yml.withIncludeExclude");
        monitor.execute(taskArgs, null);
    }
}
