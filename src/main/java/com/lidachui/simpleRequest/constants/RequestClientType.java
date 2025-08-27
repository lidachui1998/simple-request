package com.lidachui.simpleRequest.constants;

import lombok.Getter;

/**
 * RequestClientType
 *
 * @author: lihuijie
 * @date: 2024/11/19 15:49
 * @version: 1.0
 */
@Getter
public enum RequestClientType {
    REST_TEMPLATE("http", "restTemplateHandler"),
    OKHTTP("http","okhttpClientHandler");

    private final String type;
    private final String beanName;

    RequestClientType(String type, String beanName) {
        this.type = type;
        this.beanName = beanName;
    }

}
