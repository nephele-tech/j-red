package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

public abstract class AbstractConfigurationNode extends AbstractNode implements ConfigurationNode {

  public AbstractConfigurationNode(final Flow flow, final JtonObject config) {
    super(flow, config);
  }

  protected final void onMessage(JtonObject msg) {
    throw new UnsupportedOperationException();
  }
}
