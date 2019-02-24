package com.nepheletech.flows.runtime.events;

import java.util.EventObject;

public class RuntimeDeployEvent extends EventObject {
  private static final long serialVersionUID = 5755953753419080411L;
  
  private final String revision;
  
  public RuntimeDeployEvent(Object source, String revision) {
    super(source);
    this.revision = revision;
  }

  public String getRevision() { return revision; }
}
