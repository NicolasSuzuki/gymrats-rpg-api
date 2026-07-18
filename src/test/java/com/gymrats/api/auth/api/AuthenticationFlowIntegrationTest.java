package com.gymrats.api.auth.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.gymrats.api.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationFlowIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired UserRepository users;

  @Test
  void registersAuthenticatesAndReturnsCurrentUser() throws Exception {
    var registerBody = """
        {"name":"Nicolas","email":"nicolas@example.com","password":"password123"}
        """;
    var registerResult = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.tokenType").value("Bearer")).andReturn();
    String registrationToken = JsonPath.read(registerResult.getResponse().getContentAsString(), "$.accessToken");

    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + registrationToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("nicolas@example.com"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.active").value(true))
        .andExpect(jsonPath("$.passwordHash").doesNotExist())
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());

    var loginResult = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
        {"email":"NICOLAS@example.com","password":"password123"}
        """))
        .andExpect(status().isOk()).andReturn();
    String loginToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + loginToken)).andExpect(status().isOk());
  }

  @Test
  void rejectsDuplicateEmailAndAnonymousAccess() throws Exception {
    var body = "{\"name\":\"Nicolas\",\"email\":\"duplicate@example.com\",\"password\":\"password123\"}";
    var duplicatedWithDifferentCase = "{\"name\":\"Nicolas\",\"email\":\"DUPLICATE@example.com\",\"password\":\"password123\"}";
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(duplicatedWithDifferentCase)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"email\":\"invalid\",\"password\":\"short\"}"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR")).andExpect(jsonPath("$.fields").isArray());
    mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer invalid-token")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void rejectsInvalidCredentialsAndInactiveUsersWithTheSameMessage() throws Exception {
    var body = "{\"name\":\"Inactive\",\"email\":\"inactive@example.com\",\"password\":\"password123\"}";
    var registration = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn();
    String issuedToken = JsonPath.read(registration.getResponse().getContentAsString(), "$.accessToken");
    var inactive = users.findByEmailIgnoreCase("inactive@example.com").orElseThrow();
    inactive.deactivate();
    users.saveAndFlush(inactive);

    var inactiveLogin = "{\"email\":\"inactive@example.com\",\"password\":\"password123\"}";
    var unknownLogin = "{\"email\":\"unknown@example.com\",\"password\":\"password123\"}";
    mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(inactiveLogin))
        .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS")).andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
    mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(unknownLogin))
        .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS")).andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + issuedToken))
        .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }
}
