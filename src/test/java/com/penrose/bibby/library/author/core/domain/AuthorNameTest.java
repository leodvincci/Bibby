package com.penrose.bibby.library.author.core.domain;

import com.penrose.bibby.library.catalog.author.core.domain.AuthorName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthorNameTest {

    @Test
    void normalized() {
        AuthorName authorName = new AuthorName("  John     Doe  ");
        System.out.println("Normalized Author Name: '" + authorName + "'");
        assertEquals("John Doe", authorName.toString());
    }

    @Test
    void normalized_2(){
        AuthorName authorName = new AuthorName("  Jane   A.           Smith  ");
        System.out.println("Normalized Author Name: '" + authorName.normalized() + "'");
        assertEquals("Jane A Smith", authorName.normalized());
    }


    @Test
    void parse_authors_middle_name(){
        AuthorName authorName = new AuthorName("  Jane   A.           Smith  ");
        AuthorName normalizedAuthorName = new AuthorName(authorName.normalized());
        String middleName = normalizedAuthorName.parseMiddleName();
        System.out.println("Parse Author Middle Name: '" + middleName + "'");

        assertEquals("A", middleName);
    }

    @Test
    void parse_authors_middle_name_no_period(){
        AuthorName authorName = new AuthorName("  Jane   A           Smith  ");
        AuthorName normalizedAuthorName = new AuthorName(authorName.normalized());
        String middleName = normalizedAuthorName.parseMiddleName();
        System.out.println("Parse Author Middle Name: '" + middleName + "'");

        assertEquals("A", middleName);
    }

    @Test
    void parse_authors_middle_name_no_middle_name(){
        AuthorName authorName = new AuthorName("  Jane            Smith  ");
        AuthorName normalizedAuthorName = new AuthorName(authorName.normalized());
        String middleName = normalizedAuthorName.parseMiddleName();
        System.out.println("Parse Author Middle Name: '" + middleName + "'");

        assertEquals("", middleName);
    }

    @Test
    void parse_authors_middle_name_only_first_name(){
        AuthorName authorName = new AuthorName("  Jane            ");
        AuthorName normalizedAuthorName = new AuthorName(authorName.normalized());
        String middleName = normalizedAuthorName.parseMiddleName();
        System.out.println("Parse Author Middle Name: '" + middleName + "'");

        assertEquals("", middleName);
    }


    @Test
    void parse_authors_first_name(){
        AuthorName authorName = new AuthorName("  Leo   D.           Penrose  ");
        AuthorName normalizedAuthorName = new AuthorName(authorName.normalized());
        String firstName = normalizedAuthorName.parseFirstName();
        System.out.println("Parse Author First Name: '" + firstName + "'");

        assertEquals("Leo", firstName);
    }

    @Test
    void getAuthorName() {
        AuthorName authorName = new AuthorName("  Leo   D.           Penrose  ");
        assertEquals("Leo D Penrose", authorName.getAuthorName());
    }

    @Test
    void parseFirstName() {
        AuthorName authorName = new AuthorName("  Leo   D.           Penrose  ");
        String firstName = authorName.parseFirstName();
        System.out.println("Parse Author First Name: '" + firstName + "'");

        assertEquals("Leo", firstName);
    }

    @Test
    void constructor_shouldNormalizeIncomingName() {
        AuthorName name = new AuthorName("  J.  R. R.   Tolkien.  ");

        // periods removed, extra spaces collapsed, trimmed
        assertEquals("J R R Tolkien", name.getAuthorName());
    }

    @Test
    void getAuthorName_shouldReturnNormalizedName() {
        AuthorName name = new AuthorName("  Jane   A.   Doe  ");

        assertEquals("Jane A Doe", name.getAuthorName());
    }

    @Test
    void parseFirstName_shouldReturnFirstToken() {
        AuthorName name = new AuthorName("  Isaac   Asimov  ");

        assertEquals("Isaac", name.parseFirstName());
    }

    @Test
    void parseLastName_shouldReturnLastTokenWhenMultipleParts() {
        AuthorName name = new AuthorName("  Neil   de   Grasse   Tyson  ");

        assertEquals("Tyson", name.parseLastName());
    }

    @Test
    void parseLastName_shouldReturnEmptyStringWhenSingleName() {
        AuthorName name = new AuthorName("Madonna");

        assertEquals("", name.parseLastName());
    }

    @Test
    void parseMiddleName_shouldReturnEmptyStringWhenNoMiddleName() {
        AuthorName name = new AuthorName("Ada Lovelace");

        assertEquals("", name.parseMiddleName());
    }

    @Test
    void parseMiddleName_shouldReturnSingleMiddleName() {
        AuthorName name = new AuthorName("Jane Amanda Doe");

        assertEquals("Amanda", name.parseMiddleName());
    }

    @Test
    void parseMiddleName_shouldReturnAllMiddleNamesJoinedBySingleSpaces() {
        AuthorName name = new AuthorName("John Ronald Reuel Tolkien");

        assertEquals("Ronald Reuel", name.parseMiddleName());
    }

    @Test
    void normalized_shouldReturnAlreadyNormalizedNameAndBeIdempotent() {
        AuthorName name = new AuthorName("  J.   K.   Rowling   ");

        String firstCall = name.normalized();
        String secondCall = name.normalized();

        assertEquals("J K Rowling", firstCall);
        assertEquals(firstCall, secondCall); // idempotent
        assertEquals(name.getAuthorName(), firstCall);
    }

    @Test
    void normalizeString_shouldNormalizeArbitraryInputString() {
        AuthorName name = new AuthorName("Placeholder"); // just to get an instance

        String normalized = name.normalizeString("  G.   R. R.   Martin. ");

        assertEquals("G R R Martin", normalized);
    }

    @Test
    void toString_shouldReturnNormalizedAuthorName() {
        AuthorName name = new AuthorName("  Chinua   Achebe. ");

        assertEquals("Chinua Achebe", name.toString());
    }



}