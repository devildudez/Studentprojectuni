package com.example.student.controller;

import com.example.student.configuration.SecurityConfiguration;
import com.example.student.dto.*;
import com.example.student.security.JwtFilter;
import com.example.student.security.JwtService;
import com.example.student.service.CustomUserDetailsService;
import com.example.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
@Import({SecurityConfiguration.class, JwtFilter.class})
class StudentControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean StudentService studentService;
    @MockitoBean JwtService jwtService;
    @MockitoBean CustomUserDetailsService userDetailsService;

    @Test
    void findAllAllowsTeacherAndReturnsList() throws Exception {
        when(studentService.findAll()).thenReturn(new StudentResponseList(
                List.of(new StudentResponseForList(1L, "Alex", "Smith")), 1));

        mvc.perform(get("/api/v1/student").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.response[0].name").value("Alex"));
        verify(studentService).findAll();
    }

    @Test
    void findAllForbidsStudentRole() throws Exception {
        mvc.perform(get("/api/v1/student").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(studentService);
    }

    @Test
    void findStudentByIdReturnsFullResponse() throws Exception {
        when(studentService.findById(5L)).thenReturn(new StudentFullResponse(5L, "Alex", "Smith", "123", "a@example.com"));
        mvc.perform(get("/api/v1/student/5").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.email").value("a@example.com"));
        verify(studentService).findById(5L);
    }

    @Test
    void getFullInfoByIdReturnsFullResponse() throws Exception {
        when(studentService.findById(6L)).thenReturn(new StudentFullResponse(6L, "Sam", "Lee", "456", "s@example.com"));
        mvc.perform(get("/api/v1/student/info/6").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Sam"));
        verify(studentService).findById(6L);
    }

    @Test
    void createStudentRequiresAdminAndReturnsCreatedDto() throws Exception {
        when(studentService.save(any())).thenReturn(new StudentResponse(7L, "Jane", "Doe"));
        mvc.perform(post("/api/v1/student/save").with(user("admin").roles("ADMIN"))
                        .contentType("application/json")
                        .content("{\"name\":\"Jane\",\"surname\":\"Doe\",\"phone_number\":\"123\",\"email\":\"jane@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Jane"));
        verify(studentService).save(any(StudentRequest.class));
    }

    @Test
    void createStudentForbidsTeacher() throws Exception {
        mvc.perform(post("/api/v1/student/save").with(user("teacher").roles("TEACHER"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(studentService);
    }

    @Test
    void updateStudentPassesIdAndRequest() throws Exception {
        when(studentService.update(eq(8L), any())).thenReturn(new StudentResponse(8L, "Jane", "Doe"));
        mvc.perform(put("/api/v1/student/8").with(user("admin").roles("ADMIN"))
                        .contentType("application/json").content("{\"phoneNumber\":\"999\",\"email\":\"new@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(8));
        verify(studentService).update(eq(8L), any(StudentUpdateRequest.class));
    }

    @Test
    void deleteStudentPassesId() throws Exception {
        mvc.perform(delete("/api/v1/student/9").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(""));
        verify(studentService).deleteById(9L);
    }

    @Test
    void studentEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/student")).andExpect(status().isForbidden());
        verifyNoInteractions(studentService);
    }
}
