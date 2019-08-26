package com.nepheletech.jred.test;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.nodes.ChangeNode;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;

public class ChangeNodeTest extends NodeTest {

  //@Test
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

  //@Test
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

  //@Test
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

  //@Test
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

  //@Test
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

  //@Test
  public void deleteProperty() {
    ChangeNode node = create(new JtonObject()
        .set("rules", new JtonArray()
            .push(new JtonObject()
                .set("t", "delete")
                .set("p", "Address.Street")
                .set("pt", "msg"))));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1)
        .set("Address", new JtonObject()
            .set("Street", "Hursley Park")
            .set("City", "Winchester")
            .set("Postcode", "SO21 2JN"));

    JtonObject expected = msg.deepCopy();
    expected.getAsJtonObject("Address")
        .remove("Street");

    node.on("#send", (topic, message) -> {
      Assert.assertEquals(message.toString(), expected.toString());
    });

    node.receive(msg);
  }

  //@Test
  public void deleteIndex() {
    ChangeNode node = create(new JtonObject()
        .set("rules", new JtonArray()
            .push(new JtonObject()
                .set("t", "delete")
                .set("p", "Phone[1]")
                .set("pt", "msg"))));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1)
        .set("Phone", new JtonArray()
            .push(new JtonObject()
                .set("type", "home")
                .set("number", "0203 544 1234"))
            .push(new JtonObject()
                .set("type", "office")
                .set("number", "01962 001234"))
            .push(new JtonObject()
                .set("type", "mobile")
                .set("number", "077 7700 1234")));

    JtonObject expected = msg.deepCopy();
    expected.getAsJtonArray("Phone")
        .remove(1);

    node.on("#send", (topic, message) -> {
      Assert.assertEquals(message.toString(), expected.toString());
    });

    node.receive(msg);
  }

  //@Test
  public void deleteIndex4() {
    ChangeNode node = create(new JtonObject()
        .set("rules", new JtonArray()
            .push(new JtonObject()
                .set("t", "delete")
                .set("p", "Phone[4]")
                .set("pt", "msg"))));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1)
        .set("Phone", new JtonArray()
            .push(new JtonObject()
                .set("type", "home")
                .set("number", "0203 544 1234"))
            .push(new JtonObject()
                .set("type", "office")
                .set("number", "01962 001234"))
            .push(new JtonObject()
                .set("type", "mobile")
                .set("number", "077 7700 1234")));

    JtonObject expected = msg.deepCopy();

    node.on("#send", (topic, message) -> {
      Assert.assertEquals(message.toString(), expected.toString());
    });

    node.receive(msg);
  }

  //@Test
  public void deleteArrayIndex2() {
    ChangeNode node = create(new JtonObject()
        .set("rules", new JtonArray()
            .push(new JtonObject()
                .set("t", "delete")
                .set("p", "array[2]")
                .set("pt", "msg"))));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1)
        .set("array", new JtonArray()
            .push(1)
            .push(2)
            .push(3)
            .push(4)
            .push(5)
            .push(6)
            .push(7)
            .push(8)
            .push(9)
            .push(0));

    node.on("#send", (String topic, JtonObject message) -> {
      Assert.assertEquals(message.getAsJtonArray("array").size(), 9);
    });

    node.receive(msg);
  }

  //@Test
  public void deleteArrayProp() {
    ChangeNode node = create(new JtonObject()
        .set("rules", new JtonArray()
            .push(new JtonObject()
                .set("t", "delete")
                .set("p", "array.lala")
                .set("pt", "msg"))));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1)
        .set("array", new JtonArray()
            .push(1)
            .push(2)
            .push(3)
            .push(4)
            .push(5)
            .push(6)
            .push(7)
            .push(8)
            .push(9)
            .push(0));

    node.on("#send", (String topic, JtonObject message) -> {
      Assert.assertEquals(message.getAsJtonArray("array").size(), 10);
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
