package com.example.demo.model.requests.validator;

public interface RequestValidator<T> {

    boolean isRequestValid(T req);

}
