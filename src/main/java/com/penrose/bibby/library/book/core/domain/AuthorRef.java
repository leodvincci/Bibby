package com.penrose.bibby.library.book.core.domain;

public class AuthorRef {
    private final Long authorId;
    private final AuthorName authorName;

    public AuthorRef(Long authorId, AuthorName authorName) {
        this.authorId = authorId;
        this.authorName = authorName;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public AuthorName getAuthorName() {
        return authorName;
    }

    public String getAuthorFirstName() {
        return authorName.firstName();
    }

    public String getAuthorLastName() {
        return authorName.lastName();
    }

}
