package com.algoforge.backend.ai.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 응답 텍스트에 들어 있는 literal escape 시퀀스(`\n`, `\t`, `\r`)를 실제 제어문자로 치환한다.
 *
 * <p>Gemini 등 일부 LLM 의 JSON 응답이 한 단계 덜 풀려 들어오면 description / inputDescription /
 * outputDescription / constraints 등 텍스트 필드 안에 두 글자 `\` + `n` 이 그대로 남아 화면에
 * `\n` 으로 보인다. 모든 백슬래시를 풀면 `\max`, `\le` 같은 KaTeX 명령이 깨지므로
 * `$...$` / `$$...$$` 수식 영역은 건드리지 않고 그 외 영역에서만 정규화한다.
 *
 * <p>ai-server 측에도 동일한 정규화가 있어 정상 응답에는 추가 변환이 발생하지 않는다.
 */
public final class AiTextNormalizer {

    private static final Pattern MATH_SPLIT =
            Pattern.compile("(\\$\\$[\\s\\S]*?\\$\\$|\\$[^\\n$]+\\$)");

    private AiTextNormalizer() {}

    public static String normalize(String input) {
        if (input == null || input.isEmpty()) return input;
        if (input.indexOf('\\') < 0) return input;

        Matcher m = MATH_SPLIT.matcher(input);
        StringBuilder out = new StringBuilder(input.length());
        int last = 0;
        while (m.find()) {
            out.append(replaceLiteralEscapes(input.substring(last, m.start())));
            out.append(input, m.start(), m.end()); // 수식 영역은 그대로 보존
            last = m.end();
        }
        out.append(replaceLiteralEscapes(input.substring(last)));
        return out.toString();
    }

    private static String replaceLiteralEscapes(String s) {
        if (s.indexOf('\\') < 0) return s;
        return s
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\r", "\n")
                .replace("\\t", "\t");
    }
}
