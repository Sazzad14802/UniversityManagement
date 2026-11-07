
package com.example.universitymanagement.controller;

import com.example.universitymanagement.dto.TeacherRequest;
import com.example.universitymanagement.dto.TeacherResponse;
import com.example.universitymanagement.model.Teacher;
import com.example.universitymanagement.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    // Returns whether the caller is an admin; throws 403 if a non-admin isn't the owner.
    private boolean checkAccess(Authentication auth, String ownerEmail) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !auth.getName().equals(ownerEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied");
        }
        return isAdmin;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<TeacherResponse>> getAllTeachers() {
        return ResponseEntity.ok(teacherService.getAllTeachers()
                .stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeacherResponse> getTeacher(
            @PathVariable Long id,
            Authentication auth) {

        Teacher teacher = teacherService.getTeacher(id);
        checkAccess(auth, teacher.getUser().getEmail());

        return ResponseEntity.ok(toResponse(teacher));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<TeacherResponse> createTeacher(
            @Valid @RequestBody TeacherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(teacherService.createTeacher(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeacherResponse> updateTeacher(
            @PathVariable Long id,
            @Valid @RequestBody TeacherRequest request,
            Authentication auth) {

        Teacher teacher = teacherService.getTeacher(id);
        boolean isAdmin = checkAccess(auth, teacher.getUser().getEmail());

        return ResponseEntity.ok(
                toResponse(teacherService.updateTeacher(id, request, isAdmin)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }

    private TeacherResponse toResponse(Teacher teacher) {
        return new TeacherResponse(
                teacher.getId(),
                teacher.getUser().getEmail(),
                teacher.getName(),
                teacher.getPhoneNumber(),
                teacher.getDepartment(),
                teacher.getDesignation()
        );
    }
}
