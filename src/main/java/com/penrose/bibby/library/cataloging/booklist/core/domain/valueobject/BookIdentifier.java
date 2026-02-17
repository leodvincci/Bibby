package com.penrose.bibby.library.cataloging.booklist.core.domain.valueobject;

/**
 * References a book within the cataloging context. Distinct from book.BookId to maintain module
 * independence.
 */
public record BookIdentifier(Long value) {}
