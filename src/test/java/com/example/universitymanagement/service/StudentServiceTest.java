package com.example.universitymanagement.service;

import com.example.universitymanagement.dto.StudentRequest;
import com.example.universitymanagement.model.Role;
import com.example.universitymanagement.model.Student;
import com.example.universitymanagement.model.User;
import com.example.universitymanagement.repository.StudentRepository;
import com.example.universitymanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Unit tests: no Spring, no database. Dependencies are Mockito mocks.
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StudentService studentService;

    @Test
    void createStudent_hashesPasswordAndAssignsStudentRole() {
        StudentRequest request = new StudentRequest(
                "rahim@uni.edu", "secret123", "Rahim", null, "CSE", "2021");

        when(userRepository.existsByEmail("rahim@uni.edu")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
        // save() returns whatever it was given, like a real repository would
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        Student student = studentService.createStudent(request);

        assertThat(student.getUser().getEmail()).isEqualTo("rahim@uni.edu");
        assertThat(student.getUser().getPassword()).isEqualTo("hashed-secret");
        assertThat(student.getUser().getRole()).isEqualTo(Role.STUDENT);
        assertThat(student.getName()).isEqualTo("Rahim");
        assertThat(student.getBatch()).isEqualTo("2021");
    }

    @Test
    void createStudent_withBlankPassword_returns400() {
        StudentRequest request = new StudentRequest(
                "rahim@uni.edu", " ", null, null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> studentService.createStudent(request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createStudent_withExistingEmail_returns409() {
        StudentRequest request = new StudentRequest(
                "taken@uni.edu", "secret123", null, null, null, null);
        when(userRepository.existsByEmail("taken@uni.edu")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> studentService.createStudent(request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateStudent_byNonAdmin_ignoresEmailChange() {
        Student existing = existingStudent();
        StudentRequest request = new StudentRequest(
                "new@uni.edu", null, "New Name", null, null, null);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(existing)).thenReturn(existing);

        Student updated = studentService.updateStudent(1L, request, false);

        assertThat(updated.getUser().getEmail()).isEqualTo("old@uni.edu");
        assertThat(updated.getName()).isEqualTo("New Name");
    }

    @Test
    void updateStudent_withBlankPassword_keepsCurrentPassword() {
        Student existing = existingStudent();
        StudentRequest request = new StudentRequest(
                "old@uni.edu", "", "Rahim", null, null, null);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(existing)).thenReturn(existing);

        Student updated = studentService.updateStudent(1L, request, true);

        assertThat(updated.getUser().getPassword()).isEqualTo("old-hash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void getStudent_whenMissing_returns404() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> studentService.getStudent(99L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Student existingStudent() {
        return new Student(new User("old@uni.edu", "old-hash", Role.STUDENT));
    }
}
