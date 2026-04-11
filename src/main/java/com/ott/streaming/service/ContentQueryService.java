package com.ott.streaming.service;

import com.ott.streaming.dto.content.EpisodePayload;
import com.ott.streaming.dto.content.GenrePayload;
import com.ott.streaming.dto.content.MoviePayload;
import com.ott.streaming.dto.content.PersonPayload;
import com.ott.streaming.dto.content.SeasonPayload;
import com.ott.streaming.dto.content.SeriesPayload;
import com.ott.streaming.dto.discovery.CatalogPagePayload;
import com.ott.streaming.dto.discovery.CatalogQueryInput;
import com.ott.streaming.entity.ContentAccessLevel;
import com.ott.streaming.entity.Episode;
import com.ott.streaming.entity.Genre;
import com.ott.streaming.entity.Movie;
import com.ott.streaming.entity.Person;
import com.ott.streaming.entity.Season;
import com.ott.streaming.entity.Series;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.EpisodeRepository;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.SeasonRepository;
import com.ott.streaming.repository.SeriesRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class ContentQueryService {

    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final UserSubscriptionService userSubscriptionService;

    public ContentQueryService(MovieRepository movieRepository,
                               SeriesRepository seriesRepository,
                               SeasonRepository seasonRepository,
                               EpisodeRepository episodeRepository,
                               UserSubscriptionService userSubscriptionService) {
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
        this.userSubscriptionService = userSubscriptionService;
    }

    public List<MoviePayload> getMovies() {
        return movieRepository.findAll().stream()
                .map(this::toMoviePayload)
                .toList();
    }

    public MoviePayload getMovieById(Long id) {
        return getMovieById(currentAuthenticatedEmail(), id);
    }

    public MoviePayload getMovieById(String email, Long id) {
        return movieRepository.findById(id)
                .map(movie -> {
                    enforceContentAccess(email, movie.getAccessLevel());
                    return toMoviePayload(movie);
                })
                .orElse(null);
    }

    public List<SeriesPayload> getSeriesList() {
        return seriesRepository.findAll().stream()
                .map(this::toSeriesPayload)
                .toList();
    }

    public SeriesPayload getSeriesById(Long id) {
        return getSeriesById(currentAuthenticatedEmail(), id);
    }

    public SeriesPayload getSeriesById(String email, Long id) {
        return seriesRepository.findById(id)
                .map(series -> {
                    enforceContentAccess(email, series.getAccessLevel());
                    return toSeriesPayload(series);
                })
                .orElse(null);
    }

    public SeasonPayload getSeasonById(Long id) {
        return getSeasonById(currentAuthenticatedEmail(), id);
    }

    public SeasonPayload getSeasonById(String email, Long id) {
        return seasonRepository.findById(id)
                .map(season -> {
                    enforceContentAccess(email, season.getSeries().getAccessLevel());
                    return toSeasonPayload(season);
                })
                .orElse(null);
    }

    public EpisodePayload getEpisodeById(Long id) {
        return getEpisodeById(currentAuthenticatedEmail(), id);
    }

    public EpisodePayload getEpisodeById(String email, Long id) {
        return episodeRepository.findById(id)
                .map(episode -> {
                    enforceContentAccess(email, episode.getSeason().getSeries().getAccessLevel());
                    return toEpisodePayload(episode);
                })
                .orElse(null);
    }

    public CatalogPagePayload discoverCatalog(CatalogQueryInput input) {
        throw new UnsupportedOperationException("Catalog discovery will be implemented in phase 6.2");
    }

    public List<GenrePayload> getMovieGenres(MoviePayload source) {
        return movieRepository.findById(source.id())
                .stream()
                .flatMap(movie -> movie.getGenres().stream())
                .map(this::toGenrePayload)
                .toList();
    }

    public List<PersonPayload> getMovieCast(MoviePayload source) {
        return movieRepository.findById(source.id())
                .stream()
                .flatMap(movie -> movie.getCast().stream())
                .map(this::toPersonPayload)
                .toList();
    }

    public List<PersonPayload> getMovieDirectors(MoviePayload source) {
        return movieRepository.findById(source.id())
                .stream()
                .flatMap(movie -> movie.getDirectors().stream())
                .map(this::toPersonPayload)
                .toList();
    }

    public List<GenrePayload> getSeriesGenres(SeriesPayload source) {
        return seriesRepository.findById(source.id())
                .stream()
                .flatMap(series -> series.getGenres().stream())
                .map(this::toGenrePayload)
                .toList();
    }

    public List<PersonPayload> getSeriesCast(SeriesPayload source) {
        return seriesRepository.findById(source.id())
                .stream()
                .flatMap(series -> series.getCast().stream())
                .map(this::toPersonPayload)
                .toList();
    }

    public List<PersonPayload> getSeriesDirectors(SeriesPayload source) {
        return seriesRepository.findById(source.id())
                .stream()
                .flatMap(series -> series.getDirectors().stream())
                .map(this::toPersonPayload)
                .toList();
    }

    public List<SeasonPayload> getSeriesSeasons(SeriesPayload source) {
        return seriesRepository.findById(source.id())
                .stream()
                .flatMap(series -> series.getSeasons().stream())
                .map(this::toSeasonPayload)
                .toList();
    }

    public List<EpisodePayload> getSeasonEpisodes(SeasonPayload source) {
        return seasonRepository.findById(source.id())
                .stream()
                .flatMap(season -> season.getEpisodes().stream())
                .map(this::toEpisodePayload)
                .toList();
    }

    private void enforceContentAccess(String email, ContentAccessLevel accessLevel) {
        if (accessLevel != ContentAccessLevel.PREMIUM) {
            return;
        }

        if (!userSubscriptionService.hasPremiumAccess(email)) {
            throw new ApiException("Premium subscription required to access this content", ErrorType.FORBIDDEN);
        }
    }

    private String currentAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String username) {
            return username;
        }
        return authentication.getName();
    }

    private MoviePayload toMoviePayload(Movie movie) {
        return new MoviePayload(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                formatDate(movie.getReleaseDate()),
                movie.getDurationMinutes(),
                movie.getMaturityRating(),
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
                series.getAccessLevel(),
                series.getCreatedAt(),
                series.getUpdatedAt()
        );
    }

    private SeasonPayload toSeasonPayload(Season season) {
        return new SeasonPayload(
                season.getId(),
                season.getSeries().getId(),
                season.getTitle(),
                season.getSeasonNumber(),
                season.getCreatedAt(),
                season.getUpdatedAt()
        );
    }

    private EpisodePayload toEpisodePayload(Episode episode) {
        return new EpisodePayload(
                episode.getId(),
                episode.getSeason().getId(),
                episode.getTitle(),
                episode.getEpisodeNumber(),
                episode.getDescription(),
                episode.getDurationMinutes(),
                formatDate(episode.getReleaseDate()),
                episode.getCreatedAt(),
                episode.getUpdatedAt()
        );
    }

    private GenrePayload toGenrePayload(Genre genre) {
        return new GenrePayload(
                genre.getId(),
                genre.getName(),
                genre.getCreatedAt(),
                genre.getUpdatedAt()
        );
    }

    private PersonPayload toPersonPayload(Person person) {
        return new PersonPayload(
                person.getId(),
                person.getName(),
                person.getBiography(),
                person.getProfileImageUrl(),
                person.getCreatedAt(),
                person.getUpdatedAt()
        );
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : date.toString();
    }
}
