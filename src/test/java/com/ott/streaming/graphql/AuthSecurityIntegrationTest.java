package com.ott.streaming.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ott.streaming.entity.Role;
import com.ott.streaming.entity.User;
import com.ott.streaming.repository.UserRepository;
import com.ott.streaming.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private record GraphQlRequest(String query) {
    }
}
