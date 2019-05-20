/*
 * Copyright NepheleTech and other contributorns, http://www.nephelerech.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
