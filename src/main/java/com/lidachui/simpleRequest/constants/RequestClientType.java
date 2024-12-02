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
    REST_TEMPLATE("restTemplateHandler"),
    OKHTTP("okhttpClientHandler"),
    HESSIAN("hessianClientProxyFactory");

    private final String beanName;

    RequestClientType(String beanName) {
        this.beanName = beanName;
    }

}
