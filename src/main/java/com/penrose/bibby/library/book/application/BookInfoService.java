package com.penrose.bibby.library.book.application;

import com.penrose.bibby.library.book.infrastructure.external.GoogleBooksResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class BookInfoService {
    public final WebClient webClient;

    public BookInfoService(WebClient webClient) {
        this.webClient = webClient;
    }


    public Mono<GoogleBooksResponse> lookupBook(String isbn){
        String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:"+(isbn);
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(GoogleBooksResponse.class);
    }


}
