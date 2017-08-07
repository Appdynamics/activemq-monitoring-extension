package com.appdynamics.extensions.activemq;



import com.google.common.collect.Lists;
import static com.appdynamics.extensions.activemq.Constants.*;

import com.appdynamics.extensions.activemq.config.MBean;
import com.appdynamics.extensions.activemq.config.Server;
import com.appdynamics.extensions.util.metrics.MetricOverride;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.openmbean.CompositeDataSupport;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.appdynamics.extensions.activemq.ActiveMQMonitorConstants.*;
import static com.appdynamics.extensions.util.metrics.MetricConstants.METRICS_SEPARATOR;


import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

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

    @Test
    public void testForCompositeValueMetrics() throws TaskExecutionException{
        ActiveMQMonitor monitor = new ActiveMQMonitor();
        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put(CONFIG_ARG, "src/test/resources/conf/config.yml.withCompositeData");
        monitor.execute(taskArgs, null);
    }

}
