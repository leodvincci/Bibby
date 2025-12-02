package com.penrose.bibby.library.book.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class BookInfoService {
    public final WebClient webClient;

    public BookInfoService(WebClient webClient) {
        this.webClient = webClient;
    }


    public Mono<String> lookupBook(String isbn){
        String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:"+(isbn);
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class);
    }

}
