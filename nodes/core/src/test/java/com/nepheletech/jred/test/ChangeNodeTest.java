package com.nepheletech.jred.test;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.nodes.ChangeNode;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;

public class ChangeNodeTest extends NodeTest {

  @Test
  public void setString() {
    ChangeNode node = create(new JtonObject()
        .set("rules", new JtonArray()
            .push(new JtonObject()
                .set("t", "set")
                .set("p", "payload")
                .set("pt", "msg")
                .set("to", "string")
                .set("tot", "str"))));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1);

    JtonObject expected = msg.deepCopy()
        .set("payload", "string");

    node.on("#send", (topic, message) -> {
      Assert.assertEquals(message.toString(), expected.toString());
    });

    node.receive(msg);
  }

  @Test
  public void setNum() {
    ChangeNode node = create(new JtonObject()
        .set("rules", new JtonArray()
            .push(new JtonObject()
                .set("t", "set")
                .set("p", "payload")
                .set("pt", "msg")
                .set("to", "123")
                .set("tot", "num"))));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1);

    JtonObject expected = msg.deepCopy()
        .set("payload", 123);

    node.on("#send", (topic, message) -> {
      Assert.assertEquals(message.toString(), expected.toString());
    });

    node.receive(msg);
  }

  @Test
  public void setNaN() {
    ChangeNode node = create(new JtonObject()
        .set("rules", new JtonArray()
            .push(new JtonObject()
                .set("t", "set")
                .set("p", "payload")
                .set("pt", "msg")
                .set("to", "not a number")
                .set("tot", "num"))));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1);

    JtonObject expected = msg.deepCopy()
        .set("payload", "NaN");

    node.on("#send", (topic, message) -> {
      Assert.assertEquals(message.toString(), expected.toString());
    });

    node.receive(msg);
  }

  @Test
  public void setBool() {
    ChangeNode node = create(new JtonObject()
        .set("rules", new JtonArray()
            .push(new JtonObject()
                .set("t", "set")
                .set("p", "payload")
                .set("pt", "msg")
                .set("to", "true")
                .set("tot", "bool"))));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1);

    JtonObject expected = msg.deepCopy()
        .set("payload", true);

    node.on("#send", (topic, message) -> {
      Assert.assertEquals(message.toString(), expected.toString());
    });

    node.receive(msg);
  }

  @Test
  public void setWithJsonPath() {
    ChangeNode node = create(new JtonObject()
        .set("rules", new JtonArray()
            .push(new JtonObject()
                .set("t", "set")
                .set("p", "payload")
                .set("pt", "msg")
                .set("to", "x")
                .set("tot", "jsonpath"))));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1)
        .set("x", 1234567890);

    JtonObject expected = msg.deepCopy()
        .set("payload", 1234567890);

    node.on("#send", (topic, message) -> {
      Assert.assertEquals(message.toString(), expected.toString());
    });

    node.receive(msg);
  }

  ChangeNode create(JtonObject config) {
    return create(createFlow(), config
        .set("id", UUID.randomUUID().toString()));
  }

  ChangeNode create(Flow flow, JtonObject config) {
    return new ChangeNode(flow, config
        .set("type", "change")
        .set("z", flow.hashCode()));
  }
}
