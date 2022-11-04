package com.example.finalproject.controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class ResponseDto<T> {
    private boolean success;
    private T data;
    private Error Error;


    public static <T>ResponseDto<T> success(T data) {

        return new ResponseDto<>(true, data, null);
    }

    public static <T>ResponseDto<T> fail(String code, String message) {
        return new ResponseDto<>(false, null, new Error(code, message));
    }

    @Getter
    @Setter
    @AllArgsConstructor
    static class Error{
        private String code;
        private String message;
    }
}

