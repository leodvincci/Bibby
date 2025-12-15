ubiquitous-language.md — key terms + meanings (20-ish max)


## Cataloging Language (Bibby)

### Core concept: Bibliographic Record

**BibliographicRecord** = the structured description we create and maintain (title, authors, identifiers like ISBN, publication info, subjects/tags, etc.).
This is the *thing Cataloging produces and owns*.

### The thing being described

The “real-world book” being described is the **Resource**.
If Bibby needs to be precise later, we can refine this using library models:

- **Work** (the abstract intellectual work)
- **Manifestation / Edition** (a published edition/format)
- **Item / Copy** (a specific physical copy)

**Important:** Cataloging owns the record; *Stacks* owns the copy/location.

### How discovery happens

Cataloging provides **access points** (title/author/subject identifiers) that **point to** a BibliographicRecord.
Discovery/search can index these access points.

### Copy linkage

**Call number / location data** links a BibliographicRecord to a specific **Copy/Item** (owned by Stacks).