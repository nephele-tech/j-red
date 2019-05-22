package com.nepheletech.jred.test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.nodes.FunctionNode;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;

public class FunctionNodeTest extends NodeTest {

  @Test(expected = UnsupportedOperationException.class)
  public void creation_NoFunc() {
    create(new JtonObject());
  }

  @Test
  public void creation_NoOutputs() {
    Assert.assertNotNull(create(new JtonObject()
        .set("func", "return msg;")));
  }

  @Test
  public void returnNULL() {
    FunctionNode node = create(new JtonObject()
        .set("func", "return null;"));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1);

    node.on("#send", (topic, message) -> {
      Assert.assertEquals(message, null);
    });

    node.receive(msg);
  }

  @Test
  public void returnMSG() {
    FunctionNode node = create(new JtonObject()
        .set("func", "return msg;"));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1);

    JtonObject expected = msg.deepCopy();

    node.on("#send", (topic, message) -> {
      Assert.assertEquals(message, expected);
    });

    node.receive(msg);
  }

  @Test
  public void implicitImports() {
    FunctionNode node = create(new JtonObject()
        .set("func", "msg.set(\"o\", new JtonObject());\n"
            + "msg.set(\"a\", new JtonArray());\n"
            + "msg.set(\"p\", new JtonPrimitive(123));\n"
            // + "msg.set(\"t\", new JtonTransient(123));\n"
            + "msg.set(\"null\", JtonNull.INSTANCE);\n"
            + "msg.set(\"JRedUtil\", JRedUtil.class.getName());\n"
            + "return msg;"));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1);

    JtonObject expected = msg.deepCopy()
        .set("o", new JtonObject())
        .set("a", new JtonArray())
        .set("p", new JtonPrimitive(123))
        // .set("t", new JtonTransient(123))
        .set("null", JtonNull.INSTANCE)
        .set("JRedUtil", JRedUtil.class.getName());

    node.on("#send", (topic, message) -> {
      Assert.assertEquals(message, expected);
    });

    node.receive(msg);
  }
  
  @Test
  public void imports() {
    FunctionNode node = create(new JtonObject()
        .set("func", "//@import java.util.*\n"
            + "//@import java.math.BigDecimal\n"
            + "msg.set(\"date\", new Date(123L));\n"
            + "msg.set(\"bd\", new BigDecimal(\"123.24\"));\n"
            + "return msg;"));

    JtonObject msg = new JtonObject()
        .set("_msgid", 1);

    JtonObject expected = msg.deepCopy()
        .set("date", new Date(123L))
        .set("bd", new BigDecimal("123.24"));

    node.on("#send", (topic, message) -> {
      Assert.assertEquals(message, expected);
    });

    node.receive(msg);
  }

  FunctionNode create(JtonObject config) {
    return create(createFlow(), config
        .set("id", UUID.randomUUID().toString()));
  }

  FunctionNode create(Flow flow, JtonObject config) {
    return new FunctionNode(flow, config
        .set("type", "convert")
        .set("z", flow.hashCode()));
  }
}
