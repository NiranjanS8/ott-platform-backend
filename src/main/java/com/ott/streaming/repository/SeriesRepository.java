package com.ott.streaming.repository;

import com.ott.streaming.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SeriesRepository extends JpaRepository<Series, Long>, JpaSpecificationExecutor<Series> {
}
