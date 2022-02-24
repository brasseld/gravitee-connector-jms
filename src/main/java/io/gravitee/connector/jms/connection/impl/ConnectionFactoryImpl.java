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

import io.gravitee.connector.jms.connection.ConnectionFactory;
import io.gravitee.connector.jms.connection.JMSContext;
import io.gravitee.connector.jms.model.JMSOptions;
import io.vertx.core.Context;
import io.vertx.core.Future;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConnectionFactoryImpl implements ConnectionFactory {

  private final Context context;
  private final JMSOptions options;
  private final javax.jms.ConnectionFactory connectionFactory;

  public ConnectionFactoryImpl(
    final Context context,
    final JMSOptions options,
    final javax.jms.ConnectionFactory connectionFactory
  ) {
    this.context = context;
    this.options = options;
    this.connectionFactory = connectionFactory;
  }

  @Override
  public Future<JMSContext> createContext() {
    return context.executeBlocking(
      event -> {
        try {
          javax.jms.JMSContext jmsContext = connectionFactory.createContext(
            javax.jms.JMSContext.AUTO_ACKNOWLEDGE
          );
          if (
            options.getClientID() != null && !options.getClientID().isEmpty()
          ) {
            jmsContext.setClientID(options.getClientID());
          }

          event.complete(new JMSContextImpl(this.context, options, jmsContext));
        } catch (Exception ex) {
          event.fail(ex);
        }
      }
    );
  }
}
