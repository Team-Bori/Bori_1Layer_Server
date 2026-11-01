package com.example.bori.domain.auth.exception;

import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;

public class LoginFailedException extends BusinessException {

    public LoginFailedException() {
        super(ErrorCode.LOGIN_FAILED);
    }
}
