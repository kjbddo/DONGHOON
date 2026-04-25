package com.algoforge.backend.problem.dto.admin;

import com.algoforge.backend.problem.domain.ProblemStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull ProblemStatus status) {}
