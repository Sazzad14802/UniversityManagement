package com.example.universitymanagement.dto;

public record LoginResponse(Long userId, Long profileId, String email, String role, String message) {
}
