package com.ott.streaming.repository;

import com.ott.streaming.entity.Series;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeriesRepository extends JpaRepository<Series, Long> {

    List<Series> findByTitleContainingIgnoreCase(String title);
}
