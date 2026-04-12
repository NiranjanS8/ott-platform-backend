package com.ott.streaming.graphql;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ott.streaming.config.GraphQlDataLoaderConfig;
import com.ott.streaming.dto.content.EpisodePayload;
import com.ott.streaming.dto.content.GenrePayload;
import com.ott.streaming.dto.content.MoviePayload;
import com.ott.streaming.dto.content.PersonPayload;
import com.ott.streaming.dto.content.SeasonPayload;
import com.ott.streaming.dto.content.SeriesPayload;
import com.ott.streaming.dto.review.RatingSummaryPayload;
import com.ott.streaming.entity.ContentAccessLevel;
import com.ott.streaming.exception.GraphQlExceptionHandler;
import com.ott.streaming.service.ContentAdminService;
import com.ott.streaming.service.ContentQueryService;
import com.ott.streaming.service.ReviewService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQlTest({ContentGraphQlController.class, ReviewGraphQlController.class})
@Import({GraphQlExceptionHandler.class, GraphQlDataLoaderConfig.class})
class ContentPublicGraphQlTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ContentAdminService contentAdminService;

    @MockitoBean
    private ContentQueryService contentQueryService;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    void moviesQueryReturnsList() {
        when(contentQueryService.getMovies()).thenReturn(List.of(
                new MoviePayload(1L, "The Matrix", "Sci-fi action film", "1999-03-31", 136, "R", "English",
                        ContentAccessLevel.FREE,
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
        MoviePayload movie = new MoviePayload(1L, "The Matrix", "Sci-fi action film", "1999-03-31", 136, "R", "English",
                ContentAccessLevel.FREE,
                now(), now());
        when(contentQueryService.getMovieById(1L)).thenReturn(movie);
        when(contentQueryService.getMovieGenresByMovieIds(argThat(ids -> ids.contains(1L))))
                .thenReturn(java.util.Map.of(1L, List.of(new GenrePayload(10L, "Action", now(), now()))));
        when(contentQueryService.getMovieCastByMovieIds(argThat(ids -> ids.contains(1L))))
                .thenReturn(java.util.Map.of(1L, List.of(new PersonPayload(20L, "Keanu Reeves", null, null, now(), now()))));
        when(contentQueryService.getMovieDirectorsByMovieIds(argThat(ids -> ids.contains(1L))))
                .thenReturn(java.util.Map.of(1L, List.of(new PersonPayload(21L, "Lana Wachowski", null, null, now(), now()))));
        when(reviewService.getMovieRatingSummariesByMovieIds(argThat(ids -> ids.contains(1L))))
                .thenReturn(java.util.Map.of(1L, new RatingSummaryPayload(4.5, 2)));

        graphQlTester.document("""
                query {
                  movie(id: "1") {
                    title
                    genres { name }
                    cast { name }
                    directors { name }
                    ratingSummary {
                      averageRating
                      reviewCount
                    }
                  }
                }
                """)
                .execute()
                .path("movie.title").entity(String.class).isEqualTo("The Matrix")
                .path("movie.genres[0].name").entity(String.class).isEqualTo("Action")
                .path("movie.cast[0].name").entity(String.class).isEqualTo("Keanu Reeves")
                .path("movie.directors[0].name").entity(String.class).isEqualTo("Lana Wachowski")
                .path("movie.ratingSummary.averageRating").entity(Double.class).isEqualTo(4.5)
                .path("movie.ratingSummary.reviewCount").entity(Integer.class).isEqualTo(2);

        verify(contentQueryService).getMovieGenresByMovieIds(argThat(ids -> ids.size() == 1 && ids.contains(1L)));
        verify(contentQueryService).getMovieCastByMovieIds(argThat(ids -> ids.size() == 1 && ids.contains(1L)));
        verify(contentQueryService).getMovieDirectorsByMovieIds(argThat(ids -> ids.size() == 1 && ids.contains(1L)));
        verify(reviewService).getMovieRatingSummariesByMovieIds(argThat(ids -> ids.size() == 1 && ids.contains(1L)));
    }

    @Test
    void nestedSeriesAndSeasonFieldsResolveRelationships() {
        SeriesPayload series = new SeriesPayload(2L, "Dark", "Mystery series", "2017-12-01", "2020-06-27", "TV-MA", "German",
                ContentAccessLevel.FREE,
                now(), now());
        SeasonPayload season = new SeasonPayload(3L, 2L, "Season 1", 1, now(), now());
        EpisodePayload episode = new EpisodePayload(4L, 3L, "Episode 1", 1, "Pilot", 45, "2017-12-01", now(), now());

        when(contentQueryService.getSeriesById(2L)).thenReturn(series);
        when(contentQueryService.getSeriesGenresBySeriesIds(argThat(ids -> ids.contains(2L))))
                .thenReturn(java.util.Map.of(2L, List.of(new GenrePayload(11L, "Sci-Fi", now(), now()))));
        when(contentQueryService.getSeriesSeasonsBySeriesIds(argThat(ids -> ids.contains(2L))))
                .thenReturn(java.util.Map.of(2L, List.of(season)));
        when(contentQueryService.getSeasonEpisodesBySeasonIds(argThat(ids -> ids.contains(3L))))
                .thenReturn(java.util.Map.of(3L, List.of(episode)));
        when(reviewService.getSeriesRatingSummariesBySeriesIds(argThat(ids -> ids.contains(2L))))
                .thenReturn(java.util.Map.of(2L, new RatingSummaryPayload(4.0, 3)));

        graphQlTester.document("""
                query {
                  series(id: "2") {
                    title
                    genres { name }
                    ratingSummary {
                      averageRating
                      reviewCount
                    }
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
                .path("series.ratingSummary.averageRating").entity(Double.class).isEqualTo(4.0)
                .path("series.ratingSummary.reviewCount").entity(Integer.class).isEqualTo(3)
                .path("series.seasons[0].title").entity(String.class).isEqualTo("Season 1")
                .path("series.seasons[0].episodes[0].title").entity(String.class).isEqualTo("Episode 1")
                .path("series.seasons[0].episodes[0].episodeNumber").entity(Integer.class).isEqualTo(1);

        verify(contentQueryService).getSeriesGenresBySeriesIds(argThat(ids -> ids.size() == 1 && ids.contains(2L)));
        verify(contentQueryService).getSeriesSeasonsBySeriesIds(argThat(ids -> ids.size() == 1 && ids.contains(2L)));
        verify(contentQueryService).getSeasonEpisodesBySeasonIds(argThat(ids -> ids.size() == 1 && ids.contains(3L)));
        verify(reviewService).getSeriesRatingSummariesBySeriesIds(argThat(ids -> ids.size() == 1 && ids.contains(2L)));
    }

    @Test
    void moviesQueryBatchesNestedResolversAcrossParents() {
        MoviePayload firstMovie = new MoviePayload(1L, "The Matrix", "Sci-fi action film", "1999-03-31", 136, "R", "English",
                ContentAccessLevel.FREE,
                now(), now());
        MoviePayload secondMovie = new MoviePayload(2L, "Inception", "Dream heist thriller", "2010-07-16", 148, "PG-13", "English",
                ContentAccessLevel.FREE,
                now(), now());

        when(contentQueryService.getMovies()).thenReturn(List.of(firstMovie, secondMovie));
        when(contentQueryService.getMovieGenresByMovieIds(argThat(ids -> ids.size() == 2 && ids.containsAll(List.of(1L, 2L)))))
                .thenReturn(java.util.Map.of(
                        1L, List.of(new GenrePayload(10L, "Action", now(), now())),
                        2L, List.of(new GenrePayload(11L, "Sci-Fi", now(), now()))
                ));
        when(reviewService.getMovieRatingSummariesByMovieIds(argThat(ids -> ids.size() == 2 && ids.containsAll(List.of(1L, 2L)))))
                .thenReturn(java.util.Map.of(
                        1L, new RatingSummaryPayload(4.5, 2),
                        2L, new RatingSummaryPayload(4.8, 5)
                ));

        graphQlTester.document("""
                query {
                  movies {
                    id
                    genres { name }
                    ratingSummary {
                      averageRating
                      reviewCount
                    }
                  }
                }
                """)
                .execute()
                .path("movies[0].genres[0].name").entity(String.class).isEqualTo("Action")
                .path("movies[1].genres[0].name").entity(String.class).isEqualTo("Sci-Fi")
                .path("movies[0].ratingSummary.reviewCount").entity(Integer.class).isEqualTo(2)
                .path("movies[1].ratingSummary.reviewCount").entity(Integer.class).isEqualTo(5);

        verify(contentQueryService).getMovieGenresByMovieIds(argThat(ids -> ids.size() == 2 && ids.containsAll(List.of(1L, 2L))));
        verify(reviewService).getMovieRatingSummariesByMovieIds(argThat(ids -> ids.size() == 2 && ids.containsAll(List.of(1L, 2L))));
    }

    private Instant now() {
        return Instant.parse("2026-04-10T10:00:00Z");
    }
}
