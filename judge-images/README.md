# judge-images

언어별 채점 전용 Docker 이미지 정의.

## 빌드

```bash
docker build -t algoforge/judge-java:21    java/
docker build -t algoforge/judge-python:3.12 python/
docker build -t algoforge/judge-cpp:13     cpp/
docker build -t algoforge/judge-node:20    node/
```

## 채점 시 권장 실행 옵션

```
docker run --rm \
  --network=none \
  --read-only \
  --tmpfs /work:rw,size=64m \
  --memory=256m --memory-swap=256m \
  --cpus=1.0 \
  --pids-limit=64 \
  --cap-drop=ALL \
  --security-opt=no-new-privileges \
  --user 65534:65534 \
  -v {host_workdir}:/host:ro \
  algoforge/judge-java:21 \
  sh -c "cp /host/Main.java /work/ && cd /work && javac Main.java && timeout 2 java -Xmx256m Main"
```

stdin은 호출 측에서 redirection하고, stdout은 캡처 후 64MB 초과 시 강제 종료합니다.

## 보안 체크리스트

- [ ] 모든 이미지를 non-root(`65534:65534`)로 실행
- [ ] `--network=none`
- [ ] read-only root + tmpfs 작업 디렉터리만 쓰기
- [ ] memory/cpu/pids 제한
- [ ] seccomp 프로파일 적용 권장 (운영)
- [ ] `--cap-drop=ALL --security-opt=no-new-privileges`
