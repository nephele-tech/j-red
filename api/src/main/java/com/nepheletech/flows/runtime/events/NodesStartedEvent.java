package com.nepheletech.flows.runtime.events;

import java.util.EventObject;

public class NodesStartedEvent extends EventObject {
  private static final long serialVersionUID = -8788644252277762854L;

  public NodesStartedEvent(Object source) {
    super(source);
  }
}
