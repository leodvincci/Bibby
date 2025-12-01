diff --git a/pom.xml b/pom.xml
index eea7046..0ec1074 100644
--- a/pom.xml
+++ b/pom.xml
@@ -59,6 +59,15 @@
             <artifactId>postgresql</artifactId>
             <scope>runtime</scope>
         </dependency>
+        <dependency>
+            <groupId>org.springframework.boot</groupId>
+            <artifactId>spring-boot-starter-webflux</artifactId>
+        </dependency>
+        <dependency>
+     [log.md](log.md)       <groupId>io.projectreactor</groupId>
+            <artifactId>reactor-test</artifactId>
+            <scope>test</scope>
+        </dependency>
     </dependencies>
     <dependencyManagement>
         <dependencies>
diff --git a/src/main/java/com/penrose/bibby/library/book/BookController.java b/src/main/java/com/penrose/bibby/library/book/BookController.java
index 499473f..854cb4a 100644
--- a/src/main/java/com/penrose/bibby/library/book/BookController.java
+++ b/src/main/java/com/penrose/bibby/library/book/BookController.java
@@ -3,16 +3,19 @@ package com.penrose.bibby.library.book;
 import com.penrose.bibby.library.author.AuthorRepository;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.*;
+import reactor.core.publisher.Mono;
 
 @RestController
 public class BookController {
 
     final BookService bookService;
     final AuthorRepository authorRepository;
+    final BookInfoService bookInfoService;
 
-    public BookController(BookService bookService, AuthorRepository authorRepository, BookRepository bookRepository){
+    public BookController(BookService bookService, AuthorRepository authorRepository, BookRepository bookRepository, BookInfoService bookInfoService){
         this.bookService = bookService;
         this.authorRepository = authorRepository;
+        this.bookInfoService = bookInfoService;
     }
 
     @PostMapping("api/v1/books")
@@ -21,6 +24,12 @@ public class BookController {
         return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
     }
 
+    @GetMapping("/lookup/{isbn}")
+    public Mono<String> getBookInfo(@PathVariable String isbn){
+        System.out.println("Controller Lookup For " + isbn);
+        return bookInfoService.lookupBook(isbn).doOnNext( body -> System.out.println(body));
+    }
+
     @GetMapping("api/v1/books")
     public void findBookByTitle(@RequestBody BookRequestDTO requestDTO){
         System.out.println("Controller Search For " + requestDTO.title());
diff --git a/src/main/resources/static/script.js b/src/main/resources/static/script.js
index 20474f3..aed7d30 100644
--- a/src/main/resources/static/script.js
+++ b/src/main/resources/static/script.js
@@ -7,7 +7,7 @@ const statusDiv = document.getElementById("status"); // add a <div id="status">
 const codeReader = new BrowserMultiFormatReader();
 
 // 👉 change this to whatever your Bibby endpoint is
-const API_URL = "/import/books";
+const API_URL = "/lookup";
 
 // simple dedupe so we don't spam the API
 let lastIsbn = "";
@@ -20,12 +20,8 @@ async function sendToApi(isbn) {
     try {
         statusDiv.textContent = "Sending to Bibby…";
 
-        const response = await fetch(API_URL, {
-            method: "POST",
-            headers: {
-                "Content-Type": "application/json"
-            },
-            body: JSON.stringify(payload)
+        const response = await fetch(`${API_URL}/${isbn}`, {
+            method: "GET"
         });
 
         if (!response.ok) {
@@ -35,7 +31,7 @@ async function sendToApi(isbn) {
             return;
         }
 
-        statusDiv.textContent = "✅ Sent to Bibby: " + isbn;
+        statusDiv.textContent = `Sent to Bibby: ${isbn}`;
         console.log("Sent to API:", isbn);
     } catch (err) {
         console.error("Network/API error:", err);
