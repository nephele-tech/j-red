package com.nepheletech.jred.test;

import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.nodes.ConvertNode;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;

public class ConvertNodeTests {

  @Test
  public void creation() {
    Assert.assertNotNull(create(new JsonObject()));
  }

  @Test
  public void double2string() {
    ConvertNode convert = create(new JsonObject()
        .set("rules", new JsonArray()
            .push(new JsonObject()
                .set("t", "string")
                .set("p", "payload")
                .set("pt", "msg"))));

    JsonObject msg = new JsonObject()
        .set("_msgid", 1)
        .set("payload", 123D);

    JsonObject expected = msg.deepCopy()
        .set("payload", "123.0");

    convert.on("#send", (String topic, JsonElement message) -> {
      Assert.assertEquals(message, expected);
    });

    convert.receive(msg);
  }

  @Test
  public void string2double() {
    ConvertNode convert = create(new JsonObject()
        .set("rules", new JsonArray()
            .push(new JsonObject()
                .set("t", "double")
                .set("p", "payload")
                .set("pt", "msg"))));

    JsonObject msg = new JsonObject()
        .set("_msgid", 1)
        .set("payload", "123.0");

    JsonObject expected = msg.deepCopy()
        .set("payload", 123D);

    convert.on("#send", (String topic, JsonElement message) -> {
      Assert.assertEquals(message, expected);
    });

    convert.receive(msg);
  }

  ConvertNode create(JsonObject config) {
    return create(createFlow(), config
        .set("id", UUID.randomUUID().toString()));
  }

  ConvertNode create(Flow flow, JsonObject config) {
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
      public void update(JsonObject global, JsonObject flow) {
        // TODO Auto-generated method stub

      }

      @Override
      public void start(JsonObject diff) {
        // TODO Auto-generated method stub

      }

      @Override
      public void stop(JsonArray stopList, JsonArray removedList) {
        // TODO Auto-generated method stub

      }

      @Override
      public JsonElement getSetting(String key) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public boolean handleStatus(Node node, JsonObject statusMessage, Node reportingNode, boolean muteStatusEvent) {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public boolean handleError(Node node, Throwable logMessage, JsonObject msg, Node reportingNode) {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public JsonObject getContext(String type) {
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
