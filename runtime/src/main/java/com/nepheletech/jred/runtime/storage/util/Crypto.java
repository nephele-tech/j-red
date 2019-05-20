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
package com.nepheletech.jred.runtime.storage.util;

import static java.lang.String.valueOf;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

public final class Crypto {

  private static MessageDigest md = null;
  private static boolean mdSet = false;

  private static MessageDigest getMessageDigest() {
    if (!mdSet) {
      synchronized (Crypto.class) {
        if (!mdSet) {
          try {
            md = MessageDigest.getInstance("MD5");
          } catch (NoSuchAlgorithmException e) {}
          mdSet = true;
        }
      }
    }
    return md;
  }

  public static String createHashOf(String data) {
    final MessageDigest md = getMessageDigest();
    if (md != null) {
      byte[] thedigest = md.digest(data.getBytes());
      return Hex.encodeHexString(thedigest);
    } else {
      return valueOf(data.hashCode()); // use hash code instead
    }
  }

  private Crypto() {}
}