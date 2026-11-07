package com.example.universitymanagement.service;

import com.example.universitymanagement.dto.StudentRequest;
import com.example.universitymanagement.model.Role;
import com.example.universitymanagement.model.Student;
import com.example.universitymanagement.model.User;
import com.example.universitymanagement.repository.StudentRepository;
import com.example.universitymanagement.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Student getStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found"));
    }

    @Transactional
    public Student createStudent(StudentRequest request) {
        if (!StringUtils.hasText(request.email())
                || !StringUtils.hasText(request.password())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Email and password are required");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email already exists");
        }

        User user = userRepository.save(new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.STUDENT
        ));

        Student student = new Student(user);
        applyProfile(student, request);
        return studentRepository.save(student);
    }

    @Transactional
    public Student updateStudent(Long id, StudentRequest request, boolean isAdmin) {
        Student student = getStudent(id);
        User user = student.getUser();

        // Only admins may change email; it is the identity stored in the login session.
        if (isAdmin && StringUtils.hasText(request.email())) {
            if (!user.getEmail().equals(request.email())
                    && userRepository.existsByEmail(request.email())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Email already exists");
            }
            user.setEmail(request.email());
        }

        if (StringUtils.hasText(request.password())) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        applyProfile(student, request);
        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = getStudent(id);
        studentRepository.delete(student);
        userRepository.delete(student.getUser());
    }

    private void applyProfile(Student student, StudentRequest request) {
        student.setName(request.name());
        student.setPhoneNumber(request.phoneNumber());
        student.setDepartment(request.department());
        student.setBatch(request.batch());
    }
}
