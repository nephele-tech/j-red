/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED API project.
 *
 * J-RED API is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED API; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;

public abstract class AbstractConfigurationNode extends AbstractNode implements ConfigurationNode {

  public AbstractConfigurationNode(final Flow flow, final JtonObject config) {
    super(flow, config);
  }
  
  @Override
  public final void configure() throws Exception {
    // no-op
  }

  @Override
  protected final JtonElement onMessage(JtonObject msg) {
    throw new UnsupportedOperationException();
  }
}
