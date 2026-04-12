package com.ott.streaming.repository;

import com.ott.streaming.entity.Episode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {

    boolean existsBySeasonIdAndEpisodeNumber(Long seasonId, Integer episodeNumber);

    Optional<Episode> findByIdAndSeasonId(Long id, Long seasonId);

    List<Episode> findBySeasonIdInOrderByEpisodeNumberAsc(Collection<Long> seasonIds);
}
