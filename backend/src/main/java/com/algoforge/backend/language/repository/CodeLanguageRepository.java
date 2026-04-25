package com.algoforge.backend.language.repository;

import com.algoforge.backend.language.domain.CodeLanguage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodeLanguageRepository extends JpaRepository<CodeLanguage, Long> {
    Optional<CodeLanguage> findByName(String name);
}
