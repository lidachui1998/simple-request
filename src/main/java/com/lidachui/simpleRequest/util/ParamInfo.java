package com.lidachui.simpleRequest.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ParamInfo
 *
 * @author: lihuijie
 * @date: 2024/11/23 11:50
 * @version: 1.0
 */
@Getter
@AllArgsConstructor
public class ParamInfo {
    /** 参数类型 */
    private final Class<?> type;

    /** 参数值 */
    private final Object value;
}
