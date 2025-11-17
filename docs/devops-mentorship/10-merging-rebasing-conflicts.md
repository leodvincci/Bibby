# Section 10: Merging, Rebasing & Conflict Resolution

## Introduction: Git's Most Powerful (and Misunderstood) Features

You've heard the warnings: "Don't rebase public branches!" or "Rebasing rewrites history!" These statements are true, but they're often delivered without context, creating fear instead of understanding.

**The reality:** Merging and rebasing are complementary tools. Understanding when to use each—and how to resolve conflicts effectively—is what separates confident Git users from those who nervously run `git merge` and hope for the best.

**What You'll Learn:**
- Merge vs rebase: technical differences and when to use each
- Three-way merge algorithm (how Git actually merges)
- Interactive rebase for cleaning up history
- Resolving complex merge conflicts with confidence
- Advanced techniques: cherry-pick, rebase onto, merge strategies
- Real conflict scenarios from Bibby's codebase

**Real-World Context:**
Imagine you've been working on a feature branch for Bibby's ISBN lookup while the main branch got 5 new commits. How do you integrate your changes? The answer depends on:
- Has your branch been pushed and shared?
- Do you want a clean linear history?
- How complex are the conflicts?
- What's your team's workflow?

This section gives you the knowledge to make that decision confidently.

---

## 1. Merge: Preserving History

### 1.1 How Merge Actually Works

**The Three-Way Merge Algorithm:**

When you run `git merge feature`, Git performs a **three-way merge**:

1. **Find merge base** (common ancestor)
2. **Compare three commits:**
   - Base (common ancestor)
   - Ours (current branch HEAD)
   - Theirs (branch being merged)
3. **Apply changes** from both branches
4. **Create merge commit** with two parents

**Visual Example with Bibby:**

```
Before merge:
      C1---C2---C3  (feature/isbn-lookup)
     /
A---B---D1---D2  (main)

git merge feature/isbn-lookup

After merge:
      C1---C2---C3
     /             \
A---B---D1---D2---M  (main)
```

**The Merge Commit (M):**
- Parent 1: D2 (main branch)
- Parent 2: C3 (feature branch)
- Tree: Combined changes from C3 and D2

### 1.2 Fast-Forward Merge

**Special case:** When no divergence exists, Git can "fast-forward."

```
Before merge:
A---B---C  (main)
         \
          D---E  (feature)

git merge feature

After fast-forward:
A---B---C---D---E  (main, feature)
```

**No merge commit created**—main pointer just moves forward.

**Forcing merge commit (even when fast-forward possible):**

```bash
git merge --no-ff feature

# Creates:
A---B---C-------M  (main)
         \     /
          D---E  (feature)
```

**When to use `--no-ff`:**
- Want explicit record of feature integration
- Grouping related commits together
- Team policy requires merge commits

**Bibby Example:**

```bash
# Create feature branch
git checkout -b feature/add-book-notes main

# Make commits
git commit -m "Add notes field to BookEntity"
git commit -m "Add notes command to CLI"
git commit -m "Add tests for book notes"

# Switch to main (no new commits on main)
git checkout main

# Fast-forward merge
git merge feature/add-book-notes
# Output: Fast-forward

git log --oneline -n 3
# 3c4d5e6 Add tests for book notes
# 2b3c4d5 Add notes command to CLI
# 1a2b3c4 Add notes field to BookEntity

# No merge commit!
```

**Force merge commit:**

```bash
git reset --hard HEAD~3  # Undo the merge
git merge --no-ff feature/add-book-notes

git log --oneline --graph -n 5
# *   4d5e6f7 Merge branch 'feature/add-book-notes'
# |\
# | * 3c4d5e6 Add tests for book notes
# | * 2b3c4d5 Add notes command to CLI
# | * 1a2b3c4 Add notes field to BookEntity
# |/
# * 0a1b2c3 Previous commit on main
```

### 1.3 Merge Strategies

Git supports multiple merge strategies. The default is **recursive** (for two branches), but others exist.

**Available Strategies:**

```bash
# 1. Recursive (default, for two branches)
git merge -s recursive feature

# 2. Ours (always prefer our changes in conflict)
git merge -s ours feature
# Use case: Merge branch but discard all their changes (just record merge)

# 3. Theirs (via strategy option)
git merge -X theirs feature
# Use case: In conflict, automatically choose their version

# 4. Octopus (for merging 3+ branches simultaneously)
git merge branch1 branch2 branch3
# Creates one merge commit with multiple parents

# 5. Resolve (simple two-branch merge, older algorithm)
git merge -s resolve feature
```

**Recursive Strategy Options:**

```bash
# Always choose ours in conflicts
git merge -X ours feature

# Always choose theirs in conflicts
git merge -X theirs feature

# Ignore whitespace changes
git merge -X ignore-space-change feature

# Patience diff algorithm (better for refactored code)
git merge -X patience feature
```

**Bibby Example: Merging Database Migration Conflicts**

```bash
# Scenario: Two branches both modified application.properties

# Your branch added:
spring.jpa.hibernate.ddl-auto=validate

# Their branch added:
spring.jpa.show-sql=true

# Auto-merge works (different lines), but to be safe:
git merge -X ours feature/database-config
# In any conflict, prefer your version

# Or accept their database config:
git merge -X theirs feature/database-config
```

---

## 2. Rebase: Rewriting History

### 2.1 How Rebase Actually Works

**Rebase replays commits** on top of a new base.

```bash
git rebase main
```

**Step-by-step process:**

```
Before rebase:
      C1---C2---C3  (feature, HEAD)
     /
A---B---D1---D2  (main)

Step 1: Find merge base (B)
Step 2: Save commits C1, C2, C3 as patches
Step 3: Checkout D2 (main)
Step 4: Apply patch C1 → creates C1'
Step 5: Apply patch C2 → creates C2'
Step 6: Apply patch C3 → creates C3'
Step 7: Move feature pointer to C3'

After rebase:
                    C1'---C2'---C3'  (feature)
                   /
A---B---D1---D2  (main)
     \
      C1---C2---C3  (orphaned, will be garbage collected)
```

**Key Insight:** C1', C2', C3' are **NEW commits** with different SHA hashes. Original commits still exist (temporarily) in reflog.

### 2.2 Rebase vs Merge: The Tradeoffs

| Aspect | Merge | Rebase |
|--------|-------|--------|
| **History** | Preserves exact history | Rewrites history |
| **Graph** | Non-linear (shows branches) | Linear (clean line) |
| **Commits** | Keeps original commits | Creates new commits |
| **Conflicts** | Resolve once | Resolve per commit |
| **Safety** | Safe for public branches | **Dangerous** for public |
| **Use Case** | Team collaboration | Local cleanup |

**When to Merge:**
- ✅ Integrating feature branch into main
- ✅ Branch has been pushed to remote and shared
- ✅ Want to preserve context of parallel development
- ✅ Collaborating with multiple people on same branch

**When to Rebase:**
- ✅ Updating feature branch with latest main changes
- ✅ Cleaning up local commit history before pushing
- ✅ Commits are only in your local repository
- ✅ Want linear, easy-to-bisect history

**The Golden Rule of Rebase:**
> **Never rebase commits that exist outside your repository.**

### 2.3 Basic Rebase Workflow in Bibby

```bash
# Start feature branch from main
git checkout -b feature/search-filters main

# Make commits
git commit -m "Add genre filter"
git commit -m "Add year filter"
git commit -m "Add rating filter"

# Meanwhile, main gets new commits
# (someone merged another feature)

# Update your branch with latest main
git fetch origin main
git rebase origin/main

# Or in one command:
git pull --rebase origin main

# Now your commits are on top of latest main
git log --oneline --graph
# * 7e8f9g0 Add rating filter      (your commits, rebased)
# * 6d7e8f9 Add year filter
# * 5c6d7e8 Add genre filter
# * 4b5c6d7 (origin/main) Latest main commit
# * 3a4b5c6 Previous main commit
```

**After rebase, force push (since history changed):**

```bash
# ONLY if branch hasn't been shared or you coordinate with team
git push --force-with-lease origin feature/search-filters
```

**`--force-with-lease` vs `--force`:**
- `--force`: Overwrites remote even if others pushed
- `--force-with-lease`: Only overwrites if remote matches your expectation (safer)

### 2.4 Interactive Rebase: Git's Power Tool

**Use cases:**
- Squash multiple commits into one
- Reorder commits
- Edit commit messages
- Split commits
- Delete commits

**Start interactive rebase:**

```bash
# Rebase last 5 commits
git rebase -i HEAD~5

# Or rebase back to specific commit
git rebase -i abc123
```

**Interactive Rebase Editor:**

```
pick 1a2b3c4 Add genre filter
pick 2b3c4d5 Fix typo
pick 3c4d5e6 Add year filter
pick 4d5e6f7 WIP: debugging
pick 5e6f7g8 Add rating filter

# Commands:
# p, pick = use commit
# r, reword = use commit, but edit message
# e, edit = use commit, but stop for amending
# s, squash = use commit, but meld into previous commit
# f, fixup = like squash, but discard commit message
# d, drop = remove commit
```

**Example: Clean up Bibby feature branch:**

```bash
git rebase -i HEAD~5

# Change to:
pick 1a2b3c4 Add genre filter
fixup 2b3c4d5 Fix typo          # Merge into previous commit
pick 3c4d5e6 Add year filter
drop 4d5e6f7 WIP: debugging     # Delete this commit
pick 5e6f7g8 Add rating filter

# Save and close editor

# Result: 3 clean commits instead of 5
git log --oneline -n 3
# 9f0g1h2 Add rating filter
# 8e9f0g1 Add year filter
# 7d8e9f0 Add genre filter (includes typo fix)
```

**Squashing all commits into one:**

```bash
git rebase -i HEAD~3

# Change to:
pick 1a2b3c4 Add search filters
squash 2b3c4d5 Add year filter
squash 3c4d5e6 Add rating filter

# Git opens editor for combined commit message:
# Add search filters
#
# - Genre filter
# - Year filter
# - Rating filter
#
# Allows users to filter books by genre, publication year, and rating.
```

**Rewording commits:**

```bash
git rebase -i HEAD~3

# Change:
pick 1a2b3c4 Add stuff         # Bad message
pick 2b3c4d5 Fix things        # Bad message
pick 3c4d5e6 More fixes        # Bad message

# To:
reword 1a2b3c4 Add stuff
reword 2b3c4d5 Fix things
reword 3c4d5e6 More fixes

# Git opens editor for each commit:
# Change "Add stuff" to "Add genre filter to search functionality"
# Change "Fix things" to "Fix null pointer exception in search query"
# Change "More fixes" to "Add validation for search parameters"
```

### 2.5 Advanced: Autosquash

**Problem:** You find a typo after committing.

**Manual way:**
```bash
git commit -m "Fix typo"
# Later: interactive rebase to squash
```

**Autosquash way:**
```bash
# Create fixup commit
git commit --fixup=1a2b3c4  # SHA of commit to fix

# Later, rebase with autosquash
git rebase -i --autosquash HEAD~5

# Git automatically marks commits for squashing!
pick 1a2b3c4 Add genre filter
fixup 5e6f7g8 fixup! Add genre filter  # Automatically arranged
pick 2b3c4d5 Add year filter
```

**Configure autosquash by default:**
```bash
git config --global rebase.autosquash true

# Now `git rebase -i` always uses autosquash
```

---

## 3. Conflict Resolution: The Reality

### 3.1 Understanding Conflicts

**Conflict occurs when:**
- Same lines modified in both branches
- File deleted in one branch, modified in other
- File renamed in one branch, modified in other

**Git cannot auto-merge because it doesn't know your intent.**

### 3.2 Anatomy of a Merge Conflict

**Scenario with Bibby:**

```bash
# On main branch
# File: src/main/java/com/penrose/bibby/library/book/BookService.java

public BookEntity addBook(String title) {
    BookEntity book = new BookEntity();
    book.setTitle(title);
    book.setStatus("AVAILABLE");  // Line 25
    return bookRepository.save(book);
}

# On feature branch
# Same file, same line changed to:
    book.setStatus("ON_SHELF");  // Line 25

# Attempt merge:
git checkout main
git merge feature/book-status

# Output:
Auto-merging src/main/java/com/penrose/bibby/library/book/BookService.java
CONFLICT (content): Merge conflict in BookService.java
Automatic merge failed; fix conflicts and then commit the result.
```

**Conflict Markers in File:**

```java
public BookEntity addBook(String title) {
    BookEntity book = new BookEntity();
    book.setTitle(title);
<<<<<<< HEAD (main)
    book.setStatus("AVAILABLE");
=======
    book.setStatus("ON_SHELF");
>>>>>>> feature/book-status
    return bookRepository.save(book);
}
```

**Conflict Marker Anatomy:**
- `<<<<<<< HEAD` = Start of our changes (current branch)
- `=======` = Separator
- `>>>>>>> feature/book-status` = End of their changes (merging branch)

### 3.3 Resolving Conflicts: Step by Step

**Step 1: Identify conflicted files**

```bash
git status

# Output:
On branch main
You have unmerged paths.
  (fix conflicts and run "git commit")

Unmerged paths:
  (use "git add <file>..." to mark resolution)
        both modified:   src/main/java/com/penrose/bibby/library/book/BookService.java
```

**Step 2: Examine the conflict**

```bash
# See the three versions
git show :1:src/main/java/com/penrose/bibby/library/book/BookService.java  # Base
git show :2:src/main/java/com/penrose/bibby/library/book/BookService.java  # Ours (HEAD)
git show :3:src/main/java/com/penrose/bibby/library/book/BookService.java  # Theirs

# Or use diff3 format (shows base too)
git checkout --conflict=diff3 src/main/java/com/penrose/bibby/library/book/BookService.java
```

**With diff3, conflict looks like:**

```java
<<<<<<< HEAD (main)
    book.setStatus("AVAILABLE");
||||||| merged common ancestor
    book.setStatus("NEW");
=======
    book.setStatus("ON_SHELF");
>>>>>>> feature/book-status
```

**Now you see:**
- Base: `"NEW"`
- Ours: `"AVAILABLE"` (changed from NEW)
- Theirs: `"ON_SHELF"` (changed from NEW)

**Step 3: Choose resolution strategy**

**Option A: Accept ours**
```bash
git checkout --ours src/main/java/com/penrose/bibby/library/book/BookService.java
git add src/main/java/com/penrose/bibby/library/book/BookService.java
```

**Option B: Accept theirs**
```bash
git checkout --theirs src/main/java/com/penrose/bibby/library/book/BookService.java
git add src/main/java/com/penrose/bibby/library/book/BookService.java
```

**Option C: Manual resolution**
```bash
# Edit file in your editor
vim src/main/java/com/penrose/bibby/library/book/BookService.java

# Remove conflict markers and choose resolution:
public BookEntity addBook(String title) {
    BookEntity book = new BookEntity();
    book.setTitle(title);
    book.setStatus(BookStatus.AVAILABLE);  // Using enum instead
    return bookRepository.save(book);
}

# Save file
git add src/main/java/com/penrose/bibby/library/book/BookService.java
```

**Step 4: Verify resolution**

```bash
# Run tests to ensure code works
mvn test

# Check that all conflicts resolved
git status
# Should show: All conflicts fixed but you are still merging.

# Review the resolution
git diff --cached
```

**Step 5: Complete the merge**

```bash
git commit

# Git opens editor with default message:
Merge branch 'feature/book-status'

Conflicts:
        src/main/java/com/penrose/bibby/library/book/BookService.java

# Enhance the message:
Merge branch 'feature/book-status'

Resolved conflict in BookService.java by using BookStatus enum
instead of string literals. This provides better type safety.

Conflicts resolved:
- BookService.java: Changed both AVAILABLE and ON_SHELF to use
  BookStatus.AVAILABLE enum value
```

### 3.4 Merge Tools

**Configure merge tool:**

```bash
# Using VS Code
git config --global merge.tool vscode
git config --global mergetool.vscode.cmd 'code --wait --merge $REMOTE $LOCAL $BASE $MERGED'

# Using IntelliJ
git config --global merge.tool intellij
git config --global mergetool.intellij.cmd '/Applications/IntelliJ\ IDEA.app/Contents/MacOS/idea merge $(cd $(dirname "$LOCAL") && pwd)/$(basename "$LOCAL") $(cd $(dirname "$REMOTE") && pwd)/$(basename "$REMOTE") $(cd $(dirname "$BASE") && pwd)/$(basename "$BASE") $(cd $(dirname "$MERGED") && pwd)/$(basename "$MERGED")'

# Using vimdiff (built-in)
git config --global merge.tool vimdiff
```

**Use merge tool:**

```bash
git mergetool

# Opens configured tool with:
# - LOCAL (ours)
# - REMOTE (theirs)
# - BASE (common ancestor)
# - MERGED (result)

# After resolving, save and close
# Git automatically stages resolved file
```

**Recommended: Using VS Code for conflicts:**

```bash
# Open in VS Code
code src/main/java/com/penrose/bibby/library/book/BookService.java
```

VS Code shows:

```
Accept Current Change | Accept Incoming Change | Accept Both Changes | Compare Changes

<<<<<<< HEAD (Current Change)
    book.setStatus("AVAILABLE");
=======
    book.setStatus("ON_SHELF");
>>>>>>> feature/book-status (Incoming Change)
```

Click buttons to resolve visually!

---

## 4. Complex Conflict Scenarios

### 4.1 Conflict During Rebase

**Difference from merge conflicts:**
- Rebase applies commits one at a time
- You may need to resolve conflicts multiple times
- "Ours" and "theirs" are **reversed** (confusing!)

**During rebase:**
- `--ours` = the branch you're rebasing **onto** (e.g., main)
- `--theirs` = your branch's changes

**Example with Bibby:**

```bash
git checkout feature/search-filters
git rebase main

# Output:
First, rewinding head to replay your work on top of it...
Applying: Add genre filter
Applying: Add year filter
CONFLICT (content): Merge conflict in BookService.java
Failed to apply 6d7e8f9... Add year filter

# Resolve conflict
vim src/main/java/com/penrose/bibby/library/book/BookService.java
# ... fix conflict ...
git add src/main/java/com/penrose/bibby/library/book/BookService.java

# Continue rebase
git rebase --continue

# If another conflict occurs, repeat
# Or abort entirely:
git rebase --abort
```

**Rebase conflict resolution options:**

```bash
# After resolving conflict:
git rebase --continue    # Proceed to next commit

# Skip this commit entirely:
git rebase --skip

# Give up and restore original state:
git rebase --abort
```

### 4.2 Conflicting File Renames

**Scenario:**
- Branch A: Renamed `BookService.java` to `BookManagementService.java`
- Branch B: Modified `BookService.java`

**Git's behavior:**

```bash
git merge feature-rename

# Output:
CONFLICT (rename/delete): BookService.java deleted in feature-rename
and modified in HEAD. Version HEAD of BookService.java left in tree.
```

**Resolution:**

```bash
# Check what happened
git status

# Output shows:
#   deleted by them: BookService.java
#   added by them:   BookManagementService.java

# Apply your changes to renamed file
vim BookManagementService.java
# ... apply modifications from BookService.java ...

# Remove old file, add new one
git rm BookService.java
git add BookManagementService.java

# Commit
git commit
```

### 4.3 Binary File Conflicts

**Problem:** Can't merge binary files (images, PDFs, JARs).

**Example:**

```bash
# Both branches modified logo.png
git merge feature/new-logo

# Output:
CONFLICT (content): Merge conflict in docs/logo.png
```

**Resolution (choose one version):**

```bash
# Keep ours
git checkout --ours docs/logo.png
git add docs/logo.png

# Or keep theirs
git checkout --theirs docs/logo.png
git add docs/logo.png

# Or manually replace with desired version
cp /path/to/correct/logo.png docs/logo.png
git add docs/logo.png
```

### 4.4 Conflicts in pom.xml (Dependency Hell)

**Common scenario with Bibby's pom.xml:**

```xml
<!-- Main branch -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <version>3.5.7</version>
</dependency>

<!-- Feature branch -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <version>3.5.8</version>
</dependency>
```

**Conflict:**

```xml
<<<<<<< HEAD
    <version>3.5.7</version>
=======
    <version>3.5.8</version>
>>>>>>> feature/upgrade-dependencies
```

**Resolution strategy:**

```bash
# Generally, choose newer version
vim pom.xml
# Select 3.5.8

git add pom.xml

# IMPORTANT: Test after resolving
mvn clean verify

# Dependencies might have breaking changes
```

**Multiple dependency conflicts:**

```bash
# Use merge strategy for XML files
git merge -X theirs feature/upgrade-dependencies

# Or for specific file:
git checkout --theirs pom.xml
git add pom.xml
```

---

## 5. Advanced Techniques

### 5.1 Cherry-Pick: Selective Commit Application

**Use case:** Apply specific commit from another branch.

**Syntax:**

```bash
git cherry-pick <commit-hash>
```

**Bibby Example:**

```bash
# You fixed a bug on feature branch
git checkout feature/search-filters
git commit -m "Fix null pointer in search query"  # Commit abc123

# Bug also affects main
git checkout main
git cherry-pick abc123

# New commit created on main with same changes
```

**Cherry-picking multiple commits:**

```bash
# Pick range of commits
git cherry-pick abc123..def456

# Pick specific commits
git cherry-pick abc123 def456 ghi789
```

**Cherry-pick conflicts:**

```bash
git cherry-pick abc123

# If conflict occurs:
# ... resolve conflict ...
git add <resolved-files>
git cherry-pick --continue

# Or abort:
git cherry-pick --abort
```

### 5.2 Rebase --onto: Advanced Branch Surgery

**Use case:** Move commits from one base to another.

**Syntax:**

```bash
git rebase --onto <new-base> <old-base> <branch>
```

**Scenario with Bibby:**

```
      F1---F2  (feature/search-filters)
     /
    E1---E2  (experimental/new-architecture)
   /
  A---B---C  (main)
```

**You want to move `feature/search-filters` directly onto `main` (skip experimental):**

```bash
git rebase --onto main experimental/new-architecture feature/search-filters

# Result:
  A---B---C  (main)
           \
            F1'---F2'  (feature/search-filters)

  E1---E2  (experimental/new-architecture, orphaned)
```

**Another use case: Remove commits from middle of branch:**

```
A---B---C---D---E---F  (feature)
```

**Remove commits D and E:**

```bash
# Rebase F onto C (skipping D and E)
git rebase --onto C E F

# Or interactively:
git rebase -i C
# Mark D and E as "drop"
```

### 5.3 Rerere: Reuse Recorded Resolution

**Rerere** = "Reuse Recorded Resolution"

**Problem:** Same conflict appears multiple times (e.g., during rebase).

**Solution:** Git remembers how you resolved it.

**Enable rerere:**

```bash
git config --global rerere.enabled true
```

**How it works:**

```bash
# First time: resolve conflict manually
git merge feature
# ... conflict in BookService.java ...
# ... resolve manually ...
git add BookService.java
git commit

# Git records resolution

# Later: same conflict appears
git rebase main
# ... same conflict ...
git rerere

# Output:
Resolved 'BookService.java' using previous resolution.

# Git auto-resolves using recorded resolution!
git add BookService.java
git rebase --continue
```

**See recorded resolutions:**

```bash
git rerere diff
git rerere status
```

### 5.4 Merge vs Rebase: Real Bibby Workflow

**Feature Development (Rebase):**

```bash
# Start feature
git checkout -b feature/bibby-020-library-stats main

# Make commits
git commit -m "Add statistics entity"
git commit -m "Add statistics service"
git commit -m "Add stats CLI command"

# Main branch updated (someone else merged)
git fetch origin

# Rebase onto latest main
git rebase origin/main

# If conflicts, resolve and continue
git rebase --continue

# Force push (your branch only)
git push --force-with-lease origin feature/bibby-020-library-stats

# Create PR
gh pr create --title "Add library statistics feature"

# After PR approval, squash merge to main
gh pr merge --squash --delete-branch
```

**Hotfix (Merge):**

```bash
# Critical bug in production
git checkout -b hotfix/book-deletion-bug main

# Fix quickly
git commit -m "Fix cascading delete bug"

# Push immediately
git push -u origin hotfix/book-deletion-bug

# Create PR for review
gh pr create --title "URGENT: Fix cascading delete bug"

# Merge (preserve context of urgent fix)
gh pr merge --merge --delete-branch  # NOT squash

# Result in history:
# *   Merge pull request #42 from hotfix/book-deletion-bug
# |\
# | * Fix cascading delete bug
# |/
# * Previous commit
```

---

## 6. Preventing and Minimizing Conflicts

### 6.1 Best Practices

**1. Pull/Rebase Frequently**

```bash
# Daily routine
git fetch origin
git rebase origin/main  # or git pull --rebase

# Smaller, frequent conflicts easier than one huge conflict
```

**2. Keep Branches Short-Lived**

```
✅ Good: Feature branch lives 1-3 days
❌ Bad: Feature branch lives 3 weeks
```

**Long-lived branches = more conflicts.**

**3. Communicate with Team**

```bash
# Before refactoring BookService.java
Slack: "I'm refactoring BookService.java today, heads up!"

# Others avoid editing that file
```

**4. Use Feature Flags, Not Long Branches**

```java
// Merge incomplete code behind flag
if (featureFlags.isNewSearchEnabled()) {
    return newSearchImplementation();
} else {
    return oldSearchImplementation();
}
```

**5. Modularize Code**

```
✅ Good: Each feature touches different files
❌ Bad: Everyone edits BookService.java
```

**Better architecture = fewer conflicts.**

### 6.2 Git Attributes for Merge Strategies

**Configure merge strategies per file type:**

```bash
# .gitattributes

# Always prefer ours for database migrations
db/migrations/* merge=ours

# Always prefer theirs for generated code
src/generated/* merge=theirs

# Custom merge driver for pom.xml
pom.xml merge=maven-pom-merge
```

**Custom merge driver for Maven poms:**

```bash
# .git/config (or global)
[merge "maven-pom-merge"]
    name = Maven POM merge driver
    driver = mvn-merge %O %A %B %L %P
    # Custom tool that understands Maven dependency resolution
```

---

## 7. Troubleshooting Common Issues

### 7.1 "I messed up the merge, how do I start over?"

```bash
# Abort merge in progress
git merge --abort

# Or abort rebase
git rebase --abort

# Back to pre-merge state!
```

### 7.2 "I accidentally committed the conflict markers!"

```bash
# Find commits with conflict markers
git log -p --all -S"<<<<<<<"

# If in latest commit:
git reset HEAD~1
# ... fix file ...
git add .
git commit -m "Fix merge conflict properly"

# If already pushed:
git revert <bad-commit>
# Create new commit fixing the issue
```

### 7.3 "Rebase made a mess, how do I undo?"

```bash
# Find previous state in reflog
git reflog

# Output:
abc123 HEAD@{0}: rebase finished: refs/heads/feature onto def456
ghi789 HEAD@{1}: commit: Add search filters
...

# Restore to before rebase
git reset --hard HEAD@{1}

# Or restore to specific commit
git reset --hard ghi789
```

### 7.4 "I have 20 conflicts, this is overwhelming!"

**Strategy: Divide and conquer**

```bash
# Abort current merge
git merge --abort

# Merge with specific file exclusion
git merge -X ours feature  # Temporarily accept all ours

# Then selectively apply their changes file by file
git checkout feature -- src/main/java/com/penrose/bibby/specific/File.java
# ... resolve this one file ...
git add src/main/java/com/penrose/bibby/specific/File.java

# Repeat for each file
```

### 7.5 "How do I undo a merge that's already pushed?"

```bash
# Find merge commit
git log --oneline --graph

# Option 1: Revert (safe, creates new commit)
git revert -m 1 <merge-commit-hash>
# -m 1 means "revert to parent 1" (usually main)

# Option 2: Reset (dangerous, rewrites history)
git reset --hard <commit-before-merge>
git push --force  # ONLY if team agrees!
```

---

## 8. Interview-Ready Scenarios

### Question: "Explain the difference between merge and rebase"

**Bad Answer:** "Merge combines branches, rebase moves commits."

**Good Answer:**
"Merge and rebase both integrate changes from one branch into another, but they differ fundamentally in how they preserve history.

Merge creates a merge commit with two parents, preserving the exact history of when changes were made on different branches. It's non-destructive—original commits remain unchanged. The downside is a non-linear history with merge commits.

Rebase replays your commits on top of another branch, creating new commits with new SHA hashes. This produces a clean, linear history that's easy to follow. However, it rewrites history, which is dangerous for shared branches.

The golden rule: never rebase commits that exist outside your repository. For example, on my Bibby project, I rebase my local feature branches onto main to stay updated, but once I push, I use merge to integrate into main. This gives me clean local development with safe team collaboration.

In practice: rebase for private cleanup, merge for public integration."

**Why It's Good:**
- Explains both technical mechanism and practical implications
- Mentions the "golden rule"
- Real example from your project
- Discusses trade-offs

### Question: "How would you resolve a complex merge conflict?"

**Bad Answer:** "I'd look at the conflict markers and pick one."

**Good Answer:**
"I approach conflicts systematically. First, I'd examine all three versions: base (common ancestor), ours, and theirs using `git show :1/:2/:3` or enabling diff3 format. This context helps me understand what each side was trying to achieve.

For code conflicts, I'd check if both changes are necessary. Often conflicts occur when two branches modify the same function but for different reasons—both changes should be preserved, just integrated correctly.

I'd resolve in my IDE using the merge tool, then crucially, I'd run the full test suite. A conflict that resolves syntactically might still break semantically. I've seen cases where choosing 'ours' was syntactically correct but lost important logic from 'theirs'.

For complex conflicts spanning multiple files, I use rerere (reuse recorded resolution) to remember how I resolved similar conflicts. I also communicate with the author of the other branch if the intent isn't clear from the code.

Finally, I document my resolution in the merge commit message: not just 'resolved conflict' but 'resolved by combining validation logic from both branches' so future developers understand the decision."

**Why It's Good:**
- Systematic approach
- Mentions specific Git tools (diff3, rerere)
- Emphasizes testing
- Shows collaboration skills
- Thinks beyond syntax to semantics

### Question: "When would you use cherry-pick?"

**Bad Answer:** "When I want to copy a commit."

**Good Answer:**
"Cherry-pick is for selectively applying specific commits when you can't merge the entire branch. I've used it in several scenarios:

First, backporting bug fixes. If we fix a critical bug on the main branch that also affects a release/1.0 maintenance branch, I'd cherry-pick just that fix commit rather than merging all the new features from main.

Second, rescuing commits from abandoned branches. If a long-running feature branch becomes stale but has one valuable commit—say, a performance optimization—I'd cherry-pick just that commit.

Third, hotfixes to multiple versions. If we support Bibby v0.2.x and v0.3.x, a security fix might need to go to both. I'd fix on main, then cherry-pick to the release/0.2.x branch.

However, I'm cautious with cherry-pick because it duplicates commits (different SHAs), which can confuse history. If I find myself cherry-picking many commits, it usually means I should be merging instead, or my branching strategy needs adjustment."

**Why It's Good:**
- Multiple concrete use cases
- Real project reference (Bibby versions)
- Acknowledges limitations and alternatives
- Shows strategic thinking

---

## 9. Summary & Practice Plan

### Key Takeaways

✅ **Merge:**
- Preserves exact history
- Creates merge commits
- Safe for shared branches
- Use for: integrating features, public branches

✅ **Rebase:**
- Creates linear history
- Rewrites commits (new SHAs)
- Dangerous for shared branches
- Use for: local cleanup, staying updated

✅ **Conflicts:**
- Occur when same lines modified differently
- Resolve with merge tools or manual editing
- Always test after resolving
- Document resolution in commit message

✅ **Advanced Tools:**
- Cherry-pick: selective commit application
- Rebase --onto: branch surgery
- Rerere: remember resolutions
- Interactive rebase: clean up history

### Practice Exercises for Bibby

**Exercise 1: Basic Merge**

```bash
# Create two branches
git checkout -b feature-A main
echo "Feature A" >> test-merge.txt
git add test-merge.txt
git commit -m "Add feature A"

git checkout main
git checkout -b feature-B main
echo "Feature B" >> test-merge.txt
git add test-merge.txt
git commit -m "Add feature B"

# Merge and resolve conflict
git checkout main
git merge feature-A
git merge feature-B  # Conflict!

# Resolve, commit, verify
```

**Exercise 2: Interactive Rebase**

```bash
git checkout -b messy-commits main

# Make 5 messy commits
git commit --allow-empty -m "WIP"
git commit --allow-empty -m "Fix typo"
git commit --allow-empty -m "Actually working now"
git commit --allow-empty -m "Final version"
git commit --allow-empty -m "Forgot to add file"

# Clean up with interactive rebase
git rebase -i HEAD~5

# Squash into 1-2 meaningful commits
```

**Exercise 3: Conflict Resolution**

```bash
# Create conflicting changes in pom.xml
git checkout -b upgrade-deps-A main
# Modify pom.xml: Spring Boot 3.5.7 → 3.5.8
git commit -am "Upgrade Spring Boot"

git checkout main
git checkout -b upgrade-deps-B main
# Modify pom.xml: Spring Boot 3.5.7 → 3.6.0
git commit -am "Upgrade Spring Boot to 3.6"

# Merge and resolve
git checkout main
git merge upgrade-deps-A
git merge upgrade-deps-B  # Conflict!

# Resolve: decide on version, test, commit
```

**Exercise 4: Rebase Practice**

```bash
git checkout -b feature-rebasing main
git commit --allow-empty -m "Feature commit 1"
git commit --allow-empty -m "Feature commit 2"

git checkout main
git commit --allow-empty -m "Main commit 1"

git checkout feature-rebasing
git rebase main  # Replay feature commits onto main

# Observe new commit SHAs
```

### Next Steps

**Immediate Actions:**

1. **Practice on Bibby:**
   - Create conflicting branches
   - Practice merge and rebase
   - Use interactive rebase to clean history

2. **Configure Tools:**
   - Set up merge tool (VS Code, IntelliJ)
   - Enable rerere globally
   - Configure diff3 conflict style

3. **Establish Workflow:**
   - Document when to merge vs rebase
   - Create conflict resolution checklist
   - Practice daily rebasing routine

### Coming Up in Section 11

**Git Tagging & Release Management:**
- Lightweight vs annotated tags
- Semantic versioning with tags
- Creating GitHub Releases
- Managing release notes
- Tag-based deployment strategies
- Applying proper versioning to Bibby

You'll learn professional release management practices that make your project production-ready and enterprise-grade.

---

**Section 10 Complete:** You now have mastery over Git's most powerful features. Merge, rebase, and conflict resolution are no longer mysterious—they're tools you can wield with confidence! 🔀✨
