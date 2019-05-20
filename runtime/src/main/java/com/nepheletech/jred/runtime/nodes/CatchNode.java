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
package com.nepheletech.jred.runtime.nodes;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;

public final class CatchNode extends AbstractNode {
  private final HashSet<String> scope;
  private final boolean uncaught;

  public CatchNode(Flow flow, JtonObject config) {
    super(flow, config);

    final JtonArray scope = config.getAsJtonArray("scope", false);
    if (scope != null) {
      this.scope = new HashSet<>();
      scope.forEach(elem -> this.scope.add(elem.asString()));
    } else {
      this.scope = null;
    }

    this.uncaught = config.getAsBoolean("uncaught", false);
  }

  public Set<String> getScope() { return (scope != null) ? Collections.unmodifiableSet(scope) : null; }

  public boolean isUncaught() { return uncaught; }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    send(msg);
  }
}
