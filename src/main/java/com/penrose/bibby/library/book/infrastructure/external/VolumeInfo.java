package com.penrose.bibby.library.book.infrastructure.external;

import java.util.List;

public record VolumeInfo(String title,
                         List<String> authors,
                         String publisher,
                         String description,
                         String isbn,
                         List<String> categories,
                         String publishedDate
                        ) {
}
