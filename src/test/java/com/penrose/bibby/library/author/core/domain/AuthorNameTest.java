package com.penrose.bibby.library.author.core.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthorNameTest {

    @Test
    void normalized() {
        AuthorName authorName = new AuthorName("  John     Doe  ");
        assertEquals("John Doe", authorName.normalized());
    }
}