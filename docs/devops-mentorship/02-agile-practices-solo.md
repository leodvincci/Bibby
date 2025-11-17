# SECTION 2: AGILE PRACTICES FOR SOLO DEVELOPERS
## Bringing Professional Agile Methodology to Your Personal Projects

---

## 🎯 Learning Objectives

By the end of this section, you will:
- Understand what Agile really means (beyond the buzzwords)
- Recognize that you're ALREADY practicing some Agile principles
- Learn to formalize your workflow for maximum productivity
- Adapt enterprise Agile practices to solo development
- Create a sustainable rhythm for your Bibby project
- Build interview-worthy stories about your process

---

## 🎉 The Good News: You're Already Doing Agile!

Before we dive into theory, let me show you something important.

### 📁 File: `docs/devlogs/devlog-2025-11-15-BROWSE-BOOKSHELF-TO-BOOKS.md`

Look at what you wrote:

```markdown
**Date:** November 15, 2025
**Slice:** `BROWSE-BOOKSHELF-TO-BOOKS`

## 🎯 Goal
Extend the browse flow so that after selecting a Bookshelf,
Bibby displays the list of Books on that shelf...
This completes the UX cycle: Bookcase → Shelf → Book.

## 🛠️ What I Implemented Today
1. Added a Lightweight Projection: BookSummary
2. Extended the Repository With a Shelf-Based Summary Query
...

## 💡 Reflections
This slice was small but meaningful.
...

## 🚀 Next Steps
- Implement "View Book Details" screen
- Add check-in / check-out options from the Book selector
...
```

**This is Agile!** You're doing:
- ✅ **Small, focused work items** ("slices")
- ✅ **Clear goals** for each work session
- ✅ **Incremental delivery** (complete flow, piece by piece)
- ✅ **Retrospectives** (reflections section)
- ✅ **Planning** (next steps)
- ✅ **Documentation** (devlog format)

### 📁 File: `docs/devlogs/devlog-2025-11-12-bibby-book-status-checkout.md`

Another example:

```markdown
**Feature:** Book Checkout (Persistent State + Personality)
**Branch:** `feat/cli-checkout`

#### 🎯 Objective
Replace the placeholder message with a real, persistent feature...

#### 🧠 Key Insight
This slice transforms Bibby from a toy interface into a living system.

#### 🔮 Next Steps
- Implement `return` command
- Introduce `checkedOutAt` timestamp
```

**More Agile practices:**
- ✅ **Feature branches** (proper Git workflow)
- ✅ **Clear acceptance criteria** (objective section)
- ✅ **Technical reflection** (key insight)
- ✅ **Backlog grooming** (next steps)

**Here's the thing:** You're doing Agile *instinctively*, but it's informal and inconsistent. Let's make it **systematic** and **scalable**.

---

## 📚 What is Agile? (The Real Story)

### The Manifesto (2001)

Four core values:

1. **Individuals and interactions** over processes and tools
2. **Working software** over comprehensive documentation
3. **Customer collaboration** over contract negotiation
4. **Responding to change** over following a plan

### What This Means for You

As a solo developer on Bibby, here's the translation:

| Agile Principle | Your Application |
|----------------|------------------|
| Individuals and interactions | You're both dev and "customer" - talk to yourself! Keep feedback loops short |
| Working software | Ship features frequently, even to yourself. Bibby should always run |
| Customer collaboration | Your future self is your customer. Document for them |
| Responding to change | Don't over-plan. Build what matters now, pivot as you learn |

### The Twelve Principles

Let me show you which ones matter most for solo developers:

**#1: "Our highest priority is to satisfy the customer through early and continuous delivery"**

**Your Reality:**
- You're building Bibby to learn and showcase skills
- "Customer" = future employers, your portfolio viewers, and yourself

**What This Means:**
- Keep Bibby always deployable
- Regular "releases" (even if it's just you using it)
- Don't let features sit half-done for weeks

**Looking at Your Git History:**

```bash
* 3e3e7cc Removed Claude Code Artifacts
* 65eff6f Merge pull request #40 from leodvincci/claude/continue-previous-work...
* 6a8bf34 Add Clean Code mentorship Section 28: Mentor's Final Guidance
* 596484d Add Clean Code mentorship Section 27: Career Development Perspective
```

**Analysis:**
- ⚠️ Recent commits are documentation, not features
- ⚠️ No visible rhythm of feature delivery
- ⚠️ Long gaps between functional changes

**Agile Recommendation:**
- Aim for at least one "working feature" commit per week
- Documentation is great, but balance it with functionality
- Set a cadence: "Every Friday, I ship something that works"

---

**#3: "Deliver working software frequently, from a couple of weeks to a couple of months, with a preference to the shorter timescale"**

**Your Current Pattern:**

Looking at your devlogs:
- Nov 15: Browse flow completion
- Nov 12: Book checkout feature
- Nov 11: Search improvements

**This is good!** You're working in short cycles (days, not months).

But let's make it more deliberate:

**Your Agile Rhythm Should Be:**

```
Week 1: Plan → Implement → Test → Deploy → Reflect
Week 2: Plan → Implement → Test → Deploy → Reflect
Week 3: Plan → Implement → Test → Deploy → Reflect
Week 4: RETROSPECTIVE + Planning for next month
```

**Not:**

```
Vague timeline: "I'll add features when I feel like it"
```

---

**#4: "Business people and developers must work together daily throughout the project"**

**Solo Developer Translation:**
- You are both the "business" (vision holder) and developer
- This means: write down your requirements BEFORE coding
- Don't skip the "product owner" hat

**Example from Your Code:**

📁 **File: `src/main/java/com/penrose/bibby/cli/BookCommands.java`**
**Lines 457-459:**

```java
@Command(command = "check-out", description = "Check-Out a book from the library")
public void checkOutBook(){
    ComponentFlow flow;
    flow = componentFlowBuilder.clone()
            .withStringInput("bookTitle" )
            .name("Book Title:")
            .and().build();
    // ... implementation
```

**Question:** Before you wrote this, did you write down:
- What happens if the book is already checked out?
- What happens if the book doesn't exist?
- What information should be shown to confirm?
- Can you checkout a book with no shelf assigned?

Your devlog shows you figured it out DURING implementation. That's reactive, not Agile.

**Agile Approach:**

Create a mini "user story" before coding:

```markdown
## User Story: Check Out Book

**As a** library owner
**I want to** check out books from my collection
**So that** I can track which books I'm currently reading

### Acceptance Criteria
- [ ] User enters book title
- [ ] System finds book (case-insensitive)
- [ ] System shows confirmation with book details
- [ ] User confirms checkout
- [ ] Book status changes to CHECKED_OUT
- [ ] System shows confirmation message
- [ ] Already checked-out books show appropriate error

### Edge Cases
- Book not found → clear error message
- Book already checked out → friendly error
- Book with no shelf → allow checkout but show warning
- Empty title → prompt again

### Out of Scope
- Multiple users (not yet implemented)
- Due dates (future feature)
- Checkout history (future feature)
```

**This takes 5 minutes to write, but saves hours of confusion.**

---

**#10: "Simplicity--the art of maximizing the amount of work not done--is essential"**

**This is CRITICAL for solo developers!**

**Your Current Approach:**

Looking at `BookEntity.java`:

```java
private String isbn;
private String publisher;
private int publicationYear;
private String genre;
private int edition;
private String description;
```

**Question:** Are you using all these fields right now?

Let me check your `addBook()` command:

📁 **File: `BookCommands.java` lines 88-169**

```java
public void addBook() throws InterruptedException {
    // Asks for: title, author count, author names
    // Does NOT ask for: ISBN, publisher, year, genre, edition, description
}
```

**Analysis:**
- ❌ You built database fields you don't use
- ❌ You're planning for future features instead of current needs
- ❌ This is "Big Design Up Front" (anti-Agile)

**Agile Principle: YAGNI** (You Aren't Gonna Need It)

**What You Should Have Done:**

**Version 1 (MVP):**
```java
@Entity
public class BookEntity {
    @Id private Long bookId;
    private String title;
    @ManyToMany private Set<AuthorEntity> authors;
}
```

**Version 2 (Add status tracking):**
```java
// NOW you need status, so NOW you add it
private BookStatus status;
```

**Version 3 (Add shelf location):**
```java
// NOW you're organizing physically, so NOW you add it
private Long shelfId;
```

**Future Version (When you actually need it):**
```java
// LATER, when you implement ISBN search, add:
private String isbn;
```

**This is iterative design.** Start small, grow as needed.

---

## 🏃‍♂️ Agile Frameworks: Which One for Solo Developers?

There are three main frameworks:

### 1. Scrum (Most Popular)

**Structure:**
- **Sprints**: Fixed 2-week cycles
- **Roles**: Product Owner, Scrum Master, Dev Team
- **Ceremonies**: Sprint Planning, Daily Standup, Sprint Review, Retrospective
- **Artifacts**: Product Backlog, Sprint Backlog, Increment

**For Solo Developers:**
- ✅ Sprints work great (gives you a rhythm)
- ❌ Roles don't translate (you're everyone)
- ⚠️ Ceremonies need adaptation

**Your Adaptation:**

```
WEEKLY SPRINT (not 2 weeks - keep it tight)

Sunday Evening (30 min): Sprint Planning
- Review backlog
- Pick 3-5 tasks for the week
- Write acceptance criteria
- Estimate effort (S/M/L)

Daily (5 min): Solo Standup (journal entry)
- What did I accomplish yesterday?
- What will I do today?
- Any blockers?

Saturday Morning (1 hour): Sprint Review + Retro
- Demo to yourself (record video!)
- What went well?
- What could improve?
- Update backlog
```

### 2. Kanban (Most Flexible)

**Structure:**
- **Visualize workflow**: Board with columns (To Do, In Progress, Done)
- **Limit WIP** (Work in Progress): Only X items in progress at once
- **Manage flow**: Optimize how work moves through system
- **Continuous improvement**: No fixed cycles

**For Solo Developers:**
- ✅ Very flexible (no fixed sprints)
- ✅ Visual (great for tracking)
- ✅ Easy to start
- ⚠️ Can become chaotic without discipline

**Your Kanban Board for Bibby:**

```
┌─────────────┬──────────────┬──────────────┬─────────┐
│   BACKLOG   │   TO DO      │ IN PROGRESS  │  DONE   │
│             │  (This Week) │   (Max: 2)   │ (Week)  │
├─────────────┼──────────────┼──────────────┼─────────┤
│ ISBN search │ Book details │ Test coverage│ Checkout│
│ Book export │ Input valid. │              │ Browse  │
│ User auth   │ Error handle │              │ Search  │
│ Stats page  │              │              │         │
│ AI shelf    │              │              │         │
└─────────────┴──────────────┴──────────────┴─────────┘
```

**Rules:**
1. **WIP Limit = 2**: Never work on more than 2 things at once
2. **Prioritize "To Do"**: Top item is most important
3. **Weekly cleanup**: Move done items to archive, replenish "To Do"

### 3. Scrumban (Hybrid)

Combines Scrum's planning with Kanban's flexibility.

**For You: I recommend starting with Scrumban**

**Why:**
- Weekly planning cycle (from Scrum)
- Flexible execution (from Kanban)
- Visual tracking (Kanban board)
- Regular retrospectives (from Scrum)

---

## 🛠️ Building Your Personal Agile Workflow

Let's create a sustainable process for Bibby specifically.

### Your Weekly Rhythm

**Sunday Evening: Planning (30 minutes)**

Create a file: `docs/sprints/sprint-[YYYY-MM-DD].md`

```markdown
# Sprint: Dec 1-7, 2025

## Sprint Goal
Make Bibby production-ready: Add tests and error handling

## Selected Tasks (From Backlog)

### Task 1: Add BookService Tests [M]
**Story:** As a developer, I need comprehensive tests for BookService
**AC:**
- [ ] Test createNewBook() with new author
- [ ] Test createNewBook() with existing author
- [ ] Test findBookByTitle() found and not found
- [ ] Test checkOutBook() status change
- [ ] 80% coverage on BookService

**Estimate:** 4 hours

### Task 2: Add Input Validation to BookCommands [S]
**Story:** As a user, I should see helpful errors for invalid input
**AC:**
- [ ] Validate author count (1-10)
- [ ] Handle non-numeric input gracefully
- [ ] Validate title not blank
- [ ] Show clear error messages

**Estimate:** 2 hours

### Task 3: Fix BookService Logic Bug [S]
**Story:** As a developer, I need to fix the duplicate book bug
**AC:**
- [ ] Fix lines 35-40 indentation issue
- [ ] Add test to prevent regression
- [ ] Update with proper error handling

**Estimate:** 1 hour

## Capacity
Total estimated: 7 hours
Available this week: 8 hours
Buffer: 1 hour ✅

## Definition of Done
- [ ] Code written and committed
- [ ] Tests written and passing
- [ ] Code reviewed (self-review checklist)
- [ ] Documented in devlog
- [ ] Deployed locally and tested
```

**Monday-Friday: Daily Log (5 minutes each morning)**

Create: `docs/sprints/daily-logs.md`

```markdown
## Monday, Dec 2
**Yesterday:** Sprint planning
**Today:** Start BookService tests, write first 3 test cases
**Blockers:** None

## Tuesday, Dec 3
**Yesterday:** Wrote 3 BookService tests
**Today:** Complete remaining tests, aim for 80% coverage
**Blockers:** Need to figure out mocking AuthorRepository

## Wednesday, Dec 4
**Yesterday:** Completed BookService tests (85% coverage!)
**Today:** Start input validation in BookCommands
**Blockers:** None - feeling productive
```

**Saturday Morning: Review + Retrospective (1 hour)**

Update sprint file:

```markdown
## Sprint Review

### Completed ✅
- [x] BookService tests (85% coverage, exceeded goal!)
- [x] Input validation (all edge cases covered)
- [x] Fixed BookService bug

### Not Completed
- None! Completed everything planned

### Unplanned Work Done
- Added logging to service layer (discovered need during testing)

## Sprint Retrospective

### What Went Well 😊
- Tests revealed 2 additional bugs I fixed
- Validation made CLI feel much more professional
- Stayed focused, finished everything

### What Could Improve 🤔
- Underestimated - only used 6 hours of planned 7
- Could have taken on one more small task
- Need better estimation

### Action Items for Next Sprint
- Estimate more confidently (I'm faster than I think)
- Add one stretch goal task
- Timebox tasks better (set 2-hour limits)

### Personal Notes
This sprint felt great - seeing test coverage go up is motivating.
The logic bug fix prevented future headaches. Ready for next sprint!
```

---

## 📋 Setting Up Your Agile Infrastructure

### Tool 1: GitHub Projects (Kanban Board)

**Set this up NOW for Bibby:**

1. Go to your repo: `github.com/leodvincci/Bibby`
2. Click "Projects" tab → "New Project"
3. Choose "Board" template
4. Name it: "Bibby Development"

**Columns:**
- 📋 **Backlog** (all ideas)
- 📅 **This Week** (sprint selection)
- 🚧 **In Progress** (WIP limit: 2)
- ✅ **Done** (this week's completions)
- 🎉 **Released** (in production/deployed)

**Card Template:**

```
Title: Add ISBN Validation

Labels: feature, enhancement
Size: M (Medium)
Sprint: Week of Dec 1

Description:
Validate ISBN-10 and ISBN-13 format with checksum algorithm.

Acceptance Criteria:
- [ ] ISBN-10 validation with checksum
- [ ] ISBN-13 validation with checksum
- [ ] User-friendly error messages
- [ ] Unit tests with valid/invalid cases

Files to Modify:
- BookEntity.java (add validation annotation)
- BookCommands.java (add ISBN input)
- BookService.java (validation logic)
```

### Tool 2: Issues for Bugs

**Use GitHub Issues systematically:**

📁 **Example Issue from YOUR code:**

**Title:** `BookService.createNewBook() updates existing books incorrectly`

**Labels:** `bug`, `priority: high`, `good first issue`

**Description:**
```markdown
## Bug Description
In BookService.java lines 35-40, incorrect indentation causes existing
books to be updated with new authors instead of being left unchanged.

## Current Behavior
```java
if (bookEntity == null) {
    bookEntity = new BookEntity();
    bookEntity.setTitle(title);
}
    bookEntity.setAuthors(authorEntity);  // ← Runs even if book exists!
    bookRepository.save(bookEntity);
```

## Expected Behavior
If book already exists, do NOT add authors or save.
Should either:
- Return existing book unchanged, OR
- Throw DuplicateBookException

## Steps to Reproduce
1. Add book "Test Book" with author "John Doe"
2. Try to add same book again with author "Jane Smith"
3. Book now has both authors (incorrect)

## Impact
- Data corruption (books get wrong authors)
- No duplicate detection
- Silent failures

## Proposed Fix
```java
if (bookEntity == null) {
    bookEntity = new BookEntity();
    bookEntity.setTitle(title);
    bookEntity.setAuthors(authorEntity);
    bookRepository.save(bookEntity);
} else {
    throw new DuplicateBookException("Book already exists: " + title);
}
```

## Additional Context
Discovered during SDLC analysis. This is a critical bug that affects
data integrity.

## Related
- [ ] Add test to prevent regression
- [ ] Add duplicate detection to CLI before calling service
```

### Tool 3: Milestones for Versions

**Set up milestones in GitHub:**

**Milestone: v0.3 - Quality & Testing**
- Target: Dec 15, 2025
- 8 issues assigned
- 5 completed, 3 remaining

**Issues in Milestone:**
- [ ] BookService test coverage (80%+)
- [ ] BookCommands input validation
- [ ] Fix BookService logic bug
- [ ] Add error handling to all commands
- [ ] JavaDoc for all public methods
- [x] Create test base classes
- [x] Set up JaCoCo
- [x] Document testing strategy

---

## 🎯 User Stories: Writing Good Requirements

Your devlogs show you understand the "what" but not always the "why."

**Current Format (from your devlog):**

```markdown
## Goal
Extend the browse flow so that after selecting a Bookshelf,
Bibby displays the list of Books on that shelf
```

**Better Format (User Story):**

```markdown
## User Story

**As a** library owner browsing my physical collection
**I want to** see all books on a specific shelf
**So that** I can find a book when I remember its location but not its title

### Acceptance Criteria
- [ ] After selecting a shelf, books on that shelf are displayed
- [ ] Books are sorted alphabetically by title
- [ ] Books show title only (not full details)
- [ ] Empty shelves show "No books on this shelf"
- [ ] User can select a book to see details

### Technical Notes
- Use BookSummary projection (not full entity)
- Query: findBookSummariesByShelfIdOrderByTitleAsc
- Magenta color for book titles
- Use SingleItemSelector ComponentFlow

### Definition of Done
- [ ] Code written and tested
- [ ] Empty shelf case handled
- [ ] Manual testing in CLI
- [ ] Devlog written
- [ ] Screenshot captured
```

**Why This Format?**

1. **"As a / I want / So that"** = Forces you to think about user value
2. **Acceptance Criteria** = Testable conditions
3. **Technical Notes** = Implementation guidance (for your future self)
4. **Definition of Done** = Clear exit criteria

### Creating User Stories for Bibby

Let me convert some of your backlog ideas into proper user stories:

**Story 1: ISBN Search**

```markdown
## User Story: Search Books by ISBN

**As a** library owner
**I want to** search for books by ISBN
**So that** I can quickly find a specific edition without ambiguity

### Background
Titles can be ambiguous (multiple editions, translations).
ISBN is unique identifier for exact edition.

### Acceptance Criteria
- [ ] User can enter ISBN-10 or ISBN-13
- [ ] System validates ISBN format and checksum
- [ ] System finds book if ISBN matches
- [ ] System shows "Not found" if no match
- [ ] Invalid ISBN shows helpful error message

### Technical Notes
- Add ISBN field to addBook() flow
- Add validation method: isValidISBN(String)
- Update BookRepository: findByIsbn(String)
- Regex patterns for ISBN-10 and ISBN-13

### Out of Scope (Future)
- Fetching book metadata from ISBN (OpenLibrary API)
- Barcode scanning
- ISBN-to-image lookup

### Size: M (4 hours)

### Priority: P2 (Nice to have)

### Dependencies
- None (independent feature)
```

**Story 2: Export Library to CSV**

```markdown
## User Story: Export Library Data

**As a** library owner
**I want to** export my entire library to CSV
**So that** I can use it in Excel or backup my data

### Acceptance Criteria
- [ ] Command: `book export --format csv --output books.csv`
- [ ] CSV includes: Title, Authors, ISBN, Status, Shelf, Bookcase
- [ ] Authors are comma-separated in one cell
- [ ] File is created in current directory
- [ ] Success message shows file location
- [ ] Handle file write errors gracefully

### Technical Notes
- New command: exportBooks()
- Service: BookService.getAllBooksForExport()
- Use projection: BookExportView
- CSV library: OpenCSV or manual StringBuilder
- Format: "Title","Author1, Author2","ISBN","Status",...

### Size: M (3 hours)

### Priority: P3 (Low)

### Dependencies
- None
```

---

## 🔄 Iteration: The Heart of Agile

### Your Current Approach (from devlogs)

**Nov 12:** Added checkout feature
**Nov 13:** Added browse flow
**Nov 15:** Completed browse flow

**This IS iterative!** You're building in small increments.

But let's make it more intentional.

### Iteration Planning for Bibby

**Feature: Book Management (High-Level)**

**Iteration 1: MVP (Week 1)**
- Add book (title + 1 author only)
- List all books (simple text output)
- Search by title (exact match)
**Goal:** Prove the concept works

**Iteration 2: Enhanced Search (Week 2)**
- Multi-author support
- Case-insensitive search
- Partial title matching
**Goal:** Make search usable

**Iteration 3: Physical Organization (Week 3)**
- Add shelf assignment
- Add bookcase concept
- Search by location
**Goal:** Match physical library

**Iteration 4: Status Tracking (Week 4)**
- Checkout/checkin
- Status display
- Checkout history
**Goal:** Track usage

**Iteration 5: Polish (Week 5)**
- Better error messages
- Loading animations
- Personality/sass
**Goal:** Improve UX

### Your Bibby Feature: "Book Details View"

Let's plan this in iterations:

**Iteration 1: Basic Details (2 hours)**
```
Show: Title, Authors, Status
Command: book details --title "..."
Output: Plain text, no formatting
```

**Iteration 2: Complete Details (1 hour)**
```
Add: ISBN, Publisher, Year, Edition, Description
Add: Shelf location, Bookcase
Format: Better layout
```

**Iteration 3: Interactive Navigation (2 hours)**
```
From browse flow → select book → auto-show details
Add: Actions menu (checkout, edit, delete)
Format: Colors, borders
```

**Iteration 4: Rich Display (1 hour)**
```
Add: Checkout count, last checkout date
Add: Related books suggestion
Format: ASCII art borders
```

**Why Break It Down?**

- Each iteration delivers VALUE
- You can stop after any iteration (still have something useful)
- You learn and adapt between iterations
- Less risk of getting stuck on a half-done feature

---

## 📊 Measuring Your Agile Success

### Velocity: How Much Can You Do Per Week?

**Track this for Bibby:**

```markdown
## Sprint Velocity Log

| Sprint | Planned Points | Completed Points | Velocity |
|--------|---------------|------------------|----------|
| Week 1 | 8 (S=1,M=3,L=5) | 8 | 100% |
| Week 2 | 8 | 5 | 63% |
| Week 3 | 6 (adjusted) | 6 | 100% |
| Week 4 | 7 | 9 (added stretch) | 128% |

**Average Velocity:** 7 points/week
**Confidence:** Can commit to 6-7 points reliably
```

**Use this to estimate:**
- "BookService tests = M = 3 points"
- "Your average velocity = 7 points/week"
- "You can do 2 medium tasks + 1 small task per week"

### Lead Time: How Long to Ship a Feature?

**Track this:**

```markdown
## Feature Lead Time

| Feature | Start Date | Finish Date | Days | Notes |
|---------|-----------|-------------|------|-------|
| Checkout | Nov 10 | Nov 12 | 2 | Smooth |
| Browse Flow | Nov 13 | Nov 15 | 3 | Complex |
| Search | Nov 11 | Nov 11 | 1 | Simple |

**Average:** 2 days per feature
**Goal:** Keep features under 3 days
```

**If a feature exceeds 3 days = BREAK IT DOWN**

### Cycle Time: How Long to Code a Task?

```markdown
## Cycle Time (Coding Time)

| Task | Time |
|------|------|
| BookSummary projection | 30 min |
| Repository query | 20 min |
| Service method | 15 min |
| CLI selector | 1 hour |
| Empty shelf handling | 20 min |
| Testing + debugging | 1 hour |

**Total:** 3.5 hours actual work time
**Calendar time:** 3 days
**Efficiency:** ~25% (normal for side projects!)
```

**This helps you estimate better.**

---

## 🎭 Agile Ceremonies for Solo Developers

### 1. Sprint Planning (Sundays, 30 min)

**Your Checklist:**

```markdown
## Sprint Planning Checklist

**Review Last Sprint:**
- [ ] What did I complete?
- [ ] What didn't I finish? (rollover or drop?)
- [ ] Did I over/under estimate?

**Review Backlog:**
- [ ] Are top items still priorities?
- [ ] Any new urgent items?
- [ ] Any items no longer relevant? (remove!)

**Select This Sprint's Work:**
- [ ] Pick 6-8 points of work
- [ ] Balance: 1 big feature + 2 small improvements
- [ ] Include at least 1 test/quality task
- [ ] Leave 20% buffer time

**Write User Stories:**
- [ ] Each task has acceptance criteria
- [ ] Each task has clear DOD
- [ ] Estimate each task (S/M/L)

**Set Sprint Goal:**
- [ ] One-sentence goal for this week
- [ ] How will Bibby be better on Saturday?
```

### 2. Daily Standup (5 min each morning)

**Your Format:**

```markdown
## Daily Log: Tuesday, Dec 3, 2025

**Time:** 9:00 AM
**Location:** Morning journal

**Yesterday:**
- Completed BookService test setup
- Wrote 3 test cases (createNewBook scenarios)
- 50% of test task done

**Today:**
- Complete remaining BookService tests
- Aim for 80% coverage
- Run full test suite

**Blockers:**
- None currently
- Might need to research mocking if I get stuck

**Energy Level:** ⚡⚡⚡⚡ (High - feeling focused)

**Time Available:** 2 hours (evening)
```

**Why This Helps:**
- Forces you to commit to daily progress
- Identifies blockers early
- Tracks your actual capacity
- Creates accountability (to yourself!)

### 3. Sprint Review (Saturdays, 30 min)

**Your Format:**

```markdown
## Sprint Review: Dec 1-7, 2025

**Sprint Goal:** Make Bibby production-ready with tests

**Demo to Myself:**
[Record a 2-minute video showing:]
- Running new tests (watch them all pass!)
- Trying invalid input (see nice error messages)
- Showing coverage report (85%!)

**Completed Stories:**
1. ✅ BookService Tests (3 pts) - DONE
   - 12 test cases written
   - 85% coverage achieved (exceeded 80% goal)
   - Found and fixed 2 additional bugs

2. ✅ Input Validation (2 pts) - DONE
   - All edge cases handled
   - Error messages are clear and helpful
   - Manually tested all scenarios

3. ✅ Fix Logic Bug (1 pt) - DONE
   - Bug fixed with test to prevent regression
   - Code reviewed and documented

**Incomplete Stories:**
- None! 🎉

**Unplanned Work:**
- Added logging to service layer (30 min)
- Updated README with testing instructions (20 min)

**Total Completed:** 6 planned points + 1 unplanned
**Velocity:** 7 points this week

**What I Learned:**
- Test-first approach would have been faster
- Mocking is easier than I thought
- I'm getting faster at writing tests

**For Portfolio:**
- Can now say "implemented TDD practices"
- Have concrete metrics (85% coverage)
- Can demo error handling in interviews
```

### 4. Sprint Retrospective (Saturdays, 30 min)

**Your Format:**

```markdown
## Sprint Retrospective: Dec 1-7, 2025

### What Went Well 😊
- Exceeded test coverage goal (85% vs 80% target)
- Stayed focused all week, no context switching
- Found bugs early through testing
- Error messages make CLI feel professional
- Completed everything I planned!

### What Didn't Go Well 😞
- Underestimated capacity (finished early)
- Spent 30 min fighting with JaCoCo setup
- Could have written better test names
- Didn't update documentation until end

### What I Learned 💡
- I work faster than I think (adjust estimates)
- Tests are motivating (seeing coverage increase)
- Small tasks are easier to finish
- Morning coding is more productive for me

### Action Items 🎯
- [ ] Increase velocity estimate to 8 points next sprint
- [ ] Document JaCoCo setup for future reference
- [ ] Add "update docs" to DOD checklist
- [ ] Try coding first thing in morning all week

### Experiments to Try 🧪
- Next sprint: Try TDD (write tests first)
- Set 2-hour time blocks with timer
- Use Pomodoro technique
- Take before/after screenshots for portfolio

### Personal Notes 📝
This was my most productive sprint yet. The key was breaking
tasks into clear acceptance criteria. I knew exactly when I was
"done" with each task. No ambiguity = faster completion.

The test coverage metric was surprisingly motivating. Watching
it climb from 20% to 85% felt like a game.

Ready for next sprint! Going to maintain this momentum.
```

---

## 📖 Your Agile Vocabulary (For Interviews)

When discussing Bibby in interviews, use these terms:

**Instead of:** "I added features when I felt like it"
**Say:** "I practiced iterative development with weekly sprints, delivering small increments of value"

**Instead of:** "I wrote down what I did"
**Say:** "I maintained a backlog, wrote user stories with acceptance criteria, and conducted regular retrospectives"

**Instead of:** "I used Git branches"
**Say:** "I followed a feature-branch workflow with pull requests, aligning with Agile continuous integration practices"

**Instead of:** "I fixed bugs as I found them"
**Say:** "I tracked issues in GitHub, prioritized by impact, and maintained a sustainable pace with a WIP limit of 2"

**Instead of:** "I have a to-do list"
**Say:** "I use a Kanban board to visualize workflow, with columns for backlog, in-progress, and done, limiting work-in-progress to maintain focus"

---

## 🛠️ ACTIONABLE SETUP: Your Agile Workspace

### Step 1: Create Sprint Directory Structure

```bash
mkdir -p docs/sprints
mkdir -p docs/user-stories
```

### Step 2: Create Template Files

**File:** `docs/sprints/template-sprint-plan.md`

```markdown
# Sprint: [START_DATE] to [END_DATE]

## Sprint Goal
[One sentence: What is this sprint achieving?]

## Capacity
**Available hours this week:** [X] hours
**Planned points:** [Y] points
**Buffer:** [20% of planned]

## Selected Stories

### Story 1: [Title] ([Size])
**As a** [role]
**I want** [feature]
**So that** [value]

**Acceptance Criteria:**
- [ ] [Criterion 1]
- [ ] [Criterion 2]

**Estimate:** [X] hours

---

## Daily Log

### Monday
**Planned:**
**Actual:**

### Tuesday
**Planned:**
**Actual:**

[Continue for each day]

---

## Sprint Review

### Completed
- [ ] Story 1
- [ ] Story 2

### Not Completed
- [ ] Story 3 (reason: ...)

### Velocity
**Planned:** [X] points
**Completed:** [Y] points
**Percentage:** [Y/X * 100]%

---

## Sprint Retrospective

### What Went Well
-

### What Could Improve
-

### Action Items
- [ ]
- [ ]

### Experiments
-
```

**File:** `docs/user-stories/template-story.md`

```markdown
# [Story Title]

**ID:** BIBBY-[NUMBER]
**Type:** [Feature/Bug/Improvement/Chore]
**Priority:** [P0/P1/P2/P3]
**Size:** [S/M/L]
**Status:** [Backlog/Selected/In Progress/Done]

## User Story

**As a** [role]
**I want** [feature]
**So that** [value]

## Background/Context
[Why is this needed? What problem does it solve?]

## Acceptance Criteria
- [ ] [Testable criterion 1]
- [ ] [Testable criterion 2]
- [ ] [Testable criterion 3]

## Technical Notes
- File(s) to modify:
- Dependencies:
- API changes:
- Database changes:

## Definition of Done
- [ ] Code written and committed
- [ ] Unit tests written and passing
- [ ] Integration tests (if applicable)
- [ ] Code self-reviewed
- [ ] Documentation updated
- [ ] Manual testing completed
- [ ] Devlog written

## Out of Scope
[What is explicitly NOT included]

## Related Stories
- Blocks: [BIBBY-X]
- Blocked by: [BIBBY-Y]
- Related to: [BIBBY-Z]

## Notes
[Additional context, screenshots, links]
```

### Step 3: Create Your Backlog

**File:** `docs/BACKLOG.md`

```markdown
# Bibby Product Backlog

**Last Updated:** [Date]
**Next Sprint Planning:** [Date]

---

## Current Sprint (In Progress)
- BIBBY-12: Add BookService tests
- BIBBY-13: Input validation
- BIBBY-14: Fix logic bug

---

## Ready for Development (Next Sprint)
- BIBBY-15: Add book details command [M]
- BIBBY-16: Implement check-in command [S]
- BIBBY-17: Add ISBN validation [M]

---

## Backlog (Prioritized)

### High Priority (P1)
- BIBBY-18: Docker containerization [L]
- BIBBY-19: CI/CD pipeline setup [L]
- BIBBY-20: Production logging [M]

### Medium Priority (P2)
- BIBBY-21: Export to CSV [M]
- BIBBY-22: Book statistics [M]
- BIBBY-23: Search by genre [S]

### Low Priority (P3)
- BIBBY-24: AI shelf recommendation [L]
- BIBBY-25: Color themes [S]
- BIBBY-26: Custom ASCII art [S]

### Ideas (Not Prioritized)
- Author biography lookup
- Book cover images
- Reading progress tracking
- Book ratings
- Reading recommendations
```

---

## 🎯 YOUR ACTION PLAN: Implementing Agile

### Week 1: Setup (2 hours total)

**Day 1: Structure (30 min)**
- [ ] Create sprint and user-story directories
- [ ] Copy templates into your repo
- [ ] Create initial BACKLOG.md
- [ ] Commit: "Setup Agile workflow structure"

**Day 2: Backlog (1 hour)**
- [ ] List all feature ideas for Bibby
- [ ] Convert to user stories (use template)
- [ ] Prioritize (P1/P2/P3)
- [ ] Size estimate (S/M/L)

**Day 3: Board (30 min)**
- [ ] Create GitHub Project board
- [ ] Add columns (Backlog/This Week/In Progress/Done)
- [ ] Add existing issues to board
- [ ] Set WIP limit to 2

### Week 2: First Sprint (1 week)

**Sunday: Planning (30 min)**
- [ ] Create sprint file from template
- [ ] Select 3-4 stories from backlog
- [ ] Write acceptance criteria
- [ ] Set sprint goal

**Monday-Friday: Daily Work**
- [ ] 5-minute daily log each morning
- [ ] Move tasks on board
- [ ] Update progress

**Saturday: Review + Retro (1 hour)**
- [ ] Complete sprint review
- [ ] Run retrospective
- [ ] Calculate velocity
- [ ] Archive sprint file

### Week 3-4: Refine & Iterate

- [ ] Adjust velocity based on Week 2
- [ ] Refine templates based on experience
- [ ] Add more details to backlog
- [ ] Continue sprint rhythm

---

## 📚 REAL EXAMPLE: User Story for Your Next Feature

Let me write a complete user story for a feature you should build next:

**File:** `docs/user-stories/BIBBY-015-book-details-view.md`

```markdown
# BIBBY-015: Book Details View Command

**Type:** Feature
**Priority:** P1 (High)
**Size:** M (3-4 hours)
**Status:** Ready for Development
**Sprint:** Week of Dec 8, 2025

## User Story

**As a** library owner
**I want to** view complete details about a specific book
**So that** I can see all information without navigating multiple commands

## Background/Context

Currently, users can search for books and checkout, but there's no
single command to see ALL details about a book (title, authors, ISBN,
status, location, checkout count, etc.). This makes it hard to verify
information or make decisions about shelving.

This will also serve as the base for future features like editing
book details and viewing checkout history.

## Acceptance Criteria

### Must Have
- [ ] Command: `book details --title "Book Title"` shows complete info
- [ ] Display includes: Title, Authors (all), ISBN, Publisher, Year, Edition
- [ ] Display includes: Status (available/checked out), Checkout count
- [ ] Display includes: Shelf location (label + bookcase)
- [ ] Book not found shows clear error message
- [ ] Output is well-formatted with clear labels

### Should Have
- [ ] Colors: Title in magenta, labels in cyan, values in white
- [ ] Border/separator between sections
- [ ] Dates formatted as "MMM dd, yyyy"

### Nice to Have
- [ ] ASCII art icon based on book status
- [ ] Related books suggestion (same genre/author)
- [ ] Quick actions menu (checkout/checkin/edit)

## Technical Implementation

### Files to Modify
1. `BookCommands.java` - Add new command method
2. `BookService.java` - Add getBookDetails() method
3. `BookRepository.java` - May need projection or custom query

### Approach
```java
@Command(command = "details", description = "View complete book details")
public void bookDetails(@Option(required = true) String title) {
    BookDetailView details = bookService.getBookDetails(title);

    if (details == null) {
        System.out.println("Book not found: " + title);
        return;
    }

    displayBookDetails(details);
}

private void displayBookDetails(BookDetailView book) {
    System.out.println("\n" + "=".repeat(60));
    System.out.println("📚 BOOK DETAILS");
    System.out.println("=".repeat(60));

    System.out.printf("%-20s %s\n", "Title:", book.getTitle());
    System.out.printf("%-20s %s\n", "Author(s):", String.join(", ", book.getAuthors()));
    // ... more fields

    System.out.println("=".repeat(60) + "\n");
}
```

### Data Layer
Create projection:
```java
public interface BookDetailView {
    String getTitle();
    List<String> getAuthors();  // formatted as "First Last"
    String getIsbn();
    // ... other fields
}
```

Or use DTO:
```java
public record BookDetailDTO(
    String title,
    List<String> authors,
    String isbn,
    String status,
    ShelfLocation location,
    int checkoutCount,
    LocalDate createdAt
) {}
```

## Definition of Done

- [ ] Code written and compiles
- [ ] Command added to BookCommands.java
- [ ] Service method implemented
- [ ] Projection/DTO created
- [ ] Unit tests for service method
- [ ] Integration test for repository query
- [ ] Manual CLI testing with multiple books
- [ ] Null handling tested
- [ ] Error cases tested
- [ ] Code self-reviewed against checklist
- [ ] JavaDoc comments added
- [ ] Devlog entry written
- [ ] Screenshot added to devlog
- [ ] Git commit with descriptive message
- [ ] Feature demoed (video/screenshot)

## Test Cases

### Happy Path
1. Book exists with all fields → displays correctly
2. Book exists with missing optional fields → handles nulls
3. Book on shelf with bookcase → shows location
4. Book not on shelf → shows "Location: Not assigned"

### Edge Cases
1. Book not found → clear error message
2. Title with special characters → finds correctly
3. Multiple authors → all displayed correctly
4. Very long title/description → wraps/truncates nicely

### Error Cases
1. Empty title → validation error
2. Null title → validation error
3. Database error → graceful error message

## Out of Scope (Future Stories)

- Editing book details (separate story)
- Viewing checkout history (separate story)
- Exporting details to PDF (separate story)
- Fetching additional metadata from APIs (separate story)

## Related Stories

- Depends on: None (independent feature)
- Blocks: BIBBY-016 (Edit book details - needs this view first)
- Related: BIBBY-010 (Book search - similar display logic)

## Notes

This is a foundational feature that many other features will build on.
Keep the display logic in a separate private method so it can be reused.

Consider creating a DisplayService or FormatterService if we add more
display commands in the future.

## Estimate Breakdown

- [ ] Design DTO/projection: 30 min
- [ ] Implement service method: 1 hour
- [ ] Implement CLI command: 1 hour
- [ ] Write tests: 1 hour
- [ ] Manual testing & polish: 30 min
- [ ] Documentation: 30 min

**Total: 4.5 hours → Round to M (Medium) task**

## Started
[Date when you move this to "In Progress"]

## Completed
[Date when you move this to "Done"]

## Actual Time Spent
[Track actual hours for better future estimates]

## Demo/Screenshots
[Add after completion]
```

---

## 🎓 KEY TAKEAWAYS

1. **You're Already Agile**: Your devlogs prove you understand iterative development

2. **Formalize It**: Add structure (sprint planning, backlog, retrospectives) to scale up

3. **It's Not Overhead**: These practices SAVE time by reducing confusion and rework

4. **Solo Adaptation**: You don't need all the ceremonies - pick what works

5. **Start Small**: Begin with weekly sprints, daily logs, and a Kanban board

6. **Measure**: Track velocity and lead time to improve estimates

7. **Interview Gold**: Being able to discuss your Agile process impresses employers

8. **Sustainable Pace**: Agile prevents burnout by limiting WIP and planning realistically

---

## 📚 RESOURCES

### Tools
- **GitHub Projects** (free, integrated)
- **Trello** (if you prefer simpler boards)
- **Notion** (for more structured documentation)
- **Linear** (best for solo devs who want polish)

### Reading
- "Agile Estimating and Planning" by Mike Cohn
- "User Stories Applied" by Mike Cohn
- "The Lean Startup" by Eric Ries (entrepreneurial Agile)

### Blogs
- [Martin Fowler on Agile](https://martinfowler.com/agile.html)
- [Henrik Kniberg's blog](https://blog.crisp.se/author/henrikkniberg)
- [Jeff Patton - Story Mapping](https://www.jpattonassociates.com/blog/)

---

## 🎯 SECTION SUMMARY

**Code Examples Used:**
- Your actual devlogs (Nov 12, Nov 15) showing Agile-like practices
- `BookCommands.java` analyzed for requirements gaps
- `BookEntity.java` showing over-design (YAGNI violation)
- Git commit history showing development rhythm
- `BookService.java` logic bug as example of missing planning

**What You're Already Doing Right:**
- ✅ Working in small slices
- ✅ Writing devlogs (retrospectives)
- ✅ Identifying next steps (backlog grooming)
- ✅ Using feature branches

**What Needs Formalization:**
- ❌ Sprint planning (currently ad-hoc)
- ❌ User stories (currently implicit)
- ❌ Backlog management (ideas scattered)
- ❌ Velocity tracking (no metrics)
- ❌ Definition of Done (inconsistent)

**Templates Created:**
- Sprint planning template
- User story template
- Daily log template
- Sprint review/retrospective template
- Complete example: BIBBY-015 user story

**Action Items for This Week:**
1. Create sprint directory structure (30 min)
2. Set up GitHub Projects Kanban board (30 min)
3. Write user stories for next 5 features (2 hours)
4. Plan first formal sprint for next week (30 min)
5. Start daily logging habit (5 min/day)

---

## ⏸️ PAUSE POINT

You've learned how to bring professional Agile practices to your solo development work. More importantly, you've seen that you're ALREADY thinking in Agile terms - now we just need to formalize it.

**Your Bibby project is about to level up from "good code" to "professional software engineering process."**

**Ready for Section 3: Build Lifecycle Fundamentals?**

---

**📊 Progress**: 2/28 sections complete
**Next**: Section 3 - Build Lifecycle & Maven Mastery
**Your move**: Type "continue" when ready! 🚀
