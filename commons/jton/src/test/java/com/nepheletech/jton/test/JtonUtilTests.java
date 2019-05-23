package com.nepheletech.jton.test;

import org.junit.Assert;
import org.junit.Test;

import com.nepheletech.jton.JtonUtil;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;

public class JtonUtilTests {

  @Test
  public void removeInvalidIndex() {
    JtonObject o = new JtonObject()
        .set("array", new JtonArray());

    JtonUtil.deleteProperty(o, "array[1]");

    Assert.assertEquals(o.getAsJtonArray("array").size(), 0);
  }

  @Test
  public void removeNegativeIndex() {
    JtonObject o = new JtonObject()
        .set("array", new JtonArray());

    JtonUtil.deleteProperty(o, "array[-1]");

    Assert.assertEquals(o.getAsJtonArray("array").size(), 0);
  }

}
