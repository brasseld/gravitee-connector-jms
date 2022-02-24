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
package io.gravitee.connector.jms.connection;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.connector.api.AbstractConnection;
import io.gravitee.connector.api.response.StatusResponse;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.WriteStream;
import io.vertx.core.Handler;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SendMessageConnection extends AbstractConnection {

  public static final String EXECUTION_CONTEXT_JMS_ATTRIBUTE =
    ExecutionContext.ATTR_PREFIX + "jms.";

  private final Logger logger = LoggerFactory.getLogger(
    SendMessageConnection.class
  );

  private final Buffer buffer = Buffer.buffer();

  private final JMSProducer jmsProducer;
  private final Destination destination;
  private final ExecutionContext context;

  public SendMessageConnection(
    ExecutionContext context,
    JMSProducer jmsProducer,
    Destination destination
  ) {
    this.context = context;
    this.jmsProducer = jmsProducer;
    this.destination = destination;
  }

  @Override
  public WriteStream<Buffer> write(Buffer content) {
    buffer.appendBuffer(content);
    return this;
  }

  @Override
  public void end() {
    Message message = jmsProducer.getContext().createMessage(buffer);
    applyProperties(message);

    jmsProducer
      .send(destination, message)
      .onSuccess(
        event ->
          responseHandler.handle(new StatusResponse(HttpStatusCode.CREATED_201))
      )
      .onFailure(
        new Handler<Throwable>() {
          @Override
          public void handle(Throwable t) {
            responseHandler.handle(
              new StatusResponse(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
            );
            logger.error(
              "Unexpected error while sending message to {}",
              destination,
              t
            );
          }
        }
      )
      .compose(unused -> jmsProducer.getContext().close());
  }

  private void applyProperties(Message message) {
    // Extract attributes from execution context's attributes
    Map<String, Object> attributes = context.getAttributes();

    // Filters only the one which concerns the connector
    attributes.forEach(
      new BiConsumer<String, Object>() {
        @Override
        public void accept(String key, Object value) {
          if (key.startsWith(EXECUTION_CONTEXT_JMS_ATTRIBUTE)) {
            final String name = key.substring(
              EXECUTION_CONTEXT_JMS_ATTRIBUTE.length()
            );
            setProperty(message, name, value);
          }
        }
      }
    );
  }

  private void setProperty(Message message, String name, Object value) {
    try {
      if (value instanceof String) {
        message.setStringProperty(name, (String) value);
      } else if (value instanceof Integer) {
        message.setIntProperty(name, (int) value);
      } else if (value instanceof Double) {
        message.setDoubleProperty(name, (double) value);
      } else if (value instanceof Short) {
        message.setShortProperty(name, (short) value);
      } else if (value instanceof Long) {
        message.setLongProperty(name, (long) value);
      } else if (value instanceof Boolean) {
        message.setBooleanProperty(name, (boolean) value);
      } else if (value instanceof Byte) {
        message.setByteProperty(name, (byte) value);
      } else if (value instanceof Float) {
        message.setFloatProperty(name, (float) value);
      } else {
        message.setObjectProperty(name, value);
      }
    } catch (JMSException ex) {
      logger.error(
        "Unable to set property {} to message {}",
        name,
        message,
        ex
      );
    }
  }
}
