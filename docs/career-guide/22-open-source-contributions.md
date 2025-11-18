# Section 22: Open Source Contributions

**Week 22 Focus:** Building Credibility Through Contributing to Established Projects

---

## Introduction: Why Open Source Matters

Open source contributions are visible proof of your skills.

**What open source contributions show:**

1. **Real-world collaboration:** You can work with existing codebases, not just greenfield projects
2. **Code review skills:** You understand how to give/receive feedback professionally
3. **Communication:** You can articulate technical decisions in writing
4. **Initiative:** You contribute without being paid—shows genuine interest
5. **Public portfolio:** GitHub shows your contributions to anyone who looks

**For career changers especially:**

Traditional candidates have professional work they can point to. You don't (yet). Open source fills that gap. When recruiters check your GitHub and see contributions to Spring Boot, PostgreSQL, or other established projects, you're no longer "just a bootcamp grad"—you're someone who contributes to the ecosystem.

**The hiring signal:**

- Developers with open source contributions get 15-20% more interview requests
- 72% of hiring managers view open source contributions positively
- GitHub contribution graph shows consistency (green squares = you code regularly)

**But here's the truth:**

Most open source contributions are NOT major features. They're documentation fixes, bug reports, test additions, example improvements. That's fine. That's how everyone starts.

**This week's mission:** Make your first open source contribution, understand the contribution workflow, identify projects worth your time, and establish yourself as someone who gives back to the community.

---

## Part 1: Types of Open Source Contributions

Code contributions get the attention, but non-code contributions build relationships.

### Non-Code Contributions (Start Here)

**1. Documentation Improvements**

**What it is:** Fixing typos, clarifying confusing sections, adding examples to docs

**Examples:**
- Spring Boot documentation has a typo → you submit PR fixing it
- PostgreSQL docs explain connection pooling but no example → you add example
- Redis configuration guide is outdated → you update with current best practices

**Why start here:**
- Low technical barrier (no complex code changes)
- High acceptance rate (maintainers love doc improvements)
- Teaches you the PR process
- Gets your name in contributor list

**Time investment:** 1-3 hours per contribution

**Skill demonstrated:** Attention to detail, writing clarity, helpfulness

**2. Bug Reports**

**What it is:** Finding bugs, reproducing them, reporting with clear steps

**Good bug report components:**
- Clear title: "NPE when calling @Cacheable method with null parameter"
- Steps to reproduce (code example)
- Expected behavior vs actual behavior
- Environment details (Java version, Spring Boot version, OS)
- Stack trace or error message

**Why it matters:**
- Helps maintainers fix bugs faster
- Shows you use the project seriously
- Often leads to you fixing the bug yourself (next step)

**Time investment:** 1-2 hours per bug report

**Skill demonstrated:** Debugging, communication, thoroughness

**3. Issue Triage and Discussion**

**What it is:** Helping maintainers manage issues by confirming bugs, asking clarifying questions, suggesting solutions

**Examples:**
- Someone reports vague bug → you ask for reproduction steps
- Two issues report same bug → you link them and suggest closing duplicate
- New feature request → you discuss trade-offs and implementation approaches

**Why it matters:**
- Maintainers have limited time—you're multiplying their impact
- Shows you understand the project deeply
- Builds relationship with maintainers

**Time investment:** 30 min - 1 hour per issue

**Skill demonstrated:** Technical judgment, collaboration, project understanding

**4. Examples and Tutorials**

**What it is:** Adding example code to project repos, often in `/examples` or `/samples` directories

**Examples:**
- Spring Boot repo has REST API example, but no Redis caching example → you add one
- PostgreSQL has basic connection example, but no connection pooling example → you add it
- Project has Java 8 examples but not Java 17 → you modernize them

**Why it matters:**
- New users need examples to get started
- Showcases your understanding of the library
- Often leads to higher visibility (examples are heavily used)

**Time investment:** 3-6 hours per example

**Skill demonstrated:** Teaching ability, practical application, completeness

### Code Contributions (After 2-3 Non-Code Contributions)

**5. Bug Fixes**

**What it is:** Fixing bugs you found or bugs reported by others

**Difficulty:** Medium

**Best first bugs:**
- Labeled "good first issue" or "beginner-friendly"
- Small scope (affects single class or method)
- Clear reproduction steps
- Test already exists (you just need to fix code to make test pass)

**Why it matters:**
- Proves you can fix real problems
- Shows you can navigate unfamiliar codebase
- Maintainers appreciate functional improvements

**Time investment:** 4-10 hours (research, fix, test, submit PR)

**Skill demonstrated:** Debugging, problem-solving, testing

**6. Feature Additions**

**What it is:** Implementing new functionality requested in issues

**Difficulty:** Medium-high

**Best first features:**
- Small, self-contained features
- Already discussed and approved by maintainers (check issue comments)
- Clear requirements
- Similar to existing features (you can copy patterns)

**Why it matters:**
- Shows you can build, not just fix
- Demonstrates architectural understanding
- High impact (features get used by thousands)

**Time investment:** 10-30 hours (design, implement, test, document, PR)

**Skill demonstrated:** Software design, implementation, completeness

**7. Test Additions**

**What it is:** Adding tests to improve code coverage or test edge cases

**Difficulty:** Low-medium

**Examples:**
- Project has 60% test coverage → you add tests to increase it
- Method has happy path test but no edge case tests → you add them
- Integration tests missing → you add end-to-end tests

**Why it matters:**
- Improves project quality
- Shows you value testing (many developers don't)
- Lower barrier than feature work

**Time investment:** 2-6 hours per test contribution

**Skill demonstrated:** Testing skill, thoroughness, quality focus

### Recommendation for Leo

**Week 22: Start with documentation improvements (2-3 contributions)**

**Why:**
- Builds confidence
- Teaches PR workflow
- Gets your name in Spring Boot / PostgreSQL / Redis contributor lists
- Low risk of rejection

**Week 23-24: Graduate to bug reports and issue triage**

**Month 3-4: First bug fix or test addition**

**Month 5-6: Small feature contribution**

**Don't rush to code contributions. Build relationships first.**

---

## Part 2: Finding Projects to Contribute To

Contribute to projects you actually use. Passion shows.

### Strategy 1: Contribute to Your Stack

**Projects you use in Bibby:**

1. **Spring Boot** (https://github.com/spring-projects/spring-boot)
   - 70K+ stars, very active
   - Great documentation, clear contribution guidelines
   - "good first issue" label

2. **Spring Data JPA** (https://github.com/spring-projects/spring-data-jpa)
   - Part of Spring ecosystem
   - Less intimidating than core Spring Framework
   - Excellent for learning JPA internals

3. **PostgreSQL** (https://github.com/postgres/postgres)
   - Massive project, can be intimidating
   - Excellent documentation
   - Start with doc contributions or examples

4. **Redis** (https://github.com/redis/redis)
   - C codebase (different from Java)
   - Documentation and examples are good starting point
   - Java client libraries might be better fit (Lettuce, Jedis)

5. **Lettuce** (https://github.com/lettuce-io/lettuce-core)
   - Redis Java client
   - Smaller project, more approachable
   - Good for first contributions

**Pick 1-2 to focus on. Don't spread thin.**

### Strategy 2: Find Beginner-Friendly Projects

**Indicators of beginner-friendly projects:**

1. **"Good first issue" or "beginner-friendly" labels**
   - GitHub has label filter
   - These issues are explicitly marked for newcomers

2. **Active maintainers who respond quickly**
   - Check issue comments
   - Do maintainers respond within 1-2 days?
   - Are they helpful or dismissive?

3. **Clear CONTRIBUTING.md file**
   - Explains how to set up development environment
   - Describes PR process
   - Lists coding standards

4. **Recent activity**
   - Last commit within 1-2 weeks
   - Issues being closed regularly
   - PRs being merged

5. **Reasonable size**
   - 100-10,000 stars is sweet spot
   - Too small (<100): might be abandoned
   - Too large (>50K): can be overwhelming

**Websites for finding beginner-friendly issues:**

- **Good First Issue** (https://goodfirstissue.dev/)
- **First Timers Only** (https://www.firsttimersonly.com/)
- **Up For Grabs** (https://up-for-grabs.net/)
- **CodeTriage** (https://www.codetriage.com/)

Filter by language (Java), difficulty (beginner), and type (documentation, bug, feature).

### Strategy 3: Contribute to Projects You Admire

**Think about:**

- Blog posts you've read and found helpful
- Libraries that solved your problems elegantly
- Tools you use daily

**For Leo:**

If you read a great blog post about Spring Boot caching and the author has an open source library, contribute to it. Authors appreciate contributions and often become valuable connections.

### Strategy 4: Industrial/Domain Projects

**Niche advantage for Leo:**

Your operations background makes you valuable for industrial software projects.

**Look for:**
- Industrial IoT libraries
- SCADA integration tools
- Time-series database projects (InfluxDB, TimescaleDB)
- Monitoring/observability projects (Prometheus, Grafana)

**Why:**
- Less competition (fewer contributors have ops background)
- Your domain knowledge is valuable
- Networking with people in your target industry

### Evaluating Projects

**Before contributing, check:**

**1. License** (is it actually open source?)
- MIT, Apache 2.0, GPL = true open source
- Proprietary or "source-available" = not open source

**2. Activity level**
- Last commit: < 1 month = active
- Issues being addressed: 80%+ response rate = healthy
- PRs being merged: 60%+ merge rate = welcoming

**3. Code of Conduct**
- Do they have one?
- Does it seem enforced?
- Red flag if toxic behavior in issues/PRs

**4. Maintainer responsiveness**
- Submit a tiny doc fix PR
- Do they respond within 1 week?
- If no response in 2 weeks, project might be abandoned (move on)

---

## Part 3: Making Your First Contribution

Walk before you run. Start with documentation.

### Step-by-Step: Documentation Fix (First Contribution)

**Goal: Fix a typo or improve clarity in Spring Boot documentation**

**Step 1: Find the issue (30 min)**

Browse Spring Boot documentation: https://docs.spring.io/spring-boot/docs/current/reference/html/

Read sections relevant to your work:
- Data access (JPA)
- Caching
- Web (REST APIs)

**Look for:**
- Typos ("teh" instead of "the")
- Unclear sentences
- Outdated examples (Java 8 syntax when Java 17 is current)
- Missing examples (explanation without code)

**Found one? Take a screenshot for reference.**

**Step 2: Find the source (15 min)**

Spring Boot docs are in the repo: https://github.com/spring-projects/spring-boot

Navigate to:
```
/spring-boot-project/spring-boot-docs/src/docs/asciidoc/
```

Find the `.adoc` file corresponding to the page you want to fix.

**Use GitHub's search** (press `/` then type your search):
- Search for unique phrase from the doc page
- Find exact file

**Step 3: Fork the repository (5 min)**

Click "Fork" button on GitHub (top right).

This creates your copy: `github.com/yourusername/spring-boot`

**Step 4: Clone your fork locally (5 min)**

```bash
git clone https://github.com/yourusername/spring-boot.git
cd spring-boot
```

**Step 5: Create a branch (2 min)**

```bash
git checkout -b fix-caching-docs-typo
```

**Branch naming convention:**
- Descriptive: `fix-typo-in-readme`, `add-redis-example`, `update-jpa-docs`
- Not: `my-changes`, `patch-1`

**Step 6: Make the change (10 min)**

Open the `.adoc` file in your editor.

Make the fix. For a typo, this is trivial. For clarity improvements, rewrite the sentence.

**Example:**

**Before:**
```
You can configure the cache with application.properties.
```

**After:**
```
You can configure the cache by setting properties in application.properties.
For example, to set TTL to 1 hour:

[source,properties]
----
spring.cache.redis.time-to-live=3600000
----
```

**Added clarity + example.**

**Step 7: Test locally (if applicable) (15-30 min)**

For code changes, you'd run tests:
```bash
./mvnw clean test
```

For documentation, you might build docs locally to see formatting:
```bash
./mvnw -pl spring-boot-project/spring-boot-docs clean prepare-package -DskipTests
```

**But for simple typo fix, this isn't necessary.**

**Step 8: Commit your change (5 min)**

```bash
git add .
git commit -m "Fix typo in caching documentation"
```

**Good commit messages:**
- **Imperative mood:** "Fix typo" not "Fixed typo" or "Fixes typo"
- **Concise:** 50 characters or less for title
- **Descriptive:** Says what changed

**For larger changes, add body:**
```bash
git commit -m "Add Redis TTL example to caching documentation

The caching section explained that TTL can be configured but
didn't show how. This commit adds a concrete example using
application.properties."
```

**Step 9: Push to your fork (2 min)**

```bash
git push origin fix-caching-docs-typo
```

**Step 10: Create Pull Request (10 min)**

Go to your fork on GitHub: `github.com/yourusername/spring-boot`

You'll see a banner: "Your recently pushed branch: fix-caching-docs-typo" with a "Compare & pull request" button.

Click it.

**Fill out PR description:**

**Title:** "Fix typo in caching documentation"

**Description:**
```
## What
Fixes typo in the caching section of the reference documentation.
"teh cache" → "the cache"

## Why
Improves readability and professionalism of documentation.

## Related Issue
N/A (minor typo fix)
```

**For larger PRs, include:**
- Screenshots (before/after if UI change)
- Test results
- Link to related issue

**Check boxes:**
- [ ] Read CONTRIBUTING.md
- [ ] Signed CLA (if project requires)
- [ ] Tests pass
- [ ] Documentation updated (if applicable)

**Click "Create pull request"**

**Step 11: Wait for review (1-7 days)**

Maintainers will review. Possible outcomes:

1. **Approved and merged** (best case)
   - You get GitHub notification
   - Your contribution is now in Spring Boot
   - Your name is in the contributor list
   - 🎉 Celebrate!

2. **Changes requested**
   - Maintainer asks for modification
   - Make the change locally
   - Commit and push to same branch
   - PR automatically updates
   - Maintainer re-reviews

3. **Closed without merge**
   - Rare for doc fixes
   - Possible reasons: duplicate, unnecessary, project direction
   - Don't take it personally
   - Ask for feedback: "Thanks for considering. Can you help me understand what I could improve for future contributions?"

**Step 12: Respond to feedback (if any)**

**Maintainer comments:** "Can you also fix the capitalization in the next sentence?"

**Your response:**
"Absolutely! Pushed a new commit with that fix. Thanks for catching it."

**Push additional commit:**
```bash
# Make the change
git add .
git commit -m "Fix capitalization as requested in review"
git push origin fix-caching-docs-typo
```

**Step 13: Celebrate and reflect (10 min)**

**Once merged:**

1. **Update your resume/LinkedIn:**
   - "Contributor to Spring Boot open source project"
   - Link to your PR

2. **Tweet/LinkedIn post:**
   - "Made my first open source contribution to @springboot! Fixed documentation typo. Small but meaningful. #OpenSource #Java"

3. **Reflect:**
   - What went smoothly?
   - What was confusing?
   - What will you do differently next time?

4. **Plan next contribution:**
   - Find another doc improvement
   - Or graduate to bug report

**Total time for first doc contribution: 2-4 hours**

---

## Part 4: The Pull Request Workflow

Understanding the process makes it less intimidating.

### Standard GitHub Workflow

**1. Fork** (your copy of the project)

**2. Clone** (download your fork locally)

**3. Branch** (create feature branch from main/master)

**4. Change** (make your edits)

**5. Commit** (save your changes with message)

**6. Push** (upload to your fork on GitHub)

**7. PR** (request maintainers merge your branch into main project)

**8. Review** (maintainers review, request changes)

**9. Iterate** (make requested changes, push again)

**10. Merge** (maintainers merge your PR—celebration!)

### Writing Great Pull Request Descriptions

**Template:**

```markdown
## Problem
[What issue does this solve? Link to issue if exists.]

The caching documentation mentioned TTL configuration but
didn't show how to actually configure it. Issue #12345.

## Solution
[What did you do to fix it?]

Added concrete example showing how to set redis.time-to-live
in application.properties.

## Changes
[List of changes made]

- Added code example to caching.adoc
- Updated related cross-reference in configuration.adoc

## Testing
[How did you verify this works?]

- Built documentation locally
- Verified example renders correctly
- Tested configuration in sample project

## Screenshots
[If UI change, show before/after]

N/A for documentation change

## Checklist
- [x] Read CONTRIBUTING.md
- [x] Tests pass (./mvnw test)
- [x] Documentation updated
- [x] Signed CLA
```

**Why this matters:**
- Makes maintainer's job easier (clear context)
- Shows professionalism
- Increases chance of acceptance

### Responding to Code Review

**Maintainer requests changes. How to respond:**

**1. Be gracious**

❌ "That's not a real problem"
❌ "Works on my machine"
✅ "Thanks for the feedback! I'll make that change."

**2. Ask clarifying questions if needed**

❌ Implement wrong thing because you misunderstood
✅ "Just to confirm, you want me to extract this into a separate method, right?"

**3. Make requested changes quickly**

- Within 1-2 days if possible
- If you need more time: "Thanks for the review. I'm traveling this week but will have this updated by Friday."

**4. Respond to each comment**

Don't ignore comments. Either:
- "Done in commit abc123"
- "I actually kept this as-is because [reason]. Let me know if you still think it should change."

**5. Don't argue excessively**

If maintainer insists on approach you disagree with, remember:
- It's their project
- They have context you don't
- Defer to their judgment

**6. Learn from feedback**

Maintainer points out issue → take note → don't repeat in next PR.

### Handling Rejection

**Sometimes PRs get closed without merging. Common reasons:**

1. **Duplicate**: Someone else already fixed this
2. **Out of scope**: Project doesn't want this feature
3. **Timing**: Good idea but project isn't ready yet
4. **Quality**: Code doesn't meet standards and author doesn't respond to feedback

**How to respond:**

❌ "This is a stupid decision"
❌ Silently disappear
✅ "Thanks for considering. I understand the reasoning. Are there other areas I could contribute to instead?"

**Learn and move on.** Every experienced contributor has rejected PRs.

---

## Part 5: Building Relationships with Maintainers

Open source is about people, not just code.

### Start Small, Build Trust

**Maintainers notice patterns:**

**Bad pattern:**
- First interaction is massive PR with no prior discussion
- No response to feedback
- Disappears after PR is merged
- Only contributes when they personally need feature

**Good pattern:**
- First contribution is small doc fix
- Responds quickly to feedback
- Fixes own bugs after merging
- Continues contributing consistently
- Helps with issue triage

**Which contributor would you trust with larger PRs?**

### Engage Beyond PRs

**Ways to build relationship:**

1. **Comment on issues thoughtfully**
   - Not just "+1" or "any updates?"
   - Offer debugging help, suggest approaches, share relevant links

2. **Help other contributors**
   - Answer questions from newcomers in issues
   - Review others' PRs (if project allows)
   - Welcome new contributors

3. **Participate in discussions**
   - Architectural decisions
   - Feature proposals
   - Roadmap planning

4. **Report bugs well**
   - Clear reproduction steps
   - Minimal example
   - Offer to fix it yourself

5. **Acknowledge maintainers**
   - Thank them publicly (Twitter, blog posts)
   - Recognize their unpaid work
   - Be patient (they're often volunteering)

### The Escalation Path

**Contribution journey in a project:**

**Month 1-2: Newcomer**
- Doc fixes
- Typos
- Small improvements
- Building trust

**Month 3-4: Regular contributor**
- Bug fixes
- Test additions
- Small features
- Maintainers recognize your name

**Month 5-6: Trusted contributor**
- Larger features
- Architectural input
- Code review privileges
- Considered for maintainer role

**Month 12+: Maintainer**
- Merge access
- Issue triage
- Guiding new contributors
- Roadmap decisions

**Not everyone wants to be a maintainer (significant time commitment). But the path is there.**

### Networking Through Open Source

**Open source contributions lead to:**

1. **Direct relationships with maintainers**
   - Many maintainers are senior engineers at tech companies
   - They remember helpful contributors
   - When they're hiring, they reach out

2. **Visibility to companies**
   - Companies sponsor projects (Pivotal/VMware sponsors Spring)
   - Active contributors get noticed by sponsor companies
   - Job opportunities emerge

3. **Conference speaking opportunities**
   - Maintainers invite contributors to speak
   - "I'm a Spring Boot contributor" gets CFP accepted

4. **Blog collaborations**
   - Maintainers share your content
   - Co-write blog posts
   - Guest post on project blogs

**Example path:**

- You contribute to Spring Boot (doc fixes, then bug fixes)
- Spring team notices your consistent contributions
- They invite you to virtual office hours
- You mention you're job searching
- They refer you to teams at VMware or partner companies
- You interview with referral (huge advantage)

**This happens regularly. Open source is a networking strategy, not just technical exercise.**

---

## Part 6: Showcasing Your Contributions

Make sure people know you contribute.

### On GitHub Profile

**1. Pin meaningful repositories**

Don't pin:
- Your fork of Spring Boot (everyone can see you forked it)
- Repos with no activity

Do pin:
- Your original projects (Bibby)
- Projects where you're a significant contributor (if applicable)

**2. Update profile README**

Add section:

```markdown
## Open Source Contributions

I contribute to several open source projects:

- **Spring Boot** - [Documentation improvements](link to PRs),
  [bug fixes](link to merged PR)
- **Lettuce** - [Added Redis caching example](link to PR)
- **PostgreSQL** - [Updated connection pooling docs](link to PR)

[View all contributions →](https://github.com/yourusername?tab=overview)
```

**3. Keep contribution graph green**

Consistent contributions = commitment

**Goal:** 3-5 contributions per week (commits to Bibby + open source PRs + blog posts)

### On Resume

**Add section:**

```
OPEN SOURCE CONTRIBUTIONS

Spring Boot (Java)                                    2024 - Present
- Contributed documentation improvements and bug fixes
- Improved caching documentation with concrete examples
- Fixed JPA N+1 query detection in development mode

Lettuce Redis Client (Java)                           2024 - Present
- Added example demonstrating Spring Boot integration
- Improved test coverage for connection pooling
```

**Keep it brief. Hiring managers scan quickly.**

### On LinkedIn

**Update headline:**

Before: "Software Engineer | Java | Spring Boot"

After: "Software Engineer | Java | Spring Boot | Open Source Contributor"

**Add to summary:**

```
I actively contribute to open source projects including Spring Boot,
PostgreSQL, and Redis client libraries. My contributions focus on
documentation improvements, practical examples, and bug fixes.

View my contributions: github.com/yourusername
```

**Post when PRs get merged:**

```
Excited to have my first contribution to @SpringBoot merged!

Improved the caching documentation with concrete examples showing
how to configure Redis TTL.

Small contribution, but it feels great to give back to a framework
I use every day.

#OpenSource #SpringBoot #Java

[Link to PR]
```

### On Blog

**Write about your contributions:**

**Article ideas:**

- "My First Open Source Contribution to Spring Boot"
- "What I Learned Contributing to PostgreSQL Documentation"
- "5 Lessons from 10 Open Source Pull Requests"

**Why this works:**

- Shows your learning process
- Helps others contribute
- Amplifies your open source work
- SEO for "[Your Name] open source"

---

## Part 7: Measuring Open Source Impact

Track your contributions to maintain momentum.

### Quantitative Metrics

**Track monthly:**

| Metric | Target (Month 1) | Target (Month 3) | Target (Month 6) |
|--------|------------------|------------------|------------------|
| PRs submitted | 3-5 | 5-8 | 8-12 |
| PRs merged | 2-3 | 4-6 | 6-10 |
| Projects contributed to | 1-2 | 2-3 | 3-5 |
| Lines of code contributed | 50-100 | 200-500 | 500-1,000 |
| Issues participated in | 5-10 | 10-15 | 15-25 |

**Quality > quantity.**

One meaningful bug fix > ten typo fixes.

### Qualitative Signals

**You're making impact when:**

1. **Maintainers recognize you**
   - "Thanks @yourusername, you've been really helpful lately"
   - They assign issues to you
   - They ask your opinion on PRs

2. **Your contributions get used**
   - Example you added gets referenced in Stack Overflow answers
   - Documentation you improved gets thousands of views
   - Feature you built gets mentioned in release notes

3. **People thank you**
   - GitHub users comment "This fix saved me hours, thank you!"
   - Maintainers highlight your contribution in project updates

4. **Invitations arrive**
   - Asked to join project's Slack/Discord
   - Invited to contributor calls
   - Offered maintainer role

### Career Impact Metrics

**The real measures:**

1. **Recruiter mentions**
   - InMails reference your open source work
   - "I saw your contributions to Spring Boot"

2. **Interview requests**
   - Applications get responses when you list open source contributions
   - Interviewers ask about your PRs

3. **Network growth**
   - Maintainers connect with you on LinkedIn
   - Other contributors reach out
   - Companies in the ecosystem notice you

**Timeline:**

- **Month 1-3:** Building foundation, making first contributions
- **Month 4-6:** Recognition from maintainers, consistent contributor status
- **Month 7-12:** Broader visibility, career opportunities emerging

**Patience matters.** Open source impact compounds slowly.

---

## Part 8: Time Management for Open Source

Don't let open source consume all your time.

### Weekly Time Budget

**Total open source time: 3-5 hours/week**

**Breakdown:**

**Finding contributions (1 hour/week):**
- Browse issues in 2-3 projects
- Look for "good first issue" labels
- Read discussions, understand project priorities

**Making contributions (2-3 hours/week):**
- Small doc fixes: 1-2 hours
- Bug reports: 1 hour
- Code contributions: 3-5 hours (do 1-2 per month, not weekly)

**Responding to reviews (30 min - 1 hour/week):**
- Address feedback on open PRs
- Answer maintainer questions

**Total content creation budget (Week 22):**
- Writing: 8-10 hours/week (Section 18)
- Video: 3-5 hours/week (Section 19)
- Community: 5-8 hours/week (Section 20)
- Speaking prep: 0-2 hours/week (Section 21 - talk is prepared)
- Open source: 3-5 hours/week (Section 22)
- **Total: 19-30 hours/week**

**If working full-time + MBA, this is upper limit.**

**Adjust:**
- Reduce video to 1-2 hours (skip this week if needed)
- Community to 3-4 hours (minimum engagement)
- Open source 3 hours (focus on one contribution this week)

**Sustainable total: 15-20 hours/week**

### Batching Open Source Work

**Don't:** Context switch constantly between Bibby, blog, and open source

**Do:** Dedicate specific time blocks

**Sample schedule:**

**Saturday morning (3 hours): Open source block**
- Hour 1: Find 2-3 issues to work on
- Hour 2-3: Make one contribution (doc fix or bug report)
- Post to LinkedIn/Twitter when done

**Sunday afternoon (1 hour): Review and respond**
- Check notifications on open PRs
- Respond to maintainer feedback
- Plan next week's contribution

**Result:** 4 hours total, focused blocks, sustainable.

---

## Part 9: Common Open Source Mistakes

### Mistake 1: Starting with Huge Code Contribution

**Wrong:** First PR is 500-line feature implementation with no prior discussion

**Right:** First PR is doc fix or small bug fix

**Why:** Maintainers don't trust unknown contributors with large changes. Build trust first.

### Mistake 2: Not Reading CONTRIBUTING.md

**Wrong:** Submit PR without reading contribution guidelines, violate code style, tests fail

**Right:** Read CONTRIBUTING.md first, follow guidelines exactly

**Why:** Shows respect for project. Increases acceptance chance.

### Mistake 3: Drive-By PRs

**Wrong:** Submit PR, never respond to feedback, abandon it

**Right:** Respond to feedback within 1-2 days, see PR through to merge or closure

**Why:** Drive-by PRs waste maintainer time. Damages your reputation.

### Mistake 4: Arguing with Maintainers

**Wrong:** Maintainer requests change, you argue it's not necessary

**Right:** Respectfully make the change or explain your reasoning once, then defer to their judgment

**Why:** It's their project. They have final say. Arguing damages relationship.

### Mistake 5: Contributing to Too Many Projects

**Wrong:** 10 PRs across 10 different projects in one month

**Right:** Focus on 2-3 projects, make multiple contributions to each

**Why:** Depth > breadth. Relationship building requires consistency in same project.

### Mistake 6: Only Contributing When You Need Something

**Wrong:** Never contribute, then suddenly submit feature you personally need

**Right:** Regular contributions, including things that don't directly benefit you

**Why:** Open source is community, not service. Give more than you take.

### Mistake 7: Ignoring Project Culture

**Wrong:** Casual language in PR to formal project, or vice versa

**Right:** Match tone of existing PRs and issues

**Why:** Cultural fit matters. Read the room.

---

## Part 10: Action Items for Week 22

This week, make your first 2-3 open source contributions.

### Core Deliverables (5-8 hours)

**1. Choose Projects (1 hour)**

Based on your stack:

- [ ] Identify 2-3 projects from Part 2 (Spring Boot, Spring Data JPA, Lettuce)
- [ ] Read each project's CONTRIBUTING.md
- [ ] Join project Discord/Slack if available
- [ ] Star and watch the repositories

**2. Make First Documentation Contribution (2-3 hours)**

- [ ] Find documentation issue (typo, unclear explanation, missing example)
- [ ] Fork repository
- [ ] Clone locally
- [ ] Create branch
- [ ] Make fix
- [ ] Commit with clear message
- [ ] Push to your fork
- [ ] Create PR with description
- [ ] Celebrate first PR!

**3. Submit Detailed Bug Report (1-2 hours)**

- [ ] Find a bug in project you use (or look for "can't reproduce" issues)
- [ ] Write minimal reproduction case
- [ ] Gather environment details
- [ ] Submit issue with clear steps, expected vs actual behavior
- [ ] Respond to any maintainer questions

**4. Participate in Issue Discussion (1 hour)**

- [ ] Find 3-5 open issues in your chosen projects
- [ ] Read through discussions
- [ ] Add helpful comments:
   - Confirm bugs you can reproduce
   - Suggest implementation approaches
   - Link to related issues
   - Share relevant documentation

**5. Update Your Profiles (1 hour)**

- [ ] Add "Open Source Contributor" to LinkedIn headline
- [ ] Update GitHub profile README with contributions section
- [ ] Draft tweet/LinkedIn post for when first PR merges

### Stretch Goals (Optional, 3-5 hours)

**6. Make Second Documentation Contribution**

Different project or different area:

- [ ] Find another doc improvement opportunity
- [ ] Submit PR using workflow you learned
- [ ] Aim to have 2-3 open PRs by end of week

**7. Attempt First Bug Fix**

If you found a "good first issue" bug:

- [ ] Reproduce the bug locally
- [ ] Understand the cause
- [ ] Fix it and add test
- [ ] Submit PR
- [ ] Don't worry if rejected—it's practice

**8. Write Blog Post**

"My First Open Source Contribution: Lessons Learned"

- [ ] Document your experience
- [ ] What was intimidating?
- [ ] What was easier than expected?
- [ ] What will you do differently next time?
- [ ] Publish and share

**9. Set Up Contribution Tracking**

Create spreadsheet:

| Date | Project | Type | PR Link | Status |
|------|---------|------|---------|--------|
| 2024-03-15 | Spring Boot | Docs | [link] | Merged ✅ |
| 2024-03-18 | Lettuce | Bug Report | [link] | Open 🔄 |

Update weekly to track progress.

---

## Conclusion: Open Source as Career Investment

Open source contributions are not charity. They're career investment.

**What you invest:**
- 3-5 hours per week
- Patience (PRs take time to merge)
- Humility (accepting feedback)

**What you get:**
- Visible proof of skills (GitHub history)
- Relationships with maintainers (future colleagues, references)
- Network in your niche (industrial software, backend systems)
- Credibility ("Spring Boot contributor" on resume)
- Learning (reading production code from experts)

**The compounding effect:**

- **Month 1:** First few PRs merged, learning the process
- **Month 3:** Recognized in 2-3 projects, 10-15 merged PRs
- **Month 6:** Trusted contributor, maintainers assign issues to you
- **Month 12:** 50+ contributions, strong open source profile, job offers referencing your OSS work

**For career changers especially:**

You can't point to 5 years at Google. But you CAN point to:
- "Contributor to Spring Boot, Spring Data JPA, and Lettuce"
- "20+ merged pull requests across Java ecosystem projects"
- "Active in open source community"

**That's differentiation. That's credibility. That's why you get the interview.**

**Week 22 success = 2-3 open source contributions submitted (at least 1 merged), process understood, profile updated.**

Open source is a long game. Start this week. Keep going for 6 months. Watch doors open.

---

**Week 22 Checkpoint:**

Before moving to Section 23, ensure you have:

✅ Chosen 2-3 projects to contribute to
✅ Submitted first documentation PR
✅ Submitted detailed bug report or participated in issue discussions
✅ Updated LinkedIn and GitHub profiles to highlight contributions
✅ Created tracking system for future contributions
✅ Understood the PR workflow and contribution process

**Next:** Section 23: Thought Leadership (establishing yourself as a voice in your niche through consistent, opinionated content)
