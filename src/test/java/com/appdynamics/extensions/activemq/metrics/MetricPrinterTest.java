package com.appdynamics.extensions.activemq.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;

public class MetricPrinterTest {

    private MetricWriteHelper metricWriter = mock(MetricWriteHelper.class);

    private String metricPrefix = "Server|ActiveMQ";
    private String displayName = "ActiveMQInstance";

    private Metric generateMetric () {
        Metric nodeMetric = new Metric();
        nodeMetric.setMetricKey("metricKey");
        nodeMetric.setInstanceKey("instanceKey");
        nodeMetric.setMetricValue(new BigDecimal(100));

        MetricProperties metricProperties = new MetricProperties();
        metricProperties.setMetricName("metricName");
        metricProperties.setAggregation(false);

        nodeMetric.setProperties(metricProperties);
        return nodeMetric;
    }

    @Test
    public void reportNodeMetrics_NoMetricsFound () {
        MetricPrinter metricPrinter = new MetricPrinter(metricPrefix, displayName, metricWriter);
        List<Metric> componentMetrics = Lists.newArrayList();
        metricPrinter.reportNodeMetrics(componentMetrics);
        Assert.assertTrue(metricPrinter.getTotalMetricsReported() == 0);
    }

    @Test
    public void reportNodeMetrics_MetricsPresent () {
        doNothing().when(metricWriter).printMetric(anyString(), anyString(), anyString(), anyString(), anyString());
        MetricPrinter metricPrinter = new MetricPrinter(metricPrefix, displayName, metricWriter);
        List<Metric> componentMetrics = Lists.newArrayList();
        componentMetrics.add(generateMetric());
        metricPrinter.reportNodeMetrics(componentMetrics);
        Assert.assertTrue(metricPrinter.getTotalMetricsReported() == 1);
    }
}
