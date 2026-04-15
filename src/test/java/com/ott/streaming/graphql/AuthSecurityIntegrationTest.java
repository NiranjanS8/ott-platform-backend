package com.ott.streaming.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ott.streaming.entity.Role;
import com.ott.streaming.entity.User;
import com.ott.streaming.entity.ContentAccessLevel;
import com.ott.streaming.entity.SubscriptionPlan;
import com.ott.streaming.entity.UserSubscription;
import com.ott.streaming.repository.UserRepository;
import com.ott.streaming.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ott.streaming.entity.Genre;
import com.ott.streaming.entity.Movie;
import com.ott.streaming.entity.Person;
import com.ott.streaming.entity.Review;
import com.ott.streaming.entity.Season;
import com.ott.streaming.entity.Series;
import com.ott.streaming.entity.WatchProgress;
import com.ott.streaming.entity.WatchlistItem;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.ott.streaming.repository.GenreRepository;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.PersonRepository;
import com.ott.streaming.repository.PlaybackSessionRepository;
import com.ott.streaming.repository.EpisodeRepository;
import com.ott.streaming.repository.ReviewRepository;
import com.ott.streaming.repository.SeasonRepository;
import com.ott.streaming.repository.SeriesRepository;
import com.ott.streaming.repository.SubscriptionPlanRepository;
import com.ott.streaming.repository.UserSubscriptionRepository;
import com.ott.streaming.repository.WatchProgressRepository;
import com.ott.streaming.repository.WatchlistItemRepository;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        }
)
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private GenreRepository genreRepository;

    @MockitoBean
    private PersonRepository personRepository;

    @MockitoBean
    private MovieRepository movieRepository;

    @MockitoBean
    private SeriesRepository seriesRepository;

    @MockitoBean
    private SeasonRepository seasonRepository;

    @MockitoBean
    private EpisodeRepository episodeRepository;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private WatchlistItemRepository watchlistItemRepository;

    @MockitoBean
    private WatchProgressRepository watchProgressRepository;

    @MockitoBean
    private PlaybackSessionRepository playbackSessionRepository;

    @MockitoBean
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @MockitoBean
    private UserSubscriptionRepository userSubscriptionRepository;

    @Test
    void meQueryWorksWithBearerToken() throws Exception {
        User user = buildUser(1L, "Member", "member@example.com", Role.USER);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));

        JsonNode json = executeGraphQl("""
                query {
                  me {
                    id
                    name
                    email
                    role
                  }
                }
                """, user);

        assertThat(json.at("/data/me/id").asText()).isEqualTo("1");
        assertThat(json.at("/data/me/name").asText()).isEqualTo("Member");
        assertThat(json.at("/data/me/email").asText()).isEqualTo("member@example.com");
        assertThat(json.at("/data/me/role").asText()).isEqualTo("USER");
    }

    @Test
    void adminStatusIsBlockedForNormalUser() throws Exception {
        User user = buildUser(2L, "Viewer", "viewer@example.com", Role.USER);
        when(userRepository.findByEmail("viewer@example.com")).thenReturn(Optional.of(user));

        JsonNode json = executeGraphQl("""
                query {
                  adminStatus
                }
                """, user);

        assertThat(json.at("/errors").isArray()).isTrue();
        assertThat(json.at("/errors/0/message").asText()).isEqualTo("Forbidden");
        assertThat(json.at("/errors/0/extensions/code").asText()).isEqualTo("FORBIDDEN");
    }

    @Test
    void adminStatusWorksForAdminUser() throws Exception {
        User user = buildUser(3L, "Admin", "admin@example.com", Role.ADMIN);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        JsonNode json = executeGraphQl("""
                query {
                  adminStatus
                }
                """, user);

        assertThat(json.at("/data/adminStatus").asText()).isEqualTo("ADMIN_ACCESS_GRANTED");
    }

    @Test
    void createGenreIsBlockedForNormalUser() throws Exception {
        User user = buildUser(4L, "Viewer", "viewer@example.com", Role.USER);
        when(userRepository.findByEmail("viewer@example.com")).thenReturn(Optional.of(user));

        JsonNode json = executeGraphQl("""
                mutation {
                  createGenre(input: { name: "Action" }) {
                    id
                    name
                  }
                }
                """, user);

        assertThat(json.at("/errors").isArray()).isTrue();
        assertThat(json.at("/errors/0/message").asText()).isEqualTo("Forbidden");
        assertThat(json.at("/errors/0/extensions/code").asText()).isEqualTo("FORBIDDEN");
    }

    @Test
    void createGenreWorksForAdminUser() throws Exception {
        User user = buildUser(5L, "Admin", "admin@example.com", Role.ADMIN);
        Genre genre = new Genre();
        genre.setId(11L);
        genre.setName("Action");
        ReflectionTestUtils.setField(genre, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
        ReflectionTestUtils.setField(genre, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(genreRepository.existsByNameIgnoreCase("Action")).thenReturn(false);
        when(genreRepository.save(any(Genre.class))).thenReturn(genre);

        JsonNode json = executeGraphQl("""
                mutation {
                  createGenre(input: { name: "Action" }) {
                    id
                    name
                  }
                }
                """, user);

        assertThat(json.at("/data/createGenre/id").asText()).isEqualTo("11");
        assertThat(json.at("/data/createGenre/name").asText()).isEqualTo("Action");
    }

    @Test
    void createMovieWorksForAdminUser() throws Exception {
        User user = buildUser(6L, "Admin", "admin@example.com", Role.ADMIN);
        Genre genre = new Genre();
        genre.setId(1L);
        genre.setName("Action");
        Person actor = buildPerson(2L, "Keanu Reeves");
        Person director = buildPerson(3L, "Lana Wachowski");
        Movie movie = new Movie();
        movie.setId(12L);
        movie.setTitle("The Matrix");
        movie.setDescription("Sci-fi action film");
        movie.setReleaseDate(java.time.LocalDate.parse("1999-03-31"));
        movie.setDurationMinutes(136);
        movie.setMaturityRating("R");
        ReflectionTestUtils.setField(movie, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
        ReflectionTestUtils.setField(movie, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(genreRepository.findAllById(java.util.Set.of(1L))).thenReturn(java.util.List.of(genre));
        when(personRepository.findAllById(java.util.Set.of(2L))).thenReturn(java.util.List.of(actor));
        when(personRepository.findAllById(java.util.Set.of(3L))).thenReturn(java.util.List.of(director));
        when(movieRepository.save(any(Movie.class))).thenReturn(movie);

        JsonNode json = executeGraphQl("""
                mutation {
                  createMovie(input: {
                    title: "The Matrix"
                    description: "Sci-fi action film"
                    releaseDate: "1999-03-31"
                    durationMinutes: 136
                    maturityRating: "R"
                    genreIds: ["1"]
                    castIds: ["2"]
                    directorIds: ["3"]
                  }) {
                    id
                    title
                  }
                }
                """, user);

        assertThat(json.at("/data/createMovie/id").asText()).isEqualTo("12");
        assertThat(json.at("/data/createMovie/title").asText()).isEqualTo("The Matrix");
    }

    @Test
    void createSeasonWorksForAdminUser() throws Exception {
        User user = buildUser(7L, "Admin", "admin@example.com", Role.ADMIN);
        Series series = new Series();
        series.setId(4L);
        Season season = new Season();
        season.setId(13L);
        season.setTitle("Season 1");
        season.setSeasonNumber(1);
        season.setSeries(series);
        ReflectionTestUtils.setField(season, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
        ReflectionTestUtils.setField(season, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(seriesRepository.findById(4L)).thenReturn(Optional.of(series));
        when(seasonRepository.existsBySeriesIdAndSeasonNumber(4L, 1)).thenReturn(false);
        when(seasonRepository.save(any(Season.class))).thenReturn(season);

        JsonNode json = executeGraphQl("""
                mutation {
                  createSeason(input: {
                    seriesId: "4"
                    title: "Season 1"
                    seasonNumber: 1
                  }) {
                    id
                    title
                    seasonNumber
                  }
                }
                """, user);

        assertThat(json.at("/data/createSeason/id").asText()).isEqualTo("13");
        assertThat(json.at("/data/createSeason/title").asText()).isEqualTo("Season 1");
        assertThat(json.at("/data/createSeason/seasonNumber").asInt()).isEqualTo(1);
    }

    @Test
    void addReviewWorksForAuthenticatedUser() throws Exception {
        User user = buildUser(8L, "Reviewer", "reviewer@example.com", Role.USER);
        Review review = new Review();
        review.setId(20L);
        review.setUserId(8L);
        review.setContentType(com.ott.streaming.entity.ContentType.MOVIE);
        review.setContentId(12L);
        review.setRating(5);
        review.setComment("Excellent");
        ReflectionTestUtils.setField(review, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
        ReflectionTestUtils.setField(review, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));

        when(userRepository.findByEmail("reviewer@example.com")).thenReturn(Optional.of(user));
        when(movieRepository.existsById(12L)).thenReturn(true);
        when(reviewRepository.findByUserIdAndContentTypeAndContentId(
                8L, com.ott.streaming.entity.ContentType.MOVIE, 12L
        )).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        JsonNode json = executeGraphQl("""
                mutation {
                  addReview(input: {
                    contentType: MOVIE
                    contentId: "12"
                    rating: 5
                    comment: "Excellent"
                  }) {
                    id
                    userId
                    rating
                  }
                }
                """, user);

        assertThat(json.at("/data/addReview/id").asText()).isEqualTo("20");
        assertThat(json.at("/data/addReview/userId").asText()).isEqualTo("8");
        assertThat(json.at("/data/addReview/rating").asInt()).isEqualTo(5);
    }

    @Test
    void updateReviewIsBlockedForDifferentNonAdminUser() throws Exception {
        User user = buildUser(9L, "Member", "member@example.com", Role.USER);
        Review review = new Review();
        review.setId(21L);
        review.setUserId(99L);
        review.setContentType(com.ott.streaming.entity.ContentType.MOVIE);
        review.setContentId(12L);
        review.setRating(2);

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(reviewRepository.findById(21L)).thenReturn(Optional.of(review));

        JsonNode json = executeGraphQl("""
                mutation {
                  updateReview(id: "21", input: {
                    rating: 4
                    comment: "Updated"
                  }) {
                    id
                  }
                }
                """, user);

        assertThat(json.at("/errors").isArray()).isTrue();
        assertThat(json.at("/errors/0/message").asText()).contains("not allowed to modify this review");
    }

    @Test
    void addToWatchlistWorksForAuthenticatedUser() throws Exception {
        User user = buildUser(10L, "Member", "member@example.com", Role.USER);
        WatchlistItem item = new WatchlistItem();
        item.setId(30L);
        item.setUserId(10L);
        item.setContentType(com.ott.streaming.entity.ContentType.MOVIE);
        item.setContentId(12L);
        ReflectionTestUtils.setField(item, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(item, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(movieRepository.existsById(12L)).thenReturn(true);
        when(watchlistItemRepository.existsByUserIdAndContentTypeAndContentId(
                10L, com.ott.streaming.entity.ContentType.MOVIE, 12L
        )).thenReturn(false);
        when(watchlistItemRepository.save(any(WatchlistItem.class))).thenReturn(item);

        JsonNode json = executeGraphQl("""
                mutation {
                  addToWatchlist(input: {
                    contentType: MOVIE
                    contentId: "12"
                  }) {
                    id
                    userId
                    contentId
                  }
                }
                """, user);

        assertThat(json.at("/data/addToWatchlist/id").asText()).isEqualTo("30");
        assertThat(json.at("/data/addToWatchlist/userId").asText()).isEqualTo("10");
        assertThat(json.at("/data/addToWatchlist/contentId").asText()).isEqualTo("12");
    }

    @Test
    void myWatchlistReturnsUserScopedItems() throws Exception {
        User user = buildUser(11L, "Member", "member@example.com", Role.USER);
        WatchlistItem item = new WatchlistItem();
        item.setId(31L);
        item.setUserId(11L);
        item.setContentType(com.ott.streaming.entity.ContentType.SERIES);
        item.setContentId(77L);
        ReflectionTestUtils.setField(item, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(item, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(watchlistItemRepository.findByUserIdOrderByCreatedAtDesc(11L)).thenReturn(java.util.List.of(item));

        JsonNode json = executeGraphQl("""
                query {
                  myWatchlist {
                    id
                    contentType
                    contentId
                  }
                }
                """, user);

        assertThat(json.at("/data/myWatchlist/0/id").asText()).isEqualTo("31");
        assertThat(json.at("/data/myWatchlist/0/contentType").asText()).isEqualTo("SERIES");
        assertThat(json.at("/data/myWatchlist/0/contentId").asText()).isEqualTo("77");
    }

    @Test
    void updateWatchProgressWorksForMovieProgress() throws Exception {
        User user = buildUser(12L, "Member", "member@example.com", Role.USER);
        WatchProgress progress = new WatchProgress();
        progress.setId(40L);
        progress.setUserId(12L);
        progress.setContentType(com.ott.streaming.entity.ContentType.MOVIE);
        progress.setContentId(12L);
        progress.setProgressSeconds(120);
        progress.setDurationSeconds(7200);
        progress.setCompleted(false);
        progress.setLastWatchedAt(Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(progress, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(progress, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(movieRepository.existsById(12L)).thenReturn(true);
        when(watchProgressRepository.findByUserIdAndContentTypeAndContentIdAndEpisodeIdIsNull(
                12L, com.ott.streaming.entity.ContentType.MOVIE, 12L
        )).thenReturn(Optional.empty());
        when(watchProgressRepository.save(any(WatchProgress.class))).thenReturn(progress);

        JsonNode json = executeGraphQl("""
                mutation {
                  updateWatchProgress(input: {
                    contentType: MOVIE
                    contentId: "12"
                    progressSeconds: 120
                    durationSeconds: 7200
                  }) {
                    id
                    contentType
                    progressSeconds
                  }
                }
                """, user);

        assertThat(json.at("/data/updateWatchProgress/id").asText()).isEqualTo("40");
        assertThat(json.at("/data/updateWatchProgress/contentType").asText()).isEqualTo("MOVIE");
        assertThat(json.at("/data/updateWatchProgress/progressSeconds").asInt()).isEqualTo(120);
    }

    @Test
    void markAsCompletedWorksForEpisodeProgress() throws Exception {
        User user = buildUser(13L, "Member", "member@example.com", Role.USER);
        WatchProgress progress = new WatchProgress();
        progress.setId(41L);
        progress.setUserId(13L);
        progress.setContentType(com.ott.streaming.entity.ContentType.SERIES);
        progress.setContentId(77L);
        progress.setSeasonId(8L);
        progress.setEpisodeId(9L);
        progress.setProgressSeconds(3600);
        progress.setDurationSeconds(3600);
        progress.setCompleted(true);
        progress.setLastWatchedAt(Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(progress, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(progress, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(watchProgressRepository.findByUserIdAndContentTypeAndContentIdAndEpisodeId(
                13L, com.ott.streaming.entity.ContentType.SERIES, 77L, 9L
        )).thenReturn(Optional.of(progress));
        when(watchProgressRepository.save(any(WatchProgress.class))).thenReturn(progress);

        JsonNode json = executeGraphQl("""
                mutation {
                  markAsCompleted(contentType: SERIES, contentId: "77", episodeId: "9") {
                    id
                    contentType
                    episodeId
                    completed
                  }
                }
                """, user);

        assertThat(json.at("/data/markAsCompleted/id").asText()).isEqualTo("41");
        assertThat(json.at("/data/markAsCompleted/contentType").asText()).isEqualTo("SERIES");
        assertThat(json.at("/data/markAsCompleted/episodeId").asText()).isEqualTo("9");
        assertThat(json.at("/data/markAsCompleted/completed").asBoolean()).isTrue();
    }

    @Test
    void continueWatchingReturnsOrderedInProgressEntries() throws Exception {
        User user = buildUser(14L, "Member", "member@example.com", Role.USER);
        WatchProgress recent = new WatchProgress();
        recent.setId(50L);
        recent.setUserId(14L);
        recent.setContentType(com.ott.streaming.entity.ContentType.SERIES);
        recent.setContentId(77L);
        recent.setSeasonId(8L);
        recent.setEpisodeId(9L);
        recent.setProgressSeconds(600);
        recent.setDurationSeconds(3600);
        recent.setCompleted(false);
        recent.setLastWatchedAt(Instant.parse("2026-04-11T12:00:00Z"));
        ReflectionTestUtils.setField(recent, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(recent, "updatedAt", Instant.parse("2026-04-11T12:00:00Z"));

        WatchProgress completed = new WatchProgress();
        completed.setId(51L);
        completed.setUserId(14L);
        completed.setContentType(com.ott.streaming.entity.ContentType.MOVIE);
        completed.setContentId(12L);
        completed.setProgressSeconds(7200);
        completed.setDurationSeconds(7200);
        completed.setCompleted(true);
        completed.setLastWatchedAt(Instant.parse("2026-04-11T11:00:00Z"));
        ReflectionTestUtils.setField(completed, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(completed, "updatedAt", Instant.parse("2026-04-11T11:00:00Z"));

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(watchProgressRepository.findByUserIdOrderByLastWatchedAtDesc(14L))
                .thenReturn(java.util.List.of(recent, completed));

        JsonNode json = executeGraphQl("""
                query {
                  continueWatching {
                    id
                    contentType
                    contentId
                    episodeId
                  }
                }
                """, user);

        assertThat(json.at("/data/continueWatching/0/id").asText()).isEqualTo("50");
        assertThat(json.at("/data/continueWatching/0/contentType").asText()).isEqualTo("SERIES");
        assertThat(json.at("/data/continueWatching/0/episodeId").asText()).isEqualTo("9");
        assertThat(json.at("/data/continueWatching/1").isMissingNode()).isTrue();
    }

    @Test
    void watchHistoryReturnsRecentEntriesForUser() throws Exception {
        User user = buildUser(15L, "Member", "member@example.com", Role.USER);
        WatchProgress recent = new WatchProgress();
        recent.setId(52L);
        recent.setUserId(15L);
        recent.setContentType(com.ott.streaming.entity.ContentType.MOVIE);
        recent.setContentId(12L);
        recent.setProgressSeconds(120);
        recent.setDurationSeconds(7200);
        recent.setCompleted(false);
        recent.setLastWatchedAt(Instant.parse("2026-04-11T12:00:00Z"));
        ReflectionTestUtils.setField(recent, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(recent, "updatedAt", Instant.parse("2026-04-11T12:00:00Z"));

        WatchProgress older = new WatchProgress();
        older.setId(53L);
        older.setUserId(15L);
        older.setContentType(com.ott.streaming.entity.ContentType.SERIES);
        older.setContentId(77L);
        older.setSeasonId(8L);
        older.setEpisodeId(9L);
        older.setProgressSeconds(3600);
        older.setDurationSeconds(3600);
        older.setCompleted(true);
        older.setLastWatchedAt(Instant.parse("2026-04-11T11:00:00Z"));
        ReflectionTestUtils.setField(older, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(older, "updatedAt", Instant.parse("2026-04-11T11:00:00Z"));

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(watchProgressRepository.findByUserIdOrderByLastWatchedAtDesc(15L))
                .thenReturn(java.util.List.of(recent, older));

        JsonNode json = executeGraphQl("""
                query {
                  watchHistory {
                    id
                    contentType
                    contentId
                    completed
                  }
                }
                """, user);

        assertThat(json.at("/data/watchHistory/0/id").asText()).isEqualTo("52");
        assertThat(json.at("/data/watchHistory/0/contentType").asText()).isEqualTo("MOVIE");
        assertThat(json.at("/data/watchHistory/1/id").asText()).isEqualTo("53");
        assertThat(json.at("/data/watchHistory/1/contentType").asText()).isEqualTo("SERIES");
    }

    @Test
    void createSubscriptionPlanIsBlockedForNormalUser() throws Exception {
        User user = buildUser(16L, "Member", "member@example.com", Role.USER);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));

        JsonNode json = executeGraphQl("""
                mutation {
                  createSubscriptionPlan(input: {
                    name: "Premium Monthly"
                    description: "Monthly access"
                    price: 9.99
                    durationDays: 30
                    active: true
                  }) {
                    id
                    name
                  }
                }
                """, user);

        assertThat(json.at("/errors").isArray()).isTrue();
        assertThat(json.at("/errors/0/message").asText()).isEqualTo("Forbidden");
        assertThat(json.at("/errors/0/extensions/code").asText()).isEqualTo("FORBIDDEN");
    }

    @Test
    void createSubscriptionPlanWorksForAdminUser() throws Exception {
        User user = buildUser(17L, "Admin", "admin@example.com", Role.ADMIN);
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(60L);
        plan.setName("Premium Monthly");
        plan.setDescription("Monthly access");
        plan.setPrice(new java.math.BigDecimal("9.99"));
        plan.setDurationDays(30);
        plan.setActive(true);
        ReflectionTestUtils.setField(plan, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(plan, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(subscriptionPlanRepository.existsByNameIgnoreCase("Premium Monthly")).thenReturn(false);
        when(subscriptionPlanRepository.save(any(SubscriptionPlan.class))).thenReturn(plan);

        JsonNode json = executeGraphQl("""
                mutation {
                  createSubscriptionPlan(input: {
                    name: "Premium Monthly"
                    description: "Monthly access"
                    price: 9.99
                    durationDays: 30
                    active: true
                  }) {
                    id
                    name
                    price
                  }
                }
                """, user);

        assertThat(json.at("/data/createSubscriptionPlan/id").asText()).isEqualTo("60");
        assertThat(json.at("/data/createSubscriptionPlan/name").asText()).isEqualTo("Premium Monthly");
        assertThat(json.at("/data/createSubscriptionPlan/price").decimalValue())
                .isEqualByComparingTo("9.99");
    }

    @Test
    void updateMovieAccessLevelIsBlockedForNormalUser() throws Exception {
        User user = buildUser(18L, "Member", "member@example.com", Role.USER);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));

        JsonNode json = executeGraphQl("""
                mutation {
                  updateMovieAccessLevel(id: "12", input: {
                    accessLevel: PREMIUM
                  }) {
                    id
                    accessLevel
                  }
                }
                """, user);

        assertThat(json.at("/errors").isArray()).isTrue();
        assertThat(json.at("/errors/0/message").asText()).isEqualTo("Forbidden");
        assertThat(json.at("/errors/0/extensions/code").asText()).isEqualTo("FORBIDDEN");
    }

    @Test
    void subscribeToPlanWorksForAuthenticatedUser() throws Exception {
        User user = buildUser(19L, "Member", "member@example.com", Role.USER);
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(70L);
        plan.setName("Premium Monthly");
        plan.setDescription("Monthly access");
        plan.setPrice(new java.math.BigDecimal("9.99"));
        plan.setDurationDays(30);
        plan.setActive(true);

        UserSubscription subscription = new UserSubscription();
        subscription.setId(71L);
        subscription.setUserId(19L);
        subscription.setPlanId(70L);
        subscription.setStatus(com.ott.streaming.entity.SubscriptionStatus.ACTIVE);
        subscription.setStartDate(Instant.parse("2026-04-11T10:00:00Z"));
        subscription.setEndDate(Instant.parse("2026-05-11T10:00:00Z"));
        ReflectionTestUtils.setField(subscription, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(subscription, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmailForUpdate("member@example.com")).thenReturn(Optional.of(user));
        when(subscriptionPlanRepository.findById(70L)).thenReturn(Optional.of(plan));
        when(userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndDateDesc(
                19L, com.ott.streaming.entity.SubscriptionStatus.ACTIVE
        )).thenReturn(Optional.empty());
        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenReturn(subscription);

        JsonNode json = executeGraphQl("""
                mutation {
                  subscribeToPlan(input: { planId: "70" }) {
                    id
                    userId
                    planId
                    status
                  }
                }
                """, user);

        assertThat(json.at("/data/subscribeToPlan/id").asText()).isEqualTo("71");
        assertThat(json.at("/data/subscribeToPlan/userId").asText()).isEqualTo("19");
        assertThat(json.at("/data/subscribeToPlan/planId").asText()).isEqualTo("70");
        assertThat(json.at("/data/subscribeToPlan/status").asText()).isEqualTo("ACTIVE");
    }

    @Test
    void currentSubscriptionReturnsActiveSubscriptionForAuthenticatedUser() throws Exception {
        User user = buildUser(20L, "Member", "member@example.com", Role.USER);
        UserSubscription subscription = new UserSubscription();
        subscription.setId(72L);
        subscription.setUserId(20L);
        subscription.setPlanId(70L);
        subscription.setStatus(com.ott.streaming.entity.SubscriptionStatus.ACTIVE);
        subscription.setStartDate(Instant.parse("2026-04-11T10:00:00Z"));
        subscription.setEndDate(Instant.parse("2026-05-11T10:00:00Z"));
        ReflectionTestUtils.setField(subscription, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(subscription, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndDateDesc(
                20L, com.ott.streaming.entity.SubscriptionStatus.ACTIVE
        )).thenReturn(Optional.of(subscription));

        JsonNode json = executeGraphQl("""
                query {
                  currentSubscription {
                    id
                    userId
                    planId
                    status
                  }
                }
                """, user);

        assertThat(json.at("/data/currentSubscription/id").asText()).isEqualTo("72");
        assertThat(json.at("/data/currentSubscription/userId").asText()).isEqualTo("20");
        assertThat(json.at("/data/currentSubscription/planId").asText()).isEqualTo("70");
        assertThat(json.at("/data/currentSubscription/status").asText()).isEqualTo("ACTIVE");
    }

    @Test
    void premiumMovieQueryReturnsMetadataWithoutActiveSubscription() throws Exception {
        Movie movie = new Movie();
        movie.setId(90L);
        movie.setTitle("Premium Movie");
        movie.setAccessLevel(ContentAccessLevel.PREMIUM);
        ReflectionTestUtils.setField(movie, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(movie, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));

        when(movieRepository.findById(90L)).thenReturn(Optional.of(movie));

        JsonNode json = executeAnonymousGraphQl("""
                query {
                  movie(id: "90") {
                    id
                    title
                    accessLevel
                  }
                }
                """);

        assertThat(json.at("/errors").isMissingNode()).isTrue();
        assertThat(json.at("/data/movie/id").asText()).isEqualTo("90");
        assertThat(json.at("/data/movie/title").asText()).isEqualTo("Premium Movie");
        assertThat(json.at("/data/movie/accessLevel").asText()).isEqualTo("PREMIUM");
    }

    @Test
    void premiumMovieQueryWorksWithActiveSubscription() throws Exception {
        User user = buildUser(21L, "Member", "member@example.com", Role.USER);
        Movie movie = new Movie();
        movie.setId(91L);
        movie.setTitle("Premium Movie");
        movie.setAccessLevel(ContentAccessLevel.PREMIUM);
        ReflectionTestUtils.setField(movie, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(movie, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));

        UserSubscription subscription = new UserSubscription();
        subscription.setId(92L);
        subscription.setUserId(21L);
        subscription.setPlanId(70L);
        subscription.setStatus(com.ott.streaming.entity.SubscriptionStatus.ACTIVE);
        subscription.setStartDate(Instant.parse("2026-04-11T10:00:00Z"));
        subscription.setEndDate(Instant.parse("2026-05-11T10:00:00Z"));
        ReflectionTestUtils.setField(subscription, "createdAt", Instant.parse("2026-04-11T10:00:00Z"));
        ReflectionTestUtils.setField(subscription, "updatedAt", Instant.parse("2026-04-11T10:00:00Z"));

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(movieRepository.findById(91L)).thenReturn(Optional.of(movie));
        when(userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndDateDesc(
                21L, com.ott.streaming.entity.SubscriptionStatus.ACTIVE
        )).thenReturn(Optional.of(subscription));

        JsonNode json = executeGraphQl("""
                query {
                  movie(id: "91") {
                    id
                    title
                    accessLevel
                  }
                }
                """, user);

        assertThat(json.at("/data/movie/id").asText()).isEqualTo("91");
        assertThat(json.at("/data/movie/title").asText()).isEqualTo("Premium Movie");
        assertThat(json.at("/data/movie/accessLevel").asText()).isEqualTo("PREMIUM");
    }

    private JsonNode executeGraphQl(String document, User user) throws Exception {
        String token = jwtService.generateAccessToken(user);
        String payload = objectMapper.writeValueAsString(new GraphQlRequest(document));

        MvcResult result = mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode executeAnonymousGraphQl(String document) throws Exception {
        String payload = objectMapper.writeValueAsString(new GraphQlRequest(document));

        MvcResult result = mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private User buildUser(Long id, String name, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(role);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-04-09T12:00:00Z"));
        ReflectionTestUtils.setField(user, "updatedAt", Instant.parse("2026-04-09T12:00:00Z"));
        return user;
    }

    private Person buildPerson(Long id, String name) {
        Person person = new Person();
        person.setId(id);
        person.setName(name);
        ReflectionTestUtils.setField(person, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
        ReflectionTestUtils.setField(person, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
        return person;
    }

    private record GraphQlRequest(String query) {
    }
}
