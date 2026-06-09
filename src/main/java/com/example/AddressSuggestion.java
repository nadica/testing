package com.example;

public record AddressSuggestion(
    String label,
    String detail,
    String egaid,
    Double east,
    Double north,
    Double lon,
    Double lat
) {
}
