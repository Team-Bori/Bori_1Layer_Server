package com.example.bori.domain.user.exception;

import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
