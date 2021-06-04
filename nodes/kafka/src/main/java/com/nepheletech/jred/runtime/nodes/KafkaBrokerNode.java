package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

public class KafkaBrokerNode extends AbstractConfigurationNode {
  
  private final String hosts;

  public KafkaBrokerNode(Flow flow, JtonObject config) {
    super(flow, config);
    
    this.hosts = config.getAsString("hosts", "");
  }
  
  public String getHosts() {
    return hosts;
  }
}
