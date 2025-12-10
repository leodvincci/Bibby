diff --git a/src/main/java/com/penrose/bibby/cli/commands/BookCommands.java b/src/main/java/com/penrose/bibby/cli/commands/BookCommands.java
index 853db5f..a0c11c3 100644
--- a/src/main/java/com/penrose/bibby/cli/commands/BookCommands.java
+++ b/src/main/java/com/penrose/bibby/cli/commands/BookCommands.java
@@ -162,7 +162,24 @@ public class BookCommands extends AbstractShellComponent {
 
 
 
+    public String createBookCard(String title, String id, String author, String location) {
+
+        // %-42s ensures the text is left-aligned and padded to 42 characters
+        // The emojis take up extra visual space, so adjusted padding slightly
+        return """
+                ╭──────────────────────────────────────────────────────────────────────────────╮
+                │  📖 %-73s│
+                ├──────────────────────────────────────────────────────────────────────────────┤
+                │  ID: %-31s                                         │
+                │  Author: %-31s                                     │
+                │                                                                              │
+                │📍Location: %-35s                               │
+                ╰──────────────────────────────────────────────────────────────────────────────╯
+        """.formatted(title, id, author, location);
+    }
 
+// Usage:
+// System.out.println(createBookCard("Building Microservices", "978-1491950357", "Sam Newman", "PENDING / NOT SET"));
 
 
     // ───────────────────────────────────────────────────────────────────
@@ -221,21 +238,33 @@ public class BookCommands extends AbstractShellComponent {
     }
 
     public void searchByTitle() throws InterruptedException {
+
+
+
         System.out.println("\n\u001B[95mSearch by Title");
         String title = cliPrompt.promptForBookTitle();
-        System.out.println("\u001B[36m</>\u001B[0m:Hold on, I’m diving into the stacks — Let’s see if I can find " + title);
-        System.out.print("\u001B[36m</>\u001B[0m:");
-
         BookDTO bookDTO = bookFacade.findBookByTitle(title);
 
         if (bookDTO == null) {
             System.out.println("\n\u001B[36m</>\u001B[0m:I just flipped through every shelf — no luck this time.\n");
         }else if(bookDTO.shelfId() == null){
-            System.out.println("\nBook Was Found Without a Location\n");
+            String bookCard = createBookCard(
+                    bookDTO.title(),
+                    bookDTO.id().toString(),
+                    authorFacade.findByBookId(bookDTO.id()).toString(),
+                    "PENDING / NOT SET"
+            );
+            System.out.println(bookCard);
         }else{
             Optional<ShelfDTO> shelfDTO = shelfFacade.findShelfById(bookDTO.shelfId());
             Optional<BookcaseDTO> bookcaseDTO = bookcaseFacade.findBookCaseById(shelfDTO.get().bookcaseId());
-            System.out.println("\nBook Was Found \nBookcase: " + bookcaseDTO.get().bookcaseLabel() + "\n" + shelfDTO.get().shelfLabel() + "\n");
+            String bookCard = createBookCard(
+                    bookDTO.title(),
+                    bookDTO.id().toString(),
+                    authorFacade.findByBookId(bookDTO.id()).toString(),
+                    "Bookcase " + bookcaseDTO.get().bookcaseLabel() + ", Shelf " + shelfDTO.get().shelfLabel()
+            );
+            System.out.println(bookCard);
         }
         if (cliPrompt.promptSearchAgain()){
             findBook();
diff --git a/src/main/java/com/penrose/bibby/cli/prompt/application/CliPromptService.java b/src/main/java/com/penrose/bibby/cli/prompt/application/CliPromptService.java
index bc0393e..a664d95 100644
--- a/src/main/java/com/penrose/bibby/cli/prompt/application/CliPromptService.java
+++ b/src/main/java/com/penrose/bibby/cli/prompt/application/CliPromptService.java
@@ -227,8 +227,8 @@ public class CliPromptService implements PromptFacade {
     private Map<String, String> yesNoOptions() {
         // LinkedHashMap keeps insertion order so the menu shows in the order you add them
         Map<String, String> options = new LinkedHashMap<>();
-        options.put("Yes  — \u001B[32mLet's Do It\n\u001B[0m", "Yes");
-        options.put("No  —  \u001B[32mNot this time\n\u001B[0m", "No");
+        options.put("Yes  — \u001B[32mLet's Do It\u001B[0m", "Yes");
+        options.put("No  —  \u001B[32mNot this time\u001B[0m", "No");
         return options;
     }
 
diff --git a/src/main/java/com/penrose/bibby/library/author/contracts/AuthorDTO.java b/src/main/java/com/penrose/bibby/library/author/contracts/AuthorDTO.java
index f70eda8..afde5dc 100644
--- a/src/main/java/com/penrose/bibby/library/author/contracts/AuthorDTO.java
+++ b/src/main/java/com/penrose/bibby/library/author/contracts/AuthorDTO.java
@@ -43,4 +43,9 @@ public record AuthorDTO (Long id, String firstName, String lastName) {
         authorEntity.setLastName(authorRef.getAuthorLastName());
         return authorEntity;
     }
+
+    @Override
+    public String toString() {
+        return firstName + " " + lastName;
+    }
 }
