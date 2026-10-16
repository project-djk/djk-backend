package com.djk.domain.keyword.repository;

import com.djk.domain.keyword.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeywordRepository extends JpaRepository<Keyword, Long>, KeywordRepositoryCustom {
}
