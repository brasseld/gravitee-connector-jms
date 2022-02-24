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
package io.gravitee.connector.jms.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DestinationTest {

  @Test
  public void shouldExtractSimpleURL() {
    Destination destination = Destination.from(
      "mq://mqhost:1414/queue/my_queue"
    );
    Assertions.assertEquals("mqhost", destination.getHost());
    Assertions.assertEquals(1414, destination.getPort());
    Assertions.assertEquals(DestinationType.QUEUE, destination.getType());
    Assertions.assertEquals("my_queue", destination.getName());
    Assertions.assertEquals("mq", destination.getProtocol());
    Assertions.assertTrue(destination.getParameters().isEmpty());
  }

  @Test
  public void shouldExtractComplexQueueName() {
    Destination destination = Destination.from(
      "mq://mqhost:1414/queue/queue:///my_queue"
    );
    Assertions.assertEquals("mqhost", destination.getHost());
    Assertions.assertEquals(1414, destination.getPort());
    Assertions.assertEquals(DestinationType.QUEUE, destination.getType());
    Assertions.assertEquals("queue:///my_queue", destination.getName());
    Assertions.assertEquals("mq", destination.getProtocol());
    Assertions.assertTrue(destination.getParameters().isEmpty());
  }

  @Test
  public void shouldExtractWithParameters() {
    Destination destination = Destination.from(
      "mq://mqhost:1414/queue/my_queue?queueManager=MGR"
    );
    Assertions.assertEquals("mqhost", destination.getHost());
    Assertions.assertEquals(1414, destination.getPort());
    Assertions.assertEquals(DestinationType.QUEUE, destination.getType());
    Assertions.assertEquals("my_queue", destination.getName());
    Assertions.assertEquals("mq", destination.getProtocol());
    Assertions.assertEquals(1, destination.getParameters().size());
    Assertions.assertEquals(
      "MGR",
      destination.getParameters().getFirst("queueManager")
    );
  }
}
