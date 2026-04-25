# algoforge-judge-worker

채점 워커. RabbitMQ `judge.submission` 큐를 소비해 사용자 코드를 격리된 Docker 컨테이너에서 실행하고 결과를 `judge.result` 큐로 발행합니다.

## 현재 상태

스켈레톤 단계입니다. 큐 컨슈머/발행 골격, Docker 샌드박스 호출 인터페이스, 출력 비교기까지만 구성되어 있습니다.
실제 Docker 실행 로직은 `DockerSandboxRunner.run()`에 단계적으로 채웁니다.

## 의존성

- Docker (호스트에 설치 필요)
- Postgres / RabbitMQ (인프라 docker-compose로 기동)
- 언어별 채점 이미지 (`judge-images/` 의 Dockerfile로 빌드)

## 실행

```bash
./gradlew bootRun
```

## 보안 체크리스트 (운영 전 필독)

- [ ] `--network=none` 적용
- [ ] `--read-only` + `tmpfs /work`
- [ ] `--memory` / `--cpus` / `--pids-limit`
- [ ] `--cap-drop=ALL --security-opt=no-new-privileges`
- [ ] non-root 사용자 (`--user 65534:65534`)
- [ ] seccomp 프로파일 적용 (위험 syscall 차단)
- [ ] 출력 사이즈 상한 (64MB)
- [ ] 호스트 마운트는 read-only
- [ ] 컨테이너 단위 wall-clock + 호스트 timeout 이중 보호
- [ ] cgroups v2 메모리 제한 검증
