package com.appdynamics.extensions.activemq.metrics;

import com.appdynamics.extensions.activemq.ActiveMQMonitorTask;
import com.appdynamics.extensions.activemq.JMXConnectionAdapter;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.metrics.MetricProperties;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.management.*;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.remote.JMXConnector;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;

public class NodeMetricsProcessorTest {

    JMXConnector jmxConnector = mock(JMXConnector.class);
    JMXConnectionAdapter jmxConnectionAdapter = mock(JMXConnectionAdapter.class);

    @Test
    public void getNodeMetrics_NonCompositeObject () throws MalformedObjectNameException, IntrospectionException, ReflectionException,
            InstanceNotFoundException, IOException {
        Map config = YmlReader.readFromFileAsMap(new File(this.getClass().getResource("/conf/config_without_composite_object.yml").getFile()));
        List<Map> mBeans = (List) config.get("mbeans");
        Set<ObjectInstance> objectInstances = Sets.newHashSet();
        objectInstances.add(new ObjectInstance("org.apache.activemq.metrics:type=ClientRequest,scope=Read,name=Latency", "test"));

        Set<Attribute> attributes = Sets.newHashSet();
        attributes.add(new Attribute("Min", new BigDecimal(100)));
        attributes.add(new Attribute("Max", new BigDecimal(200)));

        List<String> metricNames = Lists.newArrayList();
        metricNames.add("metric1");
        metricNames.add("metric2");

        when(jmxConnectionAdapter.queryMBeans(eq(jmxConnector), Mockito.any(ObjectName.class))).thenReturn(objectInstances);
        when(jmxConnectionAdapter.getReadableAttributeNames(eq(jmxConnector), Mockito.any(ObjectInstance.class))).thenReturn(metricNames);
        when(jmxConnectionAdapter.getAttributes(eq(jmxConnector), Mockito.any(ObjectName.class), Mockito.any(String[].class))).thenReturn(attributes);


        NodeMetricsProcessor nodeMetricsProcessor = new NodeMetricsProcessor(jmxConnectionAdapter, jmxConnector);


        ActiveMQMonitorTask activeMQMonitorTask= new ActiveMQMonitorTask();
        Map<String, MetricProperties> metricPropertiesMap = activeMQMonitorTask.getMapOfProperties(mBeans.get(0));

        List<Metric> metrics = nodeMetricsProcessor.getNodeMetrics(mBeans.get(0), metricPropertiesMap, "");
        Assert.assertTrue(metrics.get(0).getMetricPath().equals("ClientRequest|Read|Latency|Max"));
        Assert.assertTrue(metrics.get(0).getMetricName().equals("Max"));
        Assert.assertTrue(metrics.get(0).getMetricValue().equals("200"));
        Assert.assertTrue(metrics.get(0).getMetricProperties().getAggregationType().equals(metricPropertiesMap.get("Max").getAggregationType()));
        Assert.assertTrue(metrics.get(0).getMetricProperties().getClusterRollUpType().equals(metricPropertiesMap.get("Max").getClusterRollUpType()));
        Assert.assertTrue(metrics.get(0).getMetricProperties().getTimeRollUpType().equals(metricPropertiesMap.get("Max").getTimeRollUpType()));


        Assert.assertTrue(metrics.get(1).getMetricPath().equals("ClientRequest|Read|Latency|Min"));
        Assert.assertTrue(metrics.get(1).getMetricName().equals("Min"));
        Assert.assertTrue(metrics.get(1).getMetricValue().equals("100"));
        Assert.assertTrue(metrics.get(1).getMetricProperties().getAggregationType().equals(metricPropertiesMap.get("Min").getAggregationType()));
        Assert.assertTrue(metrics.get(1).getMetricProperties().getClusterRollUpType().equals(metricPropertiesMap.get("Min").getClusterRollUpType()));
        Assert.assertTrue(metrics.get(1).getMetricProperties().getTimeRollUpType().equals(metricPropertiesMap.get("Min").getTimeRollUpType()));

    }

    @Test
    public void getNodeMetrics_CompositeObject () throws MalformedObjectNameException, IntrospectionException,
            ReflectionException, InstanceNotFoundException, IOException, OpenDataException {
        Map config = YmlReader.readFromFileAsMap(new File(this.getClass().getResource
                ("/conf/config_with_composite_object.yml").getFile()));
        List<Map> mBeans = (List) config.get("mbeans");
        Set<ObjectInstance> objectInstances = Sets.newHashSet();
        objectInstances.add(new ObjectInstance("java.lang:type=Memory", "test"));

        Set<Attribute> attributes = Sets.newHashSet();
        attributes.add(new Attribute("HeapMemoryUsage", createCompositeDataSupportObject()));

        List<String> metricNames = Lists.newArrayList();
        metricNames.add("metric1");
        metricNames.add("metric2");


        when(jmxConnectionAdapter.queryMBeans(eq(jmxConnector), Mockito.any(ObjectName.class))).thenReturn(objectInstances);
        when(jmxConnectionAdapter.getReadableAttributeNames(eq(jmxConnector), Mockito.any(ObjectInstance.class))).thenReturn(metricNames);
        when(jmxConnectionAdapter.getAttributes(eq(jmxConnector), Mockito.any(ObjectName.class), Mockito.any(String[].class))).thenReturn(attributes);

        NodeMetricsProcessor nodeMetricsProcessor = new NodeMetricsProcessor(jmxConnectionAdapter, jmxConnector);


        ActiveMQMonitorTask activeMQMonitorTask= new ActiveMQMonitorTask();
        Map<String, MetricProperties> metricPropertiesMap = activeMQMonitorTask.getMapOfProperties(mBeans.get(0));
        List<Metric> metrics = nodeMetricsProcessor.getNodeMetrics(mBeans.get(0), metricPropertiesMap, "");
//
        Assert.assertTrue(metrics.get(0).getMetricPath().equals("Memory|HeapMemoryUsage.max"));
        Assert.assertTrue(metrics.get(0).getMetricName().equals("HeapMemoryUsage.max"));
        Assert.assertTrue(metrics.get(0).getMetricValue().equals("100"));
        Assert.assertTrue(metrics.get(0).getMetricProperties().getTimeRollUpType().equals(metricPropertiesMap.get("HeapMemoryUsage.max").getTimeRollUpType()));
        Assert.assertTrue(metrics.get(0).getMetricProperties().getClusterRollUpType().equals(metricPropertiesMap.get("HeapMemoryUsage.max").getClusterRollUpType()));
        Assert.assertTrue(metrics.get(0).getMetricProperties().getAggregationType().equals(metricPropertiesMap.get("HeapMemoryUsage.max").getAggregationType()));

        Assert.assertTrue(metrics.get(1).getMetricPath().equals("Memory|HeapMemoryUsage.used"));
        Assert.assertTrue(metrics.get(1).getMetricName().equals("HeapMemoryUsage.used"));
        Assert.assertTrue(metrics.get(1).getMetricValue().equals("50"));
        Assert.assertTrue(metrics.get(1).getMetricProperties().getTimeRollUpType().equals(metricPropertiesMap.get("HeapMemoryUsage.used").getTimeRollUpType()));
        Assert.assertTrue(metrics.get(1).getMetricProperties().getClusterRollUpType().equals(metricPropertiesMap.get("HeapMemoryUsage.used").getClusterRollUpType()));
        Assert.assertTrue(metrics.get(1).getMetricProperties().getAggregationType().equals(metricPropertiesMap.get("HeapMemoryUsage.used").getAggregationType()));

    }

    @Test
    public void getNodeMetrics_compositeAndNonCompositeObjects() throws MalformedObjectNameException, IntrospectionException,
            ReflectionException, InstanceNotFoundException, IOException, OpenDataException  {
        Map config = YmlReader.readFromFileAsMap(new File(this.getClass().getResource("/conf/config_with_composite_and_noncomposite_objects.yml").getFile()));
        List<Map> mBeans = (List) config.get("mbeans");
        Set<ObjectInstance> objectInstances = Sets.newHashSet();
        objectInstances.add(new ObjectInstance("java.lang:type=Memory", "test"));

        Set<Attribute> attributes = Sets.newHashSet();
        attributes.add(new Attribute("ObjectPendingFinalizationCount", 0));
        attributes.add(new Attribute("HeapMemoryUsage", createCompositeDataSupportObject()));

        List<String> metricNames = Lists.newArrayList();
        metricNames.add("metric1");
        metricNames.add("metric2");

        when(jmxConnectionAdapter.queryMBeans(eq(jmxConnector), Mockito.any(ObjectName.class))).thenReturn(objectInstances);
        when(jmxConnectionAdapter.getReadableAttributeNames(eq(jmxConnector), Mockito.any(ObjectInstance.class))).thenReturn(metricNames);
        when(jmxConnectionAdapter.getAttributes(eq(jmxConnector), Mockito.any(ObjectName.class), Mockito.any(String[].class))).thenReturn(attributes);


        NodeMetricsProcessor nodeMetricsProcessor = new NodeMetricsProcessor(jmxConnectionAdapter, jmxConnector);

        ActiveMQMonitorTask activeMQMonitorTask= new ActiveMQMonitorTask();
        Map<String, MetricProperties> metricPropertiesMap = activeMQMonitorTask.getMapOfProperties(mBeans.get(0));
        List<Metric> metrics = nodeMetricsProcessor.getNodeMetrics(mBeans.get(0), metricPropertiesMap, "");

        Assert.assertTrue(metrics.get(0).getMetricPath().equals("Memory|ObjectPendingFinalizationCount"));
        Assert.assertTrue(metrics.get(0).getMetricName().equals("ObjectPendingFinalizationCount"));
        Assert.assertTrue(metrics.get(0).getMetricValue().equals("0"));
        Assert.assertTrue(metrics.get(1).getMetricProperties().getTimeRollUpType().equals(metricPropertiesMap.get("ObjectPendingFinalizationCount").getTimeRollUpType()));
        Assert.assertTrue(metrics.get(1).getMetricProperties().getClusterRollUpType().equals(metricPropertiesMap.get("ObjectPendingFinalizationCount").getClusterRollUpType()));
        Assert.assertTrue(metrics.get(1).getMetricProperties().getAggregationType().equals(metricPropertiesMap.get("ObjectPendingFinalizationCount").getAggregationType()));

        Assert.assertTrue(metrics.get(1).getMetricPath().equals("Memory|HeapMemoryUsage.used"));
        Assert.assertTrue(metrics.get(1).getMetricName().equals("HeapMemoryUsage.used"));
        Assert.assertTrue(metrics.get(1).getMetricValue().equals("50"));
        Assert.assertTrue(metrics.get(1).getMetricProperties().getTimeRollUpType().equals(metricPropertiesMap.get("HeapMemoryUsage.used").getTimeRollUpType()));
        Assert.assertTrue(metrics.get(1).getMetricProperties().getClusterRollUpType().equals(metricPropertiesMap.get("HeapMemoryUsage.used").getClusterRollUpType()));
        Assert.assertTrue(metrics.get(1).getMetricProperties().getAggregationType().equals(metricPropertiesMap.get("HeapMemoryUsage.used").getAggregationType()));
    }

    private CompositeDataSupport createCompositeDataSupportObject () throws OpenDataException {
        String typeName = "type";
        String description = "description";
        String[] itemNames = {"max", "used"};
        String[] itemDescriptions = {"maxDesc", "usedDesc"};
        OpenType<?>[] itemTypes = new OpenType[]{new OpenType("java.lang.String", "type", "description") {
            @Override
            public boolean isValue (Object obj) {
                return true;
            }

            @Override
            public boolean equals (Object obj) {
                return false;
            }

            @Override
            public int hashCode () {
                return 0;
            }

            @Override
            public String toString () {
                return "100";
            }
        }, new OpenType("java.lang.String", "type", "description") {
            @Override
            public boolean isValue (Object obj) {
                return true;
            }

            @Override
            public boolean equals (Object obj) {
                return false;
            }

            @Override
            public int hashCode () {
                return 0;
            }

            @Override
            public String toString () {
                return "50";
            }
        }};

        CompositeType compositeType = new CompositeType(typeName, description, itemNames, itemDescriptions, itemTypes);

        String[] itemNamesForCompositeDataSupport = {"max", "used"};
        Object[] itemValuesForCompositeDataSupport = {new BigDecimal(100), new BigDecimal(50)};
        return new CompositeDataSupport(compositeType, itemNamesForCompositeDataSupport,
                itemValuesForCompositeDataSupport);
    }


}

