package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {

    @Test
    void addShouldReturnExpectedSum() {
        App app = new App();
        assertEquals(5, app.add(2, 3));
    }

    @Test
    void addShouldHandleNegativeValues() {
        App app = new App();
        assertEquals(-1, app.add(2, -3));
    }
}
