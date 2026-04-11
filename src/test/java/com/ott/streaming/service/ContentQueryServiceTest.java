package com.ott.streaming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.content.MoviePayload;
import com.ott.streaming.dto.discovery.CatalogFilterInput;
import com.ott.streaming.dto.discovery.CatalogPagePayload;
import com.ott.streaming.dto.discovery.CatalogQueryInput;
import com.ott.streaming.dto.discovery.CatalogSortOption;
import com.ott.streaming.dto.discovery.PaginationInput;
import com.ott.streaming.entity.ContentAccessLevel;
import com.ott.streaming.entity.ContentType;
import com.ott.streaming.entity.Episode;
import com.ott.streaming.entity.Genre;
import com.ott.streaming.entity.Movie;
import com.ott.streaming.entity.Season;
import com.ott.streaming.entity.Series;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.EpisodeRepository;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.SeasonRepository;
import com.ott.streaming.repository.SeriesRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContentQueryServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private SeriesRepository seriesRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private UserSubscriptionService userSubscriptionService;

    private ContentQueryService contentQueryService;

    @BeforeEach
    void setUp() {
        contentQueryService = new ContentQueryService(
                movieRepository,
                seriesRepository,
                seasonRepository,
                episodeRepository,
                userSubscriptionService
        );
    }

    @Test
    void getMoviesReturnsPremiumAndFreeMetadataWithoutSubscriptionFilter() {
        Movie freeMovie = movie(1L, "Free Movie", ContentAccessLevel.FREE);
        Movie premiumMovie = movie(2L, "Premium Movie", ContentAccessLevel.PREMIUM);
        when(movieRepository.findAll()).thenReturn(List.of(freeMovie, premiumMovie));

        List<MoviePayload> payloads = contentQueryService.getMovies();

        assertThat(payloads).hasSize(2);
        assertThat(payloads.get(1).accessLevel()).isEqualTo(ContentAccessLevel.PREMIUM);
    }

    @Test
    void getMovieByIdBlocksPremiumContentWithoutSubscription() {
        Movie premiumMovie = movie(2L, "Premium Movie", ContentAccessLevel.PREMIUM);
        when(movieRepository.findById(2L)).thenReturn(Optional.of(premiumMovie));
        when(userSubscriptionService.hasPremiumAccess(null)).thenReturn(false);

        assertThatThrownBy(() -> contentQueryService.getMovieById(null, 2L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Premium subscription required to access this content");
    }

    @Test
    void getMovieByIdAllowsPremiumContentWithSubscription() {
        Movie premiumMovie = movie(2L, "Premium Movie", ContentAccessLevel.PREMIUM);
        when(movieRepository.findById(2L)).thenReturn(Optional.of(premiumMovie));
        when(userSubscriptionService.hasPremiumAccess("member@example.com")).thenReturn(true);

        MoviePayload payload = contentQueryService.getMovieById("member@example.com", 2L);

        assertThat(payload).isNotNull();
        assertThat(payload.accessLevel()).isEqualTo(ContentAccessLevel.PREMIUM);
    }

    @Test
    void getSeasonByIdBlocksPremiumParentSeriesWithoutSubscription() {
        Series premiumSeries = series(9L, "Premium Series", ContentAccessLevel.PREMIUM);
        Season season = season(3L, premiumSeries);
        when(seasonRepository.findById(3L)).thenReturn(Optional.of(season));
        when(userSubscriptionService.hasPremiumAccess(null)).thenReturn(false);

        assertThatThrownBy(() -> contentQueryService.getSeasonById(null, 3L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Premium subscription required to access this content");
    }

    @Test
    void getEpisodeByIdBlocksPremiumParentSeriesWithoutSubscription() {
        Series premiumSeries = series(9L, "Premium Series", ContentAccessLevel.PREMIUM);
        Season season = season(3L, premiumSeries);
        Episode episode = episode(4L, season);

        when(episodeRepository.findById(4L)).thenReturn(Optional.of(episode));
        when(userSubscriptionService.hasPremiumAccess(null)).thenReturn(false);

        assertThatThrownBy(() -> contentQueryService.getEpisodeById(null, 4L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Premium subscription required to access this content");
    }

    @Test
    void discoverCatalogSearchesTitleAcrossMoviesAndSeries() {
        when(movieRepository.findByTitleContainingIgnoreCase("dark")).thenReturn(List.of(
                movie(1L, "Dark City", ContentAccessLevel.FREE)
        ));
        when(seriesRepository.findByTitleContainingIgnoreCase("dark")).thenReturn(List.of(
                series(2L, "Dark", ContentAccessLevel.PREMIUM)
        ));

        CatalogPagePayload page = contentQueryService.discoverCatalog(new CatalogQueryInput(
                " dark ",
                null,
                CatalogSortOption.TITLE_ASC,
                new PaginationInput(0, 10)
        ));

        assertThat(page.items()).hasSize(2);
        assertThat(page.items().get(0).title()).isEqualTo("Dark");
        assertThat(page.items().get(0).contentType()).isEqualTo(ContentType.SERIES);
        assertThat(page.items().get(1).title()).isEqualTo("Dark City");
        assertThat(page.pageInfo().totalElements()).isEqualTo(2);
        assertThat(page.pageInfo().totalPages()).isEqualTo(1);
    }

    @Test
    void discoverCatalogPaginatesCombinedResults() {
        when(movieRepository.findAll()).thenReturn(List.of(
                movie(1L, "Alpha", ContentAccessLevel.FREE),
                movie(2L, "Beta", ContentAccessLevel.FREE)
        ));
        when(seriesRepository.findAll()).thenReturn(List.of(
                series(3L, "Gamma", ContentAccessLevel.FREE)
        ));

        CatalogPagePayload page = contentQueryService.discoverCatalog(new CatalogQueryInput(
                null,
                null,
                CatalogSortOption.TITLE_ASC,
                new PaginationInput(1, 2)
        ));

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().title()).isEqualTo("Gamma");
        assertThat(page.pageInfo().page()).isEqualTo(1);
        assertThat(page.pageInfo().size()).isEqualTo(2);
        assertThat(page.pageInfo().totalElements()).isEqualTo(3);
        assertThat(page.pageInfo().totalPages()).isEqualTo(2);
        assertThat(page.pageInfo().hasNext()).isFalse();
        assertThat(page.pageInfo().hasPrevious()).isTrue();
    }

    @Test
    void discoverCatalogFiltersByGenreReleaseYearTypeAndAccessLevel() {
        Movie matchingMovie = movie(1L, "Arrival", ContentAccessLevel.PREMIUM);
        matchingMovie.setGenres(java.util.Set.of(genre(10L, "Sci-Fi")));
        matchingMovie.setReleaseDate(LocalDate.parse("2021-02-10"));

        Movie wrongGenreMovie = movie(2L, "Drama Film", ContentAccessLevel.PREMIUM);
        wrongGenreMovie.setGenres(java.util.Set.of(genre(11L, "Drama")));
        wrongGenreMovie.setReleaseDate(LocalDate.parse("2021-03-10"));

        Series wrongTypeSeries = series(3L, "Arrival Series", ContentAccessLevel.PREMIUM);
        wrongTypeSeries.setGenres(java.util.Set.of(genre(10L, "Sci-Fi")));
        wrongTypeSeries.setReleaseDate(LocalDate.parse("2021-04-10"));

        when(movieRepository.findAll()).thenReturn(List.of(matchingMovie, wrongGenreMovie));
        when(seriesRepository.findAll()).thenReturn(List.of(wrongTypeSeries));

        CatalogPagePayload page = contentQueryService.discoverCatalog(new CatalogQueryInput(
                null,
                new CatalogFilterInput(10L, null, 2021, null, null, ContentType.MOVIE, ContentAccessLevel.PREMIUM),
                CatalogSortOption.TITLE_ASC,
                new PaginationInput(0, 10)
        ));

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().id()).isEqualTo(1L);
        assertThat(page.items().getFirst().contentType()).isEqualTo(ContentType.MOVIE);
        assertThat(page.items().getFirst().accessLevel()).isEqualTo(ContentAccessLevel.PREMIUM);
    }

    @Test
    void discoverCatalogFiltersSeriesByReleaseYearAndAccessLevel() {
        Series matchingSeries = series(4L, "Dark", ContentAccessLevel.FREE);
        matchingSeries.setReleaseDate(LocalDate.parse("2017-12-01"));

        Series wrongYearSeries = series(5L, "1899", ContentAccessLevel.FREE);
        wrongYearSeries.setReleaseDate(LocalDate.parse("2022-11-17"));

        when(movieRepository.findAll()).thenReturn(List.of());
        when(seriesRepository.findAll()).thenReturn(List.of(matchingSeries, wrongYearSeries));

        CatalogPagePayload page = contentQueryService.discoverCatalog(new CatalogQueryInput(
                null,
                new CatalogFilterInput(null, null, 2017, null, null, ContentType.SERIES, ContentAccessLevel.FREE),
                CatalogSortOption.TITLE_ASC,
                new PaginationInput(0, 10)
        ));

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().id()).isEqualTo(4L);
        assertThat(page.items().getFirst().contentType()).isEqualTo(ContentType.SERIES);
    }

    private Movie movie(Long id, String title, ContentAccessLevel accessLevel) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setTitle(title);
        movie.setAccessLevel(accessLevel);
        movie.setReleaseDate(LocalDate.parse("2026-04-01"));
        ReflectionTestUtils.setField(movie, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
        ReflectionTestUtils.setField(movie, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
        return movie;
    }

    private Series series(Long id, String title, ContentAccessLevel accessLevel) {
        Series series = new Series();
        series.setId(id);
        series.setTitle(title);
        series.setAccessLevel(accessLevel);
        series.setReleaseDate(LocalDate.parse("2026-04-01"));
        ReflectionTestUtils.setField(series, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
        ReflectionTestUtils.setField(series, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
        return series;
    }

    private Season season(Long id, Series series) {
        Season season = new Season();
        season.setId(id);
        season.setTitle("Season 1");
        season.setSeasonNumber(1);
        season.setSeries(series);
        ReflectionTestUtils.setField(season, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
        ReflectionTestUtils.setField(season, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
        return season;
    }

    private Episode episode(Long id, Season season) {
        Episode episode = new Episode();
        episode.setId(id);
        episode.setSeason(season);
        episode.setTitle("Episode 1");
        episode.setEpisodeNumber(1);
        ReflectionTestUtils.setField(episode, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
        ReflectionTestUtils.setField(episode, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
        return episode;
    }

    private Genre genre(Long id, String name) {
        Genre genre = new Genre();
        genre.setId(id);
        genre.setName(name);
        ReflectionTestUtils.setField(genre, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
        ReflectionTestUtils.setField(genre, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
        return genre;
    }

}
