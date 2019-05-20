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

import java.io.Closeable;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public abstract class AbstractDaoFactory<T extends NepheleDao> implements Closeable {

  protected final EntityManagerFactory factory;

  protected AbstractDaoFactory(String persistenceUnitName, @SuppressWarnings("rawtypes") Map properties) {
    factory = Persistence.createEntityManagerFactory(persistenceUnitName, properties);
  }

  public final T create() {
    return create(factory.createEntityManager());
  }

  public final T create(@SuppressWarnings("rawtypes") Map map) {
    return create(factory.createEntityManager(map));
  }

  protected abstract T create(EntityManager em);

  public Map<String, Object> getProperties() { return factory.getProperties(); }

  public boolean isOpen() { return factory.isOpen(); }

  @Override
  public void close() {
    factory.close();
  }
}
