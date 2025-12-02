package com.penrose.bibby.library.book.controller;

import com.penrose.bibby.library.book.dto.BookImportRequest;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookImportController {
    Logger log = org.slf4j.LoggerFactory.getLogger(BookImportController.class);

    @PostMapping("/import/books")
    public void importBook(@RequestBody BookImportRequest request) {
        log.info(request.isbn());
        System.out.println(request.isbn());
    }
}
