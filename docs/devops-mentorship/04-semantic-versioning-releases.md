# SECTION 4: SEMANTIC VERSIONING & RELEASE MANAGEMENT
## Professional Version Control and Release Process for Your Bibby Project

---

## 🎯 Learning Objectives

By the end of this section, you will:
- Master semantic versioning (SemVer) rules
- Understand when to bump MAJOR, MINOR, or PATCH versions
- Create proper Git tags and GitHub releases
- Write professional changelogs
- Automate version management
- Establish a consistent release process
- Speak confidently about versioning in interviews

---

## 🔍 YOUR CURRENT VERSION: THE MISMATCH PROBLEM

Let's start with a critical analysis of your versioning situation.

### 📁 File: `pom.xml` (Line 13)

```xml
<version>0.0.1-SNAPSHOT</version>
```

### 📁 File: `README.md` (Line 309)

```markdown
## 🧭 **Version**

**v0.2 — Bookcase → Shelf → Book navigation implemented**
```

### 🔍 Git Tags Check

```bash
git tag -l
# Output: (empty - no tags!)
```

### 🚨 THE PROBLEM: Version Inconsistency

**You have THREE different version stories:**

1. **pom.xml says:** `0.0.1-SNAPSHOT` (Maven's truth)
2. **README says:** `v0.2` (Documentation's truth)
3. **Git says:** (nothing - no tags)

**Which is correct?** None of them are synchronized!

**Impact:**
- Confusing for users and employers
- Can't reproduce specific versions
- No release history
- Unprofessional appearance
- Build artifacts are ambiguous

---

## 📚 SEMANTIC VERSIONING 101

### What is Semantic Versioning?

**Format:** `MAJOR.MINOR.PATCH` (e.g., `2.4.7`)

**Official SemVer 2.0.0 Rules:**

```
MAJOR version when you make incompatible API changes
MINOR version when you add functionality in a backward compatible manner
PATCH version when you make backward compatible bug fixes
```

**Additional labels:**
- `-alpha`, `-beta`, `-rc1` (pre-release)
- `+build.123` (build metadata)

### Breaking Down Version Numbers

**Example: `2.4.7`**

- **MAJOR (2)**: Breaking changes since 1.x.x
- **MINOR (4)**: 4 feature releases since 2.0.0
- **PATCH (7)**: 7 bug fixes since 2.4.0

### Your Bibby Version History (Reconstructed)

Let me analyze your Git history to determine what your versions SHOULD have been:

**📊 Analyzing Your Commits:**

```bash
# Initial commits
Initial commit

# Early features
feat: add BookSummary DTO for lightweight book browse view
feat: add repository + service support for fetching BookSummary by shelf
feat: implement shelf→books browse flow with summary projection

# More features
feat(browse): add bookshelf selection flow + ShelfSummary projection
feat: implement interactive bookcase browser with aligned UI
feat: add bookcase listing with aligned columns, color formatting

# Bug fixes
fix: correct shelf assignment by using shelfId instead of shelfPosition

# Guards and improvements
feat(checkout): add CLI-level guard for already checked-out books
feat: add empty-shelf handling to shelf→book selection flow
```

### What Your Versions SHOULD Have Been:

**v0.1.0** (First working version)
- Basic book add/list functionality
- Author support
- Database persistence
- Reason: First usable feature set = 0.1.0

**v0.2.0** (Navigation feature)
- Bookcase → Shelf → Books navigation
- ShelfSummary projection
- BookSummary projection
- Browse commands
- Reason: NEW user-facing features = MINOR bump

**v0.2.1** (Bug fix)
- Fixed shelf assignment bug (shelfId vs shelfPosition)
- Reason: Bug fix, no new features = PATCH bump

**v0.3.0** (Checkout improvements - would be next)
- Checkout guards
- Empty shelf handling
- Reason: New features = MINOR bump

### The 0.x.x Convention

**Why start at 0.x.x?**

From SemVer spec:
> "Major version zero (0.y.z) is for initial development. Anything MAY change at any time. The public API SHOULD NOT be considered stable."

**When to go to 1.0.0?**

```
1.0.0 = "This is production-ready and the API is stable"
```

**For Bibby:**
- Stay on 0.x.x during development
- Go to 1.0.0 when you consider it "production ready"
- No specific timeline - could be weeks or months

**Current recommendation:** You should be at **v0.2.1** right now.

---

## 🎯 SEMANTIC VERSIONING DECISION TREE

### When to Bump MAJOR (x.0.0)

**Rule:** Breaking changes that require users to modify their usage

**Examples for Bibby:**

```java
// BREAKING CHANGE: Changed method signature
// Before (v0.5.0):
public void addBook(String title, String author)

// After (v1.0.0): MAJOR BUMP REQUIRED
public BookEntity addBook(BookRequest request)
// Users must change how they call this method
```

**Other MAJOR bumps:**
- Removed public methods
- Changed return types
- Renamed classes users depend on
- Changed database schema in incompatible way
- Changed configuration file format

**For CLI apps like Bibby:**
- Removed or renamed commands
- Changed command syntax
- Changed output format that scripts depend on

### When to Bump MINOR (x.y.0)

**Rule:** New features, backward compatible

**Examples from YOUR actual commits:**

```bash
feat: implement interactive bookcase browser
feat: add bookcase listing with aligned columns
feat(browse): add bookshelf selection flow
feat: add empty-shelf handling to shelf→book selection flow
```

**All of these are MINOR bumps** because:
- Added NEW functionality
- Didn't break existing features
- Users can upgrade without changing their usage

### When to Bump PATCH (x.y.z)

**Rule:** Bug fixes, no new features

**Example from YOUR commits:**

```bash
fix: correct shelf assignment by using shelfId instead of shelfPosition
```

**This is a PATCH bump** because:
- Fixed a bug
- No new features
- Existing functionality works better

**Other PATCH examples:**
- Performance improvements
- Documentation fixes
- Dependency updates (security)
- Internal refactoring (no user-visible changes)

### The Gray Areas

**Question:** Is it a feature or a bug fix?

**Example 1:**
```bash
feat(checkout): add CLI-level guard for already checked-out books
```

**Analysis:**
- Prevents invalid state (sounds like a bug fix)
- But it's NEW validation logic (feature)
- **Verdict:** MINOR bump (err on side of being conservative)

**Example 2:**
```bash
Improved error messages for invalid ISBN
```

**Analysis:**
- Makes existing feature better
- No new functionality
- **Verdict:** Could be either PATCH or MINOR
- **Recommendation:** PATCH (it's an improvement, not a feature)

**Rule of thumb:**
- If users might notice and say "Oh cool, that's new!" = MINOR
- If users say "Oh, that was broken before" = PATCH
- If users say "Oh no, my workflow broke" = MAJOR

---

## 🏷️ GIT TAGGING: VERSION CONTROL FOR RELEASES

### Why Tags Matter

**Git Tags = Bookmarks in your commit history**

Without tags:
```
How do I get version 0.2.0?
¯\_(ツ)_/¯
```

With tags:
```bash
git checkout v0.2.0  # Exact version restored!
```

### Types of Git Tags

**1. Lightweight Tags (Don't use these)**
```bash
git tag v0.2.0
# Just a pointer to a commit, no metadata
```

**2. Annotated Tags (Use these!)**
```bash
git tag -a v0.2.0 -m "Release version 0.2.0: Bookcase navigation"
# Includes: tagger name, date, message, and is a Git object
```

### Your Git Tag Strategy

**Based on YOUR project:**

```bash
# Tag your current state as v0.2.1
git tag -a v0.2.1 -m "Release version 0.2.1

Features:
- Bookcase → Shelf → Book navigation
- ShelfSummary and BookSummary projections
- Interactive browse commands
- Empty shelf handling

Bug Fixes:
- Fixed shelf assignment using shelfId

This represents the current stable state of the CLI."

# Push tag to GitHub
git push origin v0.2.1

# List all tags
git tag -l

# Show tag details
git show v0.2.1
```

### Tag Naming Conventions

**Good:**
```
v0.2.1
v1.0.0
v2.3.4-beta
v1.5.0-rc1
```

**Bad:**
```
0.2.1          (missing 'v' prefix - inconsistent with GitHub)
release-0.2.1  (verbose)
bibby-v0.2.1   (project name not needed)
version_0.2.1  (underscores instead of dots)
```

**Best Practice:**
- Always use `v` prefix
- Use dots for version parts
- Use hyphens for pre-release labels

### Tagging Historical Commits

**What if you want to tag old versions retroactively?**

```bash
# Find the commit hash for when you completed a feature
git log --oneline --all --grep="implement interactive bookcase browser"
# Output: 8b4098f feat: implement interactive bookcase browser...

# Tag that specific commit
git tag -a v0.2.0 8b4098f -m "Release version 0.2.0: Bookcase browser"

# Push all tags
git push origin --tags
```

**Your Action Item:**

I recommend tagging these key points in your history:

```bash
# Tag the bookcase browser completion as v0.2.0
git tag -a v0.2.0 8b4098f -m "Version 0.2.0: Bookcase → Shelf → Book navigation"

# Tag the shelf assignment fix as v0.2.1
git tag -a v0.2.1 bc3b0c1 -m "Version 0.2.1: Fix shelf assignment bug"

# Tag current state with DevOps sections as v0.3.0 (docs are a minor release)
git tag -a v0.3.0 -m "Version 0.3.0: DevOps mentorship content added"

# Push all tags
git push origin --tags
```

---

## 📝 CHANGELOG: DOCUMENTING YOUR RELEASES

### What is a CHANGELOG?

**CHANGELOG.md** is a file that lists notable changes between versions.

**Purpose:**
- Users can see what's new
- Developers understand version history
- Required for professional projects
- Helpful for future you

### Your Bibby CHANGELOG (What It Should Look Like)

**File:** `CHANGELOG.md`

```markdown
# Changelog

All notable changes to Bibby will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- DevOps mentorship guide with 28 sections

### Changed
- Updated README with better architecture documentation

### Fixed
- None

## [0.2.1] - 2025-11-13

### Fixed
- Corrected shelf assignment to use shelfId instead of shelfPosition
- Prevented null pointer exceptions in shelf lookup

## [0.2.0] - 2025-11-15

### Added
- **Browse Flow**: Complete Bookcase → Shelf → Book navigation
- BookSummary projection for lightweight book display
- ShelfSummary projection for faster shelf queries
- Interactive shelf selection with ComponentFlow
- Empty shelf handling with user-friendly messages
- Book selection from shelf with title display

### Changed
- Improved repository queries for better performance
- Enhanced CLI output formatting with colors

### Deprecated
- None

### Removed
- None

### Fixed
- None

### Security
- None

## [0.1.0] - 2025-10-31

### Added
- Initial Bibby CLI application
- Book add command with interactive flow
- Multi-author support
- Book search by title (case-insensitive)
- Book checkout and check-in commands
- Shelf assignment flow
- PostgreSQL database persistence
- Spring Shell interactive interface
- Custom Bibby prompt with personality

### Changed
- None

### Fixed
- None

## [0.0.1] - 2025-10-15

### Added
- Project initialization
- Basic Spring Boot setup
- Database connection

---

[Unreleased]: https://github.com/leodvincci/Bibby/compare/v0.2.1...HEAD
[0.2.1]: https://github.com/leodvincci/Bibby/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/leodvincci/Bibby/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/leodvincci/Bibby/compare/v0.0.1...v0.1.0
[0.0.1]: https://github.com/leodvincci/Bibby/releases/tag/v0.0.1
```

### CHANGELOG Sections Explained

**[Unreleased]**
- Work in progress
- Not yet released
- Moves to a version section on release

**[Version] - Date**
- Specific release
- Date in YYYY-MM-DD format

**Categories:**

| Category | Purpose | Example |
|----------|---------|---------|
| **Added** | New features | New command, new functionality |
| **Changed** | Changes to existing features | Modified behavior, UI changes |
| **Deprecated** | Soon-to-be removed features | "Will remove in v1.0.0" |
| **Removed** | Removed features | Deleted command, removed API |
| **Fixed** | Bug fixes | Fixed crash, corrected calculation |
| **Security** | Security updates | Patched vulnerability, updated deps |

### CHANGELOG Best Practices

**✅ DO:**
- Write for humans (not commit messages)
- Group by category
- Link to GitHub compare/release pages
- Date everything (YYYY-MM-DD)
- Keep it updated with each release

**❌ DON'T:**
- Copy Git commit log verbatim
- Include every tiny change
- Use vague descriptions
- Skip versions
- Write in first person ("I added...")

### Automating CHANGELOG Updates

**Tool: conventional-changelog**

```bash
# Install
npm install -g conventional-changelog-cli

# Generate CHANGELOG
conventional-changelog -p angular -i CHANGELOG.md -s

# Generate for specific version
conventional-changelog -p angular -i CHANGELOG.md -s -r 1
```

**This works if you use conventional commits:**

```bash
feat: add book details view
fix: resolve ISBN validation bug
docs: update README with new commands
chore: bump dependency versions
```

---

## 🚀 GITHUB RELEASES: MAKING IT OFFICIAL

### What are GitHub Releases?

**GitHub Releases = Git Tags + Release Notes + Artifacts**

**Benefits:**
- User-friendly release page
- Download links for JARs
- Automatic notifications
- Professional appearance
- SEO friendly

### Your First GitHub Release

**Step 1: Create Tag (if not done)**

```bash
git tag -a v0.2.1 -m "Release version 0.2.1"
git push origin v0.2.1
```

**Step 2: Create Release on GitHub**

```bash
# Option 1: Via Web UI
1. Go to https://github.com/leodvincci/Bibby/releases
2. Click "Draft a new release"
3. Choose tag: v0.2.1
4. Release title: "v0.2.1 - Bookcase Navigation & Bug Fixes"
5. Description: (see template below)
6. Attach artifacts: Bibby-0.2.1.jar
7. Click "Publish release"

# Option 2: Via GitHub CLI
gh release create v0.2.1 \
  --title "v0.2.1 - Bookcase Navigation & Bug Fixes" \
  --notes-file RELEASE_NOTES.md \
  ./target/Bibby-0.2.1.jar
```

### Release Notes Template

**File:** `RELEASE_NOTES.md`

```markdown
# Bibby v0.2.1 - Bookcase Navigation & Bug Fixes

## 🎉 What's New

This release brings the complete Bookcase → Shelf → Book navigation
experience to Bibby CLI, along with critical bug fixes.

## ✨ Features

### 📚 Browse Your Library
Navigate your physical library structure interactively:
- Select from all bookcases
- Choose a specific shelf
- View books on that shelf
- See book titles sorted alphabetically

**Usage:**
```bash
Bibby:_ browse bookcases
```

### 🔍 Smart Projections
New lightweight data projections for faster queries:
- `BookSummary`: Just title and ID (no heavy entity loading)
- `ShelfSummary`: Shelf info with book count

### 🛡️ Empty Shelf Handling
Graceful handling when shelves have no books:
- Clear message: "No books found on this shelf"
- No empty selectors
- Better user experience

## 🐛 Bug Fixes

- **Fixed:** Shelf assignment now correctly uses `shelfId` instead of `shelfPosition`
- **Fixed:** Null pointer exceptions in shelf lookup operations

## 📦 Installation

**Download JAR:**
[Bibby-0.2.1.jar](link-to-jar)

**Run:**
```bash
java -jar Bibby-0.2.1.jar
```

**Requirements:**
- Java 17+
- PostgreSQL database

## 🔄 Upgrading from v0.2.0

No breaking changes! Simply replace the JAR file.

Database migrations are automatic on startup.

## 📊 Metrics

- **Features Added:** 5
- **Bugs Fixed:** 2
- **Commits:** 15
- **Contributors:** 1

## 🙏 Acknowledgments

Thank you to everyone who provided feedback on the browse flow!

## 📝 Full Changelog

See [CHANGELOG.md](link) for complete details.

**Full Changelog:** [v0.2.0...v0.2.1](compare-link)
```

### Release Automation with GitHub Actions

**File:** `.github/workflows/release.yml`

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Get version from tag
        id: version
        run: echo "VERSION=${GITHUB_REF#refs/tags/v}" >> $GITHUB_OUTPUT

      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          files: target/Bibby-*.jar
          body_path: RELEASE_NOTES.md
          draft: false
          prerelease: false
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

**How it works:**
1. You push a tag: `git push origin v0.2.1`
2. GitHub Actions detects the tag
3. Builds the JAR
4. Creates GitHub release automatically
5. Attaches JAR to release

---

## 🔄 MAVEN VERSION MANAGEMENT

### Updating Version in pom.xml

**Manual Method:**

```xml
<!-- Before -->
<version>0.0.1-SNAPSHOT</version>

<!-- After -->
<version>0.2.1</version>
```

**Maven Versions Plugin Method:**

```bash
# Set specific version
mvn versions:set -DnewVersion=0.2.1

# Set next snapshot
mvn versions:set -DnewVersion=0.3.0-SNAPSHOT

# Revert if you made a mistake
mvn versions:revert

# Commit the change
mvn versions:commit
```

### SNAPSHOT vs. Release Versions

**SNAPSHOT:**
```xml
<version>0.2.1-SNAPSHOT</version>
```

**Meaning:**
- Work in progress
- Can be republished with same version
- Maven re-downloads on each build
- Never deploy SNAPSHOT to production

**Release:**
```xml
<version>0.2.1</version>
```

**Meaning:**
- Stable, immutable
- This exact version never changes
- Maven caches permanently
- Safe for production

### Version Workflow

**Development:**
```
0.2.1-SNAPSHOT → (develop features) → 0.2.1-SNAPSHOT
```

**Release:**
```
0.2.1-SNAPSHOT → mvn versions:set -DnewVersion=0.2.1 →
git tag v0.2.1 →
mvn versions:set -DnewVersion=0.3.0-SNAPSHOT
```

**Post-Release:**
```
0.3.0-SNAPSHOT → (next features) → 0.3.0-SNAPSHOT
```

---

## 📋 RELEASE PROCESS CHECKLIST

Here's your step-by-step release process for Bibby:

### Pre-Release Checklist

```markdown
## Pre-Release Checklist for v0.X.Y

- [ ] All planned features completed
- [ ] All tests passing (`mvn clean verify`)
- [ ] Test coverage ≥ 80%
- [ ] Code quality checks pass (Checkstyle, SpotBugs)
- [ ] Documentation updated
  - [ ] README.md reflects new features
  - [ ] JavaDoc comments complete
  - [ ] Command help text updated
- [ ] CHANGELOG.md updated with release notes
- [ ] No open P0/P1 bugs
- [ ] Manual testing completed
  - [ ] Tested all new features
  - [ ] Regression testing (old features still work)
  - [ ] Edge cases tested
- [ ] Dependencies up to date (security patches)
- [ ] Version decided (MAJOR.MINOR.PATCH)
```

### Release Steps

```bash
# 1. Update version in pom.xml
mvn versions:set -DnewVersion=0.2.1
mvn versions:commit

# 2. Update CHANGELOG.md
# - Move [Unreleased] items to [0.2.1]
# - Add release date
# - Update comparison links

# 3. Commit version bump
git add pom.xml CHANGELOG.md
git commit -m "chore: bump version to 0.2.1"

# 4. Create Git tag
git tag -a v0.2.1 -m "Release version 0.2.1

Features:
- Bookcase navigation
- Empty shelf handling

Bug Fixes:
- Shelf assignment fix"

# 5. Push to GitHub
git push origin main
git push origin v0.2.1

# 6. Build release artifacts
mvn clean package -Pprod

# 7. Create GitHub Release
gh release create v0.2.1 \
  --title "v0.2.1 - Bookcase Navigation & Bug Fixes" \
  --notes-file RELEASE_NOTES.md \
  ./target/Bibby-0.2.1.jar

# 8. Bump to next SNAPSHOT version
mvn versions:set -DnewVersion=0.3.0-SNAPSHOT
mvn versions:commit

# 9. Commit SNAPSHOT version
git add pom.xml
git commit -m "chore: prepare for next development iteration (0.3.0-SNAPSHOT)"
git push origin main

# 10. Announce release
# - Update README badge
# - Tweet/share (if applicable)
# - Notify users
```

### Post-Release Checklist

```markdown
## Post-Release Checklist

- [ ] GitHub release created and visible
- [ ] JAR attached to release
- [ ] Release notes are clear
- [ ] pom.xml updated to next SNAPSHOT
- [ ] README updated with new version
- [ ] Docker image built and pushed (if applicable)
- [ ] Announcement sent
- [ ] Monitor for issues
- [ ] Plan next release
```

---

## 🎯 RELEASE STRATEGIES

### Strategy 1: Time-Based Releases

**Example:** Release every 2 weeks

**Pros:**
- Predictable schedule
- Regular cadence
- Users know when to expect updates

**Cons:**
- May release without significant changes
- Pressure to meet deadlines

**Good for:** Established projects with active development

### Strategy 2: Feature-Based Releases

**Example:** Release when a feature is complete

**Pros:**
- Each release has clear value
- No artificial deadlines
- Quality over quantity

**Cons:**
- Unpredictable schedule
- Features may take weeks/months

**Good for:** Early stage projects (like Bibby now)

### Strategy 3: Continuous Delivery

**Example:** Every merge to main is a release

**Pros:**
- Fastest feedback
- Small, manageable changes
- Modern approach

**Cons:**
- Requires excellent CI/CD
- Needs automated testing
- More releases to manage

**Good for:** Mature projects with strong automation

### Recommendation for Bibby

**Current stage:** Feature-based releases

**Why:**
- You're still adding major features
- Releases have clear themes
- Quality matters more than speed

**Future:** Move to time-based (weekly/biweekly) as project matures

---

## 🏷️ PRE-RELEASE VERSIONS

### Alpha, Beta, Release Candidate

**Alpha (0.3.0-alpha.1):**
```bash
# Early testing version
# Features incomplete
# Expect bugs
# For developers only
```

**Beta (0.3.0-beta.1):**
```bash
# Feature complete
# Still has bugs
# For testers
# API may still change slightly
```

**Release Candidate (0.3.0-rc.1):**
```bash
# Final testing before release
# No known critical bugs
# API frozen
# One final check before v0.3.0
```

**Creating Pre-Release:**

```bash
# Tag as beta
git tag -a v0.3.0-beta.1 -m "Beta 1 for version 0.3.0"
git push origin v0.3.0-beta.1

# Create pre-release on GitHub
gh release create v0.3.0-beta.1 \
  --title "v0.3.0-beta.1" \
  --notes "Beta release for testing" \
  --prerelease \
  ./target/Bibby-0.3.0-beta.1.jar
```

**In pom.xml:**

```xml
<version>0.3.0-beta.1</version>
```

---

## 🎓 KEY TAKEAWAYS

1. **Semantic Versioning is NOT Optional**: Professional projects use SemVer

2. **Your Versions are Inconsistent**: pom.xml (0.0.1-SNAPSHOT) vs README (v0.2)

3. **Git Tags = Release Markers**: No tags = no reproducible versions

4. **CHANGELOG.md is Your History**: Document changes for users and your future self

5. **GitHub Releases are Your Portfolio**: Make releases look professional

6. **SNAPSHOT vs. Release**: Never deploy SNAPSHOT to production

7. **Release Process is Repeatable**: Checklist ensures consistency

8. **When in Doubt, MINOR Bump**: Better safe than breaking users

---

## 📝 ACTION ITEMS FOR BIBBY

### Week 1: Fix Version Inconsistency (2 hours)

**Day 1: Create CHANGELOG.md (1 hour)**
- [ ] Copy template from this section
- [ ] Fill in historical versions (0.1.0, 0.2.0, 0.2.1)
- [ ] Add dates from Git log
- [ ] Commit: "docs: add CHANGELOG.md"

**Day 2: Create Git Tags (30 min)**
- [ ] Tag historical commits retroactively
- [ ] Tag v0.2.0 (bookcase browser)
- [ ] Tag v0.2.1 (shelf fix)
- [ ] Tag v0.3.0 (current state)
- [ ] Push all tags: `git push origin --tags`

**Day 3: Update pom.xml Version (15 min)**
- [ ] Change from 0.0.1-SNAPSHOT to 0.3.0
- [ ] Commit: "chore: bump version to 0.3.0"
- [ ] Push to GitHub

**Day 4: Create GitHub Release (15 min)**
- [ ] Go to GitHub Releases page
- [ ] Create release for v0.3.0
- [ ] Use release notes template
- [ ] Attach JAR file
- [ ] Publish

### Week 2: Establish Release Process (1 hour)

**Day 1: Create Release Templates (30 min)**
- [ ] Create `docs/RELEASE_PROCESS.md`
- [ ] Copy release checklist from this section
- [ ] Customize for your workflow
- [ ] Commit

**Day 2: Set Up Version Badge (15 min)**
- [ ] Add version badge to README
- [ ] Update README with latest version
- [ ] Link to releases page

**Day 3: Plan Next Release (15 min)**
- [ ] Decide what goes in v0.4.0
- [ ] Create milestone in GitHub
- [ ] Add issues to milestone

### Week 3: Automation (Optional, 2 hours)

- [ ] Set up GitHub Actions for releases
- [ ] Configure automated CHANGELOG generation
- [ ] Test release automation

---

## 📚 RESOURCES

### Official Documentation
- [Semantic Versioning](https://semver.org/) - The official spec
- [Keep a Changelog](https://keepachangelog.com/) - CHANGELOG format
- [GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github)

### Tools
- [Maven Versions Plugin](https://www.mojohaus.org/versions-maven-plugin/)
- [Conventional Changelog](https://github.com/conventional-changelog/conventional-changelog)
- [GitHub CLI](https://cli.github.com/) - Create releases from terminal

### Reading
- [How to Version Your Software](https://www.sitepoint.com/semantic-versioning-why-you-should-using/)
- [Git Tagging Best Practices](https://www.atlassian.com/git/tutorials/inspecting-a-repository/git-tag)

---

## 🎯 SECTION SUMMARY

**Your Current State Analyzed:**
- pom.xml: `0.0.1-SNAPSHOT` ❌
- README: `v0.2` ❌
- Git tags: None ❌
- CHANGELOG.md: Doesn't exist ❌
- GitHub Releases: None ❌

**What You Learned:**
- ✅ Semantic versioning rules (MAJOR.MINOR.PATCH)
- ✅ When to bump each version component
- ✅ Git tagging (lightweight vs. annotated)
- ✅ CHANGELOG format and best practices
- ✅ GitHub Releases creation and automation
- ✅ Maven version management
- ✅ SNAPSHOT vs. release versions
- ✅ Complete release process checklist
- ✅ Pre-release versioning (alpha, beta, RC)

**What You Should Be At:**
- **Recommended:** v0.3.0 (includes all current features + DevOps docs)
- **pom.xml:** Update to 0.3.0
- **Git tags:** Create v0.2.0, v0.2.1, v0.3.0
- **CHANGELOG.md:** Document all releases
- **GitHub:** Create releases for each tag

**Templates Provided:**
- Complete CHANGELOG.md with historical versions
- Release notes template
- Pre-release checklist
- Release process checklist
- Post-release checklist
- GitHub Actions workflow for automated releases

**Immediate Actions:**
1. Create CHANGELOG.md (1 hour)
2. Create Git tags for historical versions (30 min)
3. Update pom.xml to 0.3.0 (15 min)
4. Create GitHub release for v0.3.0 (30 min)
5. Update README with correct version (15 min)

**Total Time to Fix Everything: ~2.5 hours**

---

## ⏸️ PAUSE POINT

You now understand how to properly version your software and manage releases professionally.

**Your Bibby project will go from:**
- ❌ Confusing version numbers
- ❌ No release history
- ❌ Can't reproduce versions

**To:**
- ✅ Clear semantic versioning
- ✅ Professional CHANGELOG
- ✅ Tagged releases in Git
- ✅ GitHub releases with artifacts
- ✅ Reproducible builds

**This is HUGE for interviews!** You can now say:
> "I follow semantic versioning strictly, maintain a detailed changelog, use Git tags for releases, and automate deployments through GitHub Actions. My latest release is v0.3.0, which you can download from the releases page."

**Ready for Section 5: CI Fundamentals?**

---

**📊 Progress**: 4/28 sections complete (14%)
**Next**: Section 5 - Continuous Integration Fundamentals (GitHub Actions)
**Your move**: Type "continue" when ready! 🚀
