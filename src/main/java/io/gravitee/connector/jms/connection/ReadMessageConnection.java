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
import io.gravitee.connector.api.response.AbstractResponse;
import io.gravitee.connector.api.response.StatusResponse;
import io.gravitee.connector.jms.connection.message.BufferResponse;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.stream.WriteStream;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReadMessageConnection extends AbstractConnection {

  private final Logger logger = LoggerFactory.getLogger(
    ReadMessageConnection.class
  );

  private static final String QUERY_PARAMETER_TIMEOUT = "timeout";
  private static final long DEFAULT_TIMEOUT = -1L;

  private final JMSConsumer jmsConsumer;
  private final ProxyRequest request;

  public ReadMessageConnection(JMSConsumer jmsConsumer, ProxyRequest request) {
    this.jmsConsumer = jmsConsumer;
    this.request = request;
  }

  @Override
  public WriteStream<Buffer> write(Buffer content) {
    return this;
  }

  @Override
  public void end() {
    String sTimeout = request.parameters().getFirst(QUERY_PARAMETER_TIMEOUT);
    long timeout = DEFAULT_TIMEOUT;

    if (sTimeout != null) {
      try {
        timeout = Long.parseLong(sTimeout);
      } catch (NumberFormatException nfe) {}
    }

    Future<Message> messageFuture = jmsConsumer.receive(timeout);
    messageFuture
      .onSuccess(this::forward)
      .onFailure(
        t -> {
          responseHandler.handle(
            new StatusResponse(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
          );
          logger.error(
            "Unexpected error while reading message from {}",
            jmsConsumer,
            t
          );
          jmsConsumer.close();
        }
      );
  }

  private void forward(Message message) {
    AbstractResponse response;

    if (!message.isEmpty()) {
      Buffer body = message.getBody();
      response = new BufferResponse(body);

      message
        .getPropertyNames()
        .asIterator()
        .forEachRemaining(
          s -> {
            Object property = message.getProperty(s);
            if (property != null) {
              response.headers().set(s, property.toString());
            }
          }
        );

      responseHandler.handle(response);
      response.bodyHandler().handle(body);
      response.endHandler().handle(null);
    } else {
      responseHandler.handle(new StatusResponse(HttpStatusCode.NOT_FOUND_404));
    }
  }
}
