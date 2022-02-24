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
package io.gravitee.connector.jms.connection.message;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.connector.api.response.AbstractResponse;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.stream.ReadStream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BufferResponse extends AbstractResponse {

  private final HttpHeaders headers = HttpHeaders.create();

  private final Buffer buffer;

  public BufferResponse(Buffer buffer) {
    this.buffer = buffer;

    if (this.buffer != null) {
      headers.set(
        io.gravitee.common.http.HttpHeaders.CONTENT_LENGTH,
        Integer.toString(this.buffer.length())
      );
    }
  }

  @Override
  public int status() {
    return HttpStatusCode.OK_200;
  }

  @Override
  public HttpHeaders headers() {
    return headers;
  }

  @Override
  public ReadStream<Buffer> resume() {
    return this;
  }

  @Override
  public boolean connected() {
    return true;
  }
}
