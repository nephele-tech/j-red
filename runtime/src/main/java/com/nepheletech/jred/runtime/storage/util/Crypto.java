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