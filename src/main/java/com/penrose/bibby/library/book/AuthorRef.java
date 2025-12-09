package com.penrose.bibby.library.book;

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
        return authorName.getFirstName();
    }

    public String getAuthorLastName() {
        return authorName.getLastName();
    }

}
