package com.penrose.bibby.library.cataloging.book.infrastructure.external;

import java.util.List;

public record GoogleBookResponseBrief(
    String isbn, String title, List<String> authors, String publisher) {}
