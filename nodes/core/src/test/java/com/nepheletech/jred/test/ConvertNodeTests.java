package com.nepheletech.jred.test;

import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.nodes.ConvertNode;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;

public class ConvertNodeTests {

  @Test
  public void creation() {
    Assert.assertNotNull(create(new JtonObject()));
  }

  @Test
  public void double2string() {
    ConvertNode convert = create(new JtonObject()
        .set("rules", new JtonArray()
            .push(new JtonObject()
                .set("t", "string")
                .set("p", "payload")
                .set("pt", "msg"))));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1)
        .set("payload", 123D);

    JtonObject expected = msg.deepCopy()
        .set("payload", "123.0");

    convert.on("#send", (String topic, JtonElement message) -> {
      Assert.assertEquals(message, expected);
    });

    convert.receive(msg);
  }

  @Test
  public void string2double() {
    ConvertNode convert = create(new JtonObject()
        .set("rules", new JtonArray()
            .push(new JtonObject()
                .set("t", "double")
                .set("p", "payload")
                .set("pt", "msg"))));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1)
        .set("payload", "123.0");

    JtonObject expected = msg.deepCopy()
        .set("payload", 123D);

    convert.on("#send", (String topic, JtonElement message) -> {
      Assert.assertEquals(message, expected);
    });

    convert.receive(msg);
  }

  ConvertNode create(JtonObject config) {
    return create(createFlow(), config
        .set("id", UUID.randomUUID().toString()));
  }

  ConvertNode create(Flow flow, JtonObject config) {
    try {
      return new ConvertNode(flow, config
          .set("type", "convert")
          .set("z", flow.hashCode()));
    } catch (RuntimeException e) {
      e.printStackTrace();
      return null;
    }
  }

  Flow createFlow() {
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
    };
  }
}
