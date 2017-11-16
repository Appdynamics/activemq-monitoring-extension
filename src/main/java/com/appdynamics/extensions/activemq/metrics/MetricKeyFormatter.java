package com.appdynamics.extensions.activemq.metrics;


import com.appdynamics.extensions.activemq.ActiveMQMBeansKeyPropertiesEnum;
import com.google.common.base.Strings;

import javax.management.ObjectInstance;
import javax.management.ObjectName;
import static com.appdynamics.extensions.activemq.Constants.METRICS_SEPARATOR;
class MetricKeyFormatter {

    private ObjectName getObjectName (ObjectInstance instance) {
        return instance.getObjectName();
    }

    String getInstanceKey (ObjectInstance instance) {
        if (instance == null) {
            return "";
        }
        // Standard jmx keys. {type, scope, name, keyspace, path etc.}
        String type = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.TYPE.toString());
        String brokerName = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.BROKERNAME.toString());
        String domain = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.DOMAIN.toString());
        String subType = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.SUBTYPE.toString());
        String name = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.NAME.toString());
        String service = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.SERVICE.toString());
        String scope = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.SCOPE.toString());
        String cache = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.CACHE.toString());
        String path = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.PATH.toString());
        String keyspace = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.KEYSPACE.toString());
        String destination = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.DESTINATION.toString());
        String destinationName = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.DESTINATIONNAME.toString());
        String destinationType = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.DESTINATIONTYPE.toString());
        String nodeID = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.NODEID.toString());

        StringBuilder metricsKey = new StringBuilder();
        metricsKey.append(Strings.isNullOrEmpty(type) ? "" : type + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(domain) ? "" : domain + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(brokerName) ? "" : brokerName + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(subType) ? "" : subType + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(service) ? "" : service + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(destinationType) ? "" : destinationType + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(destinationName) ? "" : destinationName + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(destination) ? "" : destination + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(path) ? "" : path + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(scope) ? "" : scope + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(keyspace) ? "" : keyspace + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(name) ? "" : name + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(cache) ? "" : cache + METRICS_SEPARATOR);

//        String metricGetInstanceKey = getInstanceKey1(instance);
        return metricsKey.toString();
    }

    String getInstanceKey1(ObjectInstance instance){
        if (instance == null){
            return "";
        }
        String brokerName, type, destinationName, destinationType = "";

        if(getKeyProperty(instance, "type") == null){
             brokerName = getKeyProperty(instance, "BrokerName");
             type = getKeyProperty(instance, "type");
             destinationName = getKeyProperty(instance, "Destination");
        }
        else
        {
             brokerName = getKeyProperty(instance, "brokerName");
             type = getKeyProperty(instance, "type");
             destinationType = getKeyProperty(instance, "destinationType");
             destinationName = getKeyProperty(instance, "destinationName");

        }

        StringBuilder metricsKey = new StringBuilder();
        metricsKey.append(Strings.isNullOrEmpty(type) ? "" : type + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(brokerName) ? "" : brokerName + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(destinationType) ? "" : destinationType + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(destinationName) ? "" : destinationName + METRICS_SEPARATOR);

        return metricsKey.toString();
    }





    private String getKeyProperty (ObjectInstance instance, String property) {
        if (instance == null) {
            return "";
        }
        return getObjectName(instance).getKeyProperty(property);
    }

    String getNodeKey (ObjectInstance instance, String metricName, String instanceKey) {
        StringBuilder metricKey = new StringBuilder(instanceKey);
        String tier = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.TIER.toString());
        String responsibility = getKeyProperty(instance, ActiveMQMBeansKeyPropertiesEnum.RESPONSIBILITY.toString());
        metricKey.append(Strings.isNullOrEmpty(tier) ? "" : tier + "|");
        metricKey.append(Strings.isNullOrEmpty(responsibility) ? "" : responsibility + "|");
        metricKey.append(metricName);
        return metricKey.toString();
    }
}
