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

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ProducerOptions {

  private DeliveryMode deliveryMode = DeliveryMode.PERSISTENT;

  private String messageType;

  private long timeToLive = 0L;

  private long deliveryDelay = 0L;

  private int priority = 4;

  private boolean disableMessageID = false;

  private boolean disableMessageTimestamp = false;

  private String correlationID;

  public DeliveryMode getDeliveryMode() {
    return deliveryMode;
  }

  public void setDeliveryMode(DeliveryMode deliveryMode) {
    this.deliveryMode = deliveryMode;
  }

  public String getMessageType() {
    return messageType;
  }

  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  public long getTimeToLive() {
    return timeToLive;
  }

  public void setTimeToLive(long timeToLive) {
    this.timeToLive = timeToLive;
  }

  public long getDeliveryDelay() {
    return deliveryDelay;
  }

  public void setDeliveryDelay(long deliveryDelay) {
    this.deliveryDelay = deliveryDelay;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public boolean isDisableMessageID() {
    return disableMessageID;
  }

  public void setDisableMessageID(boolean disableMessageID) {
    this.disableMessageID = disableMessageID;
  }

  public boolean isDisableMessageTimestamp() {
    return disableMessageTimestamp;
  }

  public void setDisableMessageTimestamp(boolean disableMessageTimestamp) {
    this.disableMessageTimestamp = disableMessageTimestamp;
  }

  public String getCorrelationID() {
    return correlationID;
  }

  public void setCorrelationID(String correlationID) {
    this.correlationID = correlationID;
  }
}
