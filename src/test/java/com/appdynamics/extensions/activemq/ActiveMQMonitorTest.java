package com.appdynamics.extensions.activemq;


 import com.google.common.collect.Maps;
 import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
 import org.junit.Test;

 import java.util.Map;
public class ActiveMQMonitorTest {
    public static final String CONFIG_ARG = "config-file";

    @Test
    public void testActiveMQMonitorExtension() throws TaskExecutionException{
        ActiveMQMonitor activeMQMonitor = new ActiveMQMonitor();

        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put(CONFIG_ARG, "src/test/resources/conf/config.yml");
        activeMQMonitor.execute(taskArgs, null);
    }
}
