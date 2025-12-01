# Devlog: Google Books API Integration for Barcode Scanner

**Date:** 2025-11-30  
**Feature:** ISBN Lookup via Google Books API

## Summary

Integrated the Google Books API with Bibby's barcode scanner to automatically fetch book metadata when scanning ISBN barcodes. This eliminates manual data entry for book imports.

## Problem

Previously, adding books required manually entering title, author, and other metadata. With the webcam barcode scanner in place, the next logical step was to automatically retrieve book information from the scanned ISBN.

## Solution

### Backend Changes

**Added WebFlux for Reactive HTTP Client**

Added `spring-boot-starter-webflux` dependency to `pom.xml` for non-blocking HTTP calls to external APIs:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Created BookInfoService**

New service class that wraps WebClient calls to the Google Books API:
- Endpoint: `https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}`
- Returns reactive `Mono<String>` with full JSON response

**Added Lookup Endpoint**

New endpoint in `BookController`:

```java
@GetMapping("/lookup/{isbn}")
public Mono<String> getBookInfo(@PathVariable String isbn){
    return bookInfoService.lookupBook(isbn);
}
```

### Frontend Changes

Updated `script.js` to call the new lookup endpoint:
- Changed from `POST /import/books` to `GET /lookup/{isbn}`
- Simplified fetch call (no JSON body needed)

## Example Response

Scanning ISBN `9781449373320` returns:

```json
{
  "title": "Designing Data-intensive Applications",
  "subtitle": "The Big Ideas Behind Reliable, Scalable, and Maintainable Systems",
  "authors": ["Martin Kleppmann"],
  "publisher": "Oreilly & Associates Incorporated",
  "publishedDate": "2017",
  "pageCount": 590,
  "categories": ["Computers"],
  "description": "..."
}
```

## Technical Decisions

**Why WebFlux/WebClient over RestTemplate?**
- RestTemplate is deprecated in favor of WebClient
- Non-blocking I/O is better for external API calls
- Aligns with modern Spring practices
- Good learning opportunity for reactive programming

**Why return raw JSON for now?**
- Quick iteration to prove the integration works
- Parsing and mapping to domain objects is the next step

## Next Steps

1. Parse the Google Books JSON response into a DTO
2. Map the response to Bibby's Book domain model
3. Auto-populate a book creation form or directly create the book
4. Handle cases where ISBN is not found
5. Consider caching responses to reduce API calls

## Learnings

- First time using Spring WebFlux and reactive types (`Mono`, `Flux`)
- Google Books API is free and doesn't require an API key for basic lookups
- The reactive chain with `.doOnNext()` is useful for debugging/logging

## Files Changed

- `pom.xml` — Added webflux dependencies
- `BookController.java` — Added `/lookup/{isbn}` endpoint
- `BookInfoService.java` — New service for Google Books API calls
- `script.js` — Updated scanner to use GET endpoint
