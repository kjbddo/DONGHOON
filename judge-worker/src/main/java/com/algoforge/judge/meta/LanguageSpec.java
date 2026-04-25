package com.algoforge.judge.meta;

/**
 * code_languages 테이블 한 행.
 * compileCommand / runCommand에는 {MEM} 플레이스홀더가 들어 있을 수 있다.
 */
public record LanguageSpec(
        Long id,
        String name,
        String displayName,
        String fileExtension,
        boolean compileRequired,
        String compileCommand,
        String runCommand,
        String dockerImage,
        double timeMultiplier
) {
    /**
     * 언어별 기본 소스 파일명. (Java만 클래스 이름과 일치해야 하므로 Main.java 고정)
     */
    public String sourceFileName() {
        return switch (name) {
            case "JAVA" -> "Main.java";
            case "PYTHON" -> "main.py";
            case "CPP" -> "main.cpp";
            case "JAVASCRIPT" -> "main.js";
            default -> "main" + (fileExtension == null ? "" : fileExtension);
        };
    }

    public String renderRunCommand(int memoryMb) {
        return runCommand == null ? "" : runCommand.replace("{MEM}", String.valueOf(memoryMb));
    }

    public String renderCompileCommand() {
        return compileCommand == null ? null : compileCommand;
    }
}
