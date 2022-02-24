/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.connector.jms;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import io.gravitee.connector.jms.endpoint.JMSEndpoint;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

@ExtendWith(MockitoExtension.class)
public class IBMMqConnectorTest extends AbstractJMSConnectorTest {

  private static final String MQ_QMGR_NAME = "QM1";
  private static final String CHANNEL = "DEV.APP.SVRCONN";
  private static final String APP_USER = "app";
  private static final String APP_PASSWORD = "passw0rd";
  private static final String QUEUE_NAME = "DEV.QUEUE.1";

  private static final GenericContainer<?> container = new GenericContainer<>(
    DockerImageName.parse("ibmcom/mq:latest")
  )
    .withEnv("LICENSE", "accept")
    .withEnv("MQ_QMGR_NAME", MQ_QMGR_NAME)
    .withEnv("MQ_APP_PASSWORD", APP_PASSWORD)
    .withExposedPorts(1414, 9443);

  @BeforeAll
  public static void startContainers() throws Exception {
    Startables.deepStart(container).join();
  }

  @AfterAll
  public static void stopContainers() {
    container.stop();
  }

  protected JMSEndpoint createDefaultEndpoint() {
    JMSEndpoint endpoint = new JMSEndpoint(
      "mq-endpoint",
      "mq://" +
      container.getHost() +
      ':' +
      container.getMappedPort(1414) +
      "/queue/" +
      QUEUE_NAME
    );
    endpoint.setUsername(APP_USER);
    endpoint.setPassword(container.getEnvMap().get("MQ_APP_PASSWORD"));
    endpoint.addProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
    endpoint.addProperty(
      WMQConstants.WMQ_QUEUE_MANAGER,
      container.getEnvMap().get("MQ_QMGR_NAME")
    );

    return endpoint;
  }

  @Override
  protected ConnectionFactory createConnectionFactory() throws JMSException {
    JmsFactoryFactory ff;
    JmsConnectionFactory cf;

    ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
    cf = ff.createConnectionFactory();
    cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, container.getHost());
    cf.setIntProperty(WMQConstants.WMQ_PORT, container.getMappedPort(1414));
    cf.setStringProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
    cf.setIntProperty(
      WMQConstants.WMQ_CONNECTION_MODE,
      WMQConstants.WMQ_CM_CLIENT
    );
    cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, MQ_QMGR_NAME);
    cf.setStringProperty(
      WMQConstants.WMQ_APPLICATIONNAME,
      "JMS Connector (test)"
    );
    cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
    cf.setStringProperty(WMQConstants.USERID, APP_USER);
    cf.setStringProperty(WMQConstants.PASSWORD, APP_PASSWORD);

    return cf;
  }

  @Override
  protected String getQueueName() {
    return QUEUE_NAME;
  }
}
