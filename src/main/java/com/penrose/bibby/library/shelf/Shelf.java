package com.penrose.bibby.library.shelf;

public class Shelf {
    private Long id;
    private String label;

    public Shelf() {

    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
}
