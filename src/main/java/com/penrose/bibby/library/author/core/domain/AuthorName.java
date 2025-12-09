package com.penrose.bibby.library.author.core.domain;

public class AuthorName {
    String authorName;

    public AuthorName(String authorName){
        this.authorName = authorName;
    }

    public String getAuthorName(){
        return this.authorName;
    }

    public String parseFirstName(){
        String[] parts = authorName.split(" ");
        return parts[0];
    }

    public String parseLastName(){
        String[] parts = authorName.split(" ");
        if(parts.length > 1){
            return parts[parts.length - 1];
        } else {
            return "";
        }
    }

    public String parseMiddleName(){
        String[] parts = authorName.split(" ");
        if(parts.length > 2){
            StringBuilder middleName = new StringBuilder();
            for(int i = 1; i < parts.length - 1; i++){
                middleName.append(parts[i]);
                if(i < parts.length - 2){
                    middleName.append(" ");
                }
            }
            return middleName.toString();
        } else {
            return "";
        }
    }

    public String normalized(){
        return authorName.trim().replaceAll("\\s+", " ");
    }


}
