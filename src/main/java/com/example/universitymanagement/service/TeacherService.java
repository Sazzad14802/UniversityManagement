package com.example.universitymanagement.service;

import com.example.universitymanagement.dto.TeacherRequest;
import com.example.universitymanagement.model.Role;
import com.example.universitymanagement.model.Teacher;
import com.example.universitymanagement.model.User;
import com.example.universitymanagement.repository.TeacherRepository;
import com.example.universitymanagement.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TeacherService(TeacherRepository teacherRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    public Teacher getTeacher(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Teacher not found"));
    }

    @Transactional
    public Teacher createTeacher(TeacherRequest request) {
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
                Role.TEACHER
        ));

        Teacher teacher = new Teacher(user);
        applyProfile(teacher, request);
        return teacherRepository.save(teacher);
    }

    @Transactional
    public Teacher updateTeacher(Long id, TeacherRequest request, boolean isAdmin) {
        Teacher teacher = getTeacher(id);
        User user = teacher.getUser();

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

        applyProfile(teacher, request);
        return teacherRepository.save(teacher);
    }

    @Transactional
    public void deleteTeacher(Long id) {
        Teacher teacher = getTeacher(id);
        teacherRepository.delete(teacher);
        userRepository.delete(teacher.getUser());
    }

    private void applyProfile(Teacher teacher, TeacherRequest request) {
        teacher.setName(request.name());
        teacher.setPhoneNumber(request.phoneNumber());
        teacher.setDepartment(request.department());
        teacher.setDesignation(request.designation());
    }
}
