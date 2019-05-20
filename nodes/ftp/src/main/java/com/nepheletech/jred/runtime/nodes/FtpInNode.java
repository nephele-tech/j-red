package com.nepheletech.jred.runtime.nodes;

import org.apache.camel.CamelContext;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

public class FtpInNode extends AbstractCamelNode implements HasCredentials {

  public FtpInNode(Flow flow, JtonObject config) {
    super(flow, config);
    // TODO Auto-generated constructor stub
  }

  @Override
  public void setCredentials(JtonObject credentials) {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void addRoutes(CamelContext camelContext) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void onMessage(JtonObject msg) {
    // TODO Auto-generated method stub
    
  }

}
