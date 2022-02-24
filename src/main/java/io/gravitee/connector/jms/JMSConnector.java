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
package io.gravitee.connector.jms;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.connector.api.AbstractConnector;
import io.gravitee.connector.api.Connection;
import io.gravitee.connector.api.response.StatusResponse;
import io.gravitee.connector.jms.connection.*;
import io.gravitee.connector.jms.connection.websocket.WebsocketConnection;
import io.gravitee.connector.jms.endpoint.JMSEndpoint;
import io.gravitee.connector.jms.model.DestinationType;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.ws.WebSocketProxyRequest;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JMSConnector extends AbstractConnector<Connection, ProxyRequest> {

  private final Logger logger = LoggerFactory.getLogger(JMSConnector.class);

  private final JMSEndpoint endpoint;
  private final io.gravitee.connector.jms.model.Destination destination;

  public JMSConnector(final JMSEndpoint endpoint) {
    this.endpoint = endpoint;
    this.destination = endpoint.toJMSOptions().getDestination();
  }

  @Override
  public void request(
    ExecutionContext context,
    ProxyRequest request,
    Handler<Connection> connectionHandler
  ) {
    request.metrics().setEndpoint(endpoint.target());

    // Check the flow mode
    //  - Standard HTTP request
    //  - Streaming request (ie. websocket)
    if (isWebSocket(request)) {
      handleWebsocketRequest(context, request, connectionHandler);
    } else {
      handleRequest(context, request, connectionHandler);
    }
  }

  private void handleWebsocketRequest(
    ExecutionContext context,
    ProxyRequest request,
    Handler<Connection> connectionHandler
  ) {
    WebSocketProxyRequest wsRequest = (WebSocketProxyRequest) request;

    wsRequest
      .upgrade()
      .thenAccept(
        webSocketProxyRequest -> {
          createContext()
            .compose(this::createConsumer)
            .onSuccess(
              consumer ->
                connectionHandler.handle(
                  new WebsocketConnection(consumer, webSocketProxyRequest)
                )
            )
            .onFailure(
              new io.vertx.core.Handler<Throwable>() {
                @Override
                public void handle(Throwable t) {
                  logger.error(
                    "Unexpected error while trying to connect to the messaging system",
                    t
                  );
                  webSocketProxyRequest.reject(
                    HttpStatusCode.INTERNAL_SERVER_ERROR_500
                  );
                }
              }
            );
        }
      );
  }

  private void handleRequest(
    ExecutionContext context,
    ProxyRequest request,
    Handler<Connection> connectionHandler
  ) {
    Future<JMSContext> jmsContext = createContext();

    jmsContext.onFailure(
      new io.vertx.core.Handler<Throwable>() {
        @Override
        public void handle(Throwable t) {
          logger.error(
            "Unexpected error while trying to connect to the messaging system",
            t
          );
          DirectResponseConnection connection = new DirectResponseConnection();
          connectionHandler.handle(connection);
          connection.sendResponse(
            new StatusResponse(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
          );
        }
      }
    );

    if (
      request.method() == HttpMethod.POST || request.method() == HttpMethod.PUT
    ) {
      jmsContext
        .compose(JMSContext::createProducer)
        .onSuccess(
          producer ->
            connectionHandler.handle(
              new SendMessageConnection(
                context,
                producer,
                createDestination(producer.getContext())
              )
            )
        );
    } else if (request.method() == HttpMethod.GET) {
      jmsContext
        .compose(
          jmsContext1 ->
            jmsContext1.createConsumer(createDestination(jmsContext1))
        )
        .onSuccess(
          consumer ->
            connectionHandler.handle(
              new ReadMessageConnection(consumer, request)
            )
        );
    } else {
      DirectResponseConnection connection = new DirectResponseConnection();
      connectionHandler.handle(connection);
      connection.sendResponse(
        new StatusResponse(HttpStatusCode.BAD_REQUEST_400)
      );
    }
  }

  private Destination createDestination(JMSContext context) {
    if (destination.getType() == DestinationType.QUEUE) {
      return createQueue(context);
    } else {
      return createTopic(context);
    }
  }

  private Queue createQueue(JMSContext context) {
    return context.createQueue(destination.getName());
  }

  private Topic createTopic(JMSContext context) {
    return context.createTopic(destination.getName());
  }

  private Future<JMSConsumer> createQueueConsumer(JMSContext context) {
    Queue queue = context.createQueue(destination.getName());
    return context.createConsumer(queue);
  }

  private Future<JMSContext> createContext() {
    return ConnectionFactoryProvider
      .getProvider(destination)
      .provide(Vertx.currentContext().owner(), endpoint.toJMSOptions())
      .compose(ConnectionFactory::createContext);
  }

  private Future<JMSConsumer> createTopicConsumer(JMSContext context) {
    Topic topic = context.createTopic(destination.getName());
    return context.createConsumer(topic);
  }

  private Future<JMSConsumer> createConsumer(JMSContext context) {
    if (destination.getType() == DestinationType.QUEUE) {
      return createQueueConsumer(context);
    } else {
      return createTopicConsumer(context);
    }
  }

  private boolean isWebSocket(ProxyRequest request) {
    String connectionHeader = request.headers().get(HttpHeaders.CONNECTION);
    String upgradeHeader = request.headers().get(HttpHeaders.UPGRADE);

    return (
      request.method() == HttpMethod.GET &&
      HttpHeaderValues.UPGRADE.contentEqualsIgnoreCase(connectionHeader) &&
      HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgradeHeader)
    );
  }
}
