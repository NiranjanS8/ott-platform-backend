package com.ott.streaming.graphql;

import static org.mockito.Mockito.when;

import com.ott.streaming.dto.content.EpisodePayload;
import com.ott.streaming.dto.content.GenrePayload;
import com.ott.streaming.dto.content.MoviePayload;
import com.ott.streaming.dto.content.PersonPayload;
import com.ott.streaming.dto.content.SeasonPayload;
import com.ott.streaming.dto.content.SeriesPayload;
import com.ott.streaming.exception.GraphQlExceptionHandler;
import com.ott.streaming.service.ContentAdminService;
import com.ott.streaming.service.ContentQueryService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQlTest(ContentGraphQlController.class)
@Import(GraphQlExceptionHandler.class)
class ContentPublicGraphQlTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ContentAdminService contentAdminService;

    @MockitoBean
    private ContentQueryService contentQueryService;

    @Test
    void moviesQueryReturnsList() {
        when(contentQueryService.getMovies()).thenReturn(List.of(
                new MoviePayload(1L, "The Matrix", "Sci-fi action film", "1999-03-31", 136, "R",
                        now(), now())
        ));

        graphQlTester.document("""
                query {
                  movies {
                    id
                    title
                    releaseDate
                  }
                }
                """)
                .execute()
                .path("movies[0].id").entity(String.class).isEqualTo("1")
                .path("movies[0].title").entity(String.class).isEqualTo("The Matrix")
                .path("movies[0].releaseDate").entity(String.class).isEqualTo("1999-03-31");
    }

    @Test
    void nestedMovieFieldsResolveRelationships() {
        MoviePayload movie = new MoviePayload(1L, "The Matrix", "Sci-fi action film", "1999-03-31", 136, "R",
                now(), now());
        when(contentQueryService.getMovieById(1L)).thenReturn(movie);
        when(contentQueryService.getMovieGenres(movie)).thenReturn(List.of(
                new GenrePayload(10L, "Action", now(), now())
        ));
        when(contentQueryService.getMovieCast(movie)).thenReturn(List.of(
                new PersonPayload(20L, "Keanu Reeves", null, null, now(), now())
        ));
        when(contentQueryService.getMovieDirectors(movie)).thenReturn(List.of(
                new PersonPayload(21L, "Lana Wachowski", null, null, now(), now())
        ));

        graphQlTester.document("""
                query {
                  movie(id: "1") {
                    title
                    genres { name }
                    cast { name }
                    directors { name }
                  }
                }
                """)
                .execute()
                .path("movie.title").entity(String.class).isEqualTo("The Matrix")
                .path("movie.genres[0].name").entity(String.class).isEqualTo("Action")
                .path("movie.cast[0].name").entity(String.class).isEqualTo("Keanu Reeves")
                .path("movie.directors[0].name").entity(String.class).isEqualTo("Lana Wachowski");
    }

    @Test
    void nestedSeriesAndSeasonFieldsResolveRelationships() {
        SeriesPayload series = new SeriesPayload(2L, "Dark", "Mystery series", "2017-12-01", "2020-06-27", "TV-MA",
                now(), now());
        SeasonPayload season = new SeasonPayload(3L, 2L, "Season 1", 1, now(), now());
        EpisodePayload episode = new EpisodePayload(4L, 3L, "Episode 1", 1, "Pilot", 45, "2017-12-01", now(), now());

        when(contentQueryService.getSeriesById(2L)).thenReturn(series);
        when(contentQueryService.getSeriesGenres(series)).thenReturn(List.of(
                new GenrePayload(11L, "Sci-Fi", now(), now())
        ));
        when(contentQueryService.getSeriesSeasons(series)).thenReturn(List.of(season));
        when(contentQueryService.getSeasonEpisodes(season)).thenReturn(List.of(episode));

        graphQlTester.document("""
                query {
                  series(id: "2") {
                    title
                    genres { name }
                    seasons {
                      title
                      episodes {
                        title
                        episodeNumber
                      }
                    }
                  }
                }
                """)
                .execute()
                .path("series.title").entity(String.class).isEqualTo("Dark")
                .path("series.genres[0].name").entity(String.class).isEqualTo("Sci-Fi")
                .path("series.seasons[0].title").entity(String.class).isEqualTo("Season 1")
                .path("series.seasons[0].episodes[0].title").entity(String.class).isEqualTo("Episode 1")
                .path("series.seasons[0].episodes[0].episodeNumber").entity(Integer.class).isEqualTo(1);
    }

    private Instant now() {
        return Instant.parse("2026-04-10T10:00:00Z");
    }
}
