package com.nepheletech.jred.runtime;

import java.io.IOException;

import com.nepheletech.jred.runtime.flows.FlowsManager;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jred.runtime.storage.Storage;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;

public class AbstractFlowsRuntime implements FlowsRuntime {

  private final Storage storage;
  private final FlowsManager flowsManager;

  protected AbstractFlowsRuntime(Storage storage) {
    this.storage = storage;
    this.flowsManager = new FlowsManager(this);
  }

  @Override
  public Storage getStorage() { return storage; }

  public String loadFlows() {
    return loadFlows(false);
  }

  @Override
  public String loadFlows(boolean forceStart) {
    return flowsManager.load(forceStart);
  }

  @Override
  public void startFlows() {
    flowsManager.startFlows();
  }

  @Override
  public void stopFlows() {
    flowsManager.stopFlows();
  }

  @Override
  public JtonObject getFlows() { return flowsManager.getFlows(); }

  @Override
  public String setFlows(JtonArray config, String type) throws IOException {
    return flowsManager.setFlows(config, type);
  }

  @Override
  public Node getNode(String id) {
    return flowsManager.getNode(id);
  }

  @Override
  public JtonObject getNodeCredentials(String type, String id) {
    return flowsManager.getCredentials().get(id);
    
    // TODO some things I don't follow... (runtime/lib/api/flows.js)
  }
}
