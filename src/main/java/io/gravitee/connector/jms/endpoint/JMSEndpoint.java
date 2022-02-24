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
package io.gravitee.connector.jms.endpoint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.connector.api.AbstractEndpoint;
import io.gravitee.connector.jms.model.ConsumerOptions;
import io.gravitee.connector.jms.model.JMSOptions;
import io.gravitee.connector.jms.model.ProducerOptions;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JMSEndpoint extends AbstractEndpoint {

  public static final String MQ_ENDPOINT_TYPE = "jms";

  @JsonProperty("consumer")
  private ConsumerOptions consumerOptions = new ConsumerOptions();

  @JsonProperty("producer")
  private ProducerOptions producerOptions = new ProducerOptions();

  private String username;

  private String password;

  private String clientId;

  private Map<String, String> properties = new HashMap<>();

  @JsonCreator
  public JMSEndpoint(
    @JsonProperty(value = "type") String type,
    @JsonProperty(value = "name", required = true) String name,
    @JsonProperty(value = "target", required = true) String target
  ) {
    super(type != null ? type : MQ_ENDPOINT_TYPE, name, target);
  }

  public JMSEndpoint(
    @JsonProperty(value = "name", required = true) String name,
    @JsonProperty(value = "target", required = true) String target
  ) {
    this(MQ_ENDPOINT_TYPE, name, target);
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public void addProperty(String key, String value) {
    properties.put(key, value);
  }

  public JMSOptions toJMSOptions() {
    JMSOptions options = new JMSOptions(
      URI.create(target()),
      consumerOptions,
      producerOptions
    );
    options.setUsername(username);
    options.setPassword(password);

    if (properties != null) {
      properties.forEach(options::addProperty);
    }

    return options;
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

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public ConsumerOptions getConsumerOptions() {
    return consumerOptions;
  }

  public void setConsumerOptions(ConsumerOptions consumerOptions) {
    this.consumerOptions = consumerOptions;
  }

  public ProducerOptions getProducerOptions() {
    return producerOptions;
  }

  public void setProducerOptions(ProducerOptions producerOptions) {
    this.producerOptions = producerOptions;
  }
}
