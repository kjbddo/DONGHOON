# algoforge/judge-java:21
# 채점 전용 최소 JDK 이미지
FROM eclipse-temurin:21-jdk-alpine

# nobody 사용자로 실행
USER 65534:65534
WORKDIR /work

# 채점 시:
#   docker run --rm \
#     --network=none --read-only --tmpfs /work:rw,size=64m \
#     --memory=256m --memory-swap=256m --cpus=1 --pids-limit=64 \
#     --cap-drop=ALL --security-opt=no-new-privileges \
#     -v {hostWork}:/host:ro \
#     algoforge/judge-java:21 \
#     sh -c "cp /host/Main.java /work/ && javac Main.java && java -Xss64m -Xmx256m Main"

CMD ["sh"]
