package com.lidachui.simpleRequest.constants;

/**
 * RequestClientType
 *
 * @author: lihuijie
 * @date: 2024/11/19 15:49
 * @version: 1.0
 */
public enum RequestClientType {
    REST_TEMPLATE("restTemplate", "restTemplateHandler"),
    OKHTTP("okhttp", "okhttpClientHandler");

    private final String type;
    private final String beanName;

    RequestClientType(String type, String beanName) {
        this.type = type;
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }
}
