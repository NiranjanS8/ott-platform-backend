package com.ott.streaming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.content.CreateGenreInput;
import com.ott.streaming.dto.content.CreateMovieInput;
import com.ott.streaming.dto.content.CreatePersonInput;
import com.ott.streaming.dto.content.CreateEpisodeInput;
import com.ott.streaming.dto.content.CreateSeasonInput;
import com.ott.streaming.dto.content.CreateSeriesInput;
import com.ott.streaming.dto.content.UpdateGenreInput;
import com.ott.streaming.dto.content.UpdateMovieInput;
import com.ott.streaming.dto.content.UpdatePersonInput;
import com.ott.streaming.dto.content.UpdateEpisodeInput;
import com.ott.streaming.dto.content.UpdateSeasonInput;
import com.ott.streaming.dto.content.UpdateSeriesInput;
import com.ott.streaming.entity.Episode;
import com.ott.streaming.entity.Genre;
import com.ott.streaming.entity.Movie;
import com.ott.streaming.entity.Person;
import com.ott.streaming.entity.Season;
import com.ott.streaming.entity.Series;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.EpisodeRepository;
import com.ott.streaming.repository.GenreRepository;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.PersonRepository;
import com.ott.streaming.repository.SeasonRepository;
import com.ott.streaming.repository.SeriesRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContentAdminServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private SeriesRepository seriesRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private EpisodeRepository episodeRepository;

    private ContentAdminService contentAdminService;

    @BeforeEach
    void setUp() {
        contentAdminService = new ContentAdminService(
                genreRepository,
                personRepository,
                movieRepository,
                seriesRepository,
                seasonRepository,
                episodeRepository
        );
    }

    @Test
    void createGenreNormalizesNameAndRejectsDuplicates() {
        when(genreRepository.existsByNameIgnoreCase("Action")).thenReturn(false);
        when(genreRepository.save(any(Genre.class))).thenAnswer(invocation -> {
            Genre genre = invocation.getArgument(0);
            genre.setId(1L);
            ReflectionTestUtils.setField(genre, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
            ReflectionTestUtils.setField(genre, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
            return genre;
        });

        var payload = contentAdminService.createGenre(new CreateGenreInput("  Action  "));

        assertThat(payload.id()).isEqualTo(1L);
        assertThat(payload.name()).isEqualTo("Action");
    }

    @Test
    void createGenreThrowsWhenNameAlreadyExists() {
        when(genreRepository.existsByNameIgnoreCase("Action")).thenReturn(true);

        assertThatThrownBy(() -> contentAdminService.createGenre(new CreateGenreInput("Action")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Genre already exists");
    }

    @Test
    void updateGenreRejectsMissingGenre() {
        when(genreRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contentAdminService.updateGenre(3L, new UpdateGenreInput("Drama")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Genre not found");
    }

    @Test
    void createPersonNormalizesOptionalFields() {
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> {
            Person person = invocation.getArgument(0);
            person.setId(7L);
            ReflectionTestUtils.setField(person, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
            ReflectionTestUtils.setField(person, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
            return person;
        });

        var payload = contentAdminService.createPerson(new CreatePersonInput("  Keanu Reeves  ", "  Actor  ", "   "));

        assertThat(payload.id()).isEqualTo(7L);
        assertThat(payload.name()).isEqualTo("Keanu Reeves");
        assertThat(payload.biography()).isEqualTo("Actor");
        assertThat(payload.profileImageUrl()).isNull();
    }

    @Test
    void deletePersonRemovesExistingRecord() {
        when(personRepository.existsById(9L)).thenReturn(true);

        assertThat(contentAdminService.deletePerson(9L)).isTrue();
        verify(personRepository).deleteById(9L);
    }

    @Test
    void createMovieAssignsGenresCastAndDirectors() {
        Genre genre = genre(1L, "Action");
        Person actor = person(2L, "Keanu Reeves");
        Person director = person(3L, "Lana Wachowski");

        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));
        when(personRepository.findAllById(Set.of(2L))).thenReturn(List.of(actor));
        when(personRepository.findAllById(Set.of(3L))).thenReturn(List.of(director));
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> {
            Movie movie = invocation.getArgument(0);
            movie.setId(10L);
            ReflectionTestUtils.setField(movie, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
            ReflectionTestUtils.setField(movie, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
            return movie;
        });

        var payload = contentAdminService.createMovie(new CreateMovieInput(
                "  The Matrix  ",
                "  Sci-fi action film  ",
                "1999-03-31",
                136,
                "R",
                Set.of(1L),
                Set.of(2L),
                Set.of(3L)
        ));

        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(movieCaptor.capture());
        Movie savedMovie = movieCaptor.getValue();

        assertThat(savedMovie.getTitle()).isEqualTo("The Matrix");
        assertThat(savedMovie.getDescription()).isEqualTo("Sci-fi action film");
        assertThat(savedMovie.getReleaseDate()).isEqualTo(LocalDate.parse("1999-03-31"));
        assertThat(savedMovie.getGenres()).containsExactly(genre);
        assertThat(savedMovie.getCast()).containsExactly(actor);
        assertThat(savedMovie.getDirectors()).containsExactly(director);
        assertThat(payload.id()).isEqualTo(10L);
        assertThat(payload.title()).isEqualTo("The Matrix");
    }

    @Test
    void createMovieRejectsMissingRelatedRecords() {
        when(genreRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(genre(1L, "Action")));

        assertThatThrownBy(() -> contentAdminService.createMovie(new CreateMovieInput(
                "Movie",
                null,
                null,
                null,
                null,
                Set.of(1L, 2L),
                Set.of(),
                Set.of()
        )))
                .isInstanceOf(ApiException.class)
                .hasMessage("One or more genres were not found");
    }

    @Test
    void updateSeriesRejectsMissingSeries() {
        when(seriesRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contentAdminService.updateSeries(5L, new UpdateSeriesInput(
                "Dark",
                null,
                null,
                null,
                null,
                Set.of(),
                Set.of(),
                Set.of()
        )))
                .isInstanceOf(ApiException.class)
                .hasMessage("Series not found");
    }

    @Test
    void createSeriesAssignsRelations() {
        Genre genre = genre(1L, "Drama");
        Person actor = person(2L, "Actor");
        Person director = person(3L, "Director");

        when(genreRepository.findAllById(Set.of(1L))).thenReturn(List.of(genre));
        when(personRepository.findAllById(Set.of(2L))).thenReturn(List.of(actor));
        when(personRepository.findAllById(Set.of(3L))).thenReturn(List.of(director));
        when(seriesRepository.save(any(Series.class))).thenAnswer(invocation -> {
            Series series = invocation.getArgument(0);
            series.setId(20L);
            ReflectionTestUtils.setField(series, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
            ReflectionTestUtils.setField(series, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
            return series;
        });

        var payload = contentAdminService.createSeries(new CreateSeriesInput(
                "  Dark  ",
                "  Mystery series  ",
                "2017-12-01",
                "2020-06-27",
                "TV-MA",
                Set.of(1L),
                Set.of(2L),
                Set.of(3L)
        ));

        assertThat(payload.id()).isEqualTo(20L);
        assertThat(payload.title()).isEqualTo("Dark");
        assertThat(payload.releaseDate()).isEqualTo("2017-12-01");
        assertThat(payload.endDate()).isEqualTo("2020-06-27");
    }

    private Genre genre(Long id, String name) {
        Genre genre = new Genre();
        genre.setId(id);
        genre.setName(name);
        return genre;
    }

    private Person person(Long id, String name) {
        Person person = new Person();
        person.setId(id);
        person.setName(name);
        return person;
    }

    @Test
    void createSeasonRejectsMissingSeries() {
        when(seriesRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contentAdminService.createSeason(new CreateSeasonInput(99L, "Season 1", 1)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Series not found");
    }

    @Test
    void createSeasonRejectsDuplicateOrderWithinSeries() {
        Series series = new Series();
        series.setId(5L);
        when(seriesRepository.findById(5L)).thenReturn(Optional.of(series));
        when(seasonRepository.existsBySeriesIdAndSeasonNumber(5L, 1)).thenReturn(true);

        assertThatThrownBy(() -> contentAdminService.createSeason(new CreateSeasonInput(5L, "Season 1", 1)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Season number already exists for this series");
    }

    @Test
    void createSeasonPersistsWithParentSeries() {
        Series series = new Series();
        series.setId(5L);
        when(seriesRepository.findById(5L)).thenReturn(Optional.of(series));
        when(seasonRepository.existsBySeriesIdAndSeasonNumber(5L, 2)).thenReturn(false);
        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> {
            Season season = invocation.getArgument(0);
            season.setId(8L);
            ReflectionTestUtils.setField(season, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
            ReflectionTestUtils.setField(season, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
            return season;
        });

        var payload = contentAdminService.createSeason(new CreateSeasonInput(5L, "  Season 2 ", 2));

        assertThat(payload.id()).isEqualTo(8L);
        assertThat(payload.seriesId()).isEqualTo(5L);
        assertThat(payload.title()).isEqualTo("Season 2");
        assertThat(payload.seasonNumber()).isEqualTo(2);
    }

    @Test
    void createEpisodeRejectsMissingSeason() {
        when(seasonRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contentAdminService.createEpisode(new CreateEpisodeInput(77L, "Episode 1", 1, null, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Season not found");
    }

    @Test
    void createEpisodeRejectsDuplicateOrderWithinSeason() {
        Season season = new Season();
        season.setId(6L);
        when(seasonRepository.findById(6L)).thenReturn(Optional.of(season));
        when(episodeRepository.existsBySeasonIdAndEpisodeNumber(6L, 1)).thenReturn(true);

        assertThatThrownBy(() -> contentAdminService.createEpisode(new CreateEpisodeInput(6L, "Episode 1", 1, null, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Episode number already exists for this season");
    }

    @Test
    void createEpisodePersistsWithParentSeason() {
        Season season = new Season();
        season.setId(6L);
        when(seasonRepository.findById(6L)).thenReturn(Optional.of(season));
        when(episodeRepository.existsBySeasonIdAndEpisodeNumber(6L, 2)).thenReturn(false);
        when(episodeRepository.save(any(Episode.class))).thenAnswer(invocation -> {
            Episode episode = invocation.getArgument(0);
            episode.setId(9L);
            ReflectionTestUtils.setField(episode, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
            ReflectionTestUtils.setField(episode, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
            return episode;
        });

        var payload = contentAdminService.createEpisode(new CreateEpisodeInput(6L, "  Episode 2 ", 2, "  Plot  ", 42, "2020-01-01"));

        assertThat(payload.id()).isEqualTo(9L);
        assertThat(payload.seasonId()).isEqualTo(6L);
        assertThat(payload.title()).isEqualTo("Episode 2");
        assertThat(payload.episodeNumber()).isEqualTo(2);
        assertThat(payload.releaseDate()).isEqualTo("2020-01-01");
    }
}
