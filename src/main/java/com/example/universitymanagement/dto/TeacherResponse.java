package com.example.universitymanagement.dto;

public record TeacherResponse(
        Long id,
        String email,
        String name,
        String phoneNumber,
        String department,
        String designation) {
}
