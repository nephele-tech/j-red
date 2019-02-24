package com.nepheletech.jred.runtime.flows;

import com.nepheletech.jred.runtime.FlowsRuntime;
import com.nepheletech.json.JsonObject;

/**
 * This class represents a subflow - which is handled as a special type of
 * {@link FlowImpl}.
 */
public class Subflow extends FlowImpl {

  public Subflow(FlowsRuntime flowsRuntime, JsonObject globalFlow, JsonObject flow) {
    super(flowsRuntime, globalFlow, flow);
    // TODO Auto-generated constructor stub
  }

}
