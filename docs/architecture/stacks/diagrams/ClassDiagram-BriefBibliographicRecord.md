```mermaid
classDiagram
  class BriefBibliographicRecord {
    <<record>>
    +Long bookId
    +String title
    +List~String~ authors
    +int edition
    +String publisher
    +int publicationYear
    +String isbn
    +String summary
  }

```
