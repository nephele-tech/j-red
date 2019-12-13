package com.nepheletech.jton.jsonpath;

import java.util.EnumSet;
import java.util.Set;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import com.nepheletech.jton.jsonpath.JtonProvider;
import com.nepheletech.jton.jsonpath.JtonMappingProvider;

public final class JtonPathConfiguration {

  private JtonPathConfiguration() {
  }

  public static void configure() {
    Configuration.setDefaults(new Configuration.Defaults() {
      private final JsonProvider jsonProvider = new JtonProvider();
      private final MappingProvider mappingProvider = new JtonMappingProvider();

      @Override
      public JsonProvider jsonProvider() {
        return jsonProvider;
      }

      @Override
      public MappingProvider mappingProvider() {
        return mappingProvider;
      }

      @Override
      public Set<Option> options() {
        return EnumSet.noneOf(Option.class);
      }
    });
  }
}
