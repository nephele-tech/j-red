package com.nepheletech.jred.runtime.events;

public interface NodesStoppedEventListener {
  void onNodesStopped(NodesStoppedEvent event) throws Exception;
}
