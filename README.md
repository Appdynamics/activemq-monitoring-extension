# AppDynamics ActiveMQ Monitoring Extension

This extension works only with the standalone machine agent.

[![Build Status](https://travis-ci.org/MoriTanosuke/activemq-monitoring-extension.svg)](https://travis-ci.org/MoriTanosuke/activemq-monitoring-extension)

##Use Case

ActiveMQ is an open source, JMS 1.1 compliant, message-oriented middleware (MOM) from the Apache Software Foundation that provides high-availability, performance, scalability, reliability and security for enterprise messaging. 
The ActiveMQ Monitoring extension collects metrics from an ActiveMQ messaging server and uploads them to the AppDynamics Metric Browser. 

## Prerequisites ##

JMX must be enabled in ActiveMQ Messaging server for this extension to gather metrics. To enable, please see [these instructions](http://activemq.apache.org/jmx.html).

##Installation

1. Run 'mvn clean install' from the activemq-monitoring-extension directory and find the ActiveMQMonitor.zip in the "target" folder.
2. Unzip as "ActiveMQMonitor" and copy the "ActiveMQMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`

## Configuration ##

Note : Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the ActiveMQ instances by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/ActiveMQMonitor/`.
2. Configure the MBeans in the config.yml. By default, "org.apache.activemq" is configured.
   You can also add excludePatterns (regex) to exclude any queues, topics or metrics from showing up in the AppDynamics controller.

   For eg.
   ```
        # List of ActiveMQ servers
        servers:
          - host: "localhost"
            port: 1099
            username: "admin"
            password: "admin"
            displayName: "localhost"


        # ActiveMQ mbeans. Exclude patterns with regex can be used to exclude any unwanted queues, topics or metrics.
        mbeans:
          - domainName: "org.apache.activemq"
            excludePatterns: [
              localhost|Broker|.*,
              .*TEST.FOO.*,
              .*AverageEnqueueTime$
              ]

        # number of concurrent tasks
        numberOfThreads: 10

        #timeout for the thread
        threadTimeout: 30

        #prefix used to show up metrics in AppDynamics
        metricPrefix:  "Custom Metrics|ActiveMQ|"

   ```

3. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/ActiveMQMonitor/` directory. Below is the sample

     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/ActiveMQMonitor/config.yml" />
          ....
     </task-arguments>
    ```

## Metrics

The following are the metrics reported to the controller
* MemoryLimit, MemoryPercentUsage, StoreLimit, StorePercentUsage, TempLimit, TempPercentUsage, TotalConsumerCount, TotalDequeCount, TotalEnqueueCount, TotalMessageCount, TotalProducerCount
* Queue/Topic Metrics: AverageEnqueueTime, BlockedProducerWarningInterval, ConsumerCount, CursorMemoryUsage, CursorPercentUsage, DequeueCount, DispatchCount, EnqueueCount, ExpiredCount, InflightCount, MaxAuditDepth, MaxEnqueueTime, MaxPageSize, MaxProducersToAudit, MemoryLimit, MemoryPercentUsage, MemoryUsagePortion, MinEnqueueTime, ProducerCount, QueueSize

In addition to the above metrics, we also add a metric called "Metrics Collection Successful" with a value 0 when an error occurs and 1 when the metrics collection is successful.

Note : By default, a Machine agent or a AppServer agent can send a fixed number of metrics to the controller. To change this limit, please follow the instructions mentioned [here](http://docs.appdynamics.com/display/PRO14S/Metrics+Limits).
For eg.  
```    
    java -Dappdynamics.agent.maxMetrics=2500 -jar machineagent.jar
```

## Custom Dashboard
![](https://raw.github.com/Appdynamics/activemq-monitoring-extension/master/ActiveMQDashboard.png)

##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/eXchange/ActiveMQ-Monitoring-Extension/idi-p/5717) community.

##Support

For any questions or feature request, please contact [AppDynamics Support](mailto:help@appdynamics.com).


