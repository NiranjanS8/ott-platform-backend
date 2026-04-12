package com.ott.streaming.repository;

import com.ott.streaming.entity.Series;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SeriesRepository extends JpaRepository<Series, Long>, JpaSpecificationExecutor<Series> {

    @EntityGraph(attributePaths = {"genres", "cast", "directors"})
    List<Series> findByIdIn(Collection<Long> ids);
}
