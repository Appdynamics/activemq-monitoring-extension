# AppDynamics ActiveMQ Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case

ActiveMQ is an open source, JMS 1.1 compliant, message-oriented middleware (MOM) from the Apache Software Foundation that provides high-availability, performance, scalability, reliability and security for enterprise messaging. 
The ActiveMQ Monitoring extension collects metrics from an ActiveMQ messaging server and uploads them to the AppDynamics Metric Browser. 

## Prerequisites ##

JMX must be enabled in ActiveMQ Messaging server for this extension to gather metrics. To enable, please see [these instructions](http://activemq.apache.org/jmx.html).

## Troubleshooting steps ##
Before configuring the extension, please make sure to run the below steps to check if the set up is correct.

1. Telnet into your activemq server from the box where the extension is deployed.
       telnet <hostname> <port>

       <port> - It is the jmxremote.port specified.
        <hostname> - IP address

    If telnet works, it confirm the access to the activemq server.


2. Start jconsole. Jconsole comes as a utitlity with installed jdk. After giving the correct host and port , check if activemq
mbean shows up.


##Installation

1. Run 'mvn clean install' from the activemq-monitoring-extension directory and find the ActiveMQMonitor.zip in the "target" folder.
2. Unzip as "ActiveMQMonitor" and copy the "ActiveMQMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`


## Configuration ##

Note : Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the ActiveMQ instances by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/ActiveMQMonitor/`.
2. Configure the yaml file

   For eg.
   ```
        # List of ActiveMQ servers
        servers:
                 - host: "192.168.57.102"
                   port: 1616
                   username: ""
                   password: ""
                   displayName: "localhost"
                   metricOverrides:
                     - metricKey: ".*"
                       disabled: true

                     - metricKey: ".*Time"
                       disabled: false
                       postfix: "inSec"
                       multiplier: 0.000001



               # number of concurrent tasks
               numberOfThreads: 10

               #timeout for the thread
               threadTimeout: 30

               #prefix used to show up metrics in AppDynamics
               metricPrefix:  "Custom Metrics|ActiveMQ|"

               metricOverrides:
                    - metricKey: ".*"
                      disabled: true

                    - metricKey: ".*Time"
                      disabled: false
                      postfix: "inSec"
                      multiplier: 0.000001

   ```

3. MetricOverrides can be given at each server level or at the global level. MetricOverrides given at the global level will
   take precedence over server level.

   The following transformations can be done using the MetricOverrides

   a. metricKey: The identifier to identify a metric or group of metrics. Metric Key supports regex.
   b. metricPrefix: Text to be prepended before the raw metricPath. It gets appended after the displayName.
         Eg. Custom Metrics|activemq|<displayNameForServer>|<metricPrefix>|<metricName>|<metricPostfix>

   c. metricPostfix: Text to be appended to the raw metricPath.
         Eg. Custom Metrics|activemq|<displayNameForServer>|<metricPrefix>|<metricName>|<metricPostfix>

   d. multiplier: An integer or decimal to transform the metric value.

   e. timeRollup, clusterRollup, aggregator: These are AppDynamics specific fields. More info about them can be found
        https://docs.appdynamics.com/display/PRO41/Build+a+Monitoring+Extension+Using+Java

   f. disabled: This boolean value can be used to turn off reporting of metrics.

   #Please note that if more than one regex specified in metricKey satisfies a given metric, the metricOverride specified later will win.


4. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/ActiveMQMonitor/` directory. Below is the sample

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


