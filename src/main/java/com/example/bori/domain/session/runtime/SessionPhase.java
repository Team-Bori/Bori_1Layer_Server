package com.example.bori.domain.session.runtime;

public enum SessionPhase {
    // L1이 GitHub에서 zip을 받는 중 (아직 L2에 아무것도 안 보냄)
    FETCHING,
    // L2에 deploy를 보냈고 설치·실행 대기
    DEPLOYING,
    // 실행 중. 15분 유휴 판정 대상
    RUNNING
}
