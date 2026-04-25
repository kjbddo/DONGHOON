package com.algoforge.judge.meta;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 채점에 필요한 read-only 메타 정보를 JdbcTemplate으로 직접 조회한다.
 * worker는 DB를 갱신하지 않으며(=결과는 RabbitMQ로 백엔드에 위임),
 * JPA를 도입할 필요가 없으므로 가벼운 jdbc 접근만 사용한다.
 */
@Repository
@RequiredArgsConstructor
public class JudgeMetaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final RowMapper<LanguageSpec> LANG_ROW_MAPPER = (rs, i) -> new LanguageSpec(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("display_name"),
            rs.getString("file_extension"),
            rs.getBoolean("compile_required"),
            rs.getString("compile_command"),
            rs.getString("run_command"),
            rs.getString("docker_image"),
            rs.getDouble("time_multiplier")
    );

    private static final RowMapper<ProblemMeta> PROBLEM_ROW_MAPPER = (rs, i) -> new ProblemMeta(
            rs.getLong("id"),
            rs.getInt("time_limit_ms"),
            rs.getInt("memory_limit_mb")
    );

    private static final RowMapper<TestCaseRow> TC_ROW_MAPPER = (rs, i) -> new TestCaseRow(
            rs.getLong("id"),
            rs.getString("input"),
            rs.getString("expected_output"),
            rs.getBoolean("is_hidden"),
            rs.getInt("seq")
    );

    public Optional<LanguageSpec> findLanguageByName(String name) {
        try {
            LanguageSpec spec = jdbc.queryForObject(
                    "SELECT id, name, display_name, file_extension, compile_required, compile_command, " +
                            "       run_command, docker_image, time_multiplier " +
                            "  FROM code_languages WHERE name = :name",
                    Map.of("name", name),
                    LANG_ROW_MAPPER
            );
            return Optional.ofNullable(spec);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<ProblemMeta> findProblemMeta(long problemId) {
        try {
            ProblemMeta meta = jdbc.queryForObject(
                    "SELECT id, time_limit_ms, memory_limit_mb FROM problems WHERE id = :id",
                    Map.of("id", problemId),
                    PROBLEM_ROW_MAPPER
            );
            return Optional.ofNullable(meta);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<TestCaseRow> findTestCases(long problemId) {
        return jdbc.query(
                "SELECT id, input, expected_output, is_hidden, seq " +
                        "  FROM test_cases WHERE problem_id = :pid ORDER BY seq ASC, id ASC",
                Map.of("pid", problemId),
                TC_ROW_MAPPER
        );
    }
}
