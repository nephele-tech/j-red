package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.json.JsonObject;

public interface HasCredentials {
  void setCredentials(JsonObject credentials);
}
