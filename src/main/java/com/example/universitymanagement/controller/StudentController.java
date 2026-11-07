
package com.example.universitymanagement.controller;

import com.example.universitymanagement.dto.StudentRequest;
import com.example.universitymanagement.dto.StudentResponse;
import com.example.universitymanagement.model.Student;
import com.example.universitymanagement.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
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
    public ResponseEntity<List<StudentResponse>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents()
                .stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudent(
            @PathVariable Long id, Authentication auth) {
        var student = studentService.getStudent(id);
        checkAccess(auth, student.getUser().getEmail());
        return ResponseEntity.ok(toResponse(student));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(
            @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(studentService.createStudent(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequest request,
            Authentication auth) {
        var student = studentService.getStudent(id);
        boolean isAdmin = checkAccess(auth, student.getUser().getEmail());
        return ResponseEntity.ok(
                toResponse(studentService.updateStudent(id, request, isAdmin)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    private StudentResponse toResponse(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getUser().getEmail(),
                student.getName(),
                student.getPhoneNumber(),
                student.getDepartment(),
                student.getBatch()
        );
    }
}
