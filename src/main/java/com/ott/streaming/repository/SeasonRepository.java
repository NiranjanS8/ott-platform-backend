package com.ott.streaming.repository;

import com.ott.streaming.entity.Season;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    boolean existsBySeriesIdAndSeasonNumber(Long seriesId, Integer seasonNumber);

    Optional<Season> findByIdAndSeriesId(Long id, Long seriesId);
}
