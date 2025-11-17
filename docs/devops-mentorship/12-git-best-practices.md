# Section 12: Git Best Practices & Complete Workflows

## Introduction: From Good to Great

You now understand Git's internals, branching strategies, and release management. This section ties everything together with **best practices** that separate professional developers from those who just "know Git."

These aren't arbitrary rules—they're battle-tested patterns that prevent bugs, improve collaboration, and make your Git history a valuable project asset instead of cryptic noise.

**What You'll Learn:**
- Commit message conventions that create readable history
- Git hooks for automation and quality gates
- Advanced configurations that boost productivity
- Complete workflows for solo, small team, and enterprise contexts
- Common anti-patterns and how to avoid them
- Building your personal Git toolkit

**Real-World Context:**
Look at any popular open-source project (Linux kernel, React, Kubernetes). Their Git histories are pristine: clear commit messages, logical grouping, easy to bisect. That's not accident—it's discipline and tooling.

We'll apply these same practices to Bibby, making it portfolio-ready and interview-worthy.

---

## 1. Commit Message Best Practices

### 1.1 The Anatomy of a Great Commit Message

**Structure:**

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Example from Bibby:**

```
feat(cli): add ISBN lookup command

Implement ISBN lookup using OpenLibrary API. Users can now search
for books by ISBN and automatically populate book details.

The command supports both ISBN-10 and ISBN-13 formats, with
automatic validation and error handling for invalid ISBNs.

Implementation includes:
- OpenLibraryClient with rate limiting (10 req/sec)
- ISBN validation utility
- Comprehensive error messages
- Integration tests with WireMock

BREAKING CHANGE: Book entity now requires ISBN field for new books
(existing books unaffected)

Closes #42
```

**Breakdown:**

**1. Type (required):**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Code style (formatting, no logic change)
- `refactor`: Code refactoring
- `perf`: Performance improvement
- `test`: Adding/updating tests
- `build`: Build system changes (Maven, Docker)
- `ci`: CI/CD configuration changes
- `chore`: Maintenance tasks

**2. Scope (optional):**
- Component affected: `cli`, `service`, `repository`, `entity`
- Helps quickly identify impact area

**3. Subject (required):**
- Imperative mood: "add" not "added" or "adds"
- No period at the end
- Max 50 characters
- Lowercase after type

**4. Body (optional but recommended):**
- Explains **what** and **why**, not how
- Wrap at 72 characters
- Separate from subject with blank line
- Can have multiple paragraphs

**5. Footer (optional):**
- Breaking changes: `BREAKING CHANGE:`
- Issue references: `Closes #42`, `Fixes #123`
- Co-authors: `Co-authored-by: Name <email>`

### 1.2 Conventional Commits

**Specification:** https://www.conventionalcommits.org/

**Benefits:**
- Automatic changelog generation
- Semantic version bumping
- Clear communication of intent
- Easy to filter commits by type

**Bibby Examples:**

```bash
# Feature additions
git commit -m "feat(search): add fuzzy search with Levenshtein distance"
git commit -m "feat(export): add CSV export for library catalog"

# Bug fixes
git commit -m "fix(search): correct pagination offset calculation"
git commit -m "fix(cli): prevent NPE when book has no authors"

# Documentation
git commit -m "docs(readme): add installation instructions for Windows"
git commit -m "docs(api): add JavaDoc to BookService public methods"

# Refactoring
git commit -m "refactor(service): extract validation logic to separate class"
git commit -m "refactor(entity): replace String status with BookStatus enum"

# Performance
git commit -m "perf(search): add database index on book title column"
git commit -m "perf(cli): lazy load author relationships"

# Tests
git commit -m "test(service): add integration tests for book deletion"
git commit -m "test(cli): increase coverage to 85%"

# Build/CI
git commit -m "build(maven): upgrade Spring Boot to 3.5.8"
git commit -m "ci(github): add security scanning to pipeline"

# Chores
git commit -m "chore(deps): update dependencies to latest patch versions"
git commit -m "chore(git): add .gitattributes for line endings"
```

### 1.3 Bad vs Good Commit Messages

**❌ Bad:**

```
git commit -m "fixed bug"
git commit -m "updated files"
git commit -m "WIP"
git commit -m "asdfasdf"
git commit -m "Final version"
git commit -m "Final version (for real this time)"
```

**Problems:**
- No context
- Meaningless in history
- Impossible to search
- Unhelpful for debugging

**✅ Good:**

```
git commit -m "fix(search): handle null book titles in query

BookRepository.searchByTitle() threw NPE when encountering
books with null titles (shouldn't exist but data migration
left some).

Added null check with filter to exclude invalid records.
Also added database constraint to prevent future null titles.

Fixes #87"
```

**Why it's good:**
- Clear type and scope
- Describes the problem
- Explains the solution
- References the issue

### 1.4 Enforcing Commit Message Standards

**Create `.gitmessage` template:**

```bash
# ~/.gitmessage
# <type>(<scope>): <subject>
#
# <body>
#
# <footer>
#
# Type: feat, fix, docs, style, refactor, perf, test, build, ci, chore
# Scope: cli, service, repository, entity, config
# Subject: imperative mood, no period, max 50 chars
# Body: explain what and why (not how), wrap at 72 chars
# Footer: BREAKING CHANGE, Closes #123

# Example:
# feat(cli): add book checkout command
#
# Implement checkout functionality allowing users to mark books
# as checked out with due dates. Includes validation and error
# handling for already-checked-out books.
#
# Closes #42
```

**Configure Git to use template:**

```bash
git config --global commit.template ~/.gitmessage
```

**Now `git commit` opens editor with template!**

---

## 2. Git Hooks: Automation at Commit Time

### 2.1 Understanding Git Hooks

**What are hooks?**
Scripts that Git executes before/after events:
- `pre-commit`: Before commit is created
- `commit-msg`: After commit message written
- `pre-push`: Before push to remote
- `post-merge`: After merge completes

**Location:** `.git/hooks/`

**Language:** Any executable (bash, Python, Node.js)

### 2.2 Pre-Commit Hook: Quality Gates

**Create `.git/hooks/pre-commit`:**

```bash
#!/bin/bash
# Bibby pre-commit hook

set -e

echo "🔍 Running pre-commit checks..."

# 1. Run code formatter
echo "  Checking code formatting..."
mvn spotless:check || {
    echo "❌ Code formatting issues found. Run: mvn spotless:apply"
    exit 1
}

# 2. Run fast tests
echo "  Running unit tests..."
mvn test -Dtest="*Test" || {
    echo "❌ Tests failed. Fix before committing."
    exit 1
}

# 3. Check for common issues
echo "  Checking for common issues..."

# No System.out.println in production code
if git diff --cached --name-only | grep "src/main" | xargs grep -n "System.out.println" 2>/dev/null; then
    echo "❌ Found System.out.println in production code. Use logging instead."
    exit 1
fi

# No TODO without issue reference
if git diff --cached | grep -E "TODO(?!.*#[0-9]+)" 2>/dev/null; then
    echo "❌ Found TODO without issue reference. Use: TODO(#123): description"
    exit 1
fi

# No hardcoded passwords
if git diff --cached | grep -iE "(password|secret|api[_-]?key)\s*=\s*['\"][^'\"]{3,}" 2>/dev/null; then
    echo "❌ Possible hardcoded secret detected!"
    exit 1
fi

echo "✅ All pre-commit checks passed!"
```

**Make executable:**

```bash
chmod +x .git/hooks/pre-commit
```

**Test it:**

```bash
# Try committing code with System.out.println
echo "System.out.println(\"debug\");" >> src/main/java/com/penrose/bibby/BibbyApplication.java
git add .
git commit -m "test"

# Hook blocks commit with error message!
```

### 2.3 Commit-Msg Hook: Enforce Convention

**Create `.git/hooks/commit-msg`:**

```bash
#!/bin/bash
# Validate commit message format

COMMIT_MSG_FILE=$1
COMMIT_MSG=$(cat "$COMMIT_MSG_FILE")

# Regex for conventional commits
PATTERN="^(feat|fix|docs|style|refactor|perf|test|build|ci|chore)(\([a-z]+\))?: .{1,50}"

if ! echo "$COMMIT_MSG" | grep -qE "$PATTERN"; then
    echo "❌ Invalid commit message format!"
    echo ""
    echo "Format: <type>(<scope>): <subject>"
    echo ""
    echo "Types: feat, fix, docs, style, refactor, perf, test, build, ci, chore"
    echo "Example: feat(cli): add book search command"
    echo ""
    echo "Your message:"
    echo "$COMMIT_MSG"
    exit 1
fi

# Check subject length
SUBJECT=$(echo "$COMMIT_MSG" | head -n1)
if [ ${#SUBJECT} -gt 72 ]; then
    echo "⚠️  Warning: Subject line exceeds 72 characters (${#SUBJECT})"
fi

echo "✅ Commit message format valid"
```

**Make executable:**

```bash
chmod +x .git/hooks/commit-msg
```

**Test it:**

```bash
git commit -m "updated stuff"
# ❌ Invalid commit message format!

git commit -m "feat(cli): add search"
# ✅ Commit message format valid
```

### 2.4 Pre-Push Hook: Final Validation

**Create `.git/hooks/pre-push`:**

```bash
#!/bin/bash
# Final checks before pushing to remote

set -e

echo "🚀 Running pre-push checks..."

# 1. Ensure tests pass
echo "  Running full test suite..."
mvn verify || {
    echo "❌ Tests failed. Cannot push."
    exit 1
}

# 2. Check code coverage
echo "  Checking code coverage..."
COVERAGE=$(mvn jacoco:report | grep -oP "Total.*?(\d+)%" | grep -oP "\d+" | tail -1)
if [ "$COVERAGE" -lt 70 ]; then
    echo "❌ Code coverage is ${COVERAGE}% (minimum: 70%)"
    exit 1
fi

# 3. Check for unresolved merge conflicts
if git diff --name-only | xargs grep -l "^<<<<<<< HEAD" 2>/dev/null; then
    echo "❌ Unresolved merge conflicts detected!"
    exit 1
fi

# 4. Prevent force push to main
BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$BRANCH" = "main" ]; then
    read -p "⚠️  Pushing to main. Are you sure? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Push cancelled."
        exit 1
    fi
fi

echo "✅ All pre-push checks passed!"
```

### 2.5 Sharing Hooks with Team

**Problem:** `.git/hooks/` is not tracked by Git.

**Solution 1: Hooks directory in repo**

```bash
# Create hooks directory
mkdir .githooks

# Move hooks there
mv .git/hooks/pre-commit .githooks/
mv .git/hooks/commit-msg .githooks/
mv .git/hooks/pre-push .githooks/

# Track in Git
git add .githooks/
git commit -m "chore(git): add shared Git hooks"

# Configure Git to use this directory
git config core.hooksPath .githooks

# Team members run same command to enable
```

**Solution 2: Husky (Node.js projects)**

```bash
npm install husky --save-dev
npx husky init
```

**Solution 3: Pre-commit framework (Python)**

```yaml
# .pre-commit-config.yaml
repos:
  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v4.5.0
    hooks:
      - id: trailing-whitespace
      - id: end-of-file-fixer
      - id: check-yaml
      - id: check-added-large-files
```

```bash
pip install pre-commit
pre-commit install
```

---

## 3. Advanced Git Configuration

### 3.1 Essential Global Config

**Set up user identity:**

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

**Better diffs:**

```bash
# Show word diffs (not just line diffs)
git config --global diff.wordRegex "[^[:space:]]"

# Use better diff algorithm
git config --global diff.algorithm histogram

# Show submodule changes in diff
git config --global diff.submodule log
```

**Better merges:**

```bash
# Show common ancestor in conflicts (diff3)
git config --global merge.conflictstyle diff3

# Automatically reuse recorded resolutions
git config --global rerere.enabled true

# Prevent fast-forward merges by default
git config --global merge.ff false
```

**Better logs:**

```bash
# Default log format
git config --global format.pretty "format:%C(yellow)%h%C(reset) %C(blue)%ad%C(reset) %C(green)%an%C(reset) %s %C(red)%d%C(reset)"

# Show commit dates in relative format
git config --global log.date relative
```

**Better push behavior:**

```bash
# Push only current branch
git config --global push.default current

# Automatically push annotated tags
git config --global push.followTags true

# Prevent accidental force push
git config --global push.requireForce false
```

**Better pull behavior:**

```bash
# Always rebase when pulling
git config --global pull.rebase true

# Automatically stash and unstash
git config --global rebase.autoStash true
```

**Performance:**

```bash
# Enable parallel index preload
git config --global core.preloadIndex true

# Enable file system cache
git config --global core.fscache true

# Optimize for large repositories
git config --global feature.manyFiles true
```

### 3.2 Aliases: Productivity Boosters

**Create powerful aliases:**

```bash
# ~/.gitconfig or use git config --global

[alias]
    # Status
    st = status -sb

    # Logs
    lg = log --graph --pretty=format:'%C(yellow)%h%C(reset) -%C(red)%d%C(reset) %s %C(green)(%cr) %C(bold blue)<%an>%C(reset)' --abbrev-commit
    lga = log --graph --all --pretty=format:'%C(yellow)%h%C(reset) -%C(red)%d%C(reset) %s %C(green)(%cr) %C(bold blue)<%an>%C(reset)' --abbrev-commit
    last = log -1 HEAD --stat

    # Diffs
    df = diff --word-diff
    dc = diff --cached

    # Branches
    br = branch -v
    bra = branch -a -v
    brd = branch -d
    brD = branch -D

    # Commits
    ci = commit
    ca = commit --amend
    can = commit --amend --no-edit

    # Checkout
    co = checkout
    cob = checkout -b
    com = checkout main

    # Rebase
    rb = rebase
    rbi = rebase -i
    rbc = rebase --continue
    rba = rebase --abort

    # Stash
    sl = stash list
    sp = stash pop
    sa = stash apply
    ss = stash save

    # Undo
    undo = reset HEAD~1 --mixed
    unstage = reset HEAD --
    uncommit = reset --soft HEAD~1

    # Cleanup
    cleanup = !git branch --merged | grep -v '\\*\\|main\\|master' | xargs -n 1 git branch -d

    # Find
    find = !git ls-files | grep -i
    grep-history = !git rev-list --all | xargs git grep

    # Stats
    contributors = shortlog --summary --numbered --email
    count = !git log --oneline | wc -l

    # Sync
    sync = !git fetch --all --prune && git pull --rebase
    update = !git fetch origin main:main

    # Aliases for specific workflows
    wip = commit -am "WIP: work in progress"
    save = !git add -A && git commit -m 'SAVEPOINT'
    redo = !git reset HEAD~1 --mixed && git add -A && git commit -C ORIG_HEAD
```

**Usage:**

```bash
# Instead of: git status
git st

# Instead of: git log --graph --all --decorate
git lga

# Instead of: git commit --amend --no-edit
git can

# Instead of: git checkout -b feature/new-branch
git cob feature/new-branch

# Find files
git find "BookService"

# Cleanup merged branches
git cleanup
```

### 3.3 Bibby-Specific Configuration

**Create `.git/config` in Bibby repo:**

```ini
[core]
    # Use VS Code as editor
    editor = code --wait

    # Better line ending handling
    autocrlf = input

    # Hooks directory
    hooksPath = .githooks

[branch "main"]
    # Auto-setup remote tracking
    remote = origin
    merge = refs/heads/main

[remote "origin"]
    # Fetch with pruning by default
    fetch = +refs/heads/*:refs/remotes/origin/*
    prune = true

[diff "java"]
    # Better Java diff
    xfuncname = "^[ \\t]*(((public|private|protected|static|final|native|synchronized|abstract|transient)[ \\t]+)*([A-Za-z_][A-Za-z_0-9]*(<[^>]+>)?[ \\t]+)+[A-Za-z_][A-Za-z_0-9]*[ \\t]*\\([^)]*\\)[ \\t]*(throws[ \\t]+[^{]+)?[ \\t]*\\{)"

[diff "xml"]
    # Better XML diff
    xfuncname = "^[ \\t]*<[a-z].*"
```

---

## 4. Complete Workflows

### 4.1 Solo Developer Workflow (Bibby Current)

**Daily workflow:**

```bash
# Morning: Start work
git checkout main
git pull origin main

# Create feature branch
git checkout -b feature/bibby-023-book-ratings

# Work in small commits
git add src/main/java/com/penrose/bibby/library/book/Rating.java
git commit -m "feat(entity): add Rating entity"

git add src/main/java/com/penrose/bibby/library/book/BookService.java
git commit -m "feat(service): add rating functionality to BookService"

git add src/main/java/com/penrose/bibby/cli/BookCommands.java
git commit -m "feat(cli): add rate-book command"

git add src/test/
git commit -m "test(rating): add comprehensive rating tests"

# Clean up history before pushing
git rebase -i HEAD~4
# Squash into logical commits

# Push feature branch
git push -u origin feature/bibby-023-book-ratings

# Create PR
gh pr create --title "Add book rating system" \
  --body "Implements user ratings (1-5 stars) for books.

Features:
- Rating entity with validation
- CLI commands: rate-book, show-ratings
- Average rating calculation
- Comprehensive tests

Closes #23"

# After PR approved
gh pr merge --squash --delete-branch

# Update local main
git checkout main
git pull origin main

# Delete local branch
git branch -d feature/bibby-023-book-ratings
```

### 4.2 Small Team Workflow (2-5 developers)

**Team conventions:**

```markdown
## Bibby Team Git Workflow

### Branch Strategy
- `main`: Production-ready code
- `feature/*`: Feature branches (short-lived, < 3 days)
- `fix/*`: Bug fixes
- `hotfix/*`: Emergency production fixes

### Rules
1. Never commit directly to main
2. All changes via Pull Request
3. PRs require 1 approval
4. CI must pass before merge
5. Squash merge for features
6. Regular merge for hotfixes

### Daily Workflow
1. Pull latest main before starting work
2. Create feature branch
3. Push daily (even if incomplete)
4. Open PR when ready for review
5. Address feedback promptly
6. Merge after approval + CI pass
```

**Example:**

```bash
# Developer A: Working on book ratings
git checkout main
git pull origin main
git checkout -b feature/book-ratings

# ... work and commit ...
git push -u origin feature/book-ratings
gh pr create --title "Add book ratings"

# Developer B: Working on export feature (simultaneously)
git checkout main
git pull origin main
git checkout -b feature/csv-export

# ... work and commit ...
git push -u origin feature/csv-export
gh pr create --title "Add CSV export"

# Both PRs merged independently
# Developer A merges first
# Developer B rebases on updated main before merge
git checkout feature/csv-export
git fetch origin
git rebase origin/main
git push --force-with-lease

# Now both features in main
```

### 4.3 Enterprise Workflow (Large Team)

**GitFlow with release branches:**

```bash
# Main branches
# - main: Production
# - develop: Integration branch
# - release/*: Release candidates
# - hotfix/*: Emergency fixes

# Feature development
git checkout develop
git pull origin develop
git checkout -b feature/JIRA-123-book-recommendations

# ... implement feature ...
# Merge to develop via PR
gh pr create --base develop --title "[JIRA-123] Book recommendations"

# Release preparation
git checkout develop
git pull origin develop
git checkout -b release/1.2.0

# Bump version
mvn versions:set -DnewVersion=1.2.0
git commit -am "chore(release): bump version to 1.2.0"

# Final testing on release branch
# ... QA finds bugs ...
git commit -m "fix(release): address QA feedback"

# Merge to main
git checkout main
git merge --no-ff release/1.2.0
git tag -a v1.2.0 -m "Release v1.2.0"

# Merge back to develop
git checkout develop
git merge --no-ff release/1.2.0

# Clean up
git branch -d release/1.2.0
```

---

## 5. Git Anti-Patterns to Avoid

### 5.1 Commit Anti-Patterns

**❌ Anti-Pattern 1: Giant commits**

```bash
# Bad: 50 files changed in one commit
git add .
git commit -m "Updated everything"
```

**✅ Fix: Atomic commits**

```bash
# Good: Logical grouping
git add src/main/java/com/penrose/bibby/entity/Rating.java
git commit -m "feat(entity): add Rating entity"

git add src/main/java/com/penrose/bibby/service/RatingService.java
git commit -m "feat(service): implement rating service"
```

**❌ Anti-Pattern 2: Meaningless messages**

```bash
git commit -m "stuff"
git commit -m "fix"
git commit -m "update"
```

**✅ Fix: Descriptive messages**

```bash
git commit -m "fix(search): handle null book titles in query"
```

**❌ Anti-Pattern 3: Mixing concerns**

```bash
# Bad: Feature + refactoring + dependency update
git add .
git commit -m "Add ratings and refactor service and update Spring"
```

**✅ Fix: Separate commits**

```bash
git commit -m "feat(rating): add book rating feature"
git commit -m "refactor(service): extract validation logic"
git commit -m "build(deps): upgrade Spring Boot to 3.5.8"
```

### 5.2 Branch Anti-Patterns

**❌ Anti-Pattern 1: Long-lived feature branches**

```bash
# Bad: Branch lives for 4 weeks
git checkout -b feature/massive-refactor
# ... 4 weeks later ...
# Hundreds of merge conflicts
```

**✅ Fix: Short branches + feature flags**

```bash
# Good: Merge incrementally with feature flag
git checkout -b feature/refactor-part-1
# ... 2 days work ...
git merge

git checkout -b feature/refactor-part-2
# ... 2 days work ...
git merge
```

**❌ Anti-Pattern 2: Not updating from main**

```bash
# Bad: Never rebase, massive conflicts at merge time
git checkout -b feature/new-feature
# ... 2 weeks pass, never update from main ...
git merge main  # 50 conflicts!
```

**✅ Fix: Daily rebasing**

```bash
# Good: Rebase daily
git fetch origin
git rebase origin/main
# Small conflicts, easy to resolve
```

### 5.3 Merge/Rebase Anti-Patterns

**❌ Anti-Pattern 1: Rewriting public history**

```bash
# Bad: Rebase commits already pushed and shared
git push origin feature/shared-branch
# Teammate pulls and adds commits
git rebase main  # Rewrites history
git push --force  # Destroys teammate's work!
```

**✅ Fix: Only rebase private branches**

```bash
# Good: Rebase before pushing, merge after
git rebase main  # Local only
git push origin feature/my-branch

# After pushing, use merge not rebase
git merge origin/main
```

**❌ Anti-Pattern 2: Resolving conflicts incorrectly**

```bash
# Bad: Accept all "ours" or "theirs" without thinking
git merge feature
# Conflict!
git checkout --ours .
git add .
git commit  # Lost important changes from feature!
```

**✅ Fix: Manual resolution**

```bash
# Good: Understand each conflict
git merge feature
# Resolve each file carefully
vim conflicted-file.java
# Keep necessary changes from both sides
git add conflicted-file.java
mvn test  # Verify it works!
git commit
```

### 5.4 Remote Anti-Patterns

**❌ Anti-Pattern 1: Force pushing to shared branches**

```bash
# Bad: Force push to main
git push --force origin main
# Everyone's work corrupted!
```

**✅ Fix: Never force push to main, use --force-with-lease for feature branches**

```bash
# Good: Only force push to your branches with safety
git push --force-with-lease origin feature/my-branch
```

**❌ Anti-Pattern 2: Large files in Git**

```bash
# Bad: Commit 500MB database dump
git add database-dump.sql
git commit -m "Add database backup"
# Repo now huge forever!
```

**✅ Fix: Use Git LFS or don't track large files**

```bash
# Good: Use .gitignore
echo "*.sql" >> .gitignore
echo "*.zip" >> .gitignore
echo "*.jar" >> .gitignore  # Unless it's a release artifact
```

---

## 6. Building Your Git Toolkit

### 6.1 Essential Tools

**1. Git GUI Clients**

```bash
# Command-line tools
tig           # Text-mode interface (great for logs)
lazygit       # Terminal UI (keyboard-driven)
gitui         # Fast terminal UI (Rust-based)

# Desktop apps
GitKraken     # Visual graph, merge tool
SourceTree    # Free, feature-rich (Atlassian)
Fork          # Fast, clean interface
Tower         # Professional ($)

# IDE integrations
VS Code Git   # Built-in, excellent
IntelliJ Git  # Powerful refactoring support
```

**2. Diff/Merge Tools**

```bash
# Configure VS Code
git config --global diff.tool vscode
git config --global difftool.vscode.cmd 'code --wait --diff $LOCAL $REMOTE'

git config --global merge.tool vscode
git config --global mergetool.vscode.cmd 'code --wait --merge $REMOTE $LOCAL $BASE $MERGED'

# Use
git difftool
git mergetool
```

**3. Helper Scripts**

Create `~/bin/git-helpers`:

```bash
#!/bin/bash
# Useful Git helper functions

# Quick commit with conventional message
function gitc() {
    TYPE=$1
    SCOPE=$2
    MSG=$3
    git commit -m "${TYPE}(${SCOPE}): ${MSG}"
}

# Usage: gitc feat cli "add search command"

# Create and checkout branch
function gitb() {
    BRANCH=$1
    git checkout -b "$BRANCH"
    git push -u origin "$BRANCH"
}

# Clean merged branches
function git-clean() {
    git branch --merged main | grep -v "main\|master" | xargs git branch -d
}

# Sync with main
function git-sync() {
    CURRENT=$(git branch --show-current)
    git checkout main
    git pull origin main
    git checkout "$CURRENT"
    git rebase main
}

# Show files changed in last commit
function git-changed() {
    git diff-tree --no-commit-id --name-only -r HEAD
}
```

### 6.2 Git Aliases Reference

**Complete `.gitconfig` aliases section:**

```ini
[alias]
    # === Status & Info ===
    s = status -sb
    st = status
    last = log -1 HEAD --stat
    ls = ls-files

    # === Logs ===
    lg = log --graph --pretty=format:'%Cred%h%Creset -%C(yellow)%d%Creset %s %Cgreen(%cr) %C(bold blue)<%an>%Creset' --abbrev-commit
    lga = log --graph --all --pretty=format:'%Cred%h%Creset -%C(yellow)%d%Creset %s %Cgreen(%cr) %C(bold blue)<%an>%Creset' --abbrev-commit
    ll = log --pretty=format:"%C(yellow)%h%Cred%d\\ %Creset%s%Cblue\\ [%cn]" --decorate --numstat
    lds = log --pretty=format:"%C(yellow)%h\\ %C(green)%ad%Creset\\ %s" --decorate --date=short

    # === Diffs ===
    d = diff
    ds = diff --staged
    dc = diff --cached
    dw = diff --word-diff
    dl = diff HEAD~1 HEAD

    # === Branches ===
    b = branch
    ba = branch -a
    bd = branch -d
    bD = branch -D
    bm = branch -m

    # === Checkout ===
    co = checkout
    cob = checkout -b
    com = checkout main
    cod = checkout develop

    # === Commits ===
    c = commit
    cm = commit -m
    ca = commit --amend
    can = commit --amend --no-edit
    cane = commit --amend --no-edit --allow-empty

    # === Add ===
    a = add
    aa = add --all
    ap = add --patch

    # === Rebase ===
    r = rebase
    ri = rebase -i
    rc = rebase --continue
    rs = rebase --skip
    ra = rebase --abort

    # === Merge ===
    m = merge
    ma = merge --abort
    mc = merge --continue

    # === Stash ===
    sl = stash list
    ss = stash save
    sp = stash pop
    sa = stash apply
    sd = stash drop

    # === Remote ===
    f = fetch
    fa = fetch --all
    fo = fetch origin
    p = push
    pf = push --force-with-lease
    pu = push -u origin HEAD
    pl = pull
    plr = pull --rebase

    # === Undo ===
    undo = reset HEAD~1 --mixed
    unstage = reset HEAD --
    uncommit = reset --soft HEAD~1

    # === Cleanup ===
    cleanup = !git branch --merged | grep -v '\\*\\|main\\|develop' | xargs -n 1 git branch -d
    prune = fetch --prune

    # === Search ===
    find = !git ls-files | grep -i
    grep-all = !git rev-list --all | xargs git grep

    # === Stats ===
    who = shortlog -s --
    contributors = shortlog --summary --numbered --email
    activity = !git log --all --oneline --no-merges --author=\"$(git config user.email)\"

    # === Workflow ===
    wip = !git add -A && git commit -m 'WIP'
    sync = !git fetch --all && git pull --rebase
    save = !git add -A && git commit -m 'SAVEPOINT'
    redo = !git reset HEAD~1 --mixed && git add -A && git commit -C ORIG_HEAD

    # === Tags ===
    tags = tag -l
    lasttag = describe --tags --abbrev=0

    # === Bisect helpers ===
    bs = bisect start
    bg = bisect good
    bb = bisect bad
    br = bisect reset
```

### 6.3 Shell Integration

**Add to `.bashrc` or `.zshrc`:**

```bash
# Show Git branch in prompt
parse_git_branch() {
    git branch 2> /dev/null | sed -e '/^[^*]/d' -e 's/* \(.*\)/(\1)/'
}

PS1="\u@\h \w \[\e[91m\]\$(parse_git_branch)\[\e[00m\]$ "

# Git completion
source /usr/share/bash-completion/completions/git

# Aliases
alias gs='git status'
alias ga='git add'
alias gc='git commit'
alias gp='git push'
alias gl='git pull'
alias gd='git diff'
alias gco='git checkout'
alias gb='git branch'
alias glog='git log --oneline --graph --all'
```

---

## 7. Interview-Ready Knowledge

### Question: "What are your Git best practices?"

**Bad Answer:** "I commit often and write good messages."

**Good Answer:**
"I follow several key practices that keep our Git history clean and useful. First, I write conventional commits with type prefixes like 'feat:', 'fix:', 'docs:' so our changelog can be auto-generated and history is searchable. Each commit should be atomic—one logical change per commit.

Second, I use Git hooks for quality gates. My pre-commit hook runs formatters and fast tests, preventing broken code from entering history. The commit-msg hook enforces our message convention.

Third, I rebase feature branches daily to stay current with main, but never rebase after pushing to shared branches—that's the golden rule that prevents teammate frustration.

For configuration, I use diff3 conflict style to see the common ancestor during conflicts, and rerere to reuse conflict resolutions. I've also set up meaningful aliases that match my workflow.

On my Bibby project, I even created a shared hooks directory in the repo so the team can use the same quality gates. The result is a clean history where every commit passes tests and follows conventions, making git bisect and git blame actually useful for debugging."

**Why It's Good:**
- Specific practices with rationale
- Technical details (diff3, rerere, hooks)
- Team considerations
- Real project example
- Explains benefits

### Question: "How do you handle a messy Git history?"

**Bad Answer:** "Delete the branch and start over."

**Good Answer:**
"It depends on whether the commits are local or already pushed. For local branches, interactive rebase is my tool of choice. I run 'git rebase -i HEAD~N' where N is the number of messy commits, then squash related commits together, reorder for logical flow, and reword messages to follow conventions.

For example, if I have 10 commits that are actually implementing one feature with 'WIP' and 'fix typo' messages scattered throughout, I'll squash them into 2-3 logical commits with proper conventional commit messages.

If the messy history is already pushed to a shared branch, I'm more cautious. I'll only rewrite history if I coordinate with the team first. Otherwise, I might create cleanup commits with good messages that explain what each previous commit should have been, or do a squash merge when bringing it into main so the mainline stays clean.

For really bad situations where history is unsalvageable, I've used 'git replace' to create a graft point, essentially saying 'pretend this commit's parent is X instead of Y,' which lets you clean up history going forward while preserving the old history for reference.

The key is understanding the tradeoff: local history can be rewritten freely, shared history requires team coordination, and public history should rarely if ever be rewritten."

**Why It's Good:**
- Distinguishes local vs shared vs public
- Specific techniques (interactive rebase, squash merge)
- Shows advanced knowledge (git replace, graft)
- Emphasizes communication
- Explains tradeoffs

### Question: "Describe your typical Git workflow for a feature"

**Bad Answer:** "I create a branch, make changes, and create a PR."

**Good Answer:**
"My workflow starts with ensuring I'm on the latest main: 'git checkout main && git pull origin main'. Then I create a feature branch with a descriptive name following our convention: 'git checkout -b feature/bibby-042-isbn-lookup'.

I work in small, atomic commits. Each commit is a complete thought—passing tests, compiling code. I commit related changes together and use conventional commit messages. If I realize I need to fix something from a previous commit, I use 'git commit --fixup=<hash>' so I can autosquash later.

Throughout development, I rebase daily on main: 'git fetch origin && git rebase origin/main' to stay current and avoid merge conflicts piling up. Before pushing, I run 'git rebase -i --autosquash' to clean up my history—squashing fixup commits, reordering for clarity, ensuring each commit message is meaningful.

Then I push with 'git push -u origin feature/bibby-042-isbn-lookup' and create a PR with 'gh pr create' including a detailed description with the problem, solution, testing notes, and issue reference.

During code review, I address feedback with new commits, not amending, so reviewers can see what changed. After approval and CI passing, I squash merge to main which combines my cleaned-up commits into one with the PR description as the message. This keeps main's history linear and focused.

Finally, I delete the feature branch: 'git branch -d feature/bibby-042-isbn-lookup' and update my local main. This whole process typically takes 2-3 days per feature, keeping branches short-lived."

**Why It's Good:**
- Step-by-step detail
- Specific commands
- Explains reasoning (atomic commits, daily rebase, squash merge)
- Mentions tools (gh CLI)
- Realistic timeline

---

## 8. Summary & Master Checklist

### Git Best Practices Checklist

**✅ Commits:**
- [ ] Atomic commits (one logical change)
- [ ] Conventional commit messages
- [ ] Subject line ≤ 50 characters
- [ ] Body wrapped at 72 characters
- [ ] Imperative mood ("add" not "added")
- [ ] Reference issues (Closes #42)

**✅ Branches:**
- [ ] Descriptive branch names
- [ ] Feature branches short-lived (< 3 days)
- [ ] Never commit directly to main
- [ ] Rebase daily on main (before pushing)
- [ ] Delete after merging

**✅ Merging:**
- [ ] Never rebase public history
- [ ] Use --force-with-lease (not --force)
- [ ] Test after resolving conflicts
- [ ] Document resolution in message
- [ ] Prefer squash merge for features

**✅ Configuration:**
- [ ] User name and email set
- [ ] diff3 conflict style enabled
- [ ] rerere enabled
- [ ] Useful aliases configured
- [ ] Git hooks set up

**✅ Hooks:**
- [ ] pre-commit: Run tests and linters
- [ ] commit-msg: Validate format
- [ ] pre-push: Final checks
- [ ] Shared via .githooks directory

**✅ Workflow:**
- [ ] Pull before starting work
- [ ] Push at least daily
- [ ] Clean history before pushing
- [ ] Comprehensive PR descriptions
- [ ] Self-review before requesting review

### Next Steps for Bibby

**Immediate (< 1 hour):**
1. Set up global Git config with aliases
2. Create commit message template
3. Configure diff3 and rerere

**Short-term (1-2 hours):**
1. Create Git hooks (pre-commit, commit-msg)
2. Set up .githooks directory
3. Document workflow in CONTRIBUTING.md

**Ongoing:**
1. Practice conventional commits
2. Use atomic commits
3. Clean history before PRs
4. Leverage aliases for speed

### Coming Up in Section 13

**AWS Core Concepts & Services:**
- Understanding cloud computing fundamentals
- AWS account setup and security (IAM, MFA)
- Core services: EC2, S3, RDS, VPC
- Deploying Bibby to AWS
- Cost optimization strategies
- AWS Free Tier utilization

You'll learn how to take Bibby from local development to cloud deployment on AWS, understanding the core services that power modern applications.

---

**Section 12 Complete:** You now have professional Git practices that match industry standards. Your Git workflow is clean, automated, and interview-worthy! 🎯✨
