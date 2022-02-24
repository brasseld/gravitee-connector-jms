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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.connector.api.ConnectorBuilder;
import io.gravitee.connector.api.Response;
import io.gravitee.connector.jms.connection.SendMessageConnection;
import io.gravitee.connector.jms.endpoint.JMSEndpoint;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.builder.ProxyRequestBuilder;
import io.gravitee.reporter.api.http.Metrics;
import io.vertx.core.Vertx;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractJMSConnectorTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  protected ConnectionFactory connectionFactory;

  @BeforeEach
  public void setUp() throws Exception {
    connectionFactory = createConnectionFactory();
  }

  @Test
  @Order(1)
  public void shouldPostMessageToQueue() throws Exception {
    String payload = "this-is-a-dummy-payload";

    HttpHeaders headers = mock(HttpHeaders.class);
    ExecutionContext context = mock(ExecutionContext.class);
    Request request = createMockRequest(context, HttpMethod.POST, headers);

    ProxyRequest proxyRequest = ProxyRequestBuilder.from(request).build();
    CountDownLatch latch = new CountDownLatch(1);

    JMSEndpoint endpoint = createDefaultEndpoint();
    JMSConnector connector = createConnector(endpoint);

    // Define specific attribute for JMSMessage
    Map<String, Object> properties = new HashMap<>();
    properties.put(
      SendMessageConnection.EXECUTION_CONTEXT_JMS_ATTRIBUTE + "myproperty",
      "property-value"
    );
    when(context.getAttributes()).thenReturn(properties);

    Vertx
      .vertx()
      .runOnContext(
        new io.vertx.core.Handler<Void>() {
          @Override
          public void handle(Void event) {
            connector.request(
              context,
              proxyRequest,
              result -> {
                result.responseHandler(
                  new Handler<Response>() {
                    @Override
                    public void handle(Response response) {
                      assertEquals(
                        HttpStatusCode.CREATED_201,
                        response.status()
                      );
                      latch.countDown();
                    }
                  }
                );

                result.write(Buffer.buffer(payload));
                result.end();
              }
            );
          }
        }
      );

    assertTrue(latch.await(10, TimeUnit.SECONDS));
  }

  @Test
  @Order(2)
  public void shouldGetMessageFromQueue() throws Exception {
    HttpHeaders headers = mock(HttpHeaders.class);
    ExecutionContext context = mock(ExecutionContext.class);
    Request request = createMockRequest(context, HttpMethod.GET, headers);

    ProxyRequest proxyRequest = ProxyRequestBuilder.from(request).build();
    CountDownLatch latch = new CountDownLatch(1);

    JMSEndpoint endpoint = createDefaultEndpoint();
    JMSConnector connector = createConnector(endpoint);

    Vertx
      .vertx()
      .runOnContext(
        new io.vertx.core.Handler<Void>() {
          @Override
          public void handle(Void event) {
            connector.request(
              context,
              proxyRequest,
              result -> {
                result.responseHandler(
                  new Handler<Response>() {
                    @Override
                    public void handle(Response response) {
                      assertEquals(HttpStatusCode.OK_200, response.status());

                      response
                        .bodyHandler(
                          new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer payload) {
                              assertEquals(
                                "this-is-a-dummy-payload",
                                payload.toString()
                              );
                              latch.countDown();
                            }
                          }
                        )
                        .endHandler(__ -> {});
                    }
                  }
                );

                result.end();
              }
            );
          }
        }
      );

    assertTrue(latch.await(10, TimeUnit.SECONDS));
  }

  @Test
  @Order(3)
  public void shouldGetMessageFromQueue_withMessageSelector() throws Exception {
    HttpHeaders headers = mock(HttpHeaders.class);
    ExecutionContext context = mock(ExecutionContext.class);
    Request request = createMockRequest(context, HttpMethod.GET, headers);

    ProxyRequest proxyRequest = ProxyRequestBuilder.from(request).build();
    CountDownLatch latch = new CountDownLatch(1);

    JMSEndpoint endpoint = createDefaultEndpoint();
    endpoint.getConsumerOptions().setMessageSelector("myproperty = 'value2'");
    JMSConnector connector = createConnector(endpoint);

    JMSContext jmsContext = connectionFactory.createContext();

    Vertx
      .vertx()
      .runOnContext(
        new io.vertx.core.Handler<Void>() {
          @Override
          public void handle(Void event) {
            connector.request(
              context,
              proxyRequest,
              result -> {
                result.responseHandler(
                  new Handler<Response>() {
                    @Override
                    public void handle(Response response) {
                      assertEquals(HttpStatusCode.OK_200, response.status());

                      response
                        .bodyHandler(
                          new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer payload) {
                              assertEquals(
                                "a-test-message2",
                                payload.toString()
                              );
                              latch.countDown();
                            }
                          }
                        )
                        .endHandler(__ -> {});
                    }
                  }
                );

                result.end();
              }
            );

            try {
              TextMessage message1 = jmsContext.createTextMessage(
                "a-test-message1"
              );
              message1.setStringProperty("myproperty", "value1");
              TextMessage message2 = jmsContext.createTextMessage(
                "a-test-message2"
              );
              message2.setStringProperty("myproperty", "value2");

              jmsContext
                .createProducer()
                .send(jmsContext.createQueue(getQueueName()), message1);
              jmsContext
                .createProducer()
                .send(jmsContext.createQueue(getQueueName()), message2);
            } catch (Exception ex) {
              Assertions.fail(ex);
            }
          }
        }
      );

    assertTrue(latch.await(10, TimeUnit.SECONDS));
  }

  protected JMSConnector createConnector(JMSEndpoint endpoint)
    throws JsonProcessingException {
    String definition = mapper.writeValueAsString(endpoint);

    ConnectorBuilder builder = mock(ConnectorBuilder.class);
    when(builder.getMapper()).thenReturn(mapper);

    return new JMSConnectorFactory()
    .create(endpoint.target(), definition, builder);
  }

  protected Request createMockRequest(
    ExecutionContext context,
    HttpMethod method,
    HttpHeaders headers
  ) {
    final Request request = mock(Request.class);
    when(request.headers()).thenReturn(headers);
    when(request.metrics())
      .thenReturn(Metrics.on(System.currentTimeMillis()).build());

    when(request.parameters()).thenReturn(new LinkedMultiValueMap<>());
    when(request.uri()).thenReturn("http://backend/");
    when(request.method()).thenReturn(method);

    lenient().when(context.request()).thenReturn(request);

    return request;
  }

  protected abstract JMSEndpoint createDefaultEndpoint();

  protected abstract ConnectionFactory createConnectionFactory()
    throws JMSException;

  protected abstract String getQueueName();
}
