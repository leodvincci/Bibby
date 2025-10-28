package com.penrose.bibby.library;

import com.penrose.bibby.library.book.BookStatus;

public class Main {

    public static void main(String[] args) {

        BookStatus bookStatus = BookStatus.AVAILABLE;

        System.out.println(BookStatus.valueOf("CHECKED_OUT"));

    }
}
