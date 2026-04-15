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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final ContentReadCacheService contentReadCacheService;

    public ContentQueryService(MovieRepository movieRepository,
                               SeriesRepository seriesRepository,
                               SeasonRepository seasonRepository,
                               EpisodeRepository episodeRepository,
                               ReviewRepository reviewRepository,
                               UserSubscriptionService userSubscriptionService,
                               ContentReadCacheService contentReadCacheService) {
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
        this.reviewRepository = reviewRepository;
        this.userSubscriptionService = userSubscriptionService;
        this.contentReadCacheService = contentReadCacheService;
    }

    public List<MoviePayload> getMovies() {
        return contentReadCacheService.getMovies();
    }

    public MoviePayload getMovieById(Long id) {
        return getMovieById(currentAuthenticatedEmail(), id);
    }

    public MoviePayload getMovieById(String email, Long id) {
        MoviePayload movie = contentReadCacheService.getMovieById(id);
        if (movie == null) {
            return null;
        }

        enforceContentAccess(email, movie.accessLevel());
        return movie;
    }

    public List<SeriesPayload> getSeriesList() {
        return contentReadCacheService.getSeriesList();
    }

    public SeriesPayload getSeriesById(Long id) {
        return getSeriesById(currentAuthenticatedEmail(), id);
    }

    public SeriesPayload getSeriesById(String email, Long id) {
        SeriesPayload series = contentReadCacheService.getSeriesById(id);
        if (series == null) {
            return null;
        }

        enforceContentAccess(email, series.accessLevel());
        return series;
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
        return getMovieGenresByMovieIds(List.of(source.id())).getOrDefault(source.id(), List.of());
    }

    public List<PersonPayload> getMovieCast(MoviePayload source) {
        return getMovieCastByMovieIds(List.of(source.id())).getOrDefault(source.id(), List.of());
    }

    public List<PersonPayload> getMovieDirectors(MoviePayload source) {
        return getMovieDirectorsByMovieIds(List.of(source.id())).getOrDefault(source.id(), List.of());
    }

    public List<GenrePayload> getSeriesGenres(SeriesPayload source) {
        return getSeriesGenresBySeriesIds(List.of(source.id())).getOrDefault(source.id(), List.of());
    }

    public List<PersonPayload> getSeriesCast(SeriesPayload source) {
        return getSeriesCastBySeriesIds(List.of(source.id())).getOrDefault(source.id(), List.of());
    }

    public List<PersonPayload> getSeriesDirectors(SeriesPayload source) {
        return getSeriesDirectorsBySeriesIds(List.of(source.id())).getOrDefault(source.id(), List.of());
    }

    public List<SeasonPayload> getSeriesSeasons(SeriesPayload source) {
        return getSeriesSeasonsBySeriesIds(List.of(source.id())).getOrDefault(source.id(), List.of());
    }

    public List<EpisodePayload> getSeasonEpisodes(SeasonPayload source) {
        return getSeasonEpisodesBySeasonIds(List.of(source.id())).getOrDefault(source.id(), List.of());
    }

    public Map<Long, List<GenrePayload>> getMovieGenresByMovieIds(Collection<Long> movieIds) {
        return loadMovies(movieIds, Movie::getGenres, this::toGenrePayload);
    }

    public Map<Long, List<PersonPayload>> getMovieCastByMovieIds(Collection<Long> movieIds) {
        return loadMovies(movieIds, Movie::getCast, this::toPersonPayload);
    }

    public Map<Long, List<PersonPayload>> getMovieDirectorsByMovieIds(Collection<Long> movieIds) {
        return loadMovies(movieIds, Movie::getDirectors, this::toPersonPayload);
    }

    public Map<Long, List<GenrePayload>> getSeriesGenresBySeriesIds(Collection<Long> seriesIds) {
        return loadSeries(seriesIds, Series::getGenres, this::toGenrePayload);
    }

    public Map<Long, List<PersonPayload>> getSeriesCastBySeriesIds(Collection<Long> seriesIds) {
        return loadSeries(seriesIds, Series::getCast, this::toPersonPayload);
    }

    public Map<Long, List<PersonPayload>> getSeriesDirectorsBySeriesIds(Collection<Long> seriesIds) {
        return loadSeries(seriesIds, Series::getDirectors, this::toPersonPayload);
    }

    public Map<Long, List<SeasonPayload>> getSeriesSeasonsBySeriesIds(Collection<Long> seriesIds) {
        Map<Long, List<SeasonPayload>> seasonsBySeriesId = initializeListMap(seriesIds);
        seasonRepository.findBySeriesIdInOrderBySeasonNumberAsc(seriesIds)
                .forEach(season -> seasonsBySeriesId.get(season.getSeries().getId()).add(toSeasonPayload(season)));
        return seasonsBySeriesId;
    }

    public Map<Long, List<EpisodePayload>> getSeasonEpisodesBySeasonIds(Collection<Long> seasonIds) {
        Map<Long, List<EpisodePayload>> episodesBySeasonId = initializeListMap(seasonIds);
        episodeRepository.findBySeasonIdInOrderByEpisodeNumberAsc(seasonIds)
                .forEach(episode -> episodesBySeasonId.get(episode.getSeason().getId()).add(toEpisodePayload(episode)));
        return episodesBySeasonId;
    }

    private void enforceContentAccess(String email, ContentAccessLevel accessLevel) {
        if (accessLevel != ContentAccessLevel.PREMIUM) {
            return;
        }

        if (!userSubscriptionService.hasPremiumAccess(email)) {
            throw ApiException.forbidden("Premium subscription required to access this content");
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
        Map<ContentKey, Double> ratings = new HashMap<>();
        ratings.putAll(aggregateAverageRatings(
                ContentType.MOVIE,
                movies.stream().map(Movie::getId).toList()
        ));
        ratings.putAll(aggregateAverageRatings(
                ContentType.SERIES,
                seriesList.stream().map(Series::getId).toList()
        ));
        return ratings;
    }

    private Map<ContentKey, Double> aggregateAverageRatings(ContentType contentType, Collection<Long> contentIds) {
        Map<ContentKey, Double> averages = new HashMap<>();
        if (contentIds.isEmpty()) {
            return averages;
        }

        Map<Long, List<Integer>> ratingsByContentId = new HashMap<>();
        contentIds.forEach(id -> ratingsByContentId.put(id, new ArrayList<>()));

        reviewRepository.findByContentTypeAndContentIdIn(contentType, contentIds)
                .forEach(review -> ratingsByContentId
                        .computeIfAbsent(review.getContentId(), ignored -> new ArrayList<>())
                        .add(review.getRating()));

        ratingsByContentId.forEach((contentId, values) -> averages.put(
                new ContentKey(contentType, contentId),
                values.isEmpty()
                        ? null
                        : values.stream().mapToInt(Integer::intValue).average().orElse(0.0)
        ));

        return averages;
    }

    private boolean shouldIncludeMovies(CatalogFilterInput filter) {
        return filter == null || filter.contentType() == null || filter.contentType() == ContentType.MOVIE;
    }

    private boolean shouldIncludeSeries(CatalogFilterInput filter) {
        return filter == null || filter.contentType() == null || filter.contentType() == ContentType.SERIES;
    }

    private <R, T> Map<Long, List<T>> loadMovies(Collection<Long> movieIds,
                                                 java.util.function.Function<Movie, java.util.Collection<R>> relationExtractor,
                                                 java.util.function.Function<R, T> mapper) {
        Map<Long, List<T>> valuesByMovieId = initializeListMap(movieIds);
        movieRepository.findByIdIn(movieIds).forEach(movie ->
                relationExtractor.apply(movie).forEach(value ->
                        valuesByMovieId.get(movie.getId()).add(mapper.apply(value)))
        );
        return valuesByMovieId;
    }

    private <R, T> Map<Long, List<T>> loadSeries(Collection<Long> seriesIds,
                                                 java.util.function.Function<Series, java.util.Collection<R>> relationExtractor,
                                                 java.util.function.Function<R, T> mapper) {
        Map<Long, List<T>> valuesBySeriesId = initializeListMap(seriesIds);
        seriesRepository.findByIdIn(seriesIds).forEach(series ->
                relationExtractor.apply(series).forEach(value ->
                        valuesBySeriesId.get(series.getId()).add(mapper.apply(value)))
        );
        return valuesBySeriesId;
    }

    private <T> Map<Long, List<T>> initializeListMap(Collection<Long> ids) {
        Map<Long, List<T>> values = new LinkedHashMap<>();
        ids.forEach(id -> values.put(id, new java.util.ArrayList<>()));
        return values;
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
