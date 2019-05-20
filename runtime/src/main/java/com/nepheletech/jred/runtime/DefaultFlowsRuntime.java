/*
 *     This file is part of J-RED project.
 *
 *   J-RED is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   J-RED is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with J-RED.  If not, see <https://www.gnu.org/licenses/>.
 */
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
