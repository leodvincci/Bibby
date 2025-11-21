## 🧱 Dev Log — Converting `BookFactory` into a Spring-Managed Component

**Date:** 2025-11-21
 **Author:** @leodvincci
 **PR:** #47 Refactor/introduce-BookFactory-as-bean

### 📝 Context

In Bibby’s little universe, the BookService had been doing a bit too much heavy lifting. Book creation was still leaning on a static method in `BookFactory`, which worked but felt like a stowaway from an earlier, less-DI-friendly era. As the domain gets more expressive and layered—authors, shelves, flows, and the eventual holy grail of “Bookcase → Shelf → Book” navigation—it made sense to tighten up how book objects enter the world.

Static factories are useful, but they refuse to play nicely with Spring's superpower: dependency injection. Static code is a hermit; Spring likes communities.

Time to bring BookFactory into the Spring ecosystem.

### 🔧 What Changed

**1. BookFactory evolved from static utility → Spring component**
 Annotated `BookFactory` with `@Component` and removed the static method. It’s now a proper bean with a clear lifecycle and can be injected anywhere creation logic is needed.

**2. Removed `static createBook` and converted to an instance method**
 No more static assumptions. Factories should behave like part of the domain, not global state masquerading as convenience.

**3. Injected the new BookFactory into BookService**
 Updated the `BookService` constructor to accept a `BookFactory` instance.
 This makes BookService cleaner, easier to test, and more aligned with SRP.

**4. Delegated object construction to the factory**
 BookService now lets BookFactory do the domain-specific creation work instead of assembling `BookEntity` manually. This keeps BookService focused on orchestration.

### 🧠 Why This Matters

Bringing BookFactory under Spring’s wing unlocks:

- **Better testability** — mockable factories = happier unit tests
- **Clearer architecture** — services orchestrate, factories construct
- **Less rigidity** — no static method handcuffs
- **Future scalability** — when books gain metadata, rules, defaults, or validation, the factory is a natural home

It also nudges Bibby closer to a domain-driven, monolith-first architecture where creation logic is centralized instead of drifting into services like stray leaves.

### 🚀 Next Steps

- Update BookService to rely fully on the factory for all BookEntity creation.
- Consider parallelizing this pattern across other domain builders (ShelfFactory? BookcaseFactory?).
- Validate author handling paths to ensure consistency with the new injected factory approach.