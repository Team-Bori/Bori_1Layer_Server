-- 사용자당 진행 중 세션 1개 (한 번에 한 보드만 사용)
CREATE UNIQUE INDEX uk_session_active_user ON board_session (user_id) WHERE ended_at IS NULL;
