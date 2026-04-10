package com.ott.streaming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.content.CreateGenreInput;
import com.ott.streaming.dto.content.CreatePersonInput;
import com.ott.streaming.dto.content.UpdateGenreInput;
import com.ott.streaming.dto.content.UpdatePersonInput;
import com.ott.streaming.entity.Genre;
import com.ott.streaming.entity.Person;
import com.ott.streaming.exception.ApiException;
import com.ott.streaming.repository.GenreRepository;
import com.ott.streaming.repository.PersonRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContentAdminServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private PersonRepository personRepository;

    private ContentAdminService contentAdminService;

    @BeforeEach
    void setUp() {
        contentAdminService = new ContentAdminService(genreRepository, personRepository);
    }

    @Test
    void createGenreNormalizesNameAndRejectsDuplicates() {
        when(genreRepository.existsByNameIgnoreCase("Action")).thenReturn(false);
        when(genreRepository.save(any(Genre.class))).thenAnswer(invocation -> {
            Genre genre = invocation.getArgument(0);
            genre.setId(1L);
            ReflectionTestUtils.setField(genre, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
            ReflectionTestUtils.setField(genre, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
            return genre;
        });

        var payload = contentAdminService.createGenre(new CreateGenreInput("  Action  "));

        assertThat(payload.id()).isEqualTo(1L);
        assertThat(payload.name()).isEqualTo("Action");
    }

    @Test
    void createGenreThrowsWhenNameAlreadyExists() {
        when(genreRepository.existsByNameIgnoreCase("Action")).thenReturn(true);

        assertThatThrownBy(() -> contentAdminService.createGenre(new CreateGenreInput("Action")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Genre already exists");
    }

    @Test
    void updateGenreRejectsMissingGenre() {
        when(genreRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contentAdminService.updateGenre(3L, new UpdateGenreInput("Drama")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Genre not found");
    }

    @Test
    void createPersonNormalizesOptionalFields() {
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> {
            Person person = invocation.getArgument(0);
            person.setId(7L);
            ReflectionTestUtils.setField(person, "createdAt", Instant.parse("2026-04-10T10:00:00Z"));
            ReflectionTestUtils.setField(person, "updatedAt", Instant.parse("2026-04-10T10:00:00Z"));
            return person;
        });

        var payload = contentAdminService.createPerson(new CreatePersonInput("  Keanu Reeves  ", "  Actor  ", "   "));

        assertThat(payload.id()).isEqualTo(7L);
        assertThat(payload.name()).isEqualTo("Keanu Reeves");
        assertThat(payload.biography()).isEqualTo("Actor");
        assertThat(payload.profileImageUrl()).isNull();
    }

    @Test
    void deletePersonRemovesExistingRecord() {
        when(personRepository.existsById(9L)).thenReturn(true);

        assertThat(contentAdminService.deletePerson(9L)).isTrue();
        verify(personRepository).deleteById(9L);
    }
}
