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
package io.gravitee.connector.jms.connection.provider.mq;

import io.gravitee.common.util.MultiValueMap;
import io.gravitee.connector.jms.connection.ConnectionFactory;
import io.gravitee.connector.jms.connection.ConnectionFactoryProvider;
import io.gravitee.connector.jms.connection.impl.ConnectionFactoryImpl;
import io.gravitee.connector.jms.model.Destination;
import io.gravitee.connector.jms.model.JMSOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MQConnectionFactoryProvider implements ConnectionFactoryProvider {

  private final Logger logger = LoggerFactory.getLogger(
    MQConnectionFactoryProvider.class
  );

  private static final Set<String> SUPPORTED_PROTOCOLS = Set.of("mq");

  @Override
  public Set<String> supportedProtocols() {
    return SUPPORTED_PROTOCOLS;
  }

  @Override
  public Future<ConnectionFactory> provide(Vertx vertx, JMSOptions options) {
    return vertx.executeBlocking(
      new Handler<>() {
        @Override
        public void handle(Promise<ConnectionFactory> promise) {
          try {
            javax.jms.ConnectionFactory cf = createConnectionFactory();
            Class<?> jmsFactoryFactoryClass =
              this.getClass()
                .getClassLoader()
                .loadClass("com.ibm.msg.client.jms.JmsPropertyContext");
            Method setObjectPropertyMethod = jmsFactoryFactoryClass.getDeclaredMethod(
              "setObjectProperty",
              String.class,
              Object.class
            );

            Destination destination = options.getDestination();

            MultiValueMap<String, String> parameters = destination.getParameters();
            if (parameters.containsKey("queueManager")) {
              setObjectPropertyMethod.invoke(
                cf,
                "XMSC_WMQ_QUEUE_MANAGER",
                parameters.getFirst("queueManager")
              );
            }
            if (parameters.containsKey("channel")) {
              setObjectPropertyMethod.invoke(
                cf,
                "XMSC_WMQ_CHANNEL",
                parameters.getFirst("channel")
              );
            }

            setObjectPropertyMethod.invoke(cf, "XMSC_WMQ_CONNECTION_MODE", 1);
            setObjectPropertyMethod.invoke(
              cf,
              "XMSC_WMQ_HOST_NAME",
              destination.getHost()
            );
            setObjectPropertyMethod.invoke(
              cf,
              "XMSC_WMQ_PORT",
              destination.getPort()
            );

            if (
              options.getUsername() != null && !options.getUsername().isEmpty()
            ) {
              setObjectPropertyMethod.invoke(
                cf,
                "XMSC_USER_AUTHENTICATION_MQCSP",
                true
              );
              setObjectPropertyMethod.invoke(
                cf,
                "XMSC_USERID",
                options.getUsername()
              );
              setObjectPropertyMethod.invoke(
                cf,
                "XMSC_PASSWORD",
                options.getPassword()
              );
            } else {
              setObjectPropertyMethod.invoke(
                cf,
                "XMSC_USER_AUTHENTICATION_MQCSP",
                false
              );
            }

            if (options.getProperties() != null) {
              Set<Map.Entry<String, String>> properties = options
                .getProperties()
                .entrySet();
              for (Map.Entry<String, String> prop : properties) {
                setObjectPropertyMethod.invoke(
                  cf,
                  prop.getKey(),
                  prop.getValue()
                );
              }
            }

            promise.complete(
              new ConnectionFactoryImpl(vertx.getOrCreateContext(), options, cf)
            );
          } catch (Exception ex) {
            logger.error(
              "An error occurs while trying to create connection to IBM MQ",
              ex
            );
            promise.fail(ex);
          }
        }
      }
    );
  }

  private javax.jms.ConnectionFactory createConnectionFactory() {
    try {
      Class<?> jmsFactoryFactoryClass =
        this.getClass()
          .getClassLoader()
          .loadClass("com.ibm.msg.client.jms.JmsFactoryFactory");
      Method getInstanceMethod = jmsFactoryFactoryClass.getDeclaredMethod(
        "getInstance",
        String.class
      );

      Object jmsFactoryFactoryObj = getInstanceMethod.invoke(
        null,
        "com.ibm.msg.client.wmq"
      );
      Method createConnectionFactoryMethod = jmsFactoryFactoryObj
        .getClass()
        .getDeclaredMethod("createConnectionFactory");
      return (javax.jms.ConnectionFactory) createConnectionFactoryMethod.invoke(
        jmsFactoryFactoryObj
      );
    } catch (Exception ex) {
      return null;
    }
  }
}
