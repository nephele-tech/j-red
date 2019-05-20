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
package com.nepheletech.dao.domain;

/**
 * {@link Pageable} implementation to represent the absence of pagination information.
 *
 * @author Oliver Gierke (springframework)
 * @author ggeorg
 */
enum Unpaged implements Pageable {

  INSTANCE;

  /*
   * (non-Javadoc)
   * @see org.springframework.data.domain.Pageable#isPaged()
   */
  @Override
  public boolean isPaged() {
    return false;
  }

  /*
   * (non-Javadoc)
   * @see org.springframework.data.domain.Pageable#previousOrFirst()
   */
  @Override
  public Pageable previousOrFirst() {
    return this;
  }

  /*
   * (non-Javadoc)
   * @see org.springframework.data.domain.Pageable#next()
   */
  @Override
  public Pageable next() {
    return this;
  }

  /*
   * (non-Javadoc)
   * @see org.springframework.data.domain.Pageable#hasPrevious()
   */
  @Override
  public boolean hasPrevious() {
    return false;
  }

  /*
   * (non-Javadoc)
   * @see org.springframework.data.domain.Pageable#getSort()
   */
  @Override
  public Sort getSort() {
    return Sort.unsorted();
  }

  /*
   * (non-Javadoc)
   * @see org.springframework.data.domain.Pageable#getPageSize()
   */
  @Override
  public int getPageSize() {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * @see org.springframework.data.domain.Pageable#getPageNumber()
   */
  @Override
  public int getPageNumber() {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * @see org.springframework.data.domain.Pageable#getOffset()
   */
  @Override
  public long getOffset() {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * @see org.springframework.data.domain.Pageable#first()
   */
  @Override
  public Pageable first() {
    return this;
  }
}
