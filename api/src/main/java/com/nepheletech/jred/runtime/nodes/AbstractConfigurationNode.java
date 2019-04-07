package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonObject;

public abstract class AbstractConfigurationNode extends AbstractNode implements ConfigurationNode {

  public AbstractConfigurationNode(final Flow flow, final JsonObject config) {
    super(flow, config);
  }

  protected final void onMessage(JsonObject msg) {
    throw new UnsupportedOperationException();
  }
}
