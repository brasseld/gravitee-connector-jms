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

import io.gravitee.connector.jms.connection.JMSConsumer;
import io.gravitee.connector.jms.connection.JMSContext;
import io.gravitee.connector.jms.connection.JMSProducer;
import io.gravitee.connector.jms.model.JMSOptions;
import io.gravitee.gateway.api.buffer.Buffer;
import io.vertx.core.Context;
import io.vertx.core.Future;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JMSContextImpl implements JMSContext {

  private final Context context;
  private final javax.jms.JMSContext jmsContext;
  private final JMSOptions options;

  public JMSContextImpl(
    final Context context,
    final JMSOptions options,
    final javax.jms.JMSContext jmsContext
  ) {
    this.context = context;
    this.options = options;
    this.jmsContext = jmsContext;
  }

  @Override
  public Future<JMSProducer> createProducer() {
    return context.executeBlocking(
      event -> {
        try {
          event.complete(
            new JMSProducerImpl(
              context,
              options.getProducerOptions(),
              this,
              jmsContext.createProducer()
            )
          );
        } catch (Exception ex) {
          event.fail(ex);
        }
      }
    );
  }

  @Override
  public Future<JMSConsumer> createConsumer(Destination destination) {
    return context.executeBlocking(
      event -> {
        try {
          javax.jms.JMSConsumer jmsConsumer;
          if (options.getConsumerOptions() == null) {
            jmsConsumer = jmsContext.createConsumer(destination);
          } else {
            jmsConsumer =
              jmsContext.createConsumer(
                destination,
                options.getConsumerOptions().getMessageSelector(),
                options.getConsumerOptions().isNoLocal()
              );
          }
          event.complete(
            new JMSConsumerImpl(
              context,
              options.getConsumerOptions(),
              jmsConsumer
            )
          );
        } catch (Exception ex) {
          event.fail(ex);
        }
      }
    );
  }

  @Override
  public Queue createQueue(String queueName) {
    return jmsContext.createQueue(queueName);
  }

  @Override
  public Topic createTopic(String topicName) {
    return jmsContext.createTopic(topicName);
  }

  @Override
  public Future<Void> close() {
    return context.executeBlocking(
      event -> {
        try {
          jmsContext.close();
          event.complete();
        } catch (Exception ex) {
          event.fail(ex);
        }
      }
    );
  }

  @Override
  public Message createMessage(Buffer buffer) {
    if (buffer != null) {
      return jmsContext.createTextMessage(buffer.toString());
    } else {
      return jmsContext.createTextMessage();
    }
  }
}
