# Section 11: Git Tagging & Release Management

## Introduction: Making Your Releases Official

In Section 4, we covered semantic versioning theory. Now we implement it with **Git tags**—the mechanism that marks specific commits as release points in your project's history.

Tags are Git's way of saying: "This commit is special—it's v1.0.0, the version users will download and run in production."

**What You'll Learn:**
- Lightweight vs annotated tags (and why the difference matters)
- Creating release tags with proper metadata
- Managing multiple release versions simultaneously
- Automated release workflows with GitHub Actions
- Release notes generation from commit history
- Tag-based deployment strategies
- Applying professional release management to Bibby

**Real-World Context:**
Currently, Bibby has no tags. Your README.md mentions "v0.2" but there's no corresponding Git tag. This means:
- No way to download a specific version
- Can't roll back to a known-good release
- No clear history of what changed between versions
- Deployment process is ambiguous

By the end of this section, Bibby will have proper release tagging that matches industry standards.

---

## 1. Understanding Git Tags

### 1.1 Tags vs Branches: Key Differences

**Branches:**
- Pointers that **move** with new commits
- Used for active development
- Mutable (can be deleted, reset)

**Tags:**
- Pointers that **never move**
- Used to mark specific points in history
- Immutable (shouldn't be changed after creation)

**Analogy:**
- Branches = bookmarks (you move them as you read)
- Tags = dog-eared pages (permanent marks)

**In Bibby's History:**

```bash
# Current state (no tags)
git log --oneline
# 49a7177 Section 10: Merging, Rebasing & Conflict Resolution
# 0e57cfc Section 9: Branching Strategies & Team Workflows
# 79fd3c1 Section 8: Git Mental Model & Internals
# ...

# With tags (what we'll create):
git log --oneline --decorate
# 49a7177 (HEAD, tag: v0.3.0) Section 10
# 0e57cfc (tag: v0.2.5) Section 9
# 79fd3c1 (tag: v0.2.0) Section 8
# ...
```

### 1.2 Lightweight Tags

**What they are:** Just a pointer to a commit (like a branch that doesn't move).

**Create lightweight tag:**

```bash
git tag v0.1.0
```

**That's it.** It creates a file in `.git/refs/tags/v0.1.0` containing the commit SHA.

**View lightweight tag:**

```bash
git show v0.1.0

# Output: Shows the commit, no tag metadata
commit 1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0
Author: Your Name <you@example.com>
Date:   Mon Jan 15 10:30:00 2025

    Initial commit
```

**Pros:**
- Quick to create
- No additional data

**Cons:**
- No metadata (who tagged, when, why)
- No GPG signature
- Not suitable for releases

### 1.3 Annotated Tags (Recommended for Releases)

**What they are:** Full Git objects with metadata.

**Create annotated tag:**

```bash
git tag -a v0.1.0 -m "Initial release of Bibby"
```

**Annotated tags store:**
- Tagger name and email
- Tagging date
- Tag message
- Optional GPG signature

**View annotated tag:**

```bash
git show v0.1.0

# Output: Shows tag metadata THEN commit
tag v0.1.0
Tagger: Your Name <you@example.com>
Date:   Mon Jan 15 10:30:00 2025

Initial release of Bibby

commit 1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0
Author: Your Name <you@example.com>
Date:   Mon Jan 15 10:30:00 2025

    Initial commit
```

**Comparison:**

| Feature | Lightweight | Annotated |
|---------|------------|-----------|
| Storage | Just a pointer | Full Git object |
| Metadata | None | Tagger, date, message |
| Signature | No | Yes (optional) |
| Use Case | Temporary markers | Releases, milestones |
| Best Practice | Development | Production |

**Professional Standard:** Always use annotated tags for releases.

---

## 2. Creating Release Tags for Bibby

### 2.1 Reconstructing Bibby's Version History

**From Section 4, we identified these versions:**

```bash
# Check current commits
git log --oneline --all

# Based on features, let's tag historical versions:
# v0.1.0 - Initial project setup
# v0.2.0 - Basic book management (add, list, search)
# v0.3.0 - Current state with full DevOps documentation
```

### 2.2 Tagging Historical Commits

**Find the right commits:**

```bash
# View full history
git log --oneline --reverse

# Output (simplified, your hashes will differ):
# abc123 Initial commit
# def456 Add book entity and repository
# ghi789 Add basic CLI commands
# ... (many commits)
# 49a7177 Section 10: Merging, Rebasing & Conflict Resolution
```

**Tag v0.1.0 (Initial Release):**

```bash
# Find first meaningful commit
git log --oneline --reverse | head -n 5

# Tag it
git tag -a v0.1.0 abc123 -m "Release v0.1.0 - Initial Bibby release

Features:
- Spring Boot CLI application
- Basic project structure
- PostgreSQL integration

This is the first working version of Bibby, the library management CLI."
```

**Tag v0.2.0 (Book Management Features):**

```bash
# Find commit where basic features were complete
# (before DevOps documentation started)
git log --oneline --grep="book" | tail -n 1

# Tag it
git tag -a v0.2.0 def456 -m "Release v0.2.0 - Book Management

New Features:
- Add books with title and authors
- List all books in library
- Search books by title
- Basic book status tracking

Improvements:
- JPA entities for Book and Author
- Many-to-many relationship handling
- Command-line interface with Spring Shell

Bug Fixes:
- Fixed book persistence issues
- Corrected author association logic"
```

**Tag v0.3.0 (Current State):**

```bash
# Tag latest commit
git tag -a v0.3.0 -m "Release v0.3.0 - DevOps Maturity

Major Update: Comprehensive DevOps documentation and infrastructure

Documentation:
- Complete DevOps mentorship guide (10 sections)
- SDLC fundamentals
- CI/CD best practices
- Git workflows and strategies

Infrastructure:
- GitHub Actions CI pipeline (proposed)
- Docker containerization guide
- Security best practices
- Branch protection strategies

This release makes Bibby production-ready with professional
software delivery practices."
```

### 2.3 Viewing Tags

**List all tags:**

```bash
git tag

# Output:
# v0.1.0
# v0.2.0
# v0.3.0
```

**List with patterns:**

```bash
# All v0.2.x tags
git tag -l "v0.2.*"

# All tags
git tag -l
```

**Show tag details:**

```bash
git show v0.3.0

# Shows tag metadata and commit
```

**Log with tags:**

```bash
git log --oneline --decorate --all

# Output:
# 49a7177 (HEAD -> main, tag: v0.3.0) Section 10
# def456 (tag: v0.2.0) Add book management
# abc123 (tag: v0.1.0) Initial commit
```

### 2.4 Pushing Tags to Remote

**Important:** Tags are NOT pushed by default!

```bash
# Push single tag
git push origin v0.3.0

# Push all tags
git push origin --tags

# Push tags with commits (recommended)
git push --follow-tags
```

**Configure automatic tag pushing:**

```bash
# Always push annotated tags with commits
git config --global push.followTags true

# Now `git push` automatically pushes annotated tags
```

---

## 3. Tag-Based Versioning Strategy

### 3.1 Semantic Versioning with Tags

**Recap from Section 4:**
- **MAJOR.MINOR.PATCH** (e.g., v2.3.1)
- MAJOR: Breaking changes
- MINOR: New features (backward compatible)
- PATCH: Bug fixes

**Tagging Strategy:**

```bash
# Bug fix release
git tag -a v0.3.1 -m "Release v0.3.1 - Bug fixes

Bug Fixes:
- Fixed NPE in book search
- Corrected database migration script
- Fixed typos in documentation"

# Minor version (new features)
git tag -a v0.4.0 -m "Release v0.4.0 - ISBN Lookup

New Features:
- ISBN lookup via OpenLibrary API
- Book cover image download
- Enhanced search with filters

Improvements:
- Better error handling
- Improved test coverage (85%)

Bug Fixes:
- Fixed search pagination"

# Major version (breaking changes)
git tag -a v1.0.0 -m "Release v1.0.0 - Production Ready

BREAKING CHANGES:
- BookStatus changed from String to enum
- API endpoint paths restructured
- Database schema updated (migration required)

New Features:
- RESTful API for library management
- User authentication and authorization
- Multi-library support

See MIGRATION.md for upgrade instructions."
```

### 3.2 Pre-release Tags

**For beta/alpha versions:**

```bash
# Alpha release (very unstable)
git tag -a v0.4.0-alpha.1 -m "Release v0.4.0-alpha.1

First alpha release of v0.4.0
- ISBN lookup feature (incomplete)
- May contain bugs, not for production use"

# Beta release (feature complete, testing)
git tag -a v0.4.0-beta.1 -m "Release v0.4.0-beta.1

Beta release of v0.4.0
- All features complete
- Testing in progress
- Bug reports welcome"

# Release candidate
git tag -a v0.4.0-rc.1 -m "Release v0.4.0-rc.1

Release candidate for v0.4.0
- All features tested
- Final review before stable release
- Production-ready if no issues found"

# Stable release
git tag -a v0.4.0 -m "Release v0.4.0

Stable release with ISBN lookup feature"
```

**Semver ordering:**
```
v0.4.0-alpha.1
v0.4.0-alpha.2
v0.4.0-beta.1
v0.4.0-beta.2
v0.4.0-rc.1
v0.4.0  (stable)
```

### 3.3 Tag Naming Conventions

**Good practices:**

```bash
# ✅ Semantic versioning with 'v' prefix
v1.2.3
v0.4.0-beta.1

# ✅ Consistent format
v1.0.0
v1.0.1
v1.1.0

# ❌ Inconsistent
1.0.0 (no v prefix)
v1.0 (missing patch)
version-1.0.0 (verbose)
rel-1.0.0 (unclear)
```

**Bibby's convention:**
```
v{MAJOR}.{MINOR}.{PATCH}[-{PRERELEASE}]

Examples:
v0.3.0
v0.4.0-alpha.1
v1.0.0
v1.2.3-rc.2
```

---

## 4. Managing Multiple Release Versions

### 4.1 Supporting Multiple Versions

**Scenario:** Bibby v1.0.0 is in production, but you still need to support v0.3.x for legacy users.

**Strategy: Release Branches**

```bash
# Create release branch for v0.3.x maintenance
git checkout v0.3.0
git checkout -b release/0.3.x

# Push to remote
git push -u origin release/0.3.x

# Any v0.3.x patches go here
git checkout release/0.3.x

# Fix critical bug
git commit -m "Fix security vulnerability in search"

# Tag patch release
git tag -a v0.3.1 -m "Release v0.3.1 - Security fix"
git push origin v0.3.1

# Meanwhile, main branch continues with v1.x development
```

**Branch structure:**

```
main (v1.x development)
  |
  * v1.1.0
  * v1.0.0
  |
release/0.3.x (maintenance)
  |
  * v0.3.2
  * v0.3.1
  * v0.3.0
```

### 4.2 Backporting Fixes

**Scenario:** Security bug found in v1.0.0 that also affects v0.3.x.

**Workflow:**

```bash
# Fix on main first
git checkout main
git checkout -b hotfix/security-fix

# Implement fix
git commit -m "Fix SQL injection in search query"

# Tag and release v1.0.1
git checkout main
git merge hotfix/security-fix
git tag -a v1.0.1 -m "Security hotfix"
git push origin v1.0.1

# Backport to v0.3.x
git checkout release/0.3.x
git cherry-pick <commit-hash>

# Tag and release v0.3.2
git tag -a v0.3.2 -m "Security hotfix (backport from v1.0.1)"
git push origin v0.3.2
```

### 4.3 End-of-Life Tags

**Mark when versions are no longer supported:**

```bash
# Tag EOL (End of Life)
git tag -a v0.2.0-eol -m "End of Life for v0.2.x series

v0.2.x is no longer supported as of 2025-06-01.
Users should upgrade to v0.3.x or v1.x.

Final supported version: v0.2.5
Security fixes: No longer provided
Bug fixes: No longer provided

Upgrade guide: docs/UPGRADE_0.2_to_0.3.md"
```

---

## 5. GitHub Releases

### 5.1 Creating GitHub Releases from Tags

**Tags vs Releases:**
- **Tags:** Git objects in your repository
- **Releases:** GitHub feature with downloadable artifacts and release notes

**Create release via GitHub CLI:**

```bash
# Create release from existing tag
gh release create v0.3.0 \
  --title "Bibby v0.3.0 - DevOps Maturity" \
  --notes "## What's New

### Documentation
- Complete DevOps mentorship guide
- CI/CD best practices
- Git workflows and strategies

### Infrastructure
- GitHub Actions CI pipeline
- Docker containerization
- Security guidelines

### Improvements
- Enhanced project structure
- Professional branching strategy
- Comprehensive testing guides

## Installation

\`\`\`bash
git clone https://github.com/leodvincci/Bibby.git
cd Bibby
git checkout v0.3.0
mvn clean package
java -jar target/Bibby-0.3.0.jar
\`\`\`

## Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for complete details."
```

**With release artifacts:**

```bash
# Build artifacts first
mvn clean package

# Create release with JAR file
gh release create v0.3.0 \
  --title "Bibby v0.3.0" \
  --notes-file RELEASE_NOTES.md \
  target/Bibby-0.3.0.jar \
  target/Bibby-0.3.0-sources.jar
```

### 5.2 Automated Release Notes

**Generate from commit messages:**

```bash
# Get commits since last tag
git log v0.2.0..HEAD --oneline

# Generate release notes
gh release create v0.3.0 --generate-notes

# GitHub automatically:
# - Lists all PRs merged
# - Credits contributors
# - Categorizes by labels (bug, feature, docs)
```

**Manual release notes template:**

Create `RELEASE_NOTES_TEMPLATE.md`:

```markdown
## 🎉 What's New in v{{VERSION}}

### ✨ New Features
<!-- List new features -->

### 🐛 Bug Fixes
<!-- List bug fixes -->

### 📚 Documentation
<!-- List documentation improvements -->

### 🔧 Improvements
<!-- List enhancements -->

### ⚠️ Breaking Changes
<!-- List breaking changes (if any) -->

### 📦 Dependency Updates
<!-- List major dependency changes -->

---

## 📥 Installation

\`\`\`bash
# Using Git
git clone https://github.com/leodvincci/Bibby.git
cd Bibby
git checkout v{{VERSION}}
mvn clean package

# Using release JAR
wget https://github.com/leodvincci/Bibby/releases/download/v{{VERSION}}/Bibby-{{VERSION}}.jar
java -jar Bibby-{{VERSION}}.jar
\`\`\`

## 🔄 Upgrade Instructions

See [UPGRADE.md](UPGRADE.md) for detailed upgrade instructions.

## 📖 Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for complete details.

## 🙏 Contributors

Thank you to all contributors who made this release possible!

<!-- Auto-generated contributor list -->
```

### 5.3 Release Assets

**Attach build artifacts:**

```bash
# JAR file
target/Bibby-0.3.0.jar

# Source archive (GitHub auto-generates)
Bibby-0.3.0.tar.gz
Bibby-0.3.0.zip

# Checksums
Bibby-0.3.0.jar.sha256

# Docker image tag
docker.io/youruser/bibby:0.3.0
```

**Generate checksums:**

```bash
# SHA256
sha256sum target/Bibby-0.3.0.jar > target/Bibby-0.3.0.jar.sha256

# Include in release
gh release upload v0.3.0 target/Bibby-0.3.0.jar.sha256
```

---

## 6. Automated Release Workflow

### 6.1 GitHub Actions Release Pipeline

Create `.github/workflows/release.yml`:

```yaml
name: Release Pipeline

on:
  push:
    tags:
      - 'v*.*.*'  # Trigger on version tags

jobs:
  build:
    name: Build and Release
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Full history for changelog

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Extract Version from Tag
        id: version
        run: |
          echo "VERSION=${GITHUB_REF#refs/tags/v}" >> $GITHUB_OUTPUT

      - name: Update pom.xml Version
        run: |
          mvn versions:set -DnewVersion=${{ steps.version.outputs.VERSION }}
          mvn versions:commit

      - name: Build Project
        run: mvn clean package -DskipTests=false

      - name: Run Tests
        run: mvn test

      - name: Generate Checksums
        run: |
          cd target
          sha256sum Bibby-${{ steps.version.outputs.VERSION }}.jar > Bibby-${{ steps.version.outputs.VERSION }}.jar.sha256

      - name: Generate Changelog
        id: changelog
        uses: mikepenz/release-changelog-builder-action@v4
        with:
          configuration: ".github/changelog-config.json"
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          name: Bibby v${{ steps.version.outputs.VERSION }}
          body: ${{ steps.changelog.outputs.changelog }}
          files: |
            target/Bibby-${{ steps.version.outputs.VERSION }}.jar
            target/Bibby-${{ steps.version.outputs.VERSION }}.jar.sha256
          draft: false
          prerelease: ${{ contains(github.ref, '-alpha') || contains(github.ref, '-beta') || contains(github.ref, '-rc') }}
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

      - name: Build Docker Image
        run: |
          docker build -t bibby:${{ steps.version.outputs.VERSION }} .
          docker tag bibby:${{ steps.version.outputs.VERSION }} bibby:latest

      - name: Push Docker Image
        if: "!contains(github.ref, '-alpha') && !contains(github.ref, '-beta')"
        run: |
          echo "${{ secrets.DOCKER_PASSWORD }}" | docker login -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin
          docker push bibby:${{ steps.version.outputs.VERSION }}
          docker push bibby:latest

  notify:
    name: Notify Release
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Send Slack Notification
        uses: slackapi/slack-github-action@v1.24.0
        with:
          payload: |
            {
              "text": "🚀 Bibby ${{ github.ref_name }} released!",
              "blocks": [
                {
                  "type": "section",
                  "text": {
                    "type": "mrkdwn",
                    "text": "*Bibby ${{ github.ref_name }} released!*\n\nView release: ${{ github.server_url }}/${{ github.repository }}/releases/tag/${{ github.ref_name }}"
                  }
                }
              ]
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

### 6.2 Changelog Configuration

Create `.github/changelog-config.json`:

```json
{
  "categories": [
    {
      "title": "## 🚀 New Features",
      "labels": ["feature", "enhancement"]
    },
    {
      "title": "## 🐛 Bug Fixes",
      "labels": ["bug", "fix"]
    },
    {
      "title": "## 📚 Documentation",
      "labels": ["documentation", "docs"]
    },
    {
      "title": "## 🔧 Improvements",
      "labels": ["improvement", "refactor"]
    },
    {
      "title": "## 🔒 Security",
      "labels": ["security"]
    },
    {
      "title": "## 📦 Dependencies",
      "labels": ["dependencies"]
    }
  ],
  "ignore_labels": [
    "ignore-changelog",
    "wontfix",
    "duplicate"
  ],
  "sort": "ASC",
  "template": "#{{CHANGELOG}}\n\n**Full Changelog**: #{{RELEASE_DIFF}}",
  "pr_template": "- #{{TITLE}} by @#{{AUTHOR}} in ##{{NUMBER}}",
  "empty_template": "No changes",
  "label_extractor": [
    {
      "pattern": "^feature",
      "target": "feature"
    },
    {
      "pattern": "^fix",
      "target": "bug"
    }
  ]
}
```

### 6.3 Triggering Releases

**Workflow:**

```bash
# 1. Ensure main is ready for release
git checkout main
git pull origin main

# 2. Run final tests
mvn clean verify

# 3. Create and push tag
git tag -a v0.4.0 -m "Release v0.4.0 - ISBN Lookup Feature

New Features:
- ISBN lookup via OpenLibrary API
- Book cover downloads
- Advanced search filters

Bug Fixes:
- Fixed pagination in search results
- Corrected author sorting

See CHANGELOG.md for full details."

# 4. Push tag (triggers GitHub Actions)
git push origin v0.4.0

# 5. GitHub Actions automatically:
#    - Builds JAR
#    - Runs tests
#    - Generates changelog
#    - Creates GitHub Release
#    - Builds Docker image
#    - Sends notifications

# 6. Monitor release
gh run list --workflow=release.yml
gh run watch
```

---

## 7. Tag Management

### 7.1 Deleting Tags

**Delete local tag:**

```bash
git tag -d v0.3.0
```

**Delete remote tag:**

```bash
git push origin --delete v0.3.0

# Or
git push origin :refs/tags/v0.3.0
```

**When to delete tags:**
- ❌ Tagged wrong commit
- ❌ Typo in tag name
- ❌ Tag pushed accidentally

**⚠️ Warning:** Deleting published tags breaks users' workflows. If tag is already released, create a new version instead.

### 7.2 Moving Tags (Not Recommended)

**Force move tag to different commit:**

```bash
# Delete old tag
git tag -d v0.3.0

# Create new tag at different commit
git tag -a v0.3.0 abc123 -m "Release v0.3.0"

# Force push (overwrites remote)
git push origin v0.3.0 --force
```

**⚠️ NEVER do this for public releases!** Users may already be using the old tag.

**Better approach:**
- If bug in release: Create v0.3.1 patch
- If wrong commit tagged: Create v0.3.0-revised or v0.4.0

### 7.3 Signed Tags (GPG)

**Why sign tags:**
- Verify author authenticity
- Prevent tag tampering
- Required by some organizations

**Create GPG-signed tag:**

```bash
# Set up GPG key first
gpg --gen-key

# Configure Git
git config --global user.signingkey <your-key-id>

# Create signed tag
git tag -s v0.3.0 -m "Release v0.3.0"
# Or
git tag -a -s v0.3.0 -m "Release v0.3.0"
```

**Verify signed tag:**

```bash
git tag -v v0.3.0

# Output:
# object 1a2b3c4d5e6f
# type commit
# tag v0.3.0
# tagger Your Name <you@example.com> 1234567890 +0000
#
# Release v0.3.0
# gpg: Signature made ...
# gpg: Good signature from "Your Name <you@example.com>"
```

**Configure automatic signing:**

```bash
git config --global tag.gpgSign true
# Now all annotated tags are signed by default
```

---

## 8. Bibby Release Checklist

### 8.1 Pre-Release Checklist

```markdown
## Pre-Release Checklist for Bibby v0.X.0

### Code Quality
- [ ] All tests pass (`mvn test`)
- [ ] No compiler warnings
- [ ] Code coverage ≥ 70%
- [ ] No high/critical security vulnerabilities (`mvn dependency-check:check`)
- [ ] Static analysis clean (Checkstyle, SpotBugs)

### Documentation
- [ ] README.md updated with new features
- [ ] CHANGELOG.md updated with all changes
- [ ] JavaDoc updated for public APIs
- [ ] Migration guide created (if breaking changes)
- [ ] Installation instructions verified

### Version Management
- [ ] Version bumped in pom.xml
- [ ] Version consistent across all files
- [ ] Git tag created with proper message
- [ ] Release notes drafted

### Testing
- [ ] Manual testing completed
- [ ] Integration tests pass
- [ ] Database migrations tested
- [ ] Backward compatibility verified (if applicable)

### Build
- [ ] Clean build succeeds (`mvn clean package`)
- [ ] JAR runs correctly
- [ ] Docker image builds successfully
- [ ] All platforms tested (if applicable)

### Release Process
- [ ] Branch protection verified
- [ ] CI/CD pipeline passing
- [ ] Release branch created (if using GitFlow)
- [ ] Final code review completed

### Post-Release
- [ ] GitHub Release created
- [ ] Artifacts uploaded (JAR, checksums)
- [ ] Docker image pushed
- [ ] Documentation deployed
- [ ] Announcement prepared
- [ ] Tag pushed to remote

### Communication
- [ ] Release announcement drafted
- [ ] Known issues documented
- [ ] Support channels notified
- [ ] Social media posts ready (if applicable)
```

### 8.2 Release Execution Script

Create `scripts/release.sh`:

```bash
#!/bin/bash
set -e

# Bibby Release Script
# Usage: ./scripts/release.sh 0.4.0

VERSION=$1

if [ -z "$VERSION" ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 0.4.0"
    exit 1
fi

echo "🚀 Starting release process for Bibby v$VERSION"

# 1. Verify clean working directory
if [ -n "$(git status --porcelain)" ]; then
    echo "❌ Working directory not clean. Commit or stash changes first."
    exit 1
fi

# 2. Verify on main branch
BRANCH=$(git branch --show-current)
if [ "$BRANCH" != "main" ]; then
    echo "❌ Must be on main branch. Currently on: $BRANCH"
    exit 1
fi

# 3. Pull latest
echo "📥 Pulling latest changes..."
git pull origin main

# 4. Update version in pom.xml
echo "📝 Updating version to $VERSION..."
mvn versions:set -DnewVersion=$VERSION
mvn versions:commit

# 5. Run tests
echo "🧪 Running tests..."
mvn clean verify

# 6. Commit version change
git add pom.xml
git commit -m "Bump version to $VERSION"

# 7. Create annotated tag
echo "🏷️  Creating tag v$VERSION..."
git tag -a "v$VERSION" -m "Release v$VERSION

See CHANGELOG.md for details."

# 8. Push commits and tags
echo "📤 Pushing to remote..."
git push origin main
git push origin "v$VERSION"

# 9. Create GitHub Release
echo "🎉 Creating GitHub Release..."
gh release create "v$VERSION" \
    --title "Bibby v$VERSION" \
    --notes-file CHANGELOG.md \
    target/Bibby-$VERSION.jar

echo "✅ Release v$VERSION complete!"
echo "📦 View release: https://github.com/leodvincci/Bibby/releases/tag/v$VERSION"
```

**Make executable:**

```bash
chmod +x scripts/release.sh
```

**Usage:**

```bash
./scripts/release.sh 0.4.0
```

---

## 9. Interview-Ready Knowledge

### Question: "Explain the difference between lightweight and annotated tags"

**Bad Answer:** "Lightweight tags are simpler, annotated have messages."

**Good Answer:**
"Git has two types of tags: lightweight and annotated, and understanding the difference is important for professional release management.

Lightweight tags are just pointers to commits—essentially immutable branch names. They're stored as simple references in .git/refs/tags/ containing only the commit SHA. They're useful for temporary bookmarks during development.

Annotated tags are full Git objects with complete metadata: tagger name and email, tagging date, a message, and optional GPG signatures. They're stored in the object database just like commits. When you run 'git show' on an annotated tag, you see the tag metadata before the commit.

For releases, I always use annotated tags because they provide crucial metadata: who released it, when, and why. This is especially important for auditing and compliance. In my Bibby project, I use 'git tag -a v1.0.0 -m' with detailed release notes including features, bug fixes, and breaking changes.

Lightweight tags are fine for local bookmarks, but for anything users will download or production will deploy, annotated tags are the professional standard."

**Why It's Good:**
- Technical accuracy (metadata, storage mechanism)
- Clear use case distinction
- Real project example
- Professional context

### Question: "How do you manage multiple release versions?"

**Bad Answer:** "Create different tags for each version."

**Good Answer:**
"Managing multiple versions requires a combination of branching strategy and tag discipline.

For active development, I maintain the main branch for the latest version. When a major version needs long-term support, I create a release branch. For example, when Bibby moves from v0.3.x to v1.0.0, I'd create a 'release/0.3.x' branch from the v0.3.0 tag.

Critical bug fixes go to main first, then get backported to release branches via cherry-pick. Each fix gets its own tag—v1.0.1 on main, v0.3.1 on the release branch. This ensures security fixes reach all supported versions.

I document the support lifecycle clearly: which versions receive security fixes, which get bug fixes, and which are end-of-life. I even create EOL tags like 'v0.2.0-eol' with a message explaining the deprecation timeline and upgrade path.

The key is having a clear policy before you need it. I've seen teams scramble when a security vulnerability hits and they don't know which versions to patch. Having release branches and a documented support policy prevents that chaos."

**Why It's Good:**
- Specific strategy (release branches, cherry-pick)
- Real versioning example
- Addresses security concerns
- Shows planning and documentation skills

### Question: "Walk me through your release process"

**Bad Answer:** "I create a tag and upload files to GitHub."

**Good Answer:**
"My release process is fully automated but starts with manual verification. First, I ensure all tests pass and code coverage meets our 70% threshold. I verify no high-severity vulnerabilities with 'mvn dependency-check:check'.

Next, I update the CHANGELOG.md following the Keep a Changelog format, documenting all features, fixes, and breaking changes. I bump the version in pom.xml following semantic versioning rules.

Then I create an annotated tag with 'git tag -a v1.0.0 -m' including a summary of the release. When I push that tag, our GitHub Actions pipeline automatically kicks in. It builds the JAR, runs the full test suite, generates checksums, and creates a GitHub Release with the changelog.

For Docker deployments, the pipeline builds and pushes the image with both the version tag and 'latest'. We use pre-release tags like '-beta.1' for testing, which the pipeline recognizes and marks as pre-release on GitHub.

Finally, I verify the release is downloadable, test the JAR independently, and post an announcement. The entire process from decision to public release takes about 30 minutes, with most of that being automated.

The key is having a checklist and automation so releases are consistent and low-stress."

**Why It's Good:**
- End-to-end process description
- Specific tools and commands
- Automation mentioned
- Quality gates emphasized
- Realistic timeline

---

## 10. Summary & Next Steps

### Key Takeaways

✅ **Tags:**
- Lightweight: Simple pointers (development)
- Annotated: Full objects with metadata (releases)
- Immutable markers in Git history

✅ **Versioning:**
- Follow semantic versioning (MAJOR.MINOR.PATCH)
- Use pre-release tags (-alpha, -beta, -rc)
- Consistent naming (v1.2.3)

✅ **Release Management:**
- Release branches for LTS versions
- Backport critical fixes via cherry-pick
- Document EOL explicitly

✅ **Automation:**
- GitHub Actions for release pipeline
- Auto-generate changelogs
- Build and publish artifacts

✅ **Professional Practices:**
- Pre-release checklist
- Release scripts
- Comprehensive release notes

### Immediate Action Items for Bibby

**Priority 1: Tag Historical Versions (30 minutes)**

```bash
# Review history
git log --oneline --reverse

# Tag v0.1.0 (initial release)
git tag -a v0.1.0 <commit-hash> -m "Release v0.1.0 - Initial release"

# Tag v0.2.0 (book features)
git tag -a v0.2.0 <commit-hash> -m "Release v0.2.0 - Book management features"

# Tag v0.3.0 (current state)
git tag -a v0.3.0 HEAD -m "Release v0.3.0 - DevOps documentation"

# Push tags
git push origin --tags
```

**Priority 2: Create GitHub Releases (1 hour)**

```bash
# Install GitHub CLI if needed
# Create releases for each tag
gh release create v0.1.0 --title "Bibby v0.1.0 - Initial Release" --notes "First release"
gh release create v0.2.0 --title "Bibby v0.2.0 - Book Management" --notes "Core features"
gh release create v0.3.0 --title "Bibby v0.3.0 - DevOps Maturity" --notes-file CHANGELOG.md
```

**Priority 3: Set Up Release Automation (2 hours)**

```bash
# Create release workflow
mkdir -p .github/workflows
# Add release.yml (from section 6.1)

# Create changelog config
# Add changelog-config.json (from section 6.2)

# Test with next release
```

**Priority 4: Document Release Process (1 hour)**

```markdown
# Create RELEASING.md

1. Update CHANGELOG.md
2. Bump version in pom.xml
3. Run: ./scripts/release.sh X.Y.Z
4. Verify GitHub Release created
5. Test JAR download
6. Announce release
```

### Practice Exercises

**Exercise 1: Create Your First Tags**

```bash
# Tag current state
git tag -a v0.3.0 -m "Current stable version"
git push origin v0.3.0

# Create GitHub Release
gh release create v0.3.0 --generate-notes
```

**Exercise 2: Simulate Patch Release**

```bash
# Fix a bug
git checkout -b hotfix/typo-fix
# ... fix typo ...
git commit -m "Fix typo in README"

# Merge to main
git checkout main
git merge hotfix/typo-fix

# Tag patch version
git tag -a v0.3.1 -m "Release v0.3.1 - Documentation fix"
git push origin v0.3.1
```

**Exercise 3: Pre-release Tag**

```bash
# Create beta tag
git tag -a v0.4.0-beta.1 -m "Beta release for testing"
gh release create v0.4.0-beta.1 --prerelease --notes "Beta version for testing"
```

### Coming Up in Section 12

**Git Best Practices & Complete Workflows:**
- Commit message conventions
- Git hooks for automation
- Advanced Git configurations
- Complete workflows for different team sizes
- Git anti-patterns to avoid
- Building your personal Git toolkit

You'll learn the final polish that makes your Git usage professional-grade, with practices that prevent common mistakes and streamline your workflow.

---

**Section 11 Complete:** Bibby now has professional release management with proper tagging, versioning, and automation. Your project is ready for production deployment! 🏷️✨
