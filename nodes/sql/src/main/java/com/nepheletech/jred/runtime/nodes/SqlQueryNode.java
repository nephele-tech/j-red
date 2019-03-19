package com.nepheletech.jred.runtime.nodes;

import org.apache.camel.component.mock.MockEndpoint;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import com.nepheletech.jred.runtime.events.NodesStartedEvent;
import com.nepheletech.jred.runtime.events.NodesStartedEventListener;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonObject;

public class SqlQueryNode extends AbstractNode implements NodesStartedEventListener {

  public SqlQueryNode(Flow flow, JsonObject config) {
    super(flow, config);
  }

  @Override
  public void onNodesStarted(NodesStartedEvent event) {
    // TODO Auto-generated method stub
    
    
    MockEndpoint p;
  }

  @Override
  protected void onMessage(JsonObject msg) {
    new EmbeddedDatabaseBuilder();
  }
}
