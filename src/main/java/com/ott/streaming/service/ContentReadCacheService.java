package com.ott.streaming.service;

import com.ott.streaming.config.CacheNames;
import com.ott.streaming.dto.content.MoviePayload;
import com.ott.streaming.dto.content.SeriesPayload;
import com.ott.streaming.entity.Movie;
import com.ott.streaming.entity.Series;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.SeriesRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ContentReadCacheService {

    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;

    public ContentReadCacheService(MovieRepository movieRepository, SeriesRepository seriesRepository) {
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
    }

    @Cacheable(CacheNames.CONTENT_MOVIES)
    public List<MoviePayload> getMovies() {
        return movieRepository.findAll().stream()
                .map(this::toMoviePayload)
                .toList();
    }

    @Cacheable(cacheNames = CacheNames.CONTENT_MOVIE_BY_ID, key = "#id", unless = "#result == null")
    public MoviePayload getMovieById(Long id) {
        return movieRepository.findById(id)
                .map(this::toMoviePayload)
                .orElse(null);
    }

    @Cacheable(CacheNames.CONTENT_SERIES)
    public List<SeriesPayload> getSeriesList() {
        return seriesRepository.findAll().stream()
                .map(this::toSeriesPayload)
                .toList();
    }

    @Cacheable(cacheNames = CacheNames.CONTENT_SERIES_BY_ID, key = "#id", unless = "#result == null")
    public SeriesPayload getSeriesById(Long id) {
        return seriesRepository.findById(id)
                .map(this::toSeriesPayload)
                .orElse(null);
    }

    private MoviePayload toMoviePayload(Movie movie) {
        return new MoviePayload(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                formatDate(movie.getReleaseDate()),
                movie.getDurationMinutes(),
                movie.getMaturityRating(),
                movie.getLanguage(),
                movie.getAccessLevel(),
                movie.getCreatedAt(),
                movie.getUpdatedAt()
        );
    }

    private SeriesPayload toSeriesPayload(Series series) {
        return new SeriesPayload(
                series.getId(),
                series.getTitle(),
                series.getDescription(),
                formatDate(series.getReleaseDate()),
                formatDate(series.getEndDate()),
                series.getMaturityRating(),
                series.getLanguage(),
                series.getAccessLevel(),
                series.getCreatedAt(),
                series.getUpdatedAt()
        );
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : date.toString();
    }
}
