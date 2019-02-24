package com.nepheletech.jred.runtime;

import com.nepheletech.jred.runtime.storage.LocalFileSystemStorage;
import com.nepheletech.jred.runtime.storage.Storage;

public class DefaultFlowsRuntime extends AbstractFlowsRuntime {

  public DefaultFlowsRuntime(String baseDir) {
    this(new LocalFileSystemStorage(baseDir));
  }

  public DefaultFlowsRuntime(Storage storage) {
    super(storage);

    // load flows
    this.loadFlows();
  }
}
