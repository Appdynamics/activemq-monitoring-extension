# AppDynamics ActiveMQ Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case

ActiveMQ is an open source, JMS 1.1 compliant, message-oriented middleware (MOM) from the Apache Software Foundation that provides high-availability, performance, scalability, reliability and security for enterprise messaging. 
The ActiveMQ Monitoring extension collects metrics from an ActiveMQ messaging server and uploads them to the AppDynamics Metric Browser. 


##Installation

JMX Metrics must be enabled in ActiveMQ Messaging server. To enable, please see [these instructions](http://activemq.apache.org/jmx.html).

1. Run 'mvn clean install' from the active-mq-monitoring-extension directory
2. Download the file ActiveMQMonitor.zip located in the 'target' directory into \<machineagent install dir\>/monitors/
3. Unzip the downloaded file
4. In \<machineagent install dir\>/monitors/ActiveMQMonitor/, open monitor.xml and configure the ActiveMQ parameters.
     <pre>
     &lt;argument name="host" is-required="true" default-value="" /&gt;
     &lt;argument name="port" is-required="true" default-value="" /&gt;
     &lt;argument name="username" is-required="true" default-value="" /&gt;
     &lt;argument name="password" is-required="true" default-value="" /&gt;
     </pre>
     For queues and topics you want to exclude, specify their full names as comma-separated values.
     <pre>
     &lt;argument name="exclude-queues" is-required="false" default-value=""/&gt;
     &lt;argument name="exclude-topics" is-required="false" default-value=""/&gt;
     </pre>
     You can edit the configuration file to specify metrics to be excluded from monitoring.
     <pre>
     &lt;argument name="exclude-metrics-path" is-required="false" default-value="monitors/ActiveMQMonitor/metrics.xml" /&gt;
</pre>
5. Restart the Machine Agent. 
 
In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | ActiveMQ

## Metrics

NOTE: By default, only some of the metrics are reported at broker, queue and topic level. This can be changed in the conf/metrics.xml file.

| Metric Name | Description |
|----------------|-------------|
|Enqueue Count				| messages sent to the queue since the last restart|
|Dequeue Count				| messages removed from the queue (ack'd by consumer) since last restart|
|Inflight Count			| messages sent to a consumer session and have not received an ack|
|Dispatch Count			| messages sent to consumer sessions (Dequeue + Inflight)|
|Queue size				| total number of messages in the queue|
|Expired Count				| messages that were not delivered because they were expired|

## Custom Dashboard
![](https://raw.github.com/Appdynamics/activemq-monitoring-extension/master/ActiveMQDashboard.png)

##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/eXchange/ActiveMQ-Monitoring-Extension/idi-p/5717) community.

##Support

For any questions or feature request, please contact [AppDynamics Support](mailto:help@appdynamics.com).


