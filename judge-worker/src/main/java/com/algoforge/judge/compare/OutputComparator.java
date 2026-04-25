package com.algoforge.judge.compare;

import org.springframework.stereotype.Component;

@Component
public class OutputComparator {

    public boolean isAccepted(String expected, String actual) {
        return normalize(expected).equals(normalize(actual));
    }

    /**
     * 줄별 trailing whitespace 제거 + 끝 newline 무시.
     */
    private String normalize(String s) {
        if (s == null) return "";
        String[] lines = s.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
        StringBuilder sb = new StringBuilder();
        int lastNonEmpty = lines.length - 1;
        while (lastNonEmpty >= 0 && lines[lastNonEmpty].trim().isEmpty()) {
            lastNonEmpty--;
        }
        for (int i = 0; i <= lastNonEmpty; i++) {
            sb.append(stripTrailing(lines[i]));
            if (i != lastNonEmpty) sb.append('\n');
        }
        return sb.toString();
    }

    private String stripTrailing(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }
}
