package com.lidachui.simpleRequest.util;

/**
 * ExceptionUtil
 *
 * @author: lihuijie
 * @date: 2025/10/21 9:22
 * @version: 1.0
 */
public class ExceptionUtil {


  public static <R> R rethrow(Throwable throwable) {
    return (R) typeErasure(throwable);
  }

  @SuppressWarnings("unchecked")
  private static <R, T extends Throwable> R typeErasure(Throwable throwable) throws T {
    throw (T) throwable;
  }

}
