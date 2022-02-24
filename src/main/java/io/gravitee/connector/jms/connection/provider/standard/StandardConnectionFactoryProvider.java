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
package io.gravitee.connector.jms.connection.provider.standard;

import io.gravitee.connector.jms.connection.ConnectionFactory;
import io.gravitee.connector.jms.connection.ConnectionFactoryProvider;
import io.gravitee.connector.jms.model.JMSOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StandardConnectionFactoryProvider
  implements ConnectionFactoryProvider {

  private static final Set<String> SUPPORTED_PROTOCOLS = Set.of("jms");

  @Override
  public Set<String> supportedProtocols() {
    return SUPPORTED_PROTOCOLS;
  }

  @Override
  public Future<ConnectionFactory> provide(Vertx vertx, JMSOptions options) {
    //TODO
    return null;
  }
}
