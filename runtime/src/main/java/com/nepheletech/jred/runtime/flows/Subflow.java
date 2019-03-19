package com.nepheletech.jred.runtime.flows;

import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.json.JsonObject;

/**
 * This class represents a subflow - which is handled as a special type of
 * {@link FlowImpl}.
 */
public class Subflow extends FlowImpl {

  /**
   * Create a {@code Subflow} object.
   * <p>
   * This takes a subflow definition and instance node, creates a clone of the
   * definition with unique ids applied and passes to the super class.
   * 
   * @param parent
   * @param globalFlow
   * @param subflowDef
   * @param subflowInstance
   */
  public Subflow(Flow parent, JsonObject globalFlow, JsonObject subflowDef, JsonObject subflowInstance) {
    super(parent, globalFlow, subflowInternalFlowConfig());
    // TODO Auto-generated constructor stub
  }

  private static JsonObject subflowInternalFlowConfig() {
    // TODO Auto-generated method stub
    return null;
  }

  public void start() {
    // TODO Auto-generated method stub

  }

  /**
   * Start the subflow.
   * <p>
   * This creates a subflow instance node to handle the inbound messages. It also
   * rewires and subflow internal node that is connected to an output so it is
   * connected to the parent flow nodes the subflow instance is wired to.
   * 
   * @param diff
   */
  @Override
  public void start(JsonObject diff) {

  }

  /**
   * Get environment variable of subflow.
   * 
   * @param name name of env var
   * @return value of env var
   */
  @Override
  public String getSetting(String name) {
    return null;
  }

  /**
   * Get a node instance from this subflow.
   * <p>
   * If the subflow has a status node, check for that, otherwise use the
   * super-class function.
   * 
   * @param id
   * @param cancelBubble if {@code true}, prevents the flow passing the request to
   *                     the parent. This stops infinite loops when the parent
   *                     asked this Flow for the node to begin with.
   */
  @Override
  public Node getNode(String id, boolean cancelBubble) {
    return null;
  }
  
  // TODO handleStatus
  
  // TODO handleError

  public Object stop() {
    // TODO Auto-generated method stub
    return null;
  }
  
  public Node getNode() { return null; }

}
