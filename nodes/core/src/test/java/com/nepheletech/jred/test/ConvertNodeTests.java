package com.nepheletech.jred.test;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.nodes.ConvertNode;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;

public class ConvertNodeTests extends NodeTest {

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
}
