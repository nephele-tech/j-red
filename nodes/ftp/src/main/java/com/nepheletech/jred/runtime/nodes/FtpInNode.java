/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED Nodes project.
 *
 * J-RED Nodes is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED Nodes is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED Nodes; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.jred.runtime.nodes;

import org.apache.camel.CamelContext;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;

public class FtpInNode extends AbstractNode implements HasCredentials {

  public FtpInNode(Flow flow, JtonObject config) {
    super(flow, config);
    // TODO Auto-generated constructor stub
  }

  @Override
  public void setCredentials(JtonObject credentials) {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected JtonElement onMessage(JtonObject msg) {
    // TODO Auto-generated method stub
    return null;
  }

}
