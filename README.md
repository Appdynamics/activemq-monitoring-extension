# AppDynamics ActiveMQ Monitoring Extension

This extension works only with the Java agent.

##Use Case

ActiveMQ is an open source, JMS 1.1 compliant, message-oriented middleware (MOM) from the Apache Software Foundation that provides high-availability, performance, scalability, reliability and security for enterprise messaging. 
The ActiveMQ Monitoring extension collects metrics from an ActiveMQ messaging server and uploads them to the AppDynamics Metric Browser. 


##Installation

JMX Metrics must be enabled in ActiveMQ Messaging server. To enable, please refer [here](http://activemq.apache.org/jmx.html)

1. Run 'ant package' from the active-mq-monitoring-extension directory
2. Download the file ActiveMQMonitor.zip located in the 'dist' directory into \<machineagent install dir\>/monitors/
3. Unzip the downloaded file
4. In \<machineagent install dir\>/monitors/ActiveMQMonitor/, open monitor.xml and configure the ActiveMQ parameters.
     <pre>
     &lt;argument name="host" is-required="true" default-value="" /&gt;
     &lt;argument name="port" is-required="true" default-value="" /&gt;
     &lt;argument name="username" is-required="true" default-value="" /&gt;
     &lt;argument name="password" is-required="true" default-value="" /&gt;
     </pre>
     The queues and topics you want to exclude, specify their full names as a comma separated values
     <pre>
     &lt;argument name="exclude-queues" is-required="false" default-value=""/&gt;
     &lt;argument name="exclude-topics" is-required="false" default-value=""/&gt;
     </pre>
     The configuration file which lists out the metrics to be excluded from monitoring on controller
     <pre>
     &lt;argument name="exclude-metrics-path" is-required="false" default-value="monitors/ActiveMQMonitor/conf/metrics.xml" /&gt;
</pre>
5. Restart the Machine Agent. 
 
In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | ActiveMQ

##Directory Structure

<table><tbody>
<tr>
<th align="left"> File/Folder </th>
<th align="left"> Description </th>
</tr>
<tr>
<td class='confluenceTd'> conf </td>
<td class='confluenceTd'> Contains the monitor.xml, metrics.xml </td>
</tr>
<tr>
<td class='confluenceTd'> lib </td>
<td class='confluenceTd'> Contains third-party project references </td>
</tr>
<tr>
<td class='confluenceTd'> src </td>
<td class='confluenceTd'> Contains source code to the ActiveMQ Monitoring Extension </td>
</tr>
<tr>
<td class='confluenceTd'> dist </td>
<td class='confluenceTd'> Only obtained when using ant. Run 'ant build' to get binaries. Run 'ant package' to get the distributable .zip file </td>
</tr>
<tr>
<td class='confluenceTd'> build.xml </td>
<td class='confluenceTd'> Ant build script to package the project (required only if changing Java code) </td>
</tr>
</tbody>
</table>

## Metrics

NOTE: By default, only some of the metrics are reported at broker, queue and topic level. This can be changed in the conf\metrics.xml file.

| Metric Name | Description |
|----------------|-------------|
|Enqueue Count				| messages sent to the queue since the last restart|
|Dequeue Count				| messages removed from the queue (ack'd by consumer) since last restart|
|Inflight Count			| messages sent to a consumer session and have not received an ack|
|Dispatch Count			| messages sent to consumer sessions (Dequeue + Inflight)|
|Queue size				| total number of messages in the queue|
|Expired Count				| messages that were not delivered because they were expired|

## Custom Dashboard
![]()

##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere] community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).


