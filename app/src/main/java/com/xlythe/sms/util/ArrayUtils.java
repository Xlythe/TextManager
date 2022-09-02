package com.xlythe.sms.util;

import java.lang.reflect.Array;
import java.util.Objects;

public class ArrayUtils {
  private ArrayUtils() {}

  public static <T> T[] concat(T[] a, T[] b) {
    int aLen = a.length;
    int bLen = b.length;

    @SuppressWarnings("unchecked")
    T[] c = (T[]) Array.newInstance(Objects.requireNonNull(a.getClass().getComponentType()), aLen + bLen);
    System.arraycopy(a, 0, c, 0, aLen);
    System.arraycopy(b, 0, c, aLen, bLen);

    return c;
  }
}
