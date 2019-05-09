package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

/**
 * Configuration options for HTTP proxy.
 * 
 * @author ggeorg
 */
public class HttpProxyNode extends AbstractConfigurationNode implements HasCredentials {

  public HttpProxyNode(Flow flow, JtonObject config) {
    super(flow, config);
  }

  @Override
  public void setCredentials(JtonObject credentials) {
    // TODO Auto-generated method stub
    
  }
}
