/*
 * Copyright NepheleTech and other contributorns, http://www.nephelerech.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
