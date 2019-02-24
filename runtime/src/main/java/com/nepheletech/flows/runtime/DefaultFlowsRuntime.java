package com.nepheletech.flows.runtime;

import com.nepheletech.flows.runtime.storage.LocalFileSystemStorage;
import com.nepheletech.flows.runtime.storage.Storage;

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
