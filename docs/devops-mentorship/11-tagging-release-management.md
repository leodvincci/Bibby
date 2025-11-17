# Section 11: Tagging & Release Management

## Introduction: From Code to Production with Confidence

You've mastered semantic versioning (Section 4), understood branching strategies (Section 9), and learned merging and rebasing (Section 10). Now it's time to connect all these pieces into a professional release workflow.

**The Problem:** Many developers know how to write code but struggle with the "last mile"—getting that code packaged, versioned, and released to users in a repeatable, professional manner.

**What You'll Learn:**
- Advanced Git tagging strategies beyond the basics
- Complete release workflows from development to production
- Hotfix and patch release management
- Tag-based deployment pipelines
- Release branch strategies
- Rollback and emergency procedures
- Real-world release scenarios with Bibby

**Real-World Context:**
Imagine it's Friday afternoon. You discover a critical bug in production (Bibby v0.3.0). You have features in development for v0.4.0 that aren't ready. How do you:
1. Create a hotfix without including incomplete features?
2. Tag and release v0.3.1 quickly?
3. Ensure the fix makes it back to your development branch?
4. Deploy confidently without breaking anything?

This section teaches you the professional workflows that make these scenarios routine rather than stressful.

---

## 1. Git Tags: Beyond the Basics

### 1.1 Understanding Tag Internals

**What is a Git tag, really?**

A tag is a **ref** (reference) that points to a specific commit, stored in `.git/refs/tags/`.

**Two types, fundamentally different:**

```bash
# Lightweight tag (just a pointer)
git tag v0.3.0

# What Git stores:
# .git/refs/tags/v0.3.0 contains: abc123def (commit SHA)
# That's it! Just a pointer, like a branch that never moves.

# Annotated tag (full Git object)
git tag -a v0.3.0 -m "Release version 0.3.0"

# What Git stores:
# 1. Tag object (new SHA) containing:
#    - Commit it points to
#    - Tagger name and email
#    - Tag date
#    - Tag message
#    - GPG signature (if signed)
# 2. .git/refs/tags/v0.3.0 points to this tag object
```

**Viewing tag objects:**

```bash
# Show tag details
git show v0.3.0

# For annotated tag, outputs:
# tag v0.3.0
# Tagger: Your Name <you@example.com>
# Date:   Fri Nov 17 10:30:00 2025 -0800
#
# Release version 0.3.0
#
# [Then shows the commit it points to]

# Show only tag metadata
git cat-file -p v0.3.0

# List all tags with annotations
git tag -n9
```

### 1.2 Why Annotated Tags Matter

**Lightweight tags are insufficient for releases because:**

1. **No metadata**: Who created this tag? When? Why?
2. **No message**: What's in this release?
3. **Can't be signed**: No cryptographic verification
4. **Not pushed by default**: `git push` doesn't include them

**Professional projects ALWAYS use annotated tags for releases.**

**Bibby Example:**

```bash
# ❌ BAD: Lightweight tag
git tag v0.3.0
# Just a pointer, no context

# ✅ GOOD: Annotated tag with detailed message
git tag -a v0.3.0 -m "Release version 0.3.0: DevOps Documentation

Features:
- Complete DevOps mentorship guide (10 sections)
- CI/CD best practices documentation
- Git workflows and branching strategies
- Build lifecycle fundamentals

This release focuses on educational content while maintaining
the stable CLI functionality from v0.2.1.

Changes since v0.2.1:
- Added docs/devops-mentorship/ directory
- Updated README with mentorship links
- No changes to core application code

Breaking Changes: None
Migration Required: None"

# Rich metadata for future reference
```

### 1.3 Signed Tags for Security

**Why sign tags?**

Prove that:
1. **You** created this tag (authentication)
2. The tag hasn't been tampered with (integrity)
3. This release is official (authorization)

**Critical for:**
- Open source projects (prevent fake releases)
- Enterprise software (compliance requirements)
- Security-sensitive applications
- Any production deployment

**Setting up GPG for tag signing:**

```bash
# Generate GPG key (if you don't have one)
gpg --full-generate-key
# Choose: RSA and RSA, 4096 bits, no expiration (or 2 years)

# List your GPG keys
gpg --list-secret-keys --keyid-format=long

# Output:
# sec   rsa4096/ABC123DEF456 2025-11-17 [SC]
#       A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0
# uid   Your Name <you@example.com>

# Configure Git to use your key
git config --global user.signingkey ABC123DEF456

# Configure Git to always sign tags
git config --global tag.gpgSign true
```

**Creating signed tags:**

```bash
# Manual signing
git tag -s v0.3.0 -m "Release version 0.3.0"

# With auto-signing enabled, just use -a
git tag -a v0.3.0 -m "Release version 0.3.0"

# Verify signature
git tag -v v0.3.0

# Output:
# object abc123def...
# type commit
# tag v0.3.0
# tagger Your Name <you@example.com> 1731862200 -0800
#
# Release version 0.3.0
# gpg: Signature made Fri Nov 17 10:30:00 2025 PST
# gpg: using RSA key ABC123DEF456
# gpg: Good signature from "Your Name <you@example.com>"
```

**Verifying tag integrity in deployment:**

```bash
# In your CI/CD pipeline
git clone https://github.com/leodvincci/Bibby.git
cd Bibby
git tag -v v0.3.0 || exit 1  # Fail if signature invalid

# Only deploy if tag is properly signed
```

### 1.4 Tag Naming Conventions Deep Dive

**Standard format: `v{MAJOR}.{MINOR}.{PATCH}[-{PRERELEASE}][+{BUILD}]`**

**Examples with context:**

```bash
# Standard releases
v0.3.0          # Release version 0.3.0
v1.0.0          # Major release (first stable)
v2.15.7         # Mature project (15 minor releases, 7 patches)

# Pre-release versions
v0.4.0-alpha.1  # First alpha for 0.4.0
v0.4.0-alpha.2  # Second alpha (after bugs found in alpha.1)
v0.4.0-beta.1   # First beta (feature complete)
v0.4.0-rc.1     # Release candidate 1
v0.4.0-rc.2     # Release candidate 2 (bugs found in rc.1)
v0.4.0          # Final release

# Build metadata (doesn't affect version precedence)
v0.3.0+20251117     # Built on November 17, 2025
v0.3.0+build.123    # Build number 123
v0.3.0+sha.abc123   # Includes commit SHA for traceability

# Hotfix releases
v0.3.1          # Patch release (bug fix)
v0.3.2          # Another patch
v1.0.1          # Hotfix for v1.0.0

# Multiple series in parallel
v0.3.5          # Latest on 0.3.x maintenance branch
v0.4.2          # Latest on 0.4.x maintenance branch
v1.0.0          # Latest stable release
```

**Special tags for Bibby project:**

```bash
# Release tags
git tag -a v0.3.0 -m "Release 0.3.0"

# Milestone tags (optional, for important commits)
git tag -a milestone/devops-guide-complete -m "Completed full DevOps mentorship guide"

# Demo tags (for presentations/interviews)
git tag -a demo/isbn-lookup -m "Demo: ISBN lookup feature"
git tag -a demo/browse-flow -m "Demo: Interactive browse flow"
```

### 1.5 Tag Management Commands

**Listing tags:**

```bash
# List all tags
git tag
git tag -l
git tag --list

# Filter tags with pattern
git tag -l "v0.3.*"
# Output: v0.3.0, v0.3.1, v0.3.2

# List with messages
git tag -n
git tag -n5  # Show up to 5 lines of message

# Sort tags by version (semantic sort)
git tag -l --sort=version:refname

# Sort by date
git tag -l --sort=-creatordate  # Newest first

# Show tags with commit info
git tag -l --format='%(refname:short) %(objectname:short) %(creatordate:short)'
```

**Creating tags:**

```bash
# Tag current commit
git tag -a v0.3.0 -m "Release 0.3.0"

# Tag specific commit
git tag -a v0.3.0 abc123def -m "Release 0.3.0"

# Tag with multi-line message
git tag -a v0.3.0 -m "Release 0.3.0: DevOps Documentation

Complete feature list:
- SDLC fundamentals
- Agile practices for solo developers
- Build lifecycle management
- CI/CD pipelines with GitHub Actions

See CHANGELOG.md for details."

# Or use editor for message
git tag -a v0.3.0
# Opens editor for tag message
```

**Pushing tags:**

```bash
# Push single tag
git push origin v0.3.0

# Push all tags
git push origin --tags

# Push only annotated tags (safer)
git push origin --follow-tags

# Configure to always push annotated tags
git config --global push.followTags true
```

**Deleting tags:**

```bash
# Delete local tag
git tag -d v0.3.0

# Delete remote tag
git push origin :refs/tags/v0.3.0
# Or
git push origin --delete v0.3.0

# Delete both local and remote
git tag -d v0.3.0 && git push origin --delete v0.3.0
```

**Moving/renaming tags (not recommended for released versions!):**

```bash
# Wrong tag name or commit? Delete and recreate
git tag -d v0.3.0
git tag -a v0.3.0 correctCommitSHA -m "Release 0.3.0"
git push origin :refs/tags/v0.3.0  # Delete remote
git push origin v0.3.0              # Push corrected tag

# ⚠️ WARNING: Only do this if tag hasn't been published yet!
# If others have pulled the tag, you'll cause confusion.
```

**Checking out tags:**

```bash
# Checkout tag (detached HEAD state)
git checkout v0.3.0

# Output:
# Note: switching to 'v0.3.0'.
# You are in 'detached HEAD' state...

# Create branch from tag
git checkout -b release/0.3.x v0.3.0

# Now you can make commits (e.g., for hotfixes)
```

---

## 2. Release Workflow Patterns

### 2.1 Feature Release Workflow (Minor/Major Version)

**Scenario:** You've completed work for Bibby v0.4.0 (new ISBN search feature).

**Complete workflow:**

```bash
# Step 1: Ensure you're on main with latest changes
git checkout main
git pull origin main

# Step 2: Verify everything works
mvn clean verify
# All tests pass ✓

# Step 3: Update version in pom.xml
mvn versions:set -DnewVersion=0.4.0
mvn versions:commit

# Step 4: Update CHANGELOG.md
vim CHANGELOG.md
# Move [Unreleased] items to [0.4.0] - 2025-11-17
# Update comparison links

# Step 5: Commit version bump
git add pom.xml CHANGELOG.md
git commit -m "chore: release version 0.4.0"

# Step 6: Create annotated tag
git tag -a v0.4.0 -m "Release version 0.4.0: ISBN Search

Features:
- ISBN lookup integration with OpenLibrary API
- Automatic book metadata population
- ISBN validation and formatting
- Offline ISBN cache for repeated lookups

Improvements:
- Enhanced error messages for API failures
- Added retry logic for network issues
- Improved test coverage to 85%

Breaking Changes: None

Upgrade: Direct upgrade from v0.3.x, no migration needed."

# Step 7: Push commit and tag
git push origin main
git push origin v0.4.0

# Step 8: Build release artifacts
mvn clean package -Pprod

# Step 9: Create GitHub Release
gh release create v0.4.0 \
  --title "v0.4.0 - ISBN Search Integration" \
  --notes "See CHANGELOG.md for full details" \
  ./target/Bibby-0.4.0.jar

# Step 10: Bump to next development version
mvn versions:set -DnewVersion=0.5.0-SNAPSHOT
mvn versions:commit

git add pom.xml
git commit -m "chore: prepare for next development iteration (0.5.0-SNAPSHOT)"
git push origin main

# Step 11: Verify release
git tag -v v0.4.0
# Check GitHub releases page
# Test JAR download
```

**Why this workflow works:**

- ✅ Clear version history
- ✅ Tagged at exact commit
- ✅ Artifacts attached to release
- ✅ Ready for next development
- ✅ Reproducible builds

### 2.2 Hotfix Workflow (Patch Version)

**Scenario:** Critical bug discovered in production (v0.3.0). Development is ongoing for v0.4.0 (not ready).

**The problem:**
```
main: v0.3.0 --- [WIP v0.4.0 features] --- [more WIP] (HEAD)
                                                        ^
                                                        Not ready to release!
```

**You need:** v0.3.1 with ONLY the bug fix, not v0.4.0 features.

**Hotfix workflow:**

```bash
# Step 1: Create hotfix branch from the release tag
git checkout -b hotfix/0.3.1 v0.3.0

# Now you're working from exactly v0.3.0 state
# No v0.4.0 WIP features included

# Step 2: Fix the bug
vim src/main/java/com/penrose/bibby/library/book/BookService.java
# Fix the critical bug

# Step 3: Write tests for the bug
vim src/test/java/com/penrose/bibby/library/book/BookServiceTest.java

# Step 4: Commit the fix
git add .
git commit -m "fix: prevent null pointer exception when shelf is deleted

- Add null check before accessing shelf.getId()
- Return meaningful error message to user
- Add test case for deleted shelf scenario

Fixes issue where deleting a shelf caused application crash
when books on that shelf were accessed.

This is a critical bug affecting production users."

# Step 5: Verify fix works
mvn clean verify
# All tests pass ✓

# Step 6: Update version to 0.3.1
mvn versions:set -DnewVersion=0.3.1
mvn versions:commit

# Step 7: Update CHANGELOG.md
vim CHANGELOG.md
# Add new [0.3.1] section with bug fix

git add pom.xml CHANGELOG.md
git commit -m "chore: bump version to 0.3.1"

# Step 8: Create tag for hotfix
git tag -a v0.3.1 -m "Hotfix release 0.3.1

Critical bug fix:
- Fixed null pointer exception when shelf is deleted

This patch release is recommended for all v0.3.0 users."

# Step 9: Push hotfix branch and tag
git push -u origin hotfix/0.3.1
git push origin v0.3.1

# Step 10: Build and release
mvn clean package -Pprod

gh release create v0.3.1 \
  --title "v0.3.1 - Critical Bug Fix" \
  --notes "**Critical bug fix:** Prevents crash when shelf is deleted. Recommended upgrade for all v0.3.0 users." \
  ./target/Bibby-0.3.1.jar

# Step 11: Merge fix back to main (critical!)
git checkout main
git merge hotfix/0.3.1

# If conflicts (because main has v0.4.0 work), resolve carefully
# The bug fix should be incorporated, but not the version numbers

git add .
git commit -m "chore: merge hotfix v0.3.1 into main"
git push origin main

# Step 12: Clean up
git branch -d hotfix/0.3.1
git push origin --delete hotfix/0.3.1

# Optional: Keep hotfix branch for maintenance
# (if you need to create v0.3.2 later)
```

**Result:**
```
v0.3.0 --- [v0.4.0 WIP] --- [more WIP] --- [merged fix] (main)
  |
  +--- [fix] v0.3.1 (hotfix/0.3.1, released)
```

**Key principles:**

1. **Branch from tag**: `git checkout -b hotfix/0.3.1 v0.3.0`
2. **Only critical fixes**: Don't add features
3. **Merge back**: Always merge hotfix to main
4. **Immediate release**: Hotfixes are urgent

### 2.3 Release Branch Strategy

**For mature projects**: Maintain long-term support (LTS) versions.

**Scenario:** Bibby reaches v1.0.0. You want to:
- Support v1.0.x with bug fixes
- Develop v1.1.0 features on main

**Setup release branches:**

```bash
# When v1.0.0 is ready
git checkout main
git tag -a v1.0.0 -m "Release 1.0.0: First stable release"
git push origin v1.0.0

# Create release branch for v1.0.x maintenance
git checkout -b release/1.0.x v1.0.0
git push -u origin release/1.0.x

# Continue development on main for v1.1.0
git checkout main
mvn versions:set -DnewVersion=1.1.0-SNAPSHOT
git commit -am "chore: prepare for v1.1.0 development"
git push origin main
```

**Now you have:**

```
main (1.1.0-SNAPSHOT)           Future features
  |
  |
  +--- release/1.0.x (1.0.0)    Maintenance/bug fixes only
         |
         +--- v1.0.0 (tag)
```

**Hotfix on release branch:**

```bash
# Bug found in v1.0.0
git checkout release/1.0.x

# Fix bug
vim src/main/java/...
git commit -m "fix: critical bug"

# Bump to v1.0.1
mvn versions:set -DnewVersion=1.0.1
git commit -am "chore: bump to v1.0.1"

# Tag
git tag -a v1.0.1 -m "Hotfix release 1.0.1"
git push origin release/1.0.x
git push origin v1.0.1

# Merge back to main
git checkout main
git merge release/1.0.x
git push origin main
```

**When to use release branches:**

- ✅ Maintaining multiple major versions (v1.x and v2.x)
- ✅ Long-term support (LTS) releases
- ✅ Enterprise customers on older versions
- ✅ Stabilization period before major release

**When NOT to use:**

- ❌ Early stage projects (like Bibby at v0.x.x)
- ❌ If you don't plan to support old versions
- ❌ Continuous delivery (every commit is releasable)

### 2.4 Pre-Release Workflow (Alpha/Beta/RC)

**Scenario:** v0.4.0 has major changes. You want feedback before official release.

**Alpha release (early testing):**

```bash
# Feature complete enough for early feedback
git checkout main

# Update version
mvn versions:set -DnewVersion=0.4.0-alpha.1
git commit -am "chore: release 0.4.0-alpha.1"

# Tag
git tag -a v0.4.0-alpha.1 -m "Alpha release 0.4.0-alpha.1

Early preview of ISBN search feature.

Warning: This is an alpha release.
- Features may be incomplete
- Expect bugs
- API may change before v0.4.0 final
- Do not use in production

Feedback welcome!"

git push origin main
git push origin v0.4.0-alpha.1

# Build and release
mvn clean package
gh release create v0.4.0-alpha.1 \
  --title "v0.4.0-alpha.1 - Early Preview" \
  --notes "**Alpha release** - for testing only" \
  --prerelease \
  ./target/Bibby-0.4.0-alpha.1.jar
```

**Beta release (feature complete):**

```bash
# All features done, needs testing
mvn versions:set -DnewVersion=0.4.0-beta.1
git commit -am "chore: release 0.4.0-beta.1"

git tag -a v0.4.0-beta.1 -m "Beta release 0.4.0-beta.1

Feature complete preview of v0.4.0.

All planned features implemented:
- ISBN search ✓
- Metadata auto-fill ✓
- ISBN validation ✓

Known issues:
- Performance needs optimization
- Error messages need improvement

API is stable but may have minor changes before final release."

git push origin main
git push origin v0.4.0-beta.1

gh release create v0.4.0-beta.1 \
  --prerelease \
  --notes "**Beta release** - feature complete, testing phase" \
  ./target/Bibby-0.4.0-beta.1.jar
```

**Release candidate (final testing):**

```bash
# Potential final release, one last check
mvn versions:set -DnewVersion=0.4.0-rc.1
git commit -am "chore: release 0.4.0-rc.1"

git tag -a v0.4.0-rc.1 -m "Release candidate 0.4.0-rc.1

Final testing before v0.4.0 release.

All features complete and tested.
All known bugs fixed.
API frozen - no changes unless critical bug found.

If no critical issues found, this will become v0.4.0."

git push origin main
git push origin v0.4.0-rc.1

gh release create v0.4.0-rc.1 \
  --prerelease \
  --notes "**Release Candidate** - final testing phase" \
  ./target/Bibby-0.4.0-rc.1.jar

# Wait 1 week for feedback...

# No issues? Release final version:
mvn versions:set -DnewVersion=0.4.0
git commit -am "chore: release version 0.4.0"
git tag -a v0.4.0 -m "Release version 0.4.0"
git push origin main v0.4.0
```

**Pre-release version progression:**

```
0.4.0-alpha.1 → 0.4.0-alpha.2 → 0.4.0-beta.1 → 0.4.0-rc.1 → 0.4.0
   (early)       (more fixes)    (complete)    (final test)  (release)
```

---

## 3. Tag-Based Deployment Strategies

### 3.1 Deploying from Tags (Not Branches)

**Anti-pattern: Deploying from branch**

```bash
# ❌ BAD: Deploy whatever is on main
git checkout main
git pull
mvn package
# Deploy to production

# Problem: "main" is a moving target!
# What exact version did we deploy?
# Can we reproduce this build?
```

**Best practice: Deploy from tags**

```bash
# ✅ GOOD: Deploy specific tagged version
git fetch --tags
git checkout v0.3.0
mvn package
# Deploy to production

# Exact version deployed: v0.3.0
# Reproducible: git checkout v0.3.0 always gives same code
```

### 3.2 Environment-Specific Tags

**Strategy for multiple environments:**

```bash
# Development environment (auto-deploy from main)
# No tags needed - deploys every commit

# Staging environment (deploy release candidates)
git tag -a staging/v0.4.0-rc.1 -m "Deployed to staging"
git push origin staging/v0.4.0-rc.1
# CI/CD detects staging/* tag and deploys to staging

# Production environment (deploy final releases only)
git tag -a prod/v0.4.0 v0.4.0 -m "Deployed to production"
git push origin prod/v0.4.0
# CI/CD detects prod/* tag and deploys to production
```

**GitHub Actions workflow for tag-based deployment:**

```yaml
# .github/workflows/deploy.yml
name: Deploy

on:
  push:
    tags:
      - 'prod/v*'
      - 'staging/v*'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Determine environment
        id: env
        run: |
          if [[ $GITHUB_REF == refs/tags/prod/* ]]; then
            echo "environment=production" >> $GITHUB_OUTPUT
            echo "version=${GITHUB_REF#refs/tags/prod/}" >> $GITHUB_OUTPUT
          elif [[ $GITHUB_REF == refs/tags/staging/* ]]; then
            echo "environment=staging" >> $GITHUB_OUTPUT
            echo "version=${GITHUB_REF#refs/tags/staging/}" >> $GITHUB_OUTPUT
          fi

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build
        run: mvn clean package -Pprod -DskipTests

      - name: Deploy to ${{ steps.env.outputs.environment }}
        run: |
          echo "Deploying version ${{ steps.env.outputs.version }} to ${{ steps.env.outputs.environment }}"
          # Your deployment commands here
          # scp target/Bibby.jar server:/opt/bibby/
          # ssh server "systemctl restart bibby"

      - name: Verify deployment
        run: |
          # Health check
          # curl https://bibby.example.com/health
```

**Usage:**

```bash
# Deploy v0.4.0 to staging
git tag -a staging/v0.4.0 v0.4.0 -m "Staging deployment"
git push origin staging/v0.4.0
# GitHub Actions deploys to staging environment

# Test in staging...

# Deploy to production (after verification)
git tag -a prod/v0.4.0 v0.4.0 -m "Production deployment"
git push origin prod/v0.4.0
# GitHub Actions deploys to production environment
```

### 3.3 Rollback Strategy with Tags

**Scenario:** v0.4.0 deployed to production has a critical issue.

**Quick rollback:**

```bash
# Current production: v0.4.0 (broken)
# Previous production: v0.3.0 (working)

# Option 1: Redeploy previous tag
git checkout v0.3.0
mvn package
# Deploy to production

# Update production tag to point to v0.3.0
git tag -f prod/current v0.3.0
git push -f origin prod/current

# Option 2: Use rollback tag
git tag -a prod/rollback-from-v0.4.0 v0.3.0 -m "Emergency rollback from v0.4.0"
git push origin prod/rollback-from-v0.4.0
# CI/CD detects rollback tag and deploys v0.3.0
```

**Track deployment history with tags:**

```bash
# Tag every production deployment
git tag -a deployed/prod/v0.3.0-20251115 v0.3.0 -m "Deployed to prod on 2025-11-15"
git tag -a deployed/prod/v0.4.0-20251117 v0.4.0 -m "Deployed to prod on 2025-11-17"
git tag -a deployed/prod/v0.3.0-20251117-rollback v0.3.0 -m "Rolled back to v0.3.0 due to issues in v0.4.0"

# View deployment history
git tag -l "deployed/prod/*" --sort=-creatordate
```

### 3.4 Blue-Green Deployment with Tags

**Strategy:** Run two production environments (blue and green), switch between them.

```bash
# Current: Blue environment runs v0.3.0
# Green environment empty

# Deploy v0.4.0 to green
git checkout v0.4.0
# Deploy to green environment
# Test green environment

# Switch traffic to green
# Update load balancer: route traffic to green

# Tag current production
git tag -a prod/blue/current v0.3.0 -m "Blue environment (standby)"
git tag -a prod/green/current v0.4.0 -m "Green environment (active)"

# If issues, switch back to blue instantly
# No redeployment needed - blue still runs v0.3.0

# Next release: Deploy v0.5.0 to blue, switch traffic
```

---

## 4. Advanced Tag Management

### 4.1 Tag Protection (GitHub)

**Protect release tags from accidental deletion or modification.**

**GitHub Settings:**

```
Repository → Settings → Tags → Protected tags

Add rule:
- Tag name pattern: v*
- Who can create: Maintainers only
- Prevent deletion: ✓
- Require signed commits: ✓
```

**Why protect tags:**

- Prevent accidental deletion of released versions
- Ensure only authorized users can create releases
- Maintain integrity of release history
- Compliance requirements

### 4.2 Tag Pruning and Cleanup

**Over time, you accumulate many tags. Clean up old pre-releases:**

```bash
# List all alpha/beta tags
git tag -l "*-alpha.*"
git tag -l "*-beta.*"
git tag -l "*-rc.*"

# Delete old pre-release tags (after final release)
git tag -d v0.4.0-alpha.1
git tag -d v0.4.0-alpha.2
git tag -d v0.4.0-beta.1
git tag -d v0.4.0-rc.1

git push origin :refs/tags/v0.4.0-alpha.1
git push origin :refs/tags/v0.4.0-alpha.2
git push origin :refs/tags/v0.4.0-beta.1
git push origin :refs/tags/v0.4.0-rc.1

# Keep only: v0.4.0 (final release)
```

**Or keep pre-releases for historical reference:**

```bash
# Tag final as "canonical" version
git tag -a v0.4.0 -m "Release version 0.4.0 (final)"

# Keep pre-releases but mark as historical
# Document in tag message that v0.4.0 is the release
```

**Automated cleanup script:**

```bash
#!/bin/bash
# cleanup-old-prereleases.sh

# Delete all alpha/beta/rc tags older than 6 months
# (After final release has been out)

CUTOFF_DATE=$(date -d "6 months ago" +%s)

git tag -l "*-alpha.*" "*-beta.*" "*-rc.*" | while read tag; do
  TAG_DATE=$(git log -1 --format=%ct "$tag")

  if [ "$TAG_DATE" -lt "$CUTOFF_DATE" ]; then
    echo "Deleting old pre-release tag: $tag"
    git tag -d "$tag"
    git push origin ":refs/tags/$tag"
  fi
done
```

### 4.3 Mirroring Tags Across Repositories

**Scenario:** You have a mirror repository (e.g., internal GitLab + public GitHub).

```bash
# Fetch all tags from origin
git fetch origin --tags

# Push all tags to mirror
git push mirror --tags

# Sync specific tag
git push mirror v0.4.0
```

**Automated sync:**

```bash
# .github/workflows/mirror-tags.yml
name: Mirror Tags

on:
  push:
    tags:
      - 'v*'

jobs:
  mirror:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0

      - name: Push to mirror
        run: |
          git remote add mirror ${{ secrets.MIRROR_REPO_URL }}
          git push mirror $GITHUB_REF
```

### 4.4 Tag Audit Trail

**Track who created which tags and when:**

```bash
# Show tag with tagger info
git show v0.4.0

# Output:
# tag v0.4.0
# Tagger: John Doe <john@example.com>
# Date:   Fri Nov 17 14:30:00 2025 -0800

# List all tags with creator and date
git tag -l --format='%(refname:short)|%(creatordate:short)|%(taggername)|%(subject)'

# Output:
# v0.3.0|2025-11-15|John Doe|Release version 0.3.0
# v0.4.0|2025-11-17|Jane Smith|Release version 0.4.0

# Find tags created by specific person
git tag -l --format='%(taggername)|%(refname:short)' | grep "John Doe"
```

**Create tag audit report:**

```bash
#!/bin/bash
# tag-audit.sh

echo "Tag Audit Report"
echo "================"
echo ""
echo "Tag | Date | Author | Commit"
echo "--- | ---- | ------ | ------"

git tag -l "v*" --sort=-creatordate | while read tag; do
  DATE=$(git tag -l "$tag" --format='%(creatordate:short)')
  AUTHOR=$(git tag -l "$tag" --format='%(taggername)')
  COMMIT=$(git rev-parse --short "$tag")
  echo "$tag | $DATE | $AUTHOR | $commit"
done
```

---

## 5. Release Automation

### 5.1 Automated Release Workflow with GitHub Actions

**Complete automated release pipeline:**

```yaml
# .github/workflows/release.yml
name: Release

on:
  push:
    tags:
      - 'v*.*.*'

jobs:
  release:
    runs-on: ubuntu-latest

    permissions:
      contents: write  # Required for creating releases

    steps:
      - name: Checkout code
        uses: actions/checkout@v3
        with:
          fetch-depth: 0  # Full history for changelog

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - name: Extract version from tag
        id: version
        run: |
          VERSION=${GITHUB_REF#refs/tags/v}
          echo "version=$VERSION" >> $GITHUB_OUTPUT
          echo "tag=${GITHUB_REF#refs/tags/}" >> $GITHUB_OUTPUT

      - name: Verify version matches pom.xml
        run: |
          POM_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
          if [ "$POM_VERSION" != "${{ steps.version.outputs.version }}" ]; then
            echo "Error: Tag version (${{ steps.version.outputs.version }}) doesn't match pom.xml ($POM_VERSION)"
            exit 1
          fi

      - name: Run tests
        run: mvn clean verify

      - name: Build release artifacts
        run: mvn package -Pprod -DskipTests

      - name: Generate changelog
        id: changelog
        run: |
          # Extract changelog for this version
          sed -n "/## \[${{ steps.version.outputs.version }}\]/,/## \[/p" CHANGELOG.md | sed '$d' > release-notes.md

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          name: Release ${{ steps.version.outputs.tag }}
          body_path: release-notes.md
          draft: false
          prerelease: ${{ contains(steps.version.outputs.version, '-') }}
          files: |
            target/Bibby-${{ steps.version.outputs.version }}.jar
            target/Bibby-${{ steps.version.outputs.version }}-sources.jar
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

      - name: Update latest tag
        run: |
          git tag -f latest
          git push -f origin latest

  notify:
    needs: release
    runs-on: ubuntu-latest
    steps:
      - name: Send notification
        run: |
          echo "Release ${{ github.ref_name }} published!"
          # Add Slack/Discord webhook notification here
```

**Trigger release:**

```bash
# Update version
mvn versions:set -DnewVersion=0.4.0
mvn versions:commit

# Update CHANGELOG.md

# Commit and tag
git add pom.xml CHANGELOG.md
git commit -m "chore: release version 0.4.0"
git tag -a v0.4.0 -m "Release version 0.4.0"

# Push (triggers automated release)
git push origin main
git push origin v0.4.0

# GitHub Actions automatically:
# 1. Runs tests
# 2. Builds JAR
# 3. Extracts changelog
# 4. Creates GitHub Release
# 5. Uploads artifacts
# 6. Sends notifications
```

### 5.2 Semantic Release (Fully Automated)

**Tool: `semantic-release`** - Automates version numbers and changelogs based on commit messages.

**Setup:**

```bash
# Install semantic-release
npm install --save-dev semantic-release @semantic-release/git @semantic-release/changelog

# Create configuration
cat > .releaserc.json <<EOF
{
  "branches": ["main"],
  "plugins": [
    "@semantic-release/commit-analyzer",
    "@semantic-release/release-notes-generator",
    "@semantic-release/changelog",
    "@semantic-release/github",
    "@semantic-release/git"
  ]
}
EOF
```

**Commit message convention:**

```bash
# PATCH release (0.3.0 → 0.3.1)
fix: correct shelf assignment bug

# MINOR release (0.3.0 → 0.4.0)
feat: add ISBN search functionality

# MAJOR release (0.3.0 → 1.0.0)
feat!: redesign API for book management

BREAKING CHANGE: BookService.addBook() now returns BookResponse instead of BookEntity
```

**GitHub Actions workflow:**

```yaml
# .github/workflows/semantic-release.yml
name: Semantic Release

on:
  push:
    branches:
      - main

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm ci
      - run: npx semantic-release
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

**How it works:**

1. You commit with conventional format: `feat: add ISBN search`
2. Push to main
3. Semantic-release analyzes commits since last tag
4. Determines version bump (major/minor/patch)
5. Updates CHANGELOG.md
6. Creates git tag
7. Creates GitHub Release
8. All automated!

**No manual version management needed.**

### 5.3 Release Checklist Automation

**Pre-release validation script:**

```bash
#!/bin/bash
# scripts/validate-release.sh

set -e

echo "🔍 Release Validation Checklist"
echo "================================"

# Check 1: Clean working directory
echo "✓ Checking working directory..."
if [ -n "$(git status --porcelain)" ]; then
  echo "❌ Working directory not clean. Commit or stash changes."
  exit 1
fi
echo "✅ Working directory clean"

# Check 2: On main branch
BRANCH=$(git branch --show-current)
if [ "$BRANCH" != "main" ]; then
  echo "❌ Not on main branch (currently on $BRANCH)"
  exit 1
fi
echo "✅ On main branch"

# Check 3: Up to date with remote
git fetch origin
if [ "$(git rev-parse HEAD)" != "$(git rev-parse origin/main)" ]; then
  echo "❌ Local main is not up to date with origin/main"
  exit 1
fi
echo "✅ Up to date with remote"

# Check 4: All tests pass
echo "✓ Running tests..."
if ! mvn clean verify > /dev/null 2>&1; then
  echo "❌ Tests failed"
  exit 1
fi
echo "✅ All tests pass"

# Check 5: CHANGELOG.md updated
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
if ! grep -q "## \[$VERSION\]" CHANGELOG.md; then
  echo "❌ CHANGELOG.md not updated for version $VERSION"
  exit 1
fi
echo "✅ CHANGELOG.md updated"

# Check 6: No SNAPSHOT version
if [[ "$VERSION" == *-SNAPSHOT ]]; then
  echo "❌ Version is SNAPSHOT: $VERSION"
  echo "   Run: mvn versions:set -DnewVersion=${VERSION%-SNAPSHOT}"
  exit 1
fi
echo "✅ Version is release version: $VERSION"

# Check 7: No existing tag
if git rev-parse "v$VERSION" > /dev/null 2>&1; then
  echo "❌ Tag v$VERSION already exists"
  exit 1
fi
echo "✅ Tag v$VERSION does not exist"

# Check 8: Dependencies up to date
echo "✓ Checking for outdated dependencies..."
OUTDATED=$(mvn versions:display-dependency-updates | grep " -> " | wc -l)
if [ "$OUTDATED" -gt 0 ]; then
  echo "⚠️  Warning: $OUTDATED dependencies have updates available"
  echo "   Run: mvn versions:display-dependency-updates"
else
  echo "✅ All dependencies up to date"
fi

# Check 9: Code quality
echo "✓ Running code quality checks..."
if ! mvn checkstyle:check spotbugs:check > /dev/null 2>&1; then
  echo "❌ Code quality checks failed"
  exit 1
fi
echo "✅ Code quality checks pass"

# Check 10: Build artifacts
echo "✓ Building release artifacts..."
if ! mvn clean package -Pprod -DskipTests > /dev/null 2>&1; then
  echo "❌ Build failed"
  exit 1
fi
echo "✅ Build successful"

echo ""
echo "================================"
echo "✅ All checks passed!"
echo ""
echo "Ready to release v$VERSION"
echo ""
echo "Next steps:"
echo "  1. git tag -a v$VERSION -m \"Release version $VERSION\""
echo "  2. git push origin main"
echo "  3. git push origin v$VERSION"
```

**Use in release process:**

```bash
# Before creating tag
./scripts/validate-release.sh

# If all checks pass, proceed with release
git tag -a v0.4.0 -m "Release version 0.4.0"
git push origin main v0.4.0
```

---

## 6. Troubleshooting and Recovery

### 6.1 "I tagged the wrong commit!"

**Scenario:** You created v0.4.0 tag on commit A, but meant to tag commit B.

**Solution (if not pushed yet):**

```bash
# Delete local tag
git tag -d v0.4.0

# Create tag on correct commit
git tag -a v0.4.0 abc123def -m "Release version 0.4.0"

# Push
git push origin v0.4.0
```

**Solution (if already pushed):**

```bash
# Delete remote tag
git push origin :refs/tags/v0.4.0

# Delete local tag
git tag -d v0.4.0

# Create tag on correct commit
git tag -a v0.4.0 abc123def -m "Release version 0.4.0"

# Force push (communicate with team!)
git push origin v0.4.0

# Notify team: "Corrected v0.4.0 tag, please delete and re-fetch"
```

### 6.2 "Tag and pom.xml version don't match!"

**Problem:**

```bash
# pom.xml says: 0.4.0
# Git tag says: v0.4.1
```

**Solution:**

```bash
# Determine which is correct
# Option 1: pom.xml is wrong, update it
mvn versions:set -DnewVersion=0.4.1
git commit -am "fix: correct version in pom.xml to match tag"
git push origin main

# Option 2: Tag is wrong, recreate it
git tag -d v0.4.1
git push origin :refs/tags/v0.4.1
git tag -a v0.4.0 -m "Release version 0.4.0"
git push origin v0.4.0
```

**Prevention:** Automate version verification (see Section 5.1 workflow).

### 6.3 "I need to delete a published release"

**Scenario:** v0.4.0 released with critical bug, must be yanked.

**Don't delete the tag** (breaks reproducibility). Instead:

```bash
# Create patch release immediately
git checkout -b hotfix/0.4.1 v0.4.0
# Fix bug
git commit -m "fix: critical bug"
mvn versions:set -DnewVersion=0.4.1
git commit -am "chore: bump to v0.4.1"
git tag -a v0.4.1 -m "Hotfix release 0.4.1"
git push origin hotfix/0.4.1 v0.4.1

# Mark v0.4.0 as deprecated on GitHub
gh release edit v0.4.0 \
  --notes "**DEPRECATED:** This release has a critical bug. Please upgrade to v0.4.1 immediately."

# Or delete GitHub Release (not tag)
gh release delete v0.4.0

# Tag remains in Git history for reproducibility
# But users are directed to v0.4.1
```

### 6.4 "Multiple people tagged the same version"

**Problem:** Concurrent releases, two v0.4.0 tags pointing to different commits.

**Detection:**

```bash
# Fetch from multiple remotes
git fetch origin
git fetch colleague

# Compare
git rev-parse v0.4.0
git rev-parse colleague/v0.4.0

# Different SHAs!
```

**Resolution:**

```bash
# Determine which is correct (communicate with team)
# Delete incorrect tags

# If origin is correct:
git push colleague :refs/tags/v0.4.0
# Colleague fetches: git fetch --tags

# If colleague is correct:
git tag -d v0.4.0
git fetch colleague
git tag v0.4.0 colleague/v0.4.0
git push origin v0.4.0
```

**Prevention:**
- Protected tags (GitHub)
- Only one person releases
- Automated releases (CI/CD)

---

## 7. Best Practices Summary

### ✅ DO:

1. **Use annotated tags** for all releases
   ```bash
   git tag -a v0.4.0 -m "Release version 0.4.0"
   ```

2. **Sign tags** for security-critical projects
   ```bash
   git tag -s v0.4.0 -m "Release version 0.4.0"
   ```

3. **Branch from tags** for hotfixes
   ```bash
   git checkout -b hotfix/0.3.1 v0.3.0
   ```

4. **Deploy from tags**, not branches
   ```bash
   git checkout v0.4.0  # Exact version
   ```

5. **Merge hotfixes back** to main
   ```bash
   git merge hotfix/0.3.1
   ```

6. **Protect release tags** on GitHub

7. **Document releases** in CHANGELOG.md

8. **Automate releases** with CI/CD

9. **Verify before tagging**
   ```bash
   mvn clean verify  # All tests pass
   ```

10. **Use semantic versioning**
    ```bash
    v{MAJOR}.{MINOR}.{PATCH}
    ```

### ❌ DON'T:

1. **Don't use lightweight tags** for releases
   ```bash
   git tag v0.4.0  # Missing metadata!
   ```

2. **Don't tag SNAPSHOT versions**
   ```bash
   git tag v0.4.0-SNAPSHOT  # Wrong!
   ```

3. **Don't delete published tags** (breaks reproducibility)

4. **Don't reuse version numbers** (immutability)

5. **Don't tag without testing**
   ```bash
   # Always run tests first!
   mvn clean verify
   ```

6. **Don't include WIP in releases**
   ```bash
   # No uncommitted changes when tagging
   git status  # Should be clean
   ```

7. **Don't forget to push tags**
   ```bash
   git push origin v0.4.0  # Don't forget!
   ```

8. **Don't mix version formats**
   ```bash
   # ❌ Inconsistent
   v0.3.0
   0.4.0
   release-0.5.0
   ```

9. **Don't create tags on feature branches** (only on main/release branches)

10. **Don't skip CHANGELOG updates**

---

## 8. Real-World Bibby Scenarios

### Scenario 1: First Production Release

**Context:** Bibby v0.5.0 is ready for production use.

```bash
# Stabilize: No new features, only bug fixes
git checkout -b release/0.5.x

# Final testing
mvn clean verify
# Manual testing...

# Release candidate
mvn versions:set -DnewVersion=0.5.0-rc.1
git commit -am "chore: release candidate 0.5.0-rc.1"
git tag -a v0.5.0-rc.1 -m "Release candidate 0.5.0-rc.1"
git push origin release/0.5.x v0.5.0-rc.1

# One week of testing...
# Bug found! Fix it on release/0.5.x

git commit -m "fix: critical bug"
mvn versions:set -DnewVersion=0.5.0-rc.2
git tag -a v0.5.0-rc.2 -m "Release candidate 0.5.0-rc.2"

# Another week... no issues!

# Final release
mvn versions:set -DnewVersion=0.5.0
git commit -am "chore: release version 0.5.0 - First Production Release"
git tag -a v0.5.0 -s -m "Release version 0.5.0: First Production Release

This is the first production-ready release of Bibby.

All planned features are complete and tested:
- Book management (add, search, checkout)
- Bookcase/shelf organization
- ISBN lookup integration
- Interactive CLI with browse flow
- Comprehensive test coverage (85%)

Recommended for production use.

Breaking Changes: None
Migration: None required"

git push origin release/0.5.x v0.5.0

# Merge back to main
git checkout main
git merge release/0.5.x
git push origin main

# Continue development
mvn versions:set -DnewVersion=0.6.0-SNAPSHOT
git commit -am "chore: prepare for v0.6.0 development"
git push origin main
```

### Scenario 2: Emergency Hotfix

**Context:** Production (v0.5.0) has critical bug. Main has v0.6.0 work in progress.

```bash
# Create hotfix from production tag
git checkout -b hotfix/0.5.1 v0.5.0

# Fix the bug
vim src/main/java/.../BookService.java
git commit -m "fix: prevent data loss on concurrent checkout"

# Test thoroughly
mvn clean verify

# Update version and release
mvn versions:set -DnewVersion=0.5.1
git commit -am "chore: hotfix release 0.5.1"
git tag -a v0.5.1 -s -m "Hotfix release 0.5.1

Critical bug fix:
- Prevents data loss on concurrent book checkout operations

Recommended immediate upgrade for all v0.5.0 users."

git push origin hotfix/0.5.1 v0.5.1

# Build and deploy immediately
mvn clean package -Pprod
# Deploy to production

# Merge to release branch
git checkout release/0.5.x
git merge hotfix/0.5.1
git push origin release/0.5.x

# Merge to main
git checkout main
git merge hotfix/0.5.1
# Resolve conflicts (version numbers)
git push origin main

# Delete hotfix branch
git branch -d hotfix/0.5.1
git push origin --delete hotfix/0.5.1
```

### Scenario 3: Beta Testing Program

**Context:** v0.6.0 has major changes, want community feedback.

```bash
# Create beta release
git checkout main
mvn versions:set -DnewVersion=0.6.0-beta.1
git commit -am "chore: beta release 0.6.0-beta.1"
git tag -a v0.6.0-beta.1 -m "Beta release 0.6.0-beta.1

New features for testing:
- Multi-library support
- Import/export functionality
- Advanced search filters

Please report issues: https://github.com/leodvincci/Bibby/issues"

git push origin main v0.6.0-beta.1

# Create pre-release on GitHub
gh release create v0.6.0-beta.1 \
  --title "v0.6.0-beta.1 - Beta Testing" \
  --notes "Beta release - please test and report issues!" \
  --prerelease \
  ./target/Bibby-0.6.0-beta.1.jar

# Collect feedback for 2 weeks...
# Fix bugs, release beta.2, beta.3...

# Final release
mvn versions:set -DnewVersion=0.6.0
git commit -am "chore: release version 0.6.0"
git tag -a v0.6.0 -s -m "Release version 0.6.0"
git push origin main v0.6.0
```

---

## 9. Interview Questions & Answers

### Q: "How do you manage releases in Git?"

**Strong Answer:**

"I use Git tags to mark specific commits as releases, following semantic versioning. For every release, I create an annotated tag with a detailed message documenting the changes.

My typical workflow is:
1. Complete features on a feature branch
2. Merge to main after code review
3. Run full test suite to ensure stability
4. Update version in pom.xml and CHANGELOG.md
5. Create an annotated, signed tag: `git tag -s v0.4.0 -m "Release 0.4.0"`
6. Push the tag, which triggers our CI/CD pipeline
7. GitHub Actions automatically builds artifacts and creates a GitHub Release

For hotfixes, I branch from the release tag, apply the fix, create a patch version tag, and merge back to main to ensure the fix is included in future releases.

In my Bibby project, I've set up automated release workflows using GitHub Actions that verify tests pass, build the JAR, and publish releases whenever I push a tag matching the pattern `v*.*.*`."

**Why it's good:**
- Specific process
- Mentions tools and patterns
- Real project example
- Shows understanding of hotfix workflow

### Q: "What's the difference between a lightweight and annotated tag?"

**Strong Answer:**

"A lightweight tag is just a pointer to a commit—essentially a name for a commit SHA. It contains no additional metadata.

An annotated tag is a full Git object that includes:
- The tagger's name and email
- The date it was created
- A tag message
- Optional GPG signature for verification

I always use annotated tags for releases because:
1. They provide context about why the tag was created
2. They can be cryptographically signed for security
3. They're pushed with `git push --follow-tags` for safer defaults
4. They support detailed release notes

For example, in Bibby, I might create:
```
git tag -a v0.4.0 -m "Release 0.4.0: ISBN Search

Features: ISBN lookup integration
Bug fixes: Shelf assignment correction"
```

This gives future developers—or myself six months from now—clear context about what this release contained."

**Why it's good:**
- Clear technical distinction
- Explains practical benefits
- Shows awareness of security (signing)
- Personal project example

### Q: "How would you roll back a bad release?"

**Strong Answer:**

"My rollback strategy depends on the situation:

For immediate production issues, I'd deploy the previous known-good version. Since I tag every release, I can quickly checkout the previous tag:
```
git checkout v0.3.0
mvn package
# Deploy to production
```

Then I'd create a hotfix branch from that tag to address the issue:
```
git checkout -b hotfix/0.3.1 v0.3.0
# Fix the bug
git commit -m "fix: critical issue"
git tag -a v0.3.1 -m "Hotfix release"
```

I specifically **don't** delete the bad release tag because that breaks reproducibility. Instead, I mark it as deprecated on GitHub and direct users to the fixed version.

If the issue is less critical, I might create a revert commit on main:
```
git revert <bad-commit>
git tag -a v0.4.1 -m "Reverts problematic changes from v0.4.0"
```

The key is having a clear deployment history through tags so you can always return to a known-good state. In my Bibby project, I maintain deployment tags like `prod/v0.4.0` that track exactly what's running in each environment."

**Why it's good:**
- Multiple strategies for different scenarios
- Explains why not to delete tags
- Shows understanding of git commands
- Mentions environment tracking

---

## 10. Summary & Next Steps

### Key Takeaways

✅ **Tags are release markers:**
- Use annotated tags for all releases
- Sign tags for security
- Never delete published tags

✅ **Workflows matter:**
- Feature releases: branch, test, tag, release
- Hotfixes: branch from tag, fix, merge back
- Pre-releases: alpha → beta → RC → final

✅ **Deploy from tags:**
- Tags are immutable (unlike branches)
- Tag-based deployments are reproducible
- Track deployments with environment-specific tags

✅ **Automate everything:**
- GitHub Actions for releases
- Semantic-release for version management
- Pre-release validation scripts

✅ **Plan for failure:**
- Rollback procedures
- Hotfix workflows
- Recovery from mistakes

### Practice Exercises

**Exercise 1: Complete Release Cycle**

```bash
# Simulate full release workflow for Bibby v0.4.0
git checkout -b feature/isbn-search
# Make commits...
git checkout main
git merge feature/isbn-search

# Release process
mvn versions:set -DnewVersion=0.4.0
# Update CHANGELOG
git commit -am "chore: release version 0.4.0"
git tag -a v0.4.0 -m "Release 0.4.0"
git push origin main v0.4.0

# Bump to next SNAPSHOT
mvn versions:set -DnewVersion=0.5.0-SNAPSHOT
git commit -am "chore: prepare for v0.5.0"
git push origin main
```

**Exercise 2: Hotfix Workflow**

```bash
# Simulate production bug
git checkout -b hotfix/0.4.1 v0.4.0
# Fix bug...
git commit -m "fix: critical bug"
mvn versions:set -DnewVersion=0.4.1
git tag -a v0.4.1 -m "Hotfix 0.4.1"

# Merge back
git checkout main
git merge hotfix/0.4.1
git push origin main v0.4.1
```

**Exercise 3: Pre-Release Workflow**

```bash
# Create alpha release
mvn versions:set -DnewVersion=0.5.0-alpha.1
git tag -a v0.5.0-alpha.1 -m "Alpha release"

# After testing, beta
mvn versions:set -DnewVersion=0.5.0-beta.1
git tag -a v0.5.0-beta.1 -m "Beta release"

# Final release
mvn versions:set -DnewVersion=0.5.0
git tag -a v0.5.0 -m "Final release"
```

### Immediate Actions for Bibby

**Week 1: Standardize Current Releases (2 hours)**

```bash
# 1. Review current state
git tag -l
mvn help:evaluate -Dexpression=project.version -q -DforceStdout

# 2. Create missing tags retroactively
# Find key commits in history
git log --oneline --all

# Tag important milestones
git tag -a v0.1.0 <commit> -m "Initial release"
git tag -a v0.2.0 <commit> -m "Browse flow release"
git tag -a v0.3.0 <commit> -m "DevOps guide release"

# 3. Push all tags
git push origin --tags

# 4. Create GitHub Releases for each tag
gh release create v0.3.0 --title "v0.3.0" --notes "See CHANGELOG.md"
```

**Week 2: Set Up Automation (3 hours)**

```bash
# 1. Create release workflow
mkdir -p .github/workflows
# Copy workflow from Section 5.1

# 2. Create validation script
mkdir -p scripts
# Copy validation script from Section 5.3

# 3. Test automation
git tag -a v0.3.1-test -m "Test automation"
git push origin v0.3.1-test
# Verify GitHub Actions runs
git push origin :refs/tags/v0.3.1-test  # Delete test tag
```

**Week 3: Document Process (1 hour)**

```bash
# Create release documentation
cat > docs/RELEASE_PROCESS.md <<EOF
# Bibby Release Process

## Prerequisites
- [ ] All tests passing
- [ ] CHANGELOG.md updated
- [ ] Version decided (semver)

## Steps
1. Run validation: ./scripts/validate-release.sh
2. Update version: mvn versions:set -DnewVersion=X.Y.Z
3. Commit: git commit -am "chore: release vX.Y.Z"
4. Tag: git tag -a vX.Y.Z -m "Release X.Y.Z"
5. Push: git push origin main vX.Y.Z
6. Monitor GitHub Actions for automated release

## Hotfix Process
1. Branch from tag: git checkout -b hotfix/X.Y.Z vX.Y.Z-1
2. Fix bug
3. Follow release steps
4. Merge to main
EOF

git add docs/RELEASE_PROCESS.md
git commit -m "docs: add release process documentation"
```

### Coming Up in Section 12

**Git Hooks & Automation:**
- Client-side hooks (pre-commit, pre-push)
- Server-side hooks (pre-receive, post-receive)
- Enforcing standards automatically
- Custom validation and checks
- Integrating with CI/CD
- Husky and lint-staged for Node projects
- Maven hooks for Java projects

You'll learn how to automate quality checks and enforce standards before code even reaches your repository.

---

**Section 11 Complete:** You now have professional-grade release management skills. You can confidently version, tag, release, and roll back software like experienced DevOps engineers! 🏷️🚀
