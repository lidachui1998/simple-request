package com.lidachui.simpleRequest.constants;

/**
 * CacheEventType
 *
 * @author: lihuijie
 * @date: 2025/2/18 10:27
 * @version: 1.0
 */
public enum CacheEventType {
  PUT,    // 添加缓存
  GET,    // 获取缓存
  EXPIRE, // 缓存过期
  REMOVE  // 手动删除
}
