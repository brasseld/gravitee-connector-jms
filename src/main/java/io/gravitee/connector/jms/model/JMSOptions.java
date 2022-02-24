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

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JMSOptions {

  private final ConsumerOptions consumerOptions;

  private final ProducerOptions producerOptions;

  private final Destination destination;

  private String username;

  private String password;

  private String clientID;

  private final Map<String, String> properties = new HashMap<>();

  public JMSOptions(
    URI uri,
    ConsumerOptions consumerOptions,
    ProducerOptions producerOptions
  ) {
    this.destination = Destination.from(uri);
    this.consumerOptions = consumerOptions;
    this.producerOptions = producerOptions;
  }

  public Destination getDestination() {
    return destination;
  }

  public ConsumerOptions getConsumerOptions() {
    return consumerOptions;
  }

  public ProducerOptions getProducerOptions() {
    return producerOptions;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getClientID() {
    return clientID;
  }

  public void setClientID(String clientID) {
    this.clientID = clientID;
  }

  public void addProperty(String key, String value) {
    properties.put(key, value);
  }

  public Map<String, String> getProperties() {
    return Collections.unmodifiableMap(properties);
  }
}
