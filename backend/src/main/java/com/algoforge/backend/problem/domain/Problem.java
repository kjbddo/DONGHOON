package com.algoforge.backend.problem.domain;

import com.algoforge.backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Entity
@Table(name = "problems")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Problem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "input_description", nullable = false, columnDefinition = "TEXT")
    private String inputDescription;

    @Column(name = "output_description", nullable = false, columnDefinition = "TEXT")
    private String outputDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "constraints", nullable = false, columnDefinition = "jsonb")
    private List<String> constraints = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "examples", nullable = false, columnDefinition = "jsonb")
    private List<Example> examples = new ArrayList<>();

    @Column(name = "time_limit_ms", nullable = false)
    private int timeLimitMs;

    @Column(name = "memory_limit_mb", nullable = false)
    private int memoryLimitMb;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProblemStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private ProblemSourceType sourceType;

    @Column(name = "is_ai_generated", nullable = false)
    private boolean aiGenerated;

    @Column(name = "ai_model_name", length = 100)
    private String aiModelName;

    @Column(name = "ai_prompt_version", length = 50)
    private String aiPromptVersion;

    @Column(name = "generated_by_user_id")
    private Long generatedByUserId;

    @Column(name = "quality_score", precision = 3, scale = 2)
    private BigDecimal qualityScore;

    @Column(name = "report_count", nullable = false)
    private int reportCount;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "problem_category_map",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<ProblemCategory> categories = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "problem_tag_map",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<ProblemTag> tags = new HashSet<>();

    @Builder
    private Problem(String title, String slug, String description,
                    String inputDescription, String outputDescription,
                    List<String> constraints, List<Example> examples,
                    int timeLimitMs, int memoryLimitMb,
                    Difficulty difficulty, ProblemStatus status,
                    ProblemSourceType sourceType,
                    boolean aiGenerated, String aiModelName, String aiPromptVersion,
                    Long generatedByUserId) {
        this.title = title;
        this.slug = slug;
        this.description = description;
        this.inputDescription = inputDescription;
        this.outputDescription = outputDescription;
        this.constraints = constraints == null ? new ArrayList<>() : new ArrayList<>(constraints);
        this.examples = examples == null ? new ArrayList<>() : new ArrayList<>(examples);
        this.timeLimitMs = timeLimitMs <= 0 ? 2000 : timeLimitMs;
        this.memoryLimitMb = memoryLimitMb <= 0 ? 256 : memoryLimitMb;
        this.difficulty = difficulty;
        this.status = status == null ? ProblemStatus.DRAFT : status;
        this.sourceType = sourceType == null ? ProblemSourceType.ADMIN_CREATED : sourceType;
        this.aiGenerated = aiGenerated;
        this.aiModelName = aiModelName;
        this.aiPromptVersion = aiPromptVersion;
        this.generatedByUserId = generatedByUserId;
        this.reportCount = 0;
        if (aiGenerated) {
            this.generatedAt = OffsetDateTime.now();
        }
    }

    // ===== 도메인 동작 =====
    public void update(String title, String description,
                       String inputDescription, String outputDescription,
                       List<String> constraints, List<Example> examples,
                       int timeLimitMs, int memoryLimitMb,
                       Difficulty difficulty) {
        this.title = title;
        this.description = description;
        this.inputDescription = inputDescription;
        this.outputDescription = outputDescription;
        this.constraints = constraints == null ? new ArrayList<>() : new ArrayList<>(constraints);
        this.examples = examples == null ? new ArrayList<>() : new ArrayList<>(examples);
        if (timeLimitMs > 0) this.timeLimitMs = timeLimitMs;
        if (memoryLimitMb > 0) this.memoryLimitMb = memoryLimitMb;
        if (difficulty != null) this.difficulty = difficulty;
    }

    public void changeStatus(ProblemStatus next) {
        if (next != null) this.status = next;
    }

    public void replaceCategories(Set<ProblemCategory> next) {
        this.categories.clear();
        if (next != null) this.categories.addAll(next);
    }

    public void replaceTags(Set<ProblemTag> next) {
        this.tags.clear();
        if (next != null) this.tags.addAll(next);
    }

    public void softDelete() {
        this.status = ProblemStatus.DELETED;
    }

    public boolean isPubliclyVisible() {
        return this.status == ProblemStatus.PUBLIC;
    }

    public void incrementReportCount() {
        this.reportCount++;
    }
}
