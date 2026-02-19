package com.penrose.bibby.library.cataloging.book.core.domain.valueObject;

public class Isbn {

    public final String isbn;

    // TODO(Leo): add ISBN-13 validation
    public Isbn(String rawIsbn) {
        if(rawIsbn == null || rawIsbn.trim().isEmpty()){
            throw new IllegalArgumentException("ISBN cannot be null or empty");
        }
        if(!rawIsbn.matches("[0-9\\-]+")){
            throw new IllegalArgumentException("ISBN can only contain digits and hyphens");
        }
        if(rawIsbn.length() > 17){
            throw new IllegalArgumentException("ISBN length cannot exceed 17 characters including hyphens");
        }
        if(rawIsbn.chars().filter(ch -> ch == '-').count() > 4){
            throw new IllegalArgumentException("ISBN cannot contain more than 4 hyphens");
        }
        if(rawIsbn.contains("--")){
            throw new IllegalArgumentException("ISBN cannot contain consecutive hyphens");
        }
        if(rawIsbn.startsWith("-") || rawIsbn.endsWith("-")){
            throw new IllegalArgumentException("ISBN cannot start or end with a hyphen");
        }
        if(rawIsbn.length() < 10){
            throw new IllegalArgumentException("ISBN length must be at least 10 characters");
        }

        this.isbn = rawIsbn;
    }

    public String isbn(){
        return isbn;
    }

    /* TODO(Leo) fix normalization logic */
    public String normalized(){
        return isbn.replaceAll("-", "").trim();
    }

    @Override
    public String toString() {
        return isbn;
    }
}
