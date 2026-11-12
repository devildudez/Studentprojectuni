
package com.example.student.service.impl;

import com.example.student.dto.*;
import com.example.student.exception.EntityNotFoundException;
import com.example.student.mapper.StudentMapper;
import com.example.student.model.Student;
import com.example.student.repository.StudentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentMapper studentMapper;

    @InjectMocks
    private StudentServiceImpl studentService;

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        student.setName("Alex");
        student.setSurname("Smith");
        student.setEmail("alex@example.com");
        student.setPhoneNumber("123456789");
    }

    // TEST 1: Create a student
    @Test
    void save_shouldCreateStudent() {
        StudentRequest request = new StudentRequest(
            "Alex", "Smith", "123456789", "alex@example.com"
        );

        StudentResponse expected =
            new StudentResponse(1L, "Alex", "Smith");

        when(studentMapper.requestToStudent(request))
            .thenReturn(student);
        when(studentRepository.save(student))
            .thenReturn(student);
        when(studentMapper.studentToResponse(student))
            .thenReturn(expected);

        StudentResponse result = studentService.save(request);

        assertEquals(1L, result.getId());
        assertEquals("Alex", result.getName());
        verify(studentRepository).save(student);
    }

    // TEST 2: Find student by ID
    @Test
    void findById_shouldReturnStudent() {
        StudentFullResponse expected =
            new StudentFullResponse(
                1L, "Alex", "Smith",
                "123456789", "alex@example.com"
            );

        when(studentRepository.findById(1L))
            .thenReturn(Optional.of(student));
        when(studentMapper.studentToFullResponse(student))
            .thenReturn(expected);

        StudentFullResponse result = studentService.findById(1L);

        assertEquals("Alex", result.getName());
        assertEquals("alex@example.com", result.getEmail());
    }

    // TEST 3: Student not found
    @Test
    void findById_shouldThrowWhenStudentMissing() {
        when(studentRepository.findById(99L))
            .thenReturn(Optional.empty());

        assertThrows(
            EntityNotFoundException.class,
            () -> studentService.findById(99L)
        );

        verify(studentMapper, never())
            .studentToFullResponse(any());
    }

    // TEST 4: Get all students
    @Test
    void findAll_shouldReturnStudentList() {
        List<Student> students = List.of(student);

        StudentResponseForList item =
            new StudentResponseForList(1L, "Alex", "Smith");

        StudentResponseList expected =
            new StudentResponseList(List.of(item), 1);

        when(studentRepository.findAll())
            .thenReturn(students);
        when(studentMapper.studentListToStudentResponseList(students))
            .thenReturn(expected);

        StudentResponseList result = studentService.findAll();

        assertEquals(1, result.getTotalCount());
        assertEquals(
            "Alex",
            result.getResponse().get(0).getName()
        );
    }

    // TEST 5: Update student information
    @Test
    void update_shouldUpdateEmailAndPhone() {
        StudentUpdateRequest request =
            new StudentUpdateRequest(
                "987654321", "new@example.com"
            );

        StudentResponse expected =
            new StudentResponse(1L, "Alex", "Smith");

        when(studentRepository.findById(1L))
            .thenReturn(Optional.of(student));
        when(studentRepository.save(student))
            .thenReturn(student);
        when(studentMapper.studentToResponse(student))
            .thenReturn(expected);

        StudentResponse result =
            studentService.update(1L, request);

        assertEquals("987654321", student.getPhoneNumber());
        assertEquals("new@example.com", student.getEmail());
        assertEquals(1L, result.getId());

        verify(studentRepository).save(student);
    }

    // TEST 6: Keep unchanged fields
    @Test
    void update_shouldKeepExistingValuesWhenRequestFieldsAreNull() {
        StudentUpdateRequest request =
            new StudentUpdateRequest(null, "new@example.com");

        when(studentRepository.findById(1L))
            .thenReturn(Optional.of(student));
        when(studentRepository.save(student))
            .thenReturn(student);
        when(studentMapper.studentToResponse(student))
            .thenReturn(
                new StudentResponse(1L, "Alex", "Smith")
            );

        studentService.update(1L, request);

        assertEquals("123456789", student.getPhoneNumber());
        assertEquals("new@example.com", student.getEmail());
    }

    // TEST 7: Delete student
    @Test
    void deleteById_shouldCallRepository() {
        studentService.deleteById(1L);

        verify(studentRepository).deleteById(1L);
    }
}
