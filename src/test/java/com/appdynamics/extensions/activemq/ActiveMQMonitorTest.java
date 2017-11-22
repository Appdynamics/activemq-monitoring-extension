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

        //TODO remove this
        Map<String, String> argsMap = Maps.newHashMap();
        argsMap.put("config-file", "/Users/bhuvnesh.kumar/repos/appdynamics/extensions/activemq-monitoring-extension/src/test/resources/conf/config_for_test.yml");
        activeMQMonitor.execute(argsMap, null);
    }
}
