package com.algoforge.backend.language.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "code_languages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodeLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 20)
    private String name;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "file_extension", nullable = false, length = 10)
    private String fileExtension;

    @Column(name = "compile_required", nullable = false)
    private boolean compileRequired;

    @Column(name = "compile_command", columnDefinition = "TEXT")
    private String compileCommand;

    @Column(name = "run_command", nullable = false, columnDefinition = "TEXT")
    private String runCommand;

    @Column(name = "docker_image", nullable = false, length = 200)
    private String dockerImage;

    @Column(name = "time_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal timeMultiplier;
}
