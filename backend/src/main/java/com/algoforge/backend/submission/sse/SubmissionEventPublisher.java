package com.algoforge.backend.submission.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 제출 채점 결과 실시간 전송을 위한 in-memory SSE 브로드캐스터.
 *
 * 단일 인스턴스 가정. 멀티 인스턴스 운영 시 Redis Pub/Sub으로 확장 필요.
 *
 * 같은 submissionId에 대해 동시에 여러 클라이언트(다른 탭, 관리자 모니터 등)가
 * 구독 가능하도록 List<SseEmitter>를 유지한다.
 */
@Slf4j
@Component
public class SubmissionEventPublisher {

    private static final long DEFAULT_TIMEOUT_MS = 5 * 60 * 1000L; // 5분

    private final Map<Long, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long submissionId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        subscribers.computeIfAbsent(submissionId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(submissionId, emitter));
        emitter.onTimeout(() -> remove(submissionId, emitter));
        emitter.onError(t -> remove(submissionId, emitter));

        try {
            emitter.send(SseEmitter.event().name("subscribed").data("ok"));
        } catch (IOException e) {
            remove(submissionId, emitter);
        }
        return emitter;
    }

    public void publish(Long submissionId, String eventName, Object data) {
        List<SseEmitter> list = subscribers.get(submissionId);
        if (list == null || list.isEmpty()) return;
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.debug("SSE 전송 실패, emitter 제거: submissionId={}", submissionId);
                remove(submissionId, emitter);
            }
        }
    }

    public void complete(Long submissionId) {
        List<SseEmitter> list = subscribers.remove(submissionId);
        if (list == null) return;
        for (SseEmitter emitter : list) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    private void remove(Long submissionId, SseEmitter emitter) {
        List<SseEmitter> list = subscribers.get(submissionId);
        if (list == null) return;
        list.remove(emitter);
        if (list.isEmpty()) subscribers.remove(submissionId);
    }
}
