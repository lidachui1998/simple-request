package com.lidachui.simpleRequest.async;

/**
 * ResponseCallback
 *
 * @author: lihuijie
 * @date: 2024/12/3 22:53
 * @version: 1.0
 */
public interface ResponseCallback<T> {

    void onSuccess(T result);

    void onFailure(Throwable throwable);
}
