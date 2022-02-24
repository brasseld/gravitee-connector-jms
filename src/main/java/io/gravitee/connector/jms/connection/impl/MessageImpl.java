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
package io.gravitee.connector.jms.connection.impl;

import io.gravitee.connector.jms.connection.Message;
import io.gravitee.gateway.api.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.Enumeration;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MessageImpl implements Message {

  private final Logger logger = LoggerFactory.getLogger(Message.class);

  private final javax.jms.Message message;

  public MessageImpl(final javax.jms.Message message) {
    this.message = message;
  }

  @Override
  public Buffer getBody() {
    try {
      if (message instanceof TextMessage) {
        TextMessage textMessage = (TextMessage) message;

        return Buffer.buffer(textMessage.getText());
      } else if (message instanceof MapMessage) {
        MapMessage mapMessage = (MapMessage) message;

        JsonObject bodyContent = new JsonObject();
        Enumeration<String> mapNames = mapMessage.getMapNames();
        while (mapNames.hasMoreElements()) {
          String fieldName = mapNames.nextElement();

          bodyContent.put(fieldName, mapMessage.getObject(fieldName));
        }

        return Buffer.buffer(bodyContent.toString());
      } else {
        logger.error(
          "Unknown JMS Message type {} could not be converted to a buffer",
          message.getClass().getName()
        );
      }
    } catch (JMSException ex) {
      logger.error("Unexpected error while looking at the message payload", ex);
    }

    return null;
  }

  @Override
  public Enumeration<String> getPropertyNames() {
    try {
      return message.getPropertyNames();
    } catch (JMSException ex) {
      return Collections.emptyEnumeration();
    }
  }

  @Override
  public Object getProperty(String name) {
    try {
      return message.getObjectProperty(name);
    } catch (JMSException e) {
      return null;
    }
  }

  @Override
  public boolean isEmpty() {
    return message == null;
  }
}
