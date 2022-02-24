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

import io.gravitee.connector.jms.connection.JMSContext;
import io.gravitee.connector.jms.connection.JMSProducer;
import io.gravitee.connector.jms.model.ProducerOptions;
import io.vertx.core.Context;
import io.vertx.core.Future;
import javax.jms.Destination;
import javax.jms.Message;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JMSProducerImpl implements JMSProducer {

  private final Context context;
  private final JMSContext jmsContext;
  private final javax.jms.JMSProducer producer;

  public JMSProducerImpl(
    final Context context,
    final ProducerOptions options,
    final JMSContext jmsContext,
    final javax.jms.JMSProducer producer
  ) {
    this.context = context;
    this.jmsContext = jmsContext;
    this.producer = producer;
    this.producer.setJMSCorrelationID(options.getCorrelationID());
    this.producer.setPriority(options.getPriority());
    this.producer.setDisableMessageTimestamp(
        options.isDisableMessageTimestamp()
      );
    this.producer.setDisableMessageID(options.isDisableMessageID());
    this.producer.setJMSType(options.getMessageType());
    this.producer.setDeliveryDelay(options.getDeliveryDelay());
    this.producer.setDeliveryMode(options.getDeliveryMode().value());
    this.producer.setTimeToLive(options.getTimeToLive());
  }

  @Override
  public JMSContext getContext() {
    return jmsContext;
  }

  @Override
  public Future<Void> send(Destination destination, Message message) {
    return context.executeBlocking(
      event -> {
        try {
          producer.send(destination, message);
          event.complete();
        } catch (Exception ex) {
          event.fail(ex);
        }
      }
    );
  }
}
