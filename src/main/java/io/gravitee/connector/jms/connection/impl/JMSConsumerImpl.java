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
import io.gravitee.connector.jms.connection.Message;
import io.gravitee.connector.jms.model.ConsumerOptions;
import io.gravitee.gateway.api.handler.Handler;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import javax.jms.JMSRuntimeException;
import javax.jms.MessageListener;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JMSConsumerImpl implements JMSConsumer, MessageListener {

  private final Context context;
  private final ConsumerOptions options;
  private final javax.jms.JMSConsumer consumer;
  private Handler<Message> messageHandler;

  public JMSConsumerImpl(
    final Context context,
    final ConsumerOptions options,
    final javax.jms.JMSConsumer consumer
  ) {
    this.context = context;
    this.options = options;
    this.consumer = consumer;
  }

  @Override
  public Future<Message> receive(long timeout) {
    return context.executeBlocking(
      event -> {
        try {
          javax.jms.Message message;
          if (timeout < 0) {
            message = consumer.receiveNoWait();
          } else if (timeout == 0) {
            message = consumer.receive();
          } else {
            message = consumer.receive(timeout);
          }
          event.complete(new MessageImpl(message));
        } catch (JMSRuntimeException ex) {
          event.fail(ex);
        }
      }
    );
  }

  @Override
  public JMSConsumer subscribe(Handler<Message> messageHandler) {
    this.messageHandler = messageHandler;
    consumer.setMessageListener(this);
    return this;
  }

  @Override
  public Future<Void> close() {
    return context.executeBlocking(
      new io.vertx.core.Handler<Promise<Void>>() {
        @Override
        public void handle(Promise<Void> event) {
          try {
            consumer.close();
            event.complete();
          } catch (JMSRuntimeException ex) {
            event.fail(ex);
          }
        }
      }
    );
  }

  @Override
  public JMSConsumer close(Handler<Void> closeHandler) {
    close().onComplete(event -> closeHandler.handle(event.result()));

    return this;
  }

  @Override
  public void onMessage(javax.jms.Message message) {
    context.runOnContext(
      event -> messageHandler.handle(new MessageImpl(message))
    );
  }
}
