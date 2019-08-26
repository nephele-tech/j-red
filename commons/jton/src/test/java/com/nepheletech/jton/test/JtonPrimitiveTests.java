package com.nepheletech.jton.test;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import com.nepheletech.jton.JtonPrimitive;

public class JtonPrimitiveTests {

  @Test(expected = IllegalArgumentException.class)
  public void nullValue() {
    new JtonPrimitive(null, false);
  }

  @Test
  public void intValue() {
    JtonPrimitive p = new JtonPrimitive(123);
    Assert.assertEquals(p.asInt(), 123);
  }

  @Test
  public void intValue2Byte() {
    JtonPrimitive p = new JtonPrimitive(1234);
    Assert.assertEquals(p.asByte(), (byte) 1234);
  }

  @Test
  public void intValue2Short() {
    JtonPrimitive p = new JtonPrimitive(1234);
    Assert.assertEquals(p.asShort(), (short) 1234);
  }

  @Test
  public void intValue2Long() {
    JtonPrimitive p = new JtonPrimitive(1234);
    Assert.assertEquals(p.asLong(), (long) 1234);
  }

  @Test
  public void intValue2Float() {
    JtonPrimitive p = new JtonPrimitive(1234);
    Assert.assertEquals(p.asFloat(), (float) 1234, 0F);
  }

  @Test
  public void intValue2Double() {
    JtonPrimitive p = new JtonPrimitive(1234);
    Assert.assertEquals(p.asDouble(), (double) 1234, 0F);
  }

  // ---

  @Test
  public void asNumber_byte() {
    JtonPrimitive p = new JtonPrimitive((byte) 1278);
    Assert.assertEquals(p.asNumber().byteValue(), (byte) 1278);
  }

  @Test
  public void asNumber_short() {
    JtonPrimitive p = new JtonPrimitive((short) 1278);
    Assert.assertEquals(p.asNumber().shortValue(), (short) 1278);
  }

  @Test
  public void asNumber_int() {
    JtonPrimitive p = new JtonPrimitive(1278);
    Assert.assertEquals(p.asNumber().intValue(), 1278);
  }

  @Test
  public void asNumber_long() {
    JtonPrimitive p = new JtonPrimitive(1278L);
    Assert.assertEquals(p.asNumber().longValue(), 1278L);
  }

  @Test
  public void asNumber_float() {
    JtonPrimitive p = new JtonPrimitive(1278F);
    Assert.assertEquals(p.asNumber().floatValue(), 1278F, 0);
  }

  @Test
  public void asNumber_double() {
    JtonPrimitive p = new JtonPrimitive(1278D);
    Assert.assertEquals(p.asNumber().floatValue(), 1278D, 0);
  }

  @Test
  public void asNumber_char() {
    JtonPrimitive p = new JtonPrimitive('3');
    Assert.assertEquals(p.asNumber().intValue(), 3);
  }

  @Test
  public void asNumber_string() {
    JtonPrimitive p = new JtonPrimitive("1278");
    Assert.assertEquals(p.asNumber().intValue(), 1278);
  }

  @Test
  public void asNumber_boolean_true() {
    JtonPrimitive p = new JtonPrimitive(Boolean.TRUE);
    Assert.assertEquals(p.asNumber().intValue(), 1);
  }

  @Test
  public void asNumber_boolean_false() {
    JtonPrimitive p = new JtonPrimitive(Boolean.FALSE);
    Assert.assertEquals(p.asNumber().intValue(), 0);
  }

  @Test
  public void asNumber_date() {
    JtonPrimitive p = new JtonPrimitive(new Date(1234567890L));
    Assert.assertEquals(p.asNumber().longValue(), 1234567890L);
  }

  @Test(expected = NumberFormatException.class)
  public void asNumber_bytes() {
    new JtonPrimitive(new byte[] {}).asNumber();
  }

  // ---

  @Test
  public void asBigInteger_byte() {
    JtonPrimitive p = new JtonPrimitive((byte) 1278);
    Assert.assertEquals(p.asBigInteger().byteValue(), (byte) 1278);
  }

  @Test
  public void asBigInteger_short() {
    JtonPrimitive p = new JtonPrimitive((short) 1278);
    Assert.assertEquals(p.asBigInteger().shortValue(), (short) 1278);
  }

  @Test
  public void asBigInteger_int() {
    JtonPrimitive p = new JtonPrimitive(1278);
    Assert.assertEquals(p.asBigInteger().intValue(), 1278);
  }

  @Test
  public void asBigInteger_long() {
    JtonPrimitive p = new JtonPrimitive(1278L);
    Assert.assertEquals(p.asBigInteger().longValue(), 1278L);
  }

  @Test
  public void asBigInteger_float() {
    JtonPrimitive p = new JtonPrimitive(1278F);
    Assert.assertEquals(p.asBigInteger().floatValue(), 1278F, 0);
  }

  @Test
  public void asBigInteger_double() {
    JtonPrimitive p = new JtonPrimitive(1278D);
    Assert.assertEquals(p.asBigInteger().floatValue(), 1278D, 0);
  }

  @Test
  public void asBigInteger_char() {
    JtonPrimitive p = new JtonPrimitive('3');
    Assert.assertEquals(p.asBigInteger().intValue(), 3);
  }

  @Test
  public void asBigInteger_string() {
    JtonPrimitive p = new JtonPrimitive("1278");
    Assert.assertEquals(p.asBigInteger().intValue(), 1278);
  }

  @Test
  public void asBigInteger_boolean_true() {
    JtonPrimitive p = new JtonPrimitive(Boolean.TRUE);
    Assert.assertEquals(p.asBigInteger().intValue(), 1);
  }

  @Test
  public void asBigInteger_boolean_false() {
    JtonPrimitive p = new JtonPrimitive(Boolean.FALSE);
    Assert.assertEquals(p.asBigInteger().intValue(), 0);
  }

  @Test
  public void asBigInteger_date() {
    JtonPrimitive p = new JtonPrimitive(new Date(1234567890L));
    Assert.assertEquals(p.asBigInteger().longValue(), 1234567890L);
  }

  @Test(expected = NumberFormatException.class)
  public void asBigInteger_bytes() {
    new JtonPrimitive(new byte[] {}).asNumber();
  }

  // ---

  @Test
  public void asBigDecimal_byte() {
    JtonPrimitive p = new JtonPrimitive((byte) 1278);
    Assert.assertEquals(p.asBigDecimal().byteValue(), (byte) 1278);
  }

  @Test
  public void asBigDecimal_short() {
    JtonPrimitive p = new JtonPrimitive((short) 1278);
    Assert.assertEquals(p.asBigDecimal().shortValue(), (short) 1278);
  }

  @Test
  public void asBigDecimal_int() {
    JtonPrimitive p = new JtonPrimitive(1278);
    Assert.assertEquals(p.asBigDecimal().intValue(), 1278);
  }

  @Test
  public void asBigDecimal_long() {
    JtonPrimitive p = new JtonPrimitive(1278L);
    Assert.assertEquals(p.asBigDecimal().longValue(), 1278L);
  }

  @Test
  public void asBigDecimal_float() {
    JtonPrimitive p = new JtonPrimitive(1278F);
    Assert.assertEquals(p.asBigDecimal().floatValue(), 1278F, 0);
  }

  @Test
  public void asBigDecimal_double() {
    JtonPrimitive p = new JtonPrimitive(1278D);
    Assert.assertEquals(p.asBigDecimal().floatValue(), 1278D, 0);
  }

  @Test
  public void asBigDecimal_char() {
    JtonPrimitive p = new JtonPrimitive('3');
    Assert.assertEquals(p.asBigDecimal().intValue(), 3);
  }

  @Test
  public void asBigDecimal_string() {
    JtonPrimitive p = new JtonPrimitive("1278");
    Assert.assertEquals(p.asBigDecimal().intValue(), 1278);
  }

  @Test
  public void asBigDecimal_boolean_true() {
    JtonPrimitive p = new JtonPrimitive(Boolean.TRUE);
    Assert.assertEquals(p.asBigDecimal().intValue(), 1);
  }

  @Test
  public void asBigDecimal_boolean_false() {
    JtonPrimitive p = new JtonPrimitive(Boolean.FALSE);
    Assert.assertEquals(p.asBigDecimal().intValue(), 0);
  }

  @Test
  public void asBigDecimal_date() {
    JtonPrimitive p = new JtonPrimitive(new Date(1234567890L));
    Assert.assertEquals(p.asBigDecimal().longValue(), 1234567890L);
  }

  @Test(expected = NumberFormatException.class)
  public void asBigDecimal_bytes() {
    new JtonPrimitive(new byte[] {}).asNumber();
  }
}
