package com.algoforge.judge.meta;

public record TestCaseRow(
        Long id,
        String input,
        String expectedOutput,
        boolean hidden,
        int seq
) {}
