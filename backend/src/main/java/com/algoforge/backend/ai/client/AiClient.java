package com.algoforge.backend.ai.client;

import com.algoforge.backend.ai.dto.CounterExampleAiRequest;
import com.algoforge.backend.ai.dto.CounterExampleAiResponse;
import com.algoforge.backend.ai.dto.FeedbackAiRequest;
import com.algoforge.backend.ai.dto.FeedbackAiResponse;
import com.algoforge.backend.ai.dto.GenerateProblemAiRequest;
import com.algoforge.backend.ai.dto.GeneratedProblemAiResponse;
import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * AI 서버 호출 어댑터.
 *
 * - CircuitBreaker / TimeLimiter / Retry는 application.yml의 'aiServer' 인스턴스 설정을 따른다.
 * - TimeLimiter는 CompletableFuture 반환 시점에서 동작.
 * - 실패 시 BusinessException(AI_GENERATION_FAILED)로 통일하여 GlobalExceptionHandler가 502로 응답.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private static final String CB = "aiServer";

    private final WebClient aiWebClient;

    // ===== 문제 생성 =====
    @CircuitBreaker(name = CB, fallbackMethod = "generateProblemFallback")
    @TimeLimiter(name = CB)
    @Retry(name = CB)
    public CompletableFuture<GeneratedProblemAiResponse> generateProblem(GenerateProblemAiRequest req) {
        return post("/ai/problems/generate", req, GeneratedProblemAiResponse.class).toFuture();
    }

    @SuppressWarnings("unused")
    public CompletableFuture<GeneratedProblemAiResponse> generateProblemFallback(
            GenerateProblemAiRequest req, Throwable t
    ) {
        return failed("문제 생성", t);
    }

    // ===== 피드백 =====
    @CircuitBreaker(name = CB, fallbackMethod = "generateFeedbackFallback")
    @TimeLimiter(name = CB)
    @Retry(name = CB)
    public CompletableFuture<FeedbackAiResponse> generateFeedback(FeedbackAiRequest req) {
        return post("/ai/feedback", req, FeedbackAiResponse.class).toFuture();
    }

    @SuppressWarnings("unused")
    public CompletableFuture<FeedbackAiResponse> generateFeedbackFallback(FeedbackAiRequest req, Throwable t) {
        return failed("피드백 생성", t);
    }

    // ===== 반례 =====
    @CircuitBreaker(name = CB, fallbackMethod = "generateCounterExampleFallback")
    @TimeLimiter(name = CB)
    @Retry(name = CB)
    public CompletableFuture<CounterExampleAiResponse> generateCounterExamples(CounterExampleAiRequest req) {
        return post("/ai/counter-examples", req, CounterExampleAiResponse.class).toFuture();
    }

    @SuppressWarnings("unused")
    public CompletableFuture<CounterExampleAiResponse> generateCounterExampleFallback(
            CounterExampleAiRequest req, Throwable t
    ) {
        return failed("반례 생성", t);
    }

    // ===== private =====
    private <Req, Res> Mono<Res> post(String path, Req body, Class<Res> resType) {
        return aiWebClient.post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class).defaultIfEmpty("").flatMap(msg -> Mono.error(
                                new BusinessException(ErrorCode.AI_GENERATION_FAILED,
                                        "AI 서버 오류 (" + resp.statusCode().value() + "): " + truncate(msg)))))
                .bodyToMono(resType);
    }

    private <T> CompletableFuture<T> failed(String operation, Throwable t) {
        Throwable cause = (t instanceof CompletionException ce && ce.getCause() != null) ? ce.getCause() : t;
        if (cause instanceof BusinessException be) {
            log.warn("[AiClient] {} 실패(business): {}", operation, be.getMessage());
            return CompletableFuture.failedFuture(be);
        }
        if (cause instanceof WebClientResponseException w) {
            log.warn("[AiClient] {} 실패(http {}): {}", operation, w.getStatusCode(), truncate(w.getResponseBodyAsString()));
        } else {
            log.warn("[AiClient] {} 실패: {}", operation, cause.toString());
        }
        return CompletableFuture.failedFuture(
                new BusinessException(ErrorCode.AI_GENERATION_FAILED, operation + " 요청을 처리하지 못했습니다.")
        );
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
