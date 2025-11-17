# Section 8: Git Mental Model & Internals

## Introduction: Understanding Git From First Principles

Most developers use Git like a black box—memorizing commands without understanding what's actually happening under the hood. This leads to **fear-based Git usage**: "Don't rebase, you'll break everything!" or "I lost my commits, they're gone forever!"

This section builds a **mental model** of how Git actually works internally. Once you understand the data structures, commands become intuitive, mistakes become recoverable, and advanced workflows become accessible.

**What You'll Learn:**
- Git's internal object model (commits, trees, blobs, refs)
- How commits form a directed acyclic graph (DAG)
- What branches really are (spoiler: just pointers)
- How to recover "lost" commits with reflog
- The staging area (index) and its purpose
- Real examples from Bibby's repository

**Why This Matters:**
Understanding Git internals is what separates junior developers from senior ones. In interviews, when asked "What's the difference between merge and rebase?", you want to explain it in terms of commit history and graph structure, not just repeat documentation.

---

## 1. Git's Data Model: Objects All The Way Down

### 1.1 The Four Object Types

Git stores everything as **content-addressable objects**. Each object has a SHA-1 hash (40 hex characters) that uniquely identifies it.

**The Four Object Types:**

1. **Blob** (Binary Large Object) - File contents
2. **Tree** - Directory structure
3. **Commit** - Snapshot with metadata
4. **Tag** - Named reference to a commit

Let's explore Bibby's actual Git objects to see this in action.

### 1.2 Exploring Bibby's Git Objects

**Find a recent commit in Bibby:**

```bash
git log --oneline -n 1

# Output (your actual commit):
# a83f587 Add DevOps Mentorship Section 7: CI/CD Best Practices & Security
```

**Inspect this commit object:**

```bash
git cat-file -p a83f587

# Output:
# tree 8f7a9e2c1b3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f
# parent 0fa1f3c
# author Your Name <your.email@example.com> 1234567890 +0000
# committer Your Name <your.email@example.com> 1234567890 +0000
#
# Add DevOps Mentorship Section 7: CI/CD Best Practices & Security
```

**Anatomy of a Commit:**
- `tree`: Points to a tree object (directory snapshot)
- `parent`: Points to previous commit(s) - this is how history is linked
- `author`: Who wrote the code
- `committer`: Who committed it (can differ in collaborative workflows)
- Message: Description of what changed

**Inspect the tree object:**

```bash
git cat-file -p 8f7a9e2  # First 7 chars of tree hash

# Output (simplified):
# 040000 tree 1a2b3c4 docs
# 100644 blob 5d6e7f8 README.md
# 100644 blob 9g0h1i2 pom.xml
# 040000 tree 3j4k5l6 src
```

**Tree Format:**
- `040000` = directory permissions (octal)
- `100644` = file permissions (regular file, readable)
- `tree` or `blob` = object type
- Hash = object ID
- Name = filename/directory name

**Inspect a blob (actual file content):**

```bash
# Find the blob hash for pom.xml
git ls-tree HEAD pom.xml

# Output:
# 100644 blob 9a8b7c6d5e4f3g2h1i0j9k8l7m6n5o4p3q2r1s0 pom.xml

git cat-file -p 9a8b7c6

# Output: The actual contents of pom.xml from that commit
# <?xml version="1.0" encoding="UTF-8"?>
# <project xmlns="http://maven.apache.org/POM/4.0.0"
# ...
```

### 1.3 The Content-Addressable Storage Model

**Key Insight:** Git doesn't store diffs or deltas initially—it stores **complete snapshots** of files.

**Example with Bibby:**

```bash
# Create a test file
echo "Hello Bibby" > test.txt
git hash-object test.txt

# Output: 3b18e512dba79e4c8300dd08aeb37f8e728b8dad

# Change one character
echo "Hello Bibby!" > test.txt
git hash-object test.txt

# Output: 8f94139338f9404f26296befa88755fc2598c289
```

**Observation:** Completely different hashes! Even a 1-character change creates a new blob.

**Why This Matters:**
- **Integrity:** Any corruption is immediately detected (hash mismatch)
- **Deduplication:** Identical files share the same blob (even across commits)
- **Efficiency:** Git packs objects later to save space (delta compression)

### 1.4 Practical Exercise: Trace a File's History in Bibby

Let's trace how `pom.xml` has evolved:

```bash
# Show all commits that modified pom.xml
git log --oneline -- pom.xml

# Output (from Bibby's history):
# a78a5cc Add DevOps Mentorship Section 3: Build Lifecycle Fundamentals
# 1b2c3d4 Initial commit
```

**See what changed in Section 3 commit:**

```bash
git show a78a5cc:pom.xml | head -n 20

# Shows pom.xml as it existed at that commit
```

**Compare two versions:**

```bash
git diff 1b2c3d4:pom.xml a78a5cc:pom.xml

# Shows differences between initial version and Section 3 update
```

**Mental Model Check:**
- Each commit has a **complete snapshot** of pom.xml (stored as a blob)
- Blobs are immutable (never change)
- When you `git diff`, Git compares two blob objects
- The commit's tree points to the blob hash

---

## 2. Commits as a Directed Acyclic Graph (DAG)

### 2.1 Understanding the Commit Graph

**Commits form a graph** where:
- **Nodes** = commits
- **Edges** = parent relationships (pointing backward in time)
- **Directed** = edges have direction (child → parent)
- **Acyclic** = no cycles (can't be your own ancestor)

**Visualize Bibby's commit history:**

```bash
git log --oneline --graph --all --decorate

# Output (simplified):
# * a83f587 (HEAD -> claude/devops-mentorship-guide-015rS9AY1ftSKD9vRaetxrVC) Section 7
# * 0fa1f3c Section 6
# * e7abf47 Section 5
# * c8d2a68 Section 4
# * a78a5cc Section 3
# * 1b2c3d4 (main) Initial commit
```

**Graph Representation:**

```
a83f587 (HEAD)
   ↓ (parent)
0fa1f3c
   ↓
e7abf47
   ↓
c8d2a68
   ↓
a78a5cc
   ↓
1b2c3d4 (main)
```

### 2.2 Merge Commits: Multiple Parents

When you merge branches, you create a commit with **two parents**.

**Example Scenario:**

```bash
# On main branch
git checkout -b feature-checkout
# ... make commits ...
git commit -m "Add book checkout feature"

# Switch back to main
git checkout main
# ... make different commits ...
git commit -m "Fix bug in search"

# Merge feature branch
git merge feature-checkout
```

**Resulting Graph:**

```
    * 5a6b7c8 (main) Merge feature-checkout
    |\
    | * 3c4d5e6 (feature-checkout) Add book checkout feature
    * | 2b3c4d5 Fix bug in search
    |/
    * 1a2b3c4 Common ancestor
```

**The Merge Commit (5a6b7c8):**

```bash
git cat-file -p 5a6b7c8

# Output:
# tree 9f8e7d6
# parent 2b3c4d5  ← First parent (main)
# parent 3c4d5e6  ← Second parent (feature-checkout)
# author ...
#
# Merge feature-checkout
```

**Mental Model:**
- Regular commits have **one parent** (linear history)
- Merge commits have **two+ parents** (converging branches)
- This is how Git represents parallel development

### 2.3 Finding Common Ancestors

**Problem:** When merging, Git needs to find where branches diverged.

**Example with Bibby:**

```bash
# Create a test scenario
git checkout -b test-branch c8d2a68  # Section 4 commit
# Now HEAD is at Section 4

# Find common ancestor with current branch
git merge-base test-branch claude/devops-mentorship-guide-015rS9AY1ftSKD9vRaetxrVC

# Output: c8d2a68 (Section 4 - where we branched from)
```

**What `merge-base` Does:**
1. Traverses commit graph backward from both branches
2. Finds the most recent commit reachable from both
3. Returns that commit's hash

**Why This Matters:**
- Merge uses 3-way merge: base + ours + theirs
- Rebase replays commits on top of new base
- Understanding this helps debug complex merges

### 2.4 Reachability: What Commits Can I Access?

**Key Concept:** A commit is "reachable" if you can trace parent links from a ref (branch, tag, HEAD) to that commit.

**Example:**

```bash
# List all commits reachable from HEAD
git rev-list HEAD | wc -l

# Output: 7 (Sections 1-7 + initial commit)

# List commits reachable from main
git rev-list main | wc -l

# Output: 1 (just initial commit, if main hasn't moved)
```

**Important:** Commits NOT reachable from any ref are **candidates for garbage collection**.

**See "unreachable" commits (before GC):**

```bash
git fsck --unreachable --no-reflogs | grep commit

# Output: Commits that exist but aren't referenced
```

---

## 3. Branches: Just Pointers (Seriously)

### 3.1 What Branches Actually Are

**The Truth:** A branch is a **40-byte file** containing a commit hash.

**Prove it with Bibby:**

```bash
# See where branches are stored
ls .git/refs/heads/

# Output:
# claude/devops-mentorship-guide-015rS9AY1ftSKD9vRaetxrVC
# main

# Read the branch file
cat .git/refs/heads/claude/devops-mentorship-guide-015rS9AY1ftSKD9vRaetxrVC

# Output:
# a83f5874e3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8  (commit hash)
```

**That's it.** A branch is literally just a pointer to a commit.

**When you commit:**

```bash
# Before commit
cat .git/refs/heads/claude/devops-mentorship-guide-015rS9AY1ftSKD9vRaetxrVC
# a83f587...

# Make a commit
echo "test" > test.txt
git add test.txt
git commit -m "Test commit"

# After commit
cat .git/refs/heads/claude/devops-mentorship-guide-015rS9AY1ftSKD9vRaetxrVC
# 7b8c9d0...  (NEW commit hash)
```

**What Happened:**
1. Git created new commit object with parent `a83f587`
2. Git updated the branch file to point to new commit `7b8c9d0`
3. That's it. The old commit still exists (referenced by parent pointer)

### 3.2 HEAD: The Special Pointer

**HEAD** points to the currently checked-out reference (usually a branch).

```bash
cat .git/HEAD

# Output:
# ref: refs/heads/claude/devops-mentorship-guide-015rS9AY1ftSKD9vRaetxrVC
```

**HEAD is a pointer to a pointer:**
- `HEAD` → branch reference → commit hash

**Detached HEAD State:**

```bash
# Checkout a specific commit (not a branch)
git checkout a83f587

# Output:
# You are in 'detached HEAD' state...

cat .git/HEAD
# Output:
# a83f5874e3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8  (direct commit hash)
```

**Now HEAD points directly to a commit, not a branch.**

**Danger:** If you commit in detached HEAD state, there's **no branch pointer** to track it. Easy to lose commits.

**Recovery:**

```bash
# Make a commit in detached HEAD
echo "detached" > detached.txt
git add detached.txt
git commit -m "Detached commit"
# Output: [detached HEAD 8c9d0e1] Detached commit

# Note the commit hash!

# Switch back to branch
git checkout claude/devops-mentorship-guide-015rS9AY1ftSKD9vRaetxrVC
# Warning: Previous HEAD position was 8c9d0e1...

# Create a branch to save the commit
git branch recover-detached 8c9d0e1
```

**Mental Model:**
- Branches are cheap (just 40 bytes)
- Creating branches doesn't copy files
- Switching branches updates HEAD and working directory
- Commits aren't "on" a branch—branches point to commits

### 3.3 Practical: How Git Switch Works

**When you run `git checkout main` (or `git switch main`):**

1. **Read** `.git/refs/heads/main` → get commit hash `1b2c3d4`
2. **Update** `.git/HEAD` → `ref: refs/heads/main`
3. **Read commit object** `1b2c3d4` → get tree hash
4. **Read tree object** → get list of files/directories
5. **Update working directory** to match tree
6. **Update staging area (index)** to match tree

**See this in action:**

```bash
# Before switch
git branch --show-current
# Output: claude/devops-mentorship-guide-015rS9AY1ftSKD9vRaetxrVC

ls docs/devops-mentorship/
# Output: 01-sdlc... 02-agile... 03-build... ... 07-cicd...

# Switch to main
git checkout main

ls docs/devops-mentorship/
# Output: (probably empty or doesn't exist on main)

# The files didn't "go away"—they're still in .git/objects
# Git just updated your working directory to match main's tree
```

---

## 4. The Staging Area (Index): Git's Secret Weapon

### 4.1 The Three States of Files

**Git has three "trees":**

1. **Working Directory** - Your actual files on disk
2. **Staging Area (Index)** - Proposed next commit
3. **Repository (HEAD)** - Last commit

**Visualize with Bibby:**

```bash
# Current state
git status

# Output:
# On branch claude/devops-mentorship-guide-015rS9AY1ftSKD9vRaetxrVC
# nothing to commit, working tree clean
```

**All three trees match.** Now let's create differences:

```bash
# Modify a file (Working Directory)
echo "# Section 8 is awesome" >> docs/devops-mentorship/08-git-mental-model.md

git status
# Output:
# Changes not staged for commit:
#   modified: docs/devops-mentorship/08-git-mental-model.md

# Working Directory ≠ Staging Area = Repository
```

**Stage the change (add to Index):**

```bash
git add docs/devops-mentorship/08-git-mental-model.md

git status
# Output:
# Changes to be committed:
#   modified: docs/devops-mentorship/08-git-mental-model.md

# Working Directory = Staging Area ≠ Repository
```

**Commit (update Repository):**

```bash
git commit -m "Update Section 8"

# Working Directory = Staging Area = Repository
```

### 4.2 The Index File: Staging Area Internals

**The staging area is stored in `.git/index`** (binary file).

**Inspect it:**

```bash
git ls-files --stage

# Output (partial):
# 100644 8f7a9e2... 0	docs/devops-mentorship/01-sdlc-fundamentals.md
# 100644 9a8b7c6... 0	docs/devops-mentorship/02-agile-practices-solo.md
# 100644 1b2c3d4... 0	pom.xml
# 100644 2c3d4e5... 0	src/main/java/com/penrose/bibby/BibbyApplication.java
```

**Format:**
- `100644` = file mode (permissions)
- `8f7a9e2...` = blob hash (SHA-1 of file contents)
- `0` = stage number (used during merge conflicts)
- Filename

**Key Insight:** The index stores **blob hashes**, not file contents. When you `git add`, Git:
1. Creates a blob object from file contents
2. Updates index to point to that blob
3. The blob is now in `.git/objects/` (even before commit)

### 4.3 Partial Staging: Git's Killer Feature

**Problem:** You made multiple changes but only want to commit some.

**Solution:** Stage hunks interactively.

**Example with Bibby:**

```bash
# Make two unrelated changes to pom.xml
# 1. Update version to 0.3.0
# 2. Add a new dependency

# Stage only the version change
git add -p pom.xml

# Output:
# diff --git a/pom.xml b/pom.xml
# ...
# -    <version>0.0.1-SNAPSHOT</version>
# +    <version>0.3.0</version>
# ...
# Stage this hunk [y,n,q,a,d,s,e,?]?

# Press 'y' to stage version change
# Press 'n' to skip dependency addition

git commit -m "Bump version to 0.3.0"

# Now you can stage and commit the dependency separately
git add -p pom.xml
# Press 'y' for dependency change
git commit -m "Add Testcontainers dependency"
```

**Why This Matters:**
- **Atomic commits:** Each commit has one logical change
- **Clean history:** Easy to understand, revert, or cherry-pick
- **Code review:** Smaller, focused commits are easier to review

### 4.4 Resetting: Moving Pointers

Git's `reset` command moves the HEAD/branch pointer and optionally updates staging area and working directory.

**Three Modes:**

**1. `git reset --soft <commit>`**
- Moves HEAD/branch pointer
- Keeps staging area and working directory unchanged
- **Use case:** Undo last commit, keep changes staged

**2. `git reset --mixed <commit>` (default)**
- Moves HEAD/branch pointer
- Updates staging area to match commit
- Keeps working directory unchanged
- **Use case:** Unstage files

**3. `git reset --hard <commit>`**
- Moves HEAD/branch pointer
- Updates staging area to match commit
- Updates working directory to match commit
- **Use case:** Discard all changes (DANGEROUS)

**Example with Bibby:**

```bash
# Current state
git log --oneline -n 2
# a83f587 Section 7
# 0fa1f3c Section 6

# Soft reset (undo commit, keep changes)
git reset --soft HEAD~1

git log --oneline -n 1
# 0fa1f3c Section 6  (back one commit)

git status
# Changes to be committed:
#   new file: docs/devops-mentorship/07-cicd-best-practices-security.md

# The Section 7 file is still there, staged!

# Restore the commit
git commit -m "Add DevOps Mentorship Section 7: CI/CD Best Practices & Security"
```

**Mental Model:**
- `reset` doesn't delete commits (they're still in reflog)
- `--soft` = "I want to redo this commit"
- `--mixed` = "I want to unstage these changes"
- `--hard` = "I want to discard everything" ⚠️

---

## 5. The Reflog: Git's Safety Net

### 5.1 What is the Reflog?

**Reflog** (reference log) records every time HEAD or a branch pointer moves.

**Think of it as:** "Undo history for Git commands"

**View Bibby's reflog:**

```bash
git reflog

# Output:
# a83f587 HEAD@{0}: commit: Add DevOps Mentorship Section 7: CI/CD Best Practices & Security
# 0fa1f3c HEAD@{1}: commit: Add DevOps Mentorship Section 6: Continuous Deployment & Delivery
# e7abf47 HEAD@{2}: commit: Add DevOps Mentorship Section 5: CI Fundamentals with GitHub Actions
# c8d2a68 HEAD@{3}: commit: Add DevOps Mentorship Section 4: Semantic Versioning & Release Management
# a78a5cc HEAD@{4}: commit: Add DevOps Mentorship Section 3: Build Lifecycle Fundamentals
# ...
```

**What it shows:**
- `HEAD@{0}` = current position
- `HEAD@{1}` = where HEAD was before last command
- `HEAD@{2}` = where HEAD was before that, etc.

**Each entry records:**
- Commit hash
- HEAD position
- Command that moved HEAD
- Timestamp

### 5.2 Recovering "Lost" Commits

**Scenario: Accidental Hard Reset**

```bash
# Current state
git log --oneline -n 1
# a83f587 Section 7

# OOPS! Accidentally hard reset
git reset --hard HEAD~3

git log --oneline -n 1
# c8d2a68 Section 4

# Sections 5, 6, 7 seem "lost"!
```

**Recovery with Reflog:**

```bash
git reflog
# c8d2a68 HEAD@{0}: reset: moving to HEAD~3
# a83f587 HEAD@{1}: commit: Section 7  ← Found it!
# 0fa1f3c HEAD@{2}: commit: Section 6
# e7abf47 HEAD@{3}: commit: Section 5

# Restore to Section 7
git reset --hard a83f587

git log --oneline -n 1
# a83f587 Section 7  ← Recovered!
```

**Mental Model:**
- Commits are **never truly deleted** (until garbage collection, ~30 days)
- Reflog tracks where pointers were
- You can always get back to a previous state
- This makes Git experiments safe

### 5.3 Finding Lost Commits After Branch Delete

**Scenario:**

```bash
# Create and commit on a branch
git checkout -b feature-temp
echo "temp" > temp.txt
git add temp.txt
git commit -m "Temporary work"
# [feature-temp 9d0e1f2] Temporary work

# Switch away and delete branch
git checkout main
git branch -D feature-temp
# Deleted branch feature-temp (was 9d0e1f2)

# Oh no! I needed that work!
```

**Recovery:**

```bash
# Reflog for the deleted branch
git reflog show feature-temp

# Output:
# 9d0e1f2 feature-temp@{0}: commit: Temporary work

# Create new branch at that commit
git branch feature-recovered 9d0e1f2

git checkout feature-recovered
cat temp.txt
# Output: temp  ← Recovered!
```

### 5.4 Reflog Expiration

**Default retention:**
- Unreachable commits: 30 days
- Reachable commits: 90 days

**Check reflog retention:**

```bash
git config gc.reflogExpire
# Output: 90 (days)

git config gc.reflogExpireUnreachable
# Output: 30 (days)
```

**Manually trigger garbage collection:**

```bash
# Dry run (see what would be deleted)
git gc --prune=now --dry-run

# Actual collection (be careful!)
git gc --prune=now
```

**Best Practice:** Don't worry about reflog—it's automatic. Just know it's there when you need it.

---

## 6. Real-World Git Internals: Debugging Bibby Issues

### 6.1 Mystery: Why is `.git` folder so big?

**Check size:**

```bash
du -sh .git
# Output: 15M (or varies)

# Find largest objects
git rev-list --objects --all | \
  git cat-file --batch-check='%(objecttype) %(objectname) %(objectsize) %(rest)' | \
  sed -n 's/^blob //p' | \
  sort -k2 -n -r | \
  head -n 10

# Output (example):
# a1b2c3d 8589934592 large-binary-file.zip
# e4f5g6h 4294967296 database-dump.sql
```

**Solution:** Remove large files from history (advanced):

```bash
# Find when file was added
git log --all --full-history --oneline -- large-binary-file.zip

# Use git filter-repo (better than filter-branch)
git filter-repo --path large-binary-file.zip --invert-paths

# Force push (rewrites history)
git push --force
```

### 6.2 Mystery: Merge Conflict - What Exactly Conflicts?

**During a merge conflict:**

```bash
git merge feature-branch
# Auto-merging pom.xml
# CONFLICT (content): Merge conflict in pom.xml

# Check the three versions
git show :1:pom.xml > pom-base.xml    # Common ancestor
git show :2:pom.xml > pom-ours.xml    # Current branch (HEAD)
git show :3:pom.xml > pom-theirs.xml  # Merging branch

# Compare them
diff pom-ours.xml pom-theirs.xml
```

**The index during conflicts:**

```bash
git ls-files --stage pom.xml

# Output:
# 100644 1a2b3c4... 1	pom.xml  ← Stage 1: base
# 100644 5d6e7f8... 2	pom.xml  ← Stage 2: ours
# 100644 9g0h1i2... 3	pom.xml  ← Stage 3: theirs
```

**After resolving conflict:**

```bash
git add pom.xml

git ls-files --stage pom.xml
# Output:
# 100644 3j4k5l6... 0	pom.xml  ← Back to stage 0 (resolved)
```

**Mental Model:**
- Normal files: stage 0
- Conflicted files: stages 1, 2, 3 (base, ours, theirs)
- `git add` promotes to stage 0 (marks as resolved)

### 6.3 Mystery: Who Changed This Line and Why?

**Git blame with context:**

```bash
# Find who last modified each line in pom.xml
git blame pom.xml | head -n 20

# Output:
# a78a5cc (You 2025-01-15 10:23 13) <version>0.0.1-SNAPSHOT</version>
# 1b2c3d4 (You 2025-01-10 14:30 14) <name>Bibby</name>

# See the full commit
git show a78a5cc

# See the commit message and context
```

**Blame through renames/moves:**

```bash
# Follow file history even through renames
git blame -C -C -C pom.xml

# -C: Detect lines moved within file
# -C -C: Detect lines moved between files
# -C -C -C: Detect lines from commits that created the file
```

**Blame a specific line range:**

```bash
# Who changed lines 30-40?
git blame -L 30,40 pom.xml
```

---

## 7. Advanced Mental Models

### 7.1 Rebase: Replay Commits on New Base

**Conceptual Model:**

```
Before rebase:
      A---B---C  (feature)
     /
D---E---F---G  (main)

After `git rebase main`:
              A'---B'---C'  (feature)
             /
D---E---F---G  (main)
```

**What Actually Happens:**

1. Git finds common ancestor (D)
2. Saves diffs for A, B, C
3. Checks out G (new base)
4. Applies diff A → creates A'
5. Applies diff B → creates B'
6. Applies diff C → creates C'
7. Moves feature branch pointer to C'

**Important:** A', B', C' are **NEW commits** (different hashes)—the originals (A, B, C) still exist in reflog.

**Try it with Bibby:**

```bash
# Create a test branch
git checkout -b test-rebase c8d2a68  # Section 4

echo "test" > test-file.txt
git add test-file.txt
git commit -m "Test commit"

# Rebase onto current branch (Section 7)
git rebase claude/devops-mentorship-guide-015rS9AY1ftSKD9vRaetxrVC

# Output:
# Successfully rebased and updated refs/heads/test-rebase

git log --oneline -n 2
# 8c9d0e1 Test commit  ← NEW hash (replayed)
# a83f587 Section 7    ← New parent
```

**Key Insight:** Rebase rewrites history. Never rebase commits pushed to shared branches.

### 7.2 Cherry-Pick: Copy Commit to Current Branch

**Model:** Cherry-pick creates a **new commit** with the same changes as the picked commit.

```
Before cherry-pick:
      A---B---C  (feature)
     /
D---E  (main, HEAD)

After `git cherry-pick B`:
      A---B---C  (feature)
     /
D---E---B'  (main)
```

**Example with Bibby:**

```bash
# On main branch
git checkout main

# Pick a specific improvement from feature branch
git cherry-pick e7abf47  # Section 5 commit

# Creates new commit with same changes but different parent
```

**Use Cases:**
- Backport bug fix to older release branch
- Apply specific feature without merging entire branch
- Rescue commit from abandoned branch

### 7.3 Bisect: Binary Search for Bugs

**Problem:** A bug exists in current code but not 10 commits ago. Which commit introduced it?

**Solution:** Binary search through commits.

```bash
# Start bisect
git bisect start

# Mark current commit as bad
git bisect bad

# Mark old commit as good
git bisect good 1b2c3d4  # Initial commit

# Output:
# Bisecting: 3 revisions left to test after this (roughly 2 steps)
# [c8d2a68] Section 4

# Test the code at this commit
mvn test

# If tests pass:
git bisect good

# If tests fail:
git bisect bad

# Repeat until Git finds the culprit commit
# Output:
# e7abf47 is the first bad commit
# commit e7abf47
# Author: ...
# Add DevOps Mentorship Section 5: CI Fundamentals

# End bisect
git bisect reset
```

**Automated bisect:**

```bash
git bisect start HEAD 1b2c3d4
git bisect run mvn test

# Git automatically tests each commit until finding the culprit
```

---

## 8. The .git Directory: A Complete Tour

```
.git/
├── HEAD                    # Current branch reference
├── config                  # Repository configuration
├── description             # Repository description (for GitWeb)
├── hooks/                  # Git hooks (pre-commit, etc.)
├── info/
│   └── exclude             # Local .gitignore (not committed)
├── objects/                # All Git objects (commits, trees, blobs)
│   ├── 1a/
│   │   └── 2b3c4d5e6f...   # Object file (first 2 chars = directory)
│   ├── pack/               # Packed objects (compression)
│   └── info/
├── refs/
│   ├── heads/              # Local branches
│   │   └── main
│   ├── remotes/            # Remote branches
│   │   └── origin/
│   │       └── main
│   └── tags/               # Tags
├── logs/                   # Reflogs
│   ├── HEAD                # HEAD reflog
│   └── refs/
│       └── heads/
│           └── main        # Branch reflog
└── index                   # Staging area (binary)
```

**Inspect objects directory:**

```bash
# Count total objects
find .git/objects -type f | wc -l

# See object types
git count-objects -v

# Output:
# count: 234           # Loose objects
# size: 1024          # Size in KB
# in-pack: 1500       # Packed objects
# packs: 1            # Number of pack files
# size-pack: 2048     # Pack size in KB
```

**Manual object lookup:**

```bash
# Given hash a83f5874e3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8
# Object stored at: .git/objects/a8/3f5874e3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8

ls .git/objects/a8/
# Output: 3f5874e3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8

# Read it (compressed with zlib)
git cat-file -p a83f587
```

---

## 9. Interview-Ready Mental Models

### Question: "Explain how Git stores data internally"

**Bad Answer:** "Git stores files and changes between them."

**Good Answer:**
"Git uses a content-addressable storage model with four object types: blobs for file contents, trees for directory structure, commits for snapshots with metadata, and tags for named references. Each object is identified by a SHA-1 hash of its contents. When you commit, Git creates a commit object pointing to a tree object, which points to blobs for each file. Commits form a directed acyclic graph through parent pointers. What's interesting is Git stores complete snapshots, not deltas—though it later compresses objects into packfiles for efficiency. This design makes Git extremely fast for operations like checkout and diff, since it's just comparing tree objects."

**Why It's Good:**
- Demonstrates deep understanding of internals
- Uses correct terminology (DAG, content-addressable, packfiles)
- Explains trade-offs (snapshots vs deltas, compression)
- Shows performance awareness

### Question: "What's the difference between git reset and git revert?"

**Bad Answer:** "reset goes back to a previous commit, revert undoes changes."

**Good Answer:**
"They solve different problems. `git reset` moves the branch pointer to a different commit—it rewrites history. It has three modes: --soft keeps changes staged, --mixed unstages them, and --hard discards them entirely. Reset is great for local work but dangerous on shared branches because it rewrites history.

`git revert`, on the other hand, creates a *new* commit that undoes changes from a previous commit. It doesn't rewrite history, so it's safe for shared branches. If you revert commit A, Git creates commit A' with inverse changes. The original commit A still exists in history.

In practice: I use reset locally to clean up my commit history before pushing, and revert for undoing changes that are already pushed to a shared branch."

**Why It's Good:**
- Explains both conceptually and practically
- Mentions the three reset modes
- Discusses when to use each
- Shows awareness of collaboration concerns

### Question: "How would you recover a deleted branch?"

**Bad Answer:** "You can't, it's deleted."

**Good Answer:**
"Branches are just pointers to commits, so deleting a branch doesn't delete the commits. I'd use the reflog to find where the branch was pointing. For example, `git reflog show deleted-branch` shows all positions of that branch pointer. Even if the branch is gone, `git reflog` shows all HEAD movements, so I can find the commit hash when I was on that branch. Then I'd create a new branch at that commit: `git branch recovered-branch <hash>`. The commits are retained in the object database for at least 30 days before garbage collection, so there's usually time to recover. This is why Git's reflog is often called its safety net."

**Why It's Good:**
- Explains the underlying model (branches as pointers)
- Provides step-by-step recovery process
- Mentions garbage collection timeline
- Demonstrates problem-solving approach

---

## 10. Practical Exercises for Bibby

### Exercise 1: Object Exploration

```bash
# 1. Find the tree hash for your latest commit
git cat-file -p HEAD

# 2. List all files in that tree
git ls-tree -r HEAD

# 3. Find the blob hash for pom.xml
git ls-tree HEAD pom.xml

# 4. View pom.xml contents from 3 commits ago
git show HEAD~3:pom.xml

# 5. Compare blob hashes between commits
git rev-parse HEAD:pom.xml
git rev-parse HEAD~3:pom.xml
# If hashes match, file hasn't changed
```

### Exercise 2: Reflog Recovery Simulation

```bash
# 1. Create a test commit
git checkout -b reflog-test
echo "test" > test.txt
git add test.txt
git commit -m "Test commit"
# Note the commit hash!

# 2. Delete the branch
git checkout claude/devops-mentorship-guide-015rS9AY1ftSKD9vRaetxrVC
git branch -D reflog-test

# 3. Recover using reflog
git reflog | grep "Test commit"
git branch reflog-recovered <hash>

# 4. Verify recovery
git checkout reflog-recovered
cat test.txt

# 5. Clean up
git checkout claude/devops-mentorship-guide-015rS9AY1ftSKD9vRaetxrVC
git branch -D reflog-recovered
```

### Exercise 3: Staging Area Manipulation

```bash
# 1. Make multiple changes to pom.xml
# - Change version
# - Add a comment
# - Add a dependency

# 2. Stage only version change
git add -p pom.xml
# Press 'y' for version, 'n' for others

# 3. Check staging area
git diff --cached pom.xml  # Staged changes
git diff pom.xml           # Unstaged changes

# 4. Commit staged changes
git commit -m "Update version"

# 5. Stage remaining changes
git add pom.xml
git commit -m "Add dependency and comments"
```

### Exercise 4: Bisect Bug Hunt

```bash
# 1. Introduce a "bug" in an old commit (simulation)
git checkout c8d2a68  # Section 4
echo "BUG" >> pom.xml
git add pom.xml
git commit --amend --no-edit

# 2. Return to current branch
git checkout claude/devops-mentorship-guide-015rS9AY1ftSKD9vRaetxrVC

# 3. Use bisect to find the bug
git bisect start
git bisect bad HEAD
git bisect good 1b2c3d4

# 4. Test each commit
grep -q "BUG" pom.xml && git bisect bad || git bisect good

# 5. Automate it
git bisect reset
git bisect start HEAD 1b2c3d4
git bisect run grep -q "BUG" pom.xml
```

---

## 11. Summary & Key Takeaways

### Mental Model Checklist

✅ **Objects:**
- Git stores four object types: blob, tree, commit, tag
- Objects are content-addressable (SHA-1 hash)
- Objects are immutable and never change

✅ **Commits:**
- Commits form a directed acyclic graph (DAG)
- Each commit points to parent(s) and a tree
- Merge commits have multiple parents

✅ **Branches:**
- Branches are just 40-byte pointers to commits
- Creating branches is cheap (no file copying)
- HEAD points to current branch (or commit in detached state)

✅ **Staging Area:**
- Index stores blob hashes for next commit
- Enables atomic, logical commits (partial staging)
- Three states: working directory, staging area, repository

✅ **Reflog:**
- Records every HEAD/branch movement
- Enables recovery of "lost" commits
- Retained for 30-90 days before garbage collection

### Common Misconceptions Debunked

❌ **"Branches contain commits"**
✅ **Truth:** Branches point to commits. Commits contain pointers to parents.

❌ **"Deleting a branch deletes commits"**
✅ **Truth:** Only the pointer is deleted. Commits remain until GC.

❌ **"Git stores file changes (deltas)"**
✅ **Truth:** Git stores complete snapshots (optimized later with packfiles).

❌ **"Lost commits are gone forever"**
✅ **Truth:** Reflog can recover commits for 30+ days.

❌ **"Reset is dangerous and loses data"**
✅ **Truth:** Reset moves pointers; reflog can undo it.

### Interview-Ready Sound Bites

**On Git's Architecture:**
"Git's content-addressable storage model makes it a distributed database where every commit is a complete snapshot, forming a directed acyclic graph through parent pointers."

**On Branches:**
"Branches are lightweight 40-byte pointers to commits—creating a branch doesn't copy files, it just creates a reference."

**On Recovery:**
"Git's reflog acts as a safety net, recording every HEAD movement for at least 30 days, making most 'mistakes' recoverable."

**On Workflow:**
"The staging area enables atomic commits—I can stage specific hunks from a file, creating clean, logical commits even when multiple concerns are mixed in my working directory."

---

## 12. Next Steps & Resources

### Hands-On Practice

1. **Explore Bibby's objects:**
   - Find the 10 largest blobs
   - Trace a file through 5 commits
   - Identify all merge commits (if any)

2. **Experiment with reflog:**
   - Simulate a hard reset "mistake"
   - Recover a deleted branch
   - Find when a specific file was modified

3. **Master the staging area:**
   - Practice `git add -p` on multi-concern changes
   - Use `git reset -p` to unstage specific hunks
   - Create 3 atomic commits from 10 changes

### Deepen Understanding

**Must-Read:**
- [Pro Git Book - Chapter 10: Git Internals](https://git-scm.com/book/en/v2/Git-Internals-Plumbing-and-Porcelain)
- [Git from the Bottom Up](https://jwiegley.github.io/git-from-the-bottom-up/)

**Visual Learning:**
- [Visualizing Git Concepts with D3](https://onlywei.github.io/explain-git-with-d3/)
- [Learn Git Branching](https://learngitbranching.js.org/) - Interactive tutorial

**Advanced Topics:**
- `git filter-repo` for history rewriting
- Git hooks for workflow automation
- Custom merge strategies

### Coming Up in Section 9

**Git Branching Strategies & Team Workflows:**
- GitFlow vs GitHub Flow vs Trunk-Based Development
- When to use each strategy (team size, release cadence)
- Implementing branch protection rules
- Code review workflows with pull requests
- Applying the right strategy to Bibby

You'll learn how to structure branch workflows for teams of any size, from solo projects (like Bibby today) to enterprise organizations with 100+ developers.

---

**Section 8 Complete:** You now understand Git at a fundamental level—not just memorizing commands, but grasping the underlying data structures. This mental model makes advanced Git operations intuitive and mistakes recoverable. 🧠✨
