package com.example.bori.domain.user.exception;

import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException() {
        super(ErrorCode.DUPLICATE_EMAIL);
    }
}
