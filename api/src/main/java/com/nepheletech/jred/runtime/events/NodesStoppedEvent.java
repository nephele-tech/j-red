package com.nepheletech.jred.runtime.events;

import java.util.EventObject;

public class NodesStoppedEvent extends EventObject {
  private static final long serialVersionUID = -4288646875451970416L;

  public NodesStoppedEvent(Object source) {
    super(source);
  }
}
