package com.example.universitymanagement.dto;

public record StudentResponse(
        Long id,
        String email,
        String name,
        String phoneNumber,
        String department,
        String batch) {
}
