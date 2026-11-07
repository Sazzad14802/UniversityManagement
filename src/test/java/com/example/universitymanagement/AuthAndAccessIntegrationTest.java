package com.example.universitymanagement;

import com.example.universitymanagement.dto.StudentRequest;
import com.example.universitymanagement.service.StudentService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Integration tests: the whole app runs against a real PostgreSQL database,
// and requests go through the real security filters and controllers.
// @Transactional rolls back each test's data, so tests don't affect each other.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthAndAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentService studentService;

    @Test
    void login_withCorrectPassword_returnsRole_andWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("admin@example.com", "123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("admin@example.com", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void student_cannotListAllStudents() throws Exception {
        studentService.createStudent(new StudentRequest(
                "it-student@test.com", "pass123", null, null, null, null));
        MockHttpSession session = login("it-student@test.com", "pass123");

        mockMvc.perform(get("/api/students").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesStudent_thenStudentLogsInAndReadsOwnProfile() throws Exception {
        MockHttpSession admin = login("admin@example.com", "123");

        MvcResult created = mockMvc.perform(post("/api/students").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "it-new@test.com", "password": "pass123", "batch": "2024"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        int studentId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        MockHttpSession student = login("it-new@test.com", "pass123");

        mockMvc.perform(get("/api/students/" + studentId).session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("it-new@test.com"))
                .andExpect(jsonPath("$.batch").value("2024"));
    }

    // Logs in and returns the session, which holds the login like the JSESSIONID cookie does.
    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }

    private String loginJson(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }
}
