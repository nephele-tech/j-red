/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED Commons project.
 *
 * J-RED Commons is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED Commons is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED Commons; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.dao.transformer;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import org.hibernate.transform.ResultTransformer;

import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;

public class JsonResultTransformer implements ResultTransformer {
  private static final long serialVersionUID = -6533713319742151790L;

  @Override
  public Object transformTuple(Object[] tuple, String[] aliases) {
    final JtonObject o = new JtonObject();
    for (int i = 0, n = tuple.length; i < n; i++) {
      final String property = Optional.of(aliases[i]).orElse("col-" + i);
      try {
        o.set(property, tuple[i], false);
      } catch (IllegalArgumentException e) {
        o.set(property, tuple[i], true);
      }
    }
    return o;
  }
  
  protected String convert(Date date) {
    final Calendar c =  Calendar.getInstance(Locale.US);
    c.setTimeZone(TimeZone.getTimeZone("UTC"));
    c.setTime(date);
    return DatatypeConverter.printDateTime(c);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public List transformList(List collection) {
    return new JtonArray(collection);
  }
}
