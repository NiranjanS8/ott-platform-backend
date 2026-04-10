package com.ott.streaming.service;

import com.ott.streaming.dto.content.CreateGenreInput;
import com.ott.streaming.dto.content.CreatePersonInput;
import com.ott.streaming.dto.content.GenrePayload;
import com.ott.streaming.dto.content.PersonPayload;
import com.ott.streaming.dto.content.UpdateGenreInput;
import com.ott.streaming.dto.content.UpdatePersonInput;
import com.ott.streaming.entity.Genre;
import com.ott.streaming.entity.Person;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.GenreRepository;
import com.ott.streaming.repository.PersonRepository;
import java.util.Locale;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ContentAdminService {

    private final GenreRepository genreRepository;
    private final PersonRepository personRepository;

    public ContentAdminService(GenreRepository genreRepository, PersonRepository personRepository) {
        this.genreRepository = genreRepository;
        this.personRepository = personRepository;
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
}
