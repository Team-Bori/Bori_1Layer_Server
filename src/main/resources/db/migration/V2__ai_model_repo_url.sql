-- 검증 모델도 학생 모델과 같은 규칙(GitHub 공개 저장소 zip, 최상위 bori.yaml)으로 배포한다.
ALTER TABLE ai_model ADD COLUMN repo_url VARCHAR(500) NOT NULL;
COMMENT ON COLUMN ai_model.repo_url IS 'GitHub 저장소 링크';
