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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Provides support for basic intra-application message passing.
 */
public class MessageBus {

  /**
   * All registered subscribers, indexed by event type.
   * <p>
   * The {@link CopyOnWriteArraySet} values make it easy and relatively
   * lightweight to get an immutable snapshot of all current subscribers to an
   * event without any locking.
   */
  private static final ConcurrentMap<String, CopyOnWriteArraySet<MessageBusListener<?>>> messageTopics = new ConcurrentHashMap<>();

  /**
   * Constructor.
   */
  private MessageBus() {}

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
    CopyOnWriteArraySet<MessageBusListener<?>> topicListeners = messageTopics.get(topic);

    if (topicListeners == null) {
      CopyOnWriteArraySet<MessageBusListener<?>> newSet = new CopyOnWriteArraySet<>();
      topicListeners = messageTopics.putIfAbsent(topic, newSet);
      if (topicListeners == null) {
        topicListeners = newSet;
      }
    }

    topicListeners.add(messageListener);

    return new Subscription() {
      @Override
      public void unsubscribe() {
        try {
          MessageBus.unsubscribeImpl(topic, messageListener);
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
  private static <T> void unsubscribeImpl(String topic, MessageBusListener<T> messageListener) {
    CopyOnWriteArraySet<MessageBusListener<?>> topicListeners = messageTopics.get(topic);

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

    sendMessage(message.getClass().getName(), message, false);
  }
  
  public static <T> void sendMessage(T message, boolean async) {
    if (message == null) { throw new NullPointerException("message can't be null"); }

    sendMessage(message.getClass().getName(), message, async);
  }
  
  /**
   * Sends a message to subscribed topic listeners.
   *
   * @param message
   */
  public static <T> void sendMessage(String topic, T message) {
    sendMessage(topic, message, false);
  }

  /**
   * Sends a message to subscribed topic listeners.
   *
   * @param message
   */
  @SuppressWarnings("unchecked")
  public static <T> void sendMessage(String topic, T message, boolean async) {
    CopyOnWriteArraySet<MessageBusListener<?>> topicListeners = messageTopics.get(topic);

    if (topicListeners != null) {
      if (async) {
        topicListeners.stream().parallel().forEach(x -> {
          ((MessageBusListener<T>) x).messageSent(topic, message);
        });
      } else {
        topicListeners.stream().forEach(x -> {
          ((MessageBusListener<T>) x).messageSent(topic, message);
        });
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
