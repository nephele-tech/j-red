/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED Commons project.
 *
 * J-RED Commons is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED Commons is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED Commons; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.dao;

import java.util.Map;

import javax.persistence.EntityManager;

public final class NepheleDaoFactory extends AbstractDaoFactory<NepheleDao> {
  private static final String PERSISTENCE_UNIT_NAME = "nephele-dao-persistence-unit";

  public NepheleDaoFactory(@SuppressWarnings("rawtypes") Map properties) {
    super(PERSISTENCE_UNIT_NAME, properties);
  }

  @Override
  protected final NepheleDao create(EntityManager em) {
    return new NepheleDao(factory.createEntityManager());
  }
}
