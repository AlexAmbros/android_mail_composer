package com.example.quickstart;

/**
 * Created by alexander on 20.04.17.
 */

public interface SendMailListener<T> {
    void onStartMailSending();
    void onEndMailSending(T result);
    void onCancelMailSending(Exception exception);
}
