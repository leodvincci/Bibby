package com.penrose.bibby.library.catalog.book.core.application;

import com.penrose.bibby.library.catalog.book.infrastructure.external.GoogleBooksResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class IsbnLookupService {
    public final WebClient webClient;

    public IsbnLookupService(WebClient webClient) {
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
