package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonObject;

/**
 * Configuration options for HTTP proxy.
 * 
 * @author ggeorg
 */
public class HttpProxyNode extends AbstractConfigurationNode implements HasCredentials {

  public HttpProxyNode(Flow flow, JsonObject config) {
    super(flow, config);
  }

  @Override
  public void setCredentials(JsonObject credentials) {
    // TODO Auto-generated method stub
    
  }
}
