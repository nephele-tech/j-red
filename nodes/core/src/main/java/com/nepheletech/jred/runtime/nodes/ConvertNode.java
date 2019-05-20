/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED Nodes project.
 *
 * J-RED Nodes is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED Nodes is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED Nodes; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.jred.runtime.nodes;

import java.util.Set;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;

public class ConvertNode extends AbstractNode {

  private static final int TBOOLEAN = 1;
  private static final int TBYTE = 2;
  private static final int TBYTES = 3; // not supported
  private static final int TSHORT = 4;
  private static final int TINT = 5;
  private static final int TLONG = 6;
  private static final int TFLOAT = 7;
  private static final int TDOUBLE = 8;
  private static final int TSTRING = 9;
  private static final int TBIGINT = 10;
  private static final int TBIGDECIMAL = 11;
  private static final int TDATE = 12;
  private static final int TSQLDATE = 13;
  private static final int TSQLTIME = 14;
  private static final int TSQLTIMESTAMP = 15;

  public static final String JTON_PREFIX = "__jton_";
  private static final int JTON_PREFIX_LENGTH = JTON_PREFIX.length();

  private final JtonArray _rules;

  public ConvertNode(Flow flow, JtonObject config) {
    super(flow, config);

    _rules = config.getAsJtonArray("rules", null);

    if (_rules != null) {
      for (JtonElement _rule : _rules) {
        final JtonObject rule = _rule.asJtonObject(false);
        if (rule != null) {
          final String t = rule.getAsString("t", null);
          if ("boolean".equals(t)) {
            rule.set("t", TBOOLEAN);
          } else if ("byte".equals(t)) {
            rule.set("t", TBYTE);
          } else if ("bytes".equals(t)) {
            rule.set("t", TBYTES);
          } else if ("short".equals(t)) {
            rule.set("t", TSHORT);
          } else if ("int".equals(t)) {
            rule.set("t", TINT);
          } else if ("long".equals(t)) {
            rule.set("t", TLONG);
          } else if ("float".equals(t)) {
            rule.set("t", TFLOAT);
          } else if ("double".equals(t)) {
            rule.set("t", TDOUBLE);
          } else if ("string".equals(t)) {
            rule.set("t", TSTRING);
          } else if ("bigint".equals(t)) {
            rule.set("t", TBIGINT);
          } else if ("bigdecimal".equals(t)) {
            rule.set("t", TBIGDECIMAL);
          } else if ("date".equals(t)) {
            rule.set("t", TDATE);
          } else if ("sqldate".equals(t)) {
            rule.set("t", TSQLDATE);
          } else if ("sqltime".equals(t)) {
            rule.set("t", TSQLTIME);
          } else if ("sqltimestamp".equals(t)) {
            rule.set("t", TSQLTIMESTAMP);
          }
        }
      }
    }
  }

  @Override
  protected void onMessage(JtonObject msg) {
    if (_rules != null) {
      for (JtonElement _rule : _rules) {
        final JtonObject rule = _rule.asJtonObject(null);
        if (rule != null) {
          final int type = rule.getAsInt("t", -1);
          final String p = rule.getAsString("p", null);
          if (type == -1 || p == null) {
            continue;
          }

          final String pt = rule.getAsString("pt", "msg");
          final JtonElement _value = JRedUtil.evaluateNodeProperty(p, pt, this, msg);

          Object value = null;

          try {
            value = convert(_value, type);
          } finally {
            JRedUtil.setNodeProperty(p, pt, this, msg, value);
          }
        }
      }
    } else {

      //
      // NO RULES: Use payload properties
      //

      final JtonObject payload = msg.getAsJtonObject("payload", false);
      if (payload != null) {
        final Set<String> keys = payload.deepCopy().keySet();
        for (String key : keys) {
          if (key.startsWith(JTON_PREFIX)) {
            key = key.substring(JTON_PREFIX_LENGTH);
            if (!payload.has(key)) {
              payload.set(key, JtonNull.INSTANCE);
            }
          }
        }
        for (String key : keys) {
          if (key.startsWith(JTON_PREFIX)) {
            continue;
          }

          final String type = payload.getAsString(JTON_PREFIX + key, null);
          if (type != null) {
            final JtonElement value = payload.get(key);
            if (value.isJtonPrimitive()) {
              payload.set(key, convert(value, type), true);
            }
          }
        }
        for (String key : keys) {
          if (key.startsWith(JTON_PREFIX)) {
            payload.remove(key);
          }
        }
      }
    }

    send(msg);
  }

  private Object convert(JtonElement value, String type) {
    if ("boolean".equals(type)) {
      return convert(value, TBOOLEAN);
    } else if ("byte".equals(type)) {
      return convert(value, TBYTE);
    } else if ("bytes".equals(type)) {
      return convert(value, TBYTES);
    } else if ("short".equals(type)) {
      return convert(value, TSHORT);
    } else if ("int".equals(type)) {
      return convert(value, TINT);
    } else if ("long".equals(type)) {
      return convert(value, TLONG);
    } else if ("float".equals(type)) {
      return convert(value, TFLOAT);
    } else if ("double".equals(type)) {
      return convert(value, TDOUBLE);
    } else if ("string".equals(type)) {
      return convert(value, TSTRING);
    } else if ("bigint".equals(type)) {
      return convert(value, TBIGINT);
    } else if ("bigdecimal".equals(type)) {
      return convert(value, TBIGDECIMAL);
    } else if ("date".equals(type)) {
      return convert(value, TDATE);
    } else if ("sqldate".equals(type)) {
      return convert(value, TSQLDATE);
    } else if ("sqltime".equals(type)) {
      return convert(value, TSQLTIME);
    } else if ("sqltimestamp".equals(type)) {
      return convert(value, TSQLTIMESTAMP);
    } else {
      return null;
    }
  }

  private Object convert(JtonElement _value, int type) {
    switch (type) {
    case TBOOLEAN:
      return _value.asBoolean(false);
    case TBYTE:
      return _value.asByte(null);
    case TBYTES:
      // LOG.t(">>> bytes conversion of: %s", _value);
      throw new UnsupportedOperationException("Can't convert to bytes");
    case TSHORT:
      return _value.asShort(null);
    case TINT:
      return _value.asInt(null);
    case TLONG:
      return _value.asLong(null);
    case TFLOAT:
      return _value.asFloat(null);
    case TDOUBLE:
      return _value.asDouble(null);
    case TSTRING:
      if (_value.isJtonNull()) {
        return null;
      } else if (_value.isJtonPrimitive()) {
        return _value.asString(null);
      } else {
        return _value.toString();
      }
    case TBIGINT:
      return _value.asBigInteger(null);
    case TBIGDECIMAL:
      return _value.asBigDecimal();
    case TDATE:
      return _value.asDate(null);
    case TSQLDATE:
      return _value.asDate(null);
    case TSQLTIME:
      return _value.asSqlTime(null);
    case TSQLTIMESTAMP:
      return _value.asSqlTime(null);
    default:
      return null;
    }
  }
}
