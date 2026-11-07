
package com.example.universitymanagement.controller;

import com.example.universitymanagement.dto.LoginRequest;
import com.example.universitymanagement.dto.LoginResponse;
import com.example.universitymanagement.model.User;
import com.example.universitymanagement.repository.StudentRepository;
import com.example.universitymanagement.repository.TeacherRepository;
import com.example.universitymanagement.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        // Student/teacher row ID, used by the frontend to load the user's own profile.
        Long profileId = switch (user.getRole()) {
            case STUDENT -> studentRepository.findByUserId(user.getId())
                    .orElseThrow().getId();
            case TEACHER -> teacherRepository.findByUserId(user.getId())
                    .orElseThrow().getId();
            case ADMIN -> null;
        };

        LoginResponse response = new LoginResponse(
                user.getId(),
                profileId,
                user.getEmail(),
                user.getRole().name(),
                "Login successful"
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {

        org.springframework.security.core.context.SecurityContextHolder
                .clearContext();

        jakarta.servlet.http.HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.noContent().build();
    }

}
