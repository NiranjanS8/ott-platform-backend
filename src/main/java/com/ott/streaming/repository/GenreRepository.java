package com.ott.streaming.repository;

import com.ott.streaming.entity.Genre;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Genre> findByNameIgnoreCase(String name);
}
