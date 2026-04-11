package com.ott.streaming;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ott.streaming.repository.GenreRepository;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.PersonRepository;
import com.ott.streaming.repository.EpisodeRepository;
import com.ott.streaming.repository.ReviewRepository;
import com.ott.streaming.repository.SeasonRepository;
import com.ott.streaming.repository.SeriesRepository;
import com.ott.streaming.repository.UserRepository;
import com.ott.streaming.repository.WatchProgressRepository;
import com.ott.streaming.repository.WatchlistItemRepository;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class StreamingApplicationTests {

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    GenreRepository genreRepository;

    @MockitoBean
    PersonRepository personRepository;

    @MockitoBean
    MovieRepository movieRepository;

    @MockitoBean
    SeriesRepository seriesRepository;

    @MockitoBean
    SeasonRepository seasonRepository;

    @MockitoBean
    EpisodeRepository episodeRepository;

    @MockitoBean
    ReviewRepository reviewRepository;

    @MockitoBean
    WatchlistItemRepository watchlistItemRepository;

    @MockitoBean
    WatchProgressRepository watchProgressRepository;

    @Test
    void contextLoads() {
    }
}
