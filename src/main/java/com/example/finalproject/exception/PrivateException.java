package com.example.finalproject.exception;

import lombok.Getter;


//사용자 정의 예외 예외를 강제 하지 않고 예외의 정보 제공이 목적이기 때문에 Exception이 아닌 RuntimeException을 상속
@Getter
public class PrivateException extends RuntimeException {
    private StatusCode statusCode;

    public PrivateException(StatusCode statusCode) {
        //지정된 상세 메시지로 새로운 런타임 예외를 구축합니다.
        super(statusCode.getStatusMsg());
        this.statusCode = statusCode;
    }
}
