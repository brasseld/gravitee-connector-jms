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

import io.gravitee.connector.jms.connection.provider.mq.MQConnectionFactoryProvider;
import io.gravitee.connector.jms.connection.provider.standard.StandardConnectionFactoryProvider;
import io.gravitee.connector.jms.model.Destination;
import io.gravitee.connector.jms.model.JMSOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ConnectionFactoryProvider {
  ConnectionFactoryProvider DEFAULT_PROVIDER = new StandardConnectionFactoryProvider();

  List<ConnectionFactoryProvider> PROVIDERS = List.of(
    new MQConnectionFactoryProvider(),
    DEFAULT_PROVIDER
  );

  Set<String> supportedProtocols();

  Future<ConnectionFactory> provide(Vertx vertx, JMSOptions options);

  static ConnectionFactoryProvider getProvider(Destination destination) {
    for (ConnectionFactoryProvider provider : PROVIDERS) {
      if (provider.supportedProtocols().contains(destination.getProtocol())) {
        return provider;
      }
    }

    // Fallback to the default provider
    return DEFAULT_PROVIDER;
  }
}
