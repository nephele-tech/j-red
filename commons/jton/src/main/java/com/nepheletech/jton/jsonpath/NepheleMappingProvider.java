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

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingException;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import com.nepheletech.jton.Gson;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.jsonpath.NepheleMappingProvider;
import com.nepheletech.jton.reflect.TypeToken;

public class NepheleMappingProvider implements MappingProvider {

  private static final Logger logger = LoggerFactory.getLogger(NepheleMappingProvider.class);

  private final Callable<Gson> factory;

  public NepheleMappingProvider(final Gson gson) {
    this(new Callable<Gson>() {
      @Override
      public Gson call() {
        return gson;
      }
    });
  }

  public NepheleMappingProvider(Callable<Gson> factory) {
    this.factory = factory;
  }

  public NepheleMappingProvider() {
    super();
    try {
      Class.forName(Gson.class.getName());
      this.factory = new Callable<Gson>() {
        @Override
        public Gson call() {
          return new Gson();
        }
      };
    } catch (ClassNotFoundException e) {
      logger.error("Gson not found on class path. No converters configured.");
      throw new JsonPathException("Gson not found on path", e);
    }
  }

  @Override
  public <T> T map(Object source, Class<T> targetType, Configuration configuration) {
    if (source == null) {
      return null;
    }
    try {
      return factory.call().getAdapter(targetType).fromJsonTree((JtonElement) source);
    } catch (Exception e) {
      throw new MappingException(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T map(Object source, TypeRef<T> targetType, Configuration configuration) {
    if (source == null) {
      return null;
    }
    try {
      return (T) factory.call().getAdapter(TypeToken.get(targetType.getType())).fromJsonTree((JtonElement) source);
    } catch (Exception e) {
      throw new MappingException(e);
    }
  }
}
