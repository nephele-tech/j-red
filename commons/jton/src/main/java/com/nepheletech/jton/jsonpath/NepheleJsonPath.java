package com.nepheletech.json.jsonpath;

import java.util.EnumSet;
import java.util.Set;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

public final class NepheleJsonPath {

  private NepheleJsonPath() {
  }

  public static void configure() {
    Configuration.setDefaults(new Configuration.Defaults() {
      private final JsonProvider jsonProvider = new NepheleJsonProvider();
      private final MappingProvider mappingProvider = new NepheleMappingProvider();

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
