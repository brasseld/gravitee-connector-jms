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

import io.gravitee.common.util.MultiValueMap;
import io.gravitee.common.util.URIUtils;
import java.net.URI;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Destination {

  private DestinationType type;

  private String name;

  private final String protocol;

  private final String host;

  private final int port;

  private MultiValueMap<String, String> parameters;

  private Destination(URI uri) {
    this.protocol = uri.getScheme();
    this.host = uri.getHost();
    this.port = uri.getPort();

    parse(uri);
  }

  private void parse(URI uri) {
    // Extract destination
    String path = uri.getPath();
    if (path.startsWith("/queue/")) {
      this.type = DestinationType.QUEUE;
      this.name = path.substring(7);
    } else if (path.startsWith("/topic/")) {
      this.type = DestinationType.TOPIC;
      this.name = path.substring(7);
    }

    // Extract extraParameters
    this.parameters = URIUtils.parameters(uri.toString());
  }

  public static Destination from(URI uri) {
    return new Destination(uri);
  }

  public static Destination from(String uri) {
    return new Destination(URI.create(uri));
  }

  public DestinationType getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public String getProtocol() {
    return protocol;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public MultiValueMap<String, String> getParameters() {
    return parameters;
  }
}
