/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nepheletech.messagebus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides support for basic intra-application message passing.
 */
public final class MessageBus {
  private static final Map<String, ListenerList<MessageBusListener<?>>> messageTopics = new ConcurrentHashMap<>();

  /**
   * Subscribes a listener to a message topic.
   *
   * @param topic
   * @param messageListener
   */
  public static <T> Subscription subscribe(Class<? super T> topic, MessageBusListener<T> messageListener) {
    return subscribe(topic.getName(), messageListener);
  }

  /**
   * Subscribes a listener to a message topic.
   *
   * @param topic
   * @param messageListener
   */
  public static <T> Subscription subscribe(String topic, MessageBusListener<T> messageListener) {
    ListenerList<MessageBusListener<?>> topicListeners = messageTopics.get(topic);

    if (topicListeners == null) {
      topicListeners = new ListenerList<MessageBusListener<?>>() {
        // empty block
      };

      messageTopics.put(topic, topicListeners);
    }

    topicListeners.add(messageListener);

    return new Subscription() {
      @Override
      public void unsubscribe() {
        try {
          MessageBus.unsubscribe(topic, messageListener);
        } catch (IllegalArgumentException e) {
          // ignore
        }
      }
    };
  }

  /**
   * Unsubscribe a listener from a message topic.
   *
   * @param topic
   * @param messageListener
   */
  @SuppressWarnings("unused")
  private static <T> void unsubscribe(Class<? super T> topic, MessageBusListener<T> messageListener) {
    unsubscribe(topic.getName(), messageListener);
  }

  /**
   * Unsubscribe a listener from a message topic.
   *
   * @param topic
   * @param messageListener
   */
  private static <T> void unsubscribe(String topic, MessageBusListener<T> messageListener) {
    ListenerList<MessageBusListener<?>> topicListeners = messageTopics.get(topic);

    if (topicListeners == null) { throw new IllegalArgumentException(topic + " does not exist."); }

    topicListeners.remove(messageListener);
    if (topicListeners.isEmpty()) {
      messageTopics.remove(topic);
    }
  }

  /**
   * Sends a message to subscribed topic listeners.
   *
   * @param message
   */
  public static <T> void sendMessage(T message) {
    if (message == null) { throw new NullPointerException("message can't be null"); }

    sendMessage(message.getClass().getName(), message);
  }

  /**
   * Sends a message to subscribed topic listeners.
   *
   * @param message
   */
  @SuppressWarnings("unchecked")
  public static <T> void sendMessage(String topic, T message) {
    ListenerList<MessageBusListener<?>> topicListeners = messageTopics.get(topic);

    if (topicListeners != null) {
      for (MessageBusListener<?> listener : topicListeners) {
        ((MessageBusListener<T>) listener).messageSent(topic, message);
      }
    }
  }

  /**
   * Unsubscribe all listeners.
   */
  public static void unsubscribeAll() {
    messageTopics.clear();
  }
}
