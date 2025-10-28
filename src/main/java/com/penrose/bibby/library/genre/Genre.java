package com.penrose.bibby.library.genre;

public class Genre {

    private Long id;
    String genreName;
    String genreDescription;

    public Genre(String genreName, String genreDescription) {
        this.genreName = genreName;
        this.genreDescription = genreDescription;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGenreName() {
        return genreName;
    }

    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }

    public String getGenreDescription() {
        return genreDescription;
    }

    public void setGenreDescription(String genreDescription) {
        this.genreDescription = genreDescription;
    }
}
