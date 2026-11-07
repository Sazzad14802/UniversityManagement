package com.example.universitymanagement.dto;

import jakarta.validation.constraints.Email;

// Email and password are required on create. On update a blank password keeps the
// current one, and email is applied only when an admin sends it.
public record TeacherRequest(
        @Email String email,
        String password,
        String name,
        String phoneNumber,
        String department,
        String designation) {
}
