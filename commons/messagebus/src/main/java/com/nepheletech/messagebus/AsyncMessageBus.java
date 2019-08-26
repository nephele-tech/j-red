package com.nepheletech.messagebus;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AsyncMessageBus extends MessageBus {

  private static AsyncMessageBus instance = null;

  private static AsyncMessageBus getInstance() {
    if (instance == null) {
      synchronized (MessageBus.class) {
        if (instance == null) {
          instance = new AsyncMessageBus(Executors.newCachedThreadPool());
        }
      }
    }

    return instance;
  }

  protected AsyncMessageBus(Executor executor) {
    super(executor);
  }

  /**
   * Subscribes a listener to a message topic.
   *
   * @param topic
   * @param messageListener
   */
  public static <T> Subscription subscribe(Class<? super T> topic, MessageBusListener<T> messageListener) {
    return getInstance().subscribeImpl(topic, messageListener);
  }

  /**
   * Subscribes a listener to a message topic.
   *
   * @param topic
   * @param messageListener
   */
  public static <T> Subscription subscribe(String topic, MessageBusListener<T> messageListener) {
    return getInstance().subscribeImpl(topic, messageListener);
  }

  /**
   * Sends a message to subscribed topic listeners.
   *
   * @param message
   */
  public static <T> void sendMessage(T message) {
    getInstance().sendMessageImpl(message);
  }

  /**
   * Sends a message to subscribed topic listeners.
   *
   * @param message
   */
  public static <T> void sendMessage(String topic, T message) {
    getInstance().sendMessageImpl(topic, message);
  }

  /**
   * Unsubscribe all listeners.
   */
  public static void unsubscribeAll() {
    getInstance().unsubscribeAllImpl();
  }

}
