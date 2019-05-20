/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED Runtime project.
 *
 * J-RED Runtime is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED Runtime is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED Runtime; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
