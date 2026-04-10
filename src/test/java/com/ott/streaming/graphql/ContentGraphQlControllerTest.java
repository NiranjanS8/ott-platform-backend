package com.ott.streaming.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ott.streaming.dto.content.GenrePayload;
import com.ott.streaming.dto.content.PersonPayload;
import com.ott.streaming.exception.GraphQlExceptionHandler;
import com.ott.streaming.service.ContentAdminService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQlTest(ContentGraphQlController.class)
@Import(GraphQlExceptionHandler.class)
class ContentGraphQlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ContentAdminService contentAdminService;

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void createGenreReturnsPayloadForAdmin() {
        when(contentAdminService.createGenre(any())).thenReturn(
                new GenrePayload(1L, "Action", Instant.parse("2026-04-10T10:00:00Z"), Instant.parse("2026-04-10T10:00:00Z"))
        );

        graphQlTester.document("""
                mutation {
                  createGenre(input: { name: "Action" }) {
                    id
                    name
                  }
                }
                """)
                .execute()
                .path("createGenre.id").entity(String.class).isEqualTo("1")
                .path("createGenre.name").entity(String.class).isEqualTo("Action");
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void createPersonReturnsPayloadForAdmin() {
        when(contentAdminService.createPerson(any())).thenReturn(
                new PersonPayload(2L, "Christopher Nolan", "Director", null,
                        Instant.parse("2026-04-10T10:00:00Z"), Instant.parse("2026-04-10T10:00:00Z"))
        );

        graphQlTester.document("""
                mutation {
                  createPerson(input: {
                    name: "Christopher Nolan"
                    biography: "Director"
                    profileImageUrl: null
                  }) {
                    id
                    name
                    biography
                  }
                }
                """)
                .execute()
                .path("createPerson.id").entity(String.class).isEqualTo("2")
                .path("createPerson.name").entity(String.class).isEqualTo("Christopher Nolan")
                .path("createPerson.biography").entity(String.class).isEqualTo("Director");
    }

    @Test
    void createGenreValidationRejectsBlankName() {
        graphQlTester.document("""
                mutation {
                  createGenre(input: { name: "" }) {
                    id
                  }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.getFirst().getMessage()).contains("Genre name is required");
                });
    }
}
