package com.appdynamics.extensions.activemq.filters;

import com.appdynamics.extensions.activemq.ActiveMQUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class IncludeFilter {
    private List dictionary;

    public IncludeFilter (List dictionary) {
        this.dictionary = dictionary;
    }

    public void applyFilter (Set<String> filteredSet, List<String> allMetrics) {
        if (allMetrics == null || dictionary == null) {
            return;
        }

        for (Object obj : dictionary) {
            Map metric = (Map) obj;
            Map.Entry firstEntry = (Map.Entry) metric.entrySet().iterator().next();
            String metricName = firstEntry.getKey().toString();
            if (ActiveMQUtil.isCompositeObject(metricName)) {
                metricName = ActiveMQUtil.getMetricNameFromCompositeObject(metricName);
            }

            if (allMetrics.contains(metricName)) {
                filteredSet.add(metricName);
            }
        }
    }
}

