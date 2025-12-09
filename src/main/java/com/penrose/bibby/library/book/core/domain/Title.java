package com.penrose.bibby.library.book.core.domain;

public class Title {

    String title;

    public Title(String title){
        this.title = title;
    }

    public String BookTitle(){
        return title;
    }

    public String getTitle(){
        return this.title;
    }

    public String title(){
        return title;
    }
    @Override
    public String toString() {
        return title;
    }
}
