package com.ott.streaming.service;

import com.ott.streaming.dto.content.CreateGenreInput;
import com.ott.streaming.dto.content.CreateMovieInput;
import com.ott.streaming.dto.content.CreatePersonInput;
import com.ott.streaming.dto.content.CreateSeriesInput;
import com.ott.streaming.dto.content.GenrePayload;
import com.ott.streaming.dto.content.MoviePayload;
import com.ott.streaming.dto.content.PersonPayload;
import com.ott.streaming.dto.content.SeriesPayload;
import com.ott.streaming.dto.content.UpdateGenreInput;
import com.ott.streaming.dto.content.UpdateMovieInput;
import com.ott.streaming.dto.content.UpdatePersonInput;
import com.ott.streaming.dto.content.UpdateSeriesInput;
import com.ott.streaming.entity.Genre;
import com.ott.streaming.entity.Movie;
import com.ott.streaming.entity.Person;
import com.ott.streaming.entity.Series;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.GenreRepository;
import com.ott.streaming.repository.MovieRepository;
import com.ott.streaming.repository.PersonRepository;
import com.ott.streaming.repository.SeriesRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ContentAdminService {

    private final GenreRepository genreRepository;
    private final PersonRepository personRepository;
    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;

    public ContentAdminService(GenreRepository genreRepository,
                               PersonRepository personRepository,
                               MovieRepository movieRepository,
                               SeriesRepository seriesRepository) {
        this.genreRepository = genreRepository;
        this.personRepository = personRepository;
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public GenrePayload createGenre(CreateGenreInput input) {
        String normalizedName = normalizeName(input.name());
        if (genreRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ApiException("Genre already exists");
        }

        Genre genre = new Genre();
        genre.setName(normalizedName);
        return toGenrePayload(genreRepository.save(genre));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public GenrePayload updateGenre(Long id, UpdateGenreInput input) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new ApiException("Genre not found"));

        String normalizedName = normalizeName(input.name());
        genreRepository.findByNameIgnoreCase(normalizedName)
                .filter(existingGenre -> !existingGenre.getId().equals(id))
                .ifPresent(existingGenre -> {
                    throw new ApiException("Genre already exists");
                });

        genre.setName(normalizedName);
        return toGenrePayload(genreRepository.save(genre));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteGenre(Long id) {
        if (!genreRepository.existsById(id)) {
            throw new ApiException("Genre not found");
        }

        genreRepository.deleteById(id);
        return true;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PersonPayload createPerson(CreatePersonInput input) {
        Person person = new Person();
        person.setName(normalizeName(input.name()));
        person.setBiography(normalizeOptionalText(input.biography()));
        person.setProfileImageUrl(normalizeOptionalText(input.profileImageUrl()));
        return toPersonPayload(personRepository.save(person));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PersonPayload updatePerson(Long id, UpdatePersonInput input) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ApiException("Person not found"));

        person.setName(normalizeName(input.name()));
        person.setBiography(normalizeOptionalText(input.biography()));
        person.setProfileImageUrl(normalizeOptionalText(input.profileImageUrl()));
        return toPersonPayload(personRepository.save(person));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean deletePerson(Long id) {
        if (!personRepository.existsById(id)) {
            throw new ApiException("Person not found");
        }

        personRepository.deleteById(id);
        return true;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public MoviePayload createMovie(CreateMovieInput input) {
        Movie movie = new Movie();
        applyMovieInput(movie, input);
        return toMoviePayload(movieRepository.save(movie));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public MoviePayload updateMovie(Long id, UpdateMovieInput input) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ApiException("Movie not found"));

        applyMovieInput(movie, input);
        return toMoviePayload(movieRepository.save(movie));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteMovie(Long id) {
        if (!movieRepository.existsById(id)) {
            throw new ApiException("Movie not found");
        }

        movieRepository.deleteById(id);
        return true;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public SeriesPayload createSeries(CreateSeriesInput input) {
        Series series = new Series();
        applySeriesInput(series, input);
        return toSeriesPayload(seriesRepository.save(series));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public SeriesPayload updateSeries(Long id, UpdateSeriesInput input) {
        Series series = seriesRepository.findById(id)
                .orElseThrow(() -> new ApiException("Series not found"));

        applySeriesInput(series, input);
        return toSeriesPayload(seriesRepository.save(series));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteSeries(Long id) {
        if (!seriesRepository.existsById(id)) {
            throw new ApiException("Series not found");
        }

        seriesRepository.deleteById(id);
        return true;
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

    private MoviePayload toMoviePayload(Movie movie) {
        return new MoviePayload(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                formatDate(movie.getReleaseDate()),
                movie.getDurationMinutes(),
                movie.getMaturityRating(),
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
                series.getCreatedAt(),
                series.getUpdatedAt()
        );
    }

    private void applyMovieInput(Movie movie, CreateMovieInput input) {
        movie.setTitle(normalizeName(input.title()));
        movie.setDescription(normalizeOptionalText(input.description()));
        movie.setReleaseDate(parseDate(input.releaseDate(), "movie release date"));
        movie.setDurationMinutes(input.durationMinutes());
        movie.setMaturityRating(normalizeOptionalText(input.maturityRating()));
        movie.setGenres(resolveGenres(input.genreIds()));
        movie.setCast(resolvePersons(input.castIds(), "cast"));
        movie.setDirectors(resolvePersons(input.directorIds(), "director"));
    }

    private void applyMovieInput(Movie movie, UpdateMovieInput input) {
        movie.setTitle(normalizeName(input.title()));
        movie.setDescription(normalizeOptionalText(input.description()));
        movie.setReleaseDate(parseDate(input.releaseDate(), "movie release date"));
        movie.setDurationMinutes(input.durationMinutes());
        movie.setMaturityRating(normalizeOptionalText(input.maturityRating()));
        movie.setGenres(resolveGenres(input.genreIds()));
        movie.setCast(resolvePersons(input.castIds(), "cast"));
        movie.setDirectors(resolvePersons(input.directorIds(), "director"));
    }

    private void applySeriesInput(Series series, CreateSeriesInput input) {
        series.setTitle(normalizeName(input.title()));
        series.setDescription(normalizeOptionalText(input.description()));
        series.setReleaseDate(parseDate(input.releaseDate(), "series release date"));
        series.setEndDate(parseDate(input.endDate(), "series end date"));
        series.setMaturityRating(normalizeOptionalText(input.maturityRating()));
        series.setGenres(resolveGenres(input.genreIds()));
        series.setCast(resolvePersons(input.castIds(), "cast"));
        series.setDirectors(resolvePersons(input.directorIds(), "director"));
    }

    private void applySeriesInput(Series series, UpdateSeriesInput input) {
        series.setTitle(normalizeName(input.title()));
        series.setDescription(normalizeOptionalText(input.description()));
        series.setReleaseDate(parseDate(input.releaseDate(), "series release date"));
        series.setEndDate(parseDate(input.endDate(), "series end date"));
        series.setMaturityRating(normalizeOptionalText(input.maturityRating()));
        series.setGenres(resolveGenres(input.genreIds()));
        series.setCast(resolvePersons(input.castIds(), "cast"));
        series.setDirectors(resolvePersons(input.directorIds(), "director"));
    }

    private Set<Genre> resolveGenres(Set<Long> genreIds) {
        List<Genre> genres = genreRepository.findAllById(genreIds);
        if (genres.size() != genreIds.size()) {
            throw new ApiException("One or more genres were not found");
        }

        return new HashSet<>(genres);
    }

    private Set<Person> resolvePersons(Set<Long> personIds, String roleLabel) {
        List<Person> persons = personRepository.findAllById(personIds);
        if (persons.size() != personIds.size()) {
            throw new ApiException("One or more " + roleLabel + " members were not found");
        }

        return new HashSet<>(persons);
    }

    private String normalizeName(String value) {
        return value.trim().replaceAll("\\s{2,}", " ");
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private LocalDate parseDate(String value, String fieldLabel) {
        String normalizedValue = normalizeOptionalText(value);
        if (normalizedValue == null) {
            return null;
        }

        try {
            return LocalDate.parse(normalizedValue);
        } catch (DateTimeParseException ex) {
            throw new ApiException("Invalid " + fieldLabel + " format. Expected yyyy-MM-dd");
        }
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : date.toString();
    }
}
