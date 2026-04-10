package com.ott.streaming.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ott.streaming.entity.Role;
import com.ott.streaming.entity.User;
import com.ott.streaming.repository.UserRepository;
import com.ott.streaming.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ott.streaming.entity.Genre;
import com.ott.streaming.entity.Movie;
import com.ott.streaming.entity.Person;
import com.ott.streaming.entity.Season;
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
import com.ott.streaming.repository.EpisodeRepository;
import com.ott.streaming.repository.SeasonRepository;
import com.ott.streaming.repository.SeriesRepository;

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
        assertThat(json.at("/errors/0/message").asText()).contains("Access Denied");
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
        assertThat(json.at("/errors/0/message").asText()).contains("Access Denied");
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
        assertThat(json.at("/data/createSeason.title").asText()).isEqualTo("Season 1");
        assertThat(json.at("/data/createSeason.seasonNumber").asInt()).isEqualTo(1);
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
