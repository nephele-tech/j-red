/*
 * Copyright 2011 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nepheletech.jton.jsonpath;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingException;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

public class JtonMappingProvider implements MappingProvider {

  @Override
  public <T> T map(Object source, Class<T> targetType, Configuration configuration) {
    if (source == null) {
      return null;
    }
    try {
      throw new UnsupportedOperationException();
      //return factory.call().getAdapter(targetType).fromJsonTree((JtonElement) source);
    } catch (Exception e) {
      throw new MappingException(e);
    }
  }

  @Override
  public <T> T map(Object source, TypeRef<T> targetType, Configuration configuration) {
    throw new UnsupportedOperationException("JTON provider does not support TypeRef! Use a Jackson or Gson based provider");
  }
}
