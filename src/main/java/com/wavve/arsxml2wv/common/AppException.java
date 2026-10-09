package com.wavve.arsxml2wv.common;

/** 사용자에게 보여 줄 처리 오류. 메시지는 콘솔에 "error: ..." 로 출력된다. */
public class AppException extends Exception {
    private static final long serialVersionUID = 1L;

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
