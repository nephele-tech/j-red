package com.nepheletech.jred.test;

import java.util.Map;

import org.apache.camel.CamelContext;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;

public class NodeTest {

  protected Flow createFlow() {
    return new Flow() {
      @Override
      public Node getNode(String id) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Node getNode(String id, boolean cancelBubble) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Map<String, Node> getActiveNodes() { // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void update(JtonObject global, JtonObject flow) {
        // TODO Auto-generated method stub

      }

      @Override
      public void start(JtonObject diff) {
        // TODO Auto-generated method stub

      }

      @Override
      public void stop(JtonArray stopList, JtonArray removedList) {
        // TODO Auto-generated method stub

      }

      @Override
      public JtonElement getSetting(String key) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public boolean handleStatus(Node node, JtonObject statusMessage, Node reportingNode, boolean muteStatusEvent) {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public boolean handleError(Node node, Throwable logMessage, JtonObject msg, Node reportingNode) {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public JtonObject getContext(String type) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void setup(Node node) {
        // TODO Auto-generated method stub

      }

      @Override
      public CamelContext getCamelContext() {
        // TODO Auto-generated method stub
        return null;
      }
    };
  }
}
