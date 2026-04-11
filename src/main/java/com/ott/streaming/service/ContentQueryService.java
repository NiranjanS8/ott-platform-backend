package com.ott.streaming.service;

import com.ott.streaming.dto.content.EpisodePayload;
import com.ott.streaming.dto.content.GenrePayload;
import com.ott.streaming.dto.content.MoviePayload;
import com.ott.streaming.dto.content.PersonPayload;
import com.ott.streaming.dto.content.SeasonPayload;
import com.ott.streaming.dto.content.SeriesPayload;
import com.ott.streaming.dto.discovery.CatalogFilterInput;
import com.ott.streaming.dto.discovery.CatalogItemPayload;
import com.ott.streaming.dto.discovery.CatalogPagePayload;
import com.ott.streaming.dto.discovery.CatalogQueryInput;
import com.ott.streaming.dto.discovery.CatalogSortOption;
import com.ott.streaming.dto.discovery.PaginationInfoPayload;
import com.ott.streaming.entity.ContentAccessLevel;
import com.ott.streaming.entity.ContentType;
import com.ott.streaming.entity.Episode;
import com.ott.streaming.entity.Genre;
import com.ott.streaming.entity.Movie;
import com.ott.streaming.entity.Person;
import com.ott.streaming.entity.Season;
import com.ott.streaming.entity.Series;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.EpisodeRepository;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.ReviewRepository;
import com.ott.streaming.repository.SeasonRepository;
import com.ott.streaming.repository.SeriesRepository;
import com.ott.streaming.repository.spec.MovieSpecifications;
import com.ott.streaming.repository.spec.SeriesSpecifications;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final ReviewRepository reviewRepository;
    private final UserSubscriptionService userSubscriptionService;

    public ContentQueryService(MovieRepository movieRepository,
                               SeriesRepository seriesRepository,
                               SeasonRepository seasonRepository,
                               EpisodeRepository episodeRepository,
                               ReviewRepository reviewRepository,
                               UserSubscriptionService userSubscriptionService) {
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
        this.reviewRepository = reviewRepository;
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
        List<CatalogItemPayload> items = buildCatalogItems(input.search(), input.filter()).stream()
                .filter(item -> matchesRatingFilter(item, input.filter()))
                .sorted(catalogComparator(input.sort()))
                .toList();

        int totalElements = items.size();
        int page = input.pagination().page();
        int size = input.pagination().size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);

        return new CatalogPagePayload(
                items.subList(fromIndex, toIndex),
                new PaginationInfoPayload(
                        page,
                        size,
                        totalElements,
                        totalPages,
                        page + 1 < totalPages,
                        page > 0 && totalPages > 0
                )
        );
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

    private List<CatalogItemPayload> buildCatalogItems(String search, CatalogFilterInput filter) {
        String normalizedSearch = normalizeSearch(search);
        List<Movie> movies = shouldIncludeMovies(filter)
                ? movieRepository.findAll(MovieSpecifications.forCatalog(normalizedSearch, filter))
                : List.of();
        List<Series> seriesList = shouldIncludeSeries(filter)
                ? seriesRepository.findAll(SeriesSpecifications.forCatalog(normalizedSearch, filter))
                : List.of();
        Map<ContentKey, Double> averageRatings = buildAverageRatings(movies, seriesList);

        return java.util.stream.Stream.concat(
                        movies.stream()
                                .map(movie -> toCatalogMovieItem(movie, averageRatings)),
                        seriesList.stream()
                                .map(series -> toCatalogSeriesItem(series, averageRatings))
                )
                .toList();
    }

    private CatalogItemPayload toCatalogMovieItem(Movie movie, Map<ContentKey, Double> averageRatings) {
        return new CatalogItemPayload(
                movie.getId(),
                ContentType.MOVIE,
                movie.getTitle(),
                movie.getDescription(),
                formatDate(movie.getReleaseDate()),
                null,
                movie.getMaturityRating(),
                movie.getLanguage(),
                movie.getAccessLevel(),
                averageRatings.get(new ContentKey(ContentType.MOVIE, movie.getId()))
        );
    }

    private CatalogItemPayload toCatalogSeriesItem(Series series, Map<ContentKey, Double> averageRatings) {
        return new CatalogItemPayload(
                series.getId(),
                ContentType.SERIES,
                series.getTitle(),
                series.getDescription(),
                formatDate(series.getReleaseDate()),
                formatDate(series.getEndDate()),
                series.getMaturityRating(),
                series.getLanguage(),
                series.getAccessLevel(),
                averageRatings.get(new ContentKey(ContentType.SERIES, series.getId()))
        );
    }

    private Comparator<CatalogItemPayload> catalogComparator(CatalogSortOption sort) {
        Comparator<CatalogItemPayload> byTitle = Comparator.comparing(
                item -> item.title() == null ? "" : item.title().toLowerCase()
        );
        Comparator<CatalogItemPayload> byReleaseDate = Comparator.comparing(
                item -> item.releaseDate() == null ? "" : item.releaseDate()
        );

        return switch (sort) {
            case TITLE_ASC -> byTitle;
            case TITLE_DESC -> byTitle.reversed();
            case OLDEST -> byReleaseDate.thenComparing(byTitle);
            case TOP_RATED -> Comparator
                    .comparing((CatalogItemPayload item) -> item.averageRating() == null ? -1.0 : item.averageRating())
                    .reversed()
                    .thenComparing(byTitle);
            case LATEST -> byReleaseDate.reversed().thenComparing(byTitle);
        };
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String normalized = search.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean matchesRatingFilter(CatalogItemPayload item, CatalogFilterInput filter) {
        if (filter == null) {
            return true;
        }
        if (filter.minRating() != null
                && (item.averageRating() == null || item.averageRating() < filter.minRating())) {
            return false;
        }
        return filter.maxRating() == null
                || (item.averageRating() != null && item.averageRating() <= filter.maxRating());
    }

    private Map<ContentKey, Double> buildAverageRatings(List<Movie> movies, List<Series> seriesList) {
        Map<ContentKey, Double> ratings = new java.util.HashMap<>();
        movies.forEach(movie -> ratings.put(
                new ContentKey(ContentType.MOVIE, movie.getId()),
                averageRating(ContentType.MOVIE, movie.getId())
        ));
        seriesList.forEach(series -> ratings.put(
                new ContentKey(ContentType.SERIES, series.getId()),
                averageRating(ContentType.SERIES, series.getId())
        ));
        return ratings;
    }

    private Double averageRating(ContentType contentType, Long contentId) {
        return reviewRepository.findByContentTypeAndContentId(contentType, contentId).stream()
                .mapToInt(review -> review.getRating())
                .average()
                .stream()
                .boxed()
                .findFirst()
                .orElse(null);
    }

    private boolean shouldIncludeMovies(CatalogFilterInput filter) {
        return filter == null || filter.contentType() == null || filter.contentType() == ContentType.MOVIE;
    }

    private boolean shouldIncludeSeries(CatalogFilterInput filter) {
        return filter == null || filter.contentType() == null || filter.contentType() == ContentType.SERIES;
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

    private record ContentKey(ContentType contentType, Long contentId) {
    }
}
