package com.example.bori.domain.session.deploy;

// 배포를 멈춰야 하는 이유. message는 학생 화면에 그대로 보여준다.
public class DeployFailure extends Exception {

    public DeployFailure(String message) {
        super(message);
    }
}
