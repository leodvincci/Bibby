# Week 44: Engineering Management & Productivity

**Semester 4, Week 44 of 52**
**Focus: Execution, Revenue & Scale**

---

## Overview

You've learned to build revenue systems, run sales processes, and manage customer success. Now it's time to learn how to **build and lead the engineering team** that makes it all possible.

Engineering management is where technical excellence meets people leadership. It's the art of:
- Building high-performing teams
- Maximizing productivity without burning out
- Shipping features while managing technical debt
- Balancing speed with quality
- Growing engineers while delivering business value

Many engineers think management is "not coding anymore." This is wrong. Great engineering managers are **force multipliers**—they make their entire team more effective.

**The math:**
- 1 engineer can write ~1,000 lines of quality code per week
- 1 manager leading 6 engineers = 6,000 lines per week
- But a great manager improving team productivity by 30% = 7,800 lines per week
- Plus they remove blockers, reduce context switching, prevent burnout
- **Net impact: 2-3x more valuable than individual contributor**

This week, you'll learn to build, lead, and scale engineering teams.

**By the end of this week, you will:**

- Understand engineering management fundamentals
- Implement agile methodologies (Scrum, Kanban)
- Measure and improve team velocity
- Conduct effective 1-on-1s and give feedback
- Manage technical debt strategically
- Handle incidents and on-call rotations
- Build engineering culture
- Scale teams from 1 to 50+
- Apply management frameworks to Bibby

**Week Structure:**
- **Part 1:** Engineering Management Fundamentals
- **Part 2:** Agile & Sprint Planning
- **Part 3:** Team Velocity & Productivity
- **Part 4:** Code Review & Quality
- **Part 5:** 1-on-1s & Feedback
- **Part 6:** Technical Debt Management
- **Part 7:** Incident Response & On-Call
- **Part 8:** Scaling Engineering Teams

---

## Part 1: Engineering Management Fundamentals

### What is Engineering Management?

**Engineering Manager (EM):** A role responsible for the people, processes, and productivity of an engineering team while ensuring technical excellence and business impact.

**Three core responsibilities:**

1. **People:** Hiring, onboarding, growth, retention, career development
2. **Process:** Sprint planning, standups, retrospectives, improving workflows
3. **Product:** Delivering features, managing technical debt, balancing trade-offs

### The Manager's Schedule vs Maker's Schedule

**Maker's Schedule (Engineers):**
- Long blocks of uninterrupted time (4+ hours)
- Deep focus on complex problems
- Context switching is expensive
- Best work happens in flow state

**Manager's Schedule (EMs):**
- Many short meetings (30-60 min)
- Context switching is the job
- Helping others unblock
- Rarely write code

**Key insight:** As a manager, your job is to **protect your team's maker time** while living on manager time yourself.

### The Manager's Toolkit

**Communication:**
- 1-on-1s (weekly with each report)
- Team meetings (sprint planning, retrospectives)
- All-hands (company updates)
- Written communication (docs, RFCs, memos)

**Processes:**
- Sprint planning and standups
- Code review and PR guidelines
- Incident response and postmortems
- Performance reviews

**Metrics:**
- Velocity (story points or PRs per sprint)
- Cycle time (code to production)
- Deployment frequency
- MTTR (mean time to recovery)
- Team happiness/satisfaction

### Code Example: Sprint Tracking Service

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Document(collection = "sprints")
public record Sprint(
    @Id String sprintId,
    String teamId,
    int sprintNumber,
    LocalDate startDate,
    LocalDate endDate,
    SprintStatus status,
    int plannedPoints,
    int completedPoints,
    List<String> goals,
    Map<String, Object> metrics
) {
    public enum SprintStatus {
        PLANNING,
        ACTIVE,
        COMPLETED,
        CANCELLED
    }

    public long durationDays() {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    public double velocity() {
        return completedPoints;
    }

    public double commitmentAccuracy() {
        return plannedPoints > 0 ?
            (completedPoints / (double) plannedPoints) * 100 : 0;
    }
}

@Document(collection = "stories")
public record Story(
    @Id String storyId,
    String sprintId,
    String title,
    String description,
    StoryType type,
    int storyPoints,
    String assignee,
    StoryStatus status,
    LocalDate createdAt,
    LocalDate completedAt,
    List<String> tags
) {
    public enum StoryType {
        FEATURE,
        BUG,
        TECH_DEBT,
        SPIKE  // Research/investigation
    }

    public enum StoryStatus {
        BACKLOG,
        TODO,
        IN_PROGRESS,
        IN_REVIEW,
        DONE,
        BLOCKED
    }

    public long cycleTimeDays() {
        if (completedAt == null) return -1;
        return ChronoUnit.DAYS.between(createdAt, completedAt);
    }
}

@Service
public class SprintManagementService {

    /**
     * Create new sprint
     */
    public Sprint createSprint(
        String teamId,
        int sprintNumber,
        LocalDate startDate,
        int durationWeeks,
        List<String> goals
    ) {
        Sprint sprint = new Sprint(
            UUID.randomUUID().toString(),
            teamId,
            sprintNumber,
            startDate,
            startDate.plusWeeks(durationWeeks),
            Sprint.SprintStatus.PLANNING,
            0,  // Will be set during planning
            0,
            goals,
            new HashMap<>()
        );

        return sprintRepository.save(sprint);
    }

    /**
     * Add story to sprint
     */
    public Story addStoryToSprint(
        String sprintId,
        String title,
        Story.StoryType type,
        int storyPoints
    ) {
        Sprint sprint = sprintRepository.findById(sprintId).orElseThrow();

        Story story = new Story(
            UUID.randomUUID().toString(),
            sprintId,
            title,
            "",
            type,
            storyPoints,
            null,  // Not yet assigned
            Story.StoryStatus.TODO,
            LocalDate.now(),
            null,
            new ArrayList<>()
        );

        // Update sprint planned points
        sprint = sprintRepository.updatePlannedPoints(
            sprintId,
            sprint.plannedPoints() + storyPoints
        );

        return storyRepository.save(story);
    }

    /**
     * Complete story
     */
    public Story completeStory(String storyId) {
        Story story = storyRepository.findById(storyId).orElseThrow();

        if (story.status() == Story.StoryStatus.DONE) {
            return story;  // Already done
        }

        Story completed = new Story(
            story.storyId(),
            story.sprintId(),
            story.title(),
            story.description(),
            story.type(),
            story.storyPoints(),
            story.assignee(),
            Story.StoryStatus.DONE,
            story.createdAt(),
            LocalDate.now(),
            story.tags()
        );

        // Update sprint completed points
        Sprint sprint = sprintRepository.findById(story.sprintId()).orElseThrow();
        sprintRepository.updateCompletedPoints(
            story.sprintId(),
            sprint.completedPoints() + story.storyPoints()
        );

        return storyRepository.save(completed);
    }

    /**
     * Calculate sprint metrics
     */
    public record SprintMetrics(
        String sprintId,
        int plannedPoints,
        int completedPoints,
        double velocity,
        double commitmentAccuracy,
        int storiesPlanned,
        int storiesCompleted,
        Map<Story.StoryType, Integer> pointsByType,
        double avgCycleTimeDays,
        List<String> blockedStories
    ) {}

    public SprintMetrics calculateMetrics(String sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId).orElseThrow();
        List<Story> stories = storyRepository.findBySprintId(sprintId);

        int storiesCompleted = (int) stories.stream()
            .filter(s -> s.status() == Story.StoryStatus.DONE)
            .count();

        // Points by type
        Map<Story.StoryType, Integer> pointsByType = new HashMap<>();
        for (Story.StoryType type : Story.StoryType.values()) {
            int points = stories.stream()
                .filter(s -> s.type() == type && s.status() == Story.StoryStatus.DONE)
                .mapToInt(Story::storyPoints)
                .sum();
            pointsByType.put(type, points);
        }

        // Average cycle time
        double avgCycleTime = stories.stream()
            .filter(s -> s.status() == Story.StoryStatus.DONE)
            .mapToLong(Story::cycleTimeDays)
            .average()
            .orElse(0);

        // Blocked stories
        List<String> blocked = stories.stream()
            .filter(s -> s.status() == Story.StoryStatus.BLOCKED)
            .map(Story::title)
            .toList();

        return new SprintMetrics(
            sprintId,
            sprint.plannedPoints(),
            sprint.completedPoints(),
            sprint.velocity(),
            sprint.commitmentAccuracy(),
            stories.size(),
            storiesCompleted,
            pointsByType,
            avgCycleTime,
            blocked
        );
    }

    /**
     * Generate sprint report
     */
    public String generateSprintReport(String sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId).orElseThrow();
        SprintMetrics metrics = calculateMetrics(sprintId);

        StringBuilder report = new StringBuilder();
        report.append("# Sprint ").append(sprint.sprintNumber()).append(" Report\n\n");
        report.append("**Duration:** ").append(sprint.startDate())
            .append(" to ").append(sprint.endDate()).append("\n");
        report.append("**Status:** ").append(sprint.status()).append("\n\n");

        report.append("## Sprint Goals\n");
        sprint.goals().forEach(goal ->
            report.append("- ").append(goal).append("\n")
        );

        report.append("\n## Metrics\n");
        report.append("- **Velocity:** ").append(metrics.velocity()).append(" points\n");
        report.append("- **Commitment Accuracy:** ")
            .append(String.format("%.0f%%", metrics.commitmentAccuracy())).append("\n");
        report.append("- **Stories Completed:** ")
            .append(metrics.storiesCompleted()).append(" / ")
            .append(metrics.storiesPlanned()).append("\n");
        report.append("- **Avg Cycle Time:** ")
            .append(String.format("%.1f", metrics.avgCycleTimeDays())).append(" days\n");

        report.append("\n## Work Breakdown\n");
        metrics.pointsByType().forEach((type, points) ->
            report.append("- **").append(type).append(":** ")
                .append(points).append(" points\n")
        );

        if (!metrics.blockedStories().isEmpty()) {
            report.append("\n## ⚠️ Blocked Stories\n");
            metrics.blockedStories().forEach(story ->
                report.append("- ").append(story).append("\n")
            );
        }

        return report.toString();
    }
}
```

---

## Part 2: Agile & Sprint Planning

### Agile Methodologies

**Scrum:**
- Fixed-length sprints (1-2 weeks)
- Ceremonies: Planning, daily standup, review, retrospective
- Roles: Product Owner, Scrum Master, Team
- Best for: Predictable roadmaps, regular releases

**Kanban:**
- Continuous flow, no sprints
- WIP (work in progress) limits
- Pull system
- Best for: Maintenance work, support teams, unpredictable priorities

**Scrumban (Hybrid):**
- Sprints + Kanban board
- Best of both worlds
- Best for: Most product teams

### Sprint Planning

**Goal:** Commit to work for the sprint that delivers business value.

**Process (2-hour meeting):**

1. **Review sprint goal (15 min)**
   - What business objective are we achieving?
   - Example: "Enable book club creation for Bibby"

2. **Review backlog (30 min)**
   - Product Manager presents prioritized stories
   - Team asks clarifying questions
   - Estimate complexity (story points)

3. **Capacity planning (15 min)**
   - Calculate team capacity (velocity from past sprints)
   - Account for PTO, holidays, meetings
   - Example: 5 engineers × 8 points/week × 2 weeks = 80 points

4. **Commit to stories (45 min)**
   - Pull top stories into sprint until capacity reached
   - Break down large stories into smaller tasks
   - Assign owners

5. **Risk identification (15 min)**
   - What could block us?
   - Dependencies on other teams?
   - Unknown unknowns?

**Story Sizing:**

Use **Fibonacci sequence** (1, 2, 3, 5, 8, 13, 21):
- **1 point:** < 2 hours, trivial
- **2 points:** Half day, straightforward
- **3 points:** 1 day, clear path
- **5 points:** 2-3 days, some complexity
- **8 points:** 4-5 days, significant work
- **13 points:** 1 week, should break down
- **21+ points:** Too big, must break down

**Estimation technique: Planning Poker**
1. Read story
2. Everyone picks a card (1, 2, 3, 5, 8, 13)
3. Reveal simultaneously
4. Discuss differences
5. Re-estimate until consensus

### Daily Standup

**Goal:** Synchronize team, identify blockers.

**Format (15 minutes max):**
- What did you do yesterday?
- What are you doing today?
- Any blockers?

**Anti-patterns to avoid:**
- ❌ Status updates to manager (should be peer-to-peer)
- ❌ Problem-solving (take offline)
- ❌ Longer than 15 minutes
- ❌ People unprepared

**Best practices:**
- ✅ Stand up (keeps it short)
- ✅ In front of board/monitor
- ✅ Focus on stories, not people
- ✅ "Parking lot" for deeper discussions

### Sprint Retrospective

**Goal:** Continuous improvement of team processes.

**Format (1 hour):**

1. **Set the stage (5 min)**
   - Create psychological safety
   - Reminder: focus on improvement, not blame

2. **Gather data (20 min)**
   - What went well?
   - What didn't go well?
   - What should we try?

3. **Generate insights (20 min)**
   - Group similar items
   - Identify patterns
   - Vote on top issues

4. **Decide actions (10 min)**
   - Pick 1-3 concrete actions
   - Assign owners
   - Define "done"

5. **Close (5 min)**
   - Recap actions
   - Appreciation shoutouts

**Example actions:**
- "Reduce PR review time from 2 days to 4 hours"
- "Add integration tests before merging"
- "Rotate on-call weekly instead of monthly"

### Code Example: Sprint Ceremonies Service

```java
@Service
public class SprintCeremoniesService {

    public record DailyStandup(
        LocalDate date,
        List<StandupUpdate> updates,
        List<String> blockers
    ) {}

    public record StandupUpdate(
        String engineerId,
        String yesterday,
        String today,
        List<String> blockers
    ) {}

    /**
     * Record daily standup
     */
    public DailyStandup recordStandup(
        String sprintId,
        List<StandupUpdate> updates
    ) {
        List<String> allBlockers = updates.stream()
            .flatMap(u -> u.blockers().stream())
            .toList();

        DailyStandup standup = new DailyStandup(
            LocalDate.now(),
            updates,
            allBlockers
        );

        standupRepository.save(standup);

        // Alert if blockers
        if (!allBlockers.isEmpty()) {
            notifyManager(sprintId, "Blockers identified: " +
                String.join(", ", allBlockers));
        }

        return standup;
    }

    /**
     * Sprint retrospective
     */
    public record RetrospectiveItem(
        String category,  // "Went Well", "Didn't Go Well", "Try Next Time"
        String description,
        int votes
    ) {}

    public record Retrospective(
        String sprintId,
        LocalDate date,
        List<RetrospectiveItem> items,
        List<ActionItem> actions
    ) {}

    public record ActionItem(
        String description,
        String owner,
        LocalDate dueDate,
        boolean completed
    ) {}

    public Retrospective conductRetrospective(
        String sprintId,
        List<RetrospectiveItem> items
    ) {
        // Sort items by votes
        List<RetrospectiveItem> sortedItems = items.stream()
            .sorted(Comparator.comparingInt(RetrospectiveItem::votes).reversed())
            .toList();

        // Top 3 items become action items
        List<ActionItem> actions = new ArrayList<>();
        for (int i = 0; i < Math.min(3, sortedItems.size()); i++) {
            RetrospectiveItem item = sortedItems.get(i);
            if (item.category().equals("Didn't Go Well") ||
                item.category().equals("Try Next Time")) {
                actions.add(new ActionItem(
                    "Address: " + item.description(),
                    null,  // Assign during meeting
                    LocalDate.now().plusWeeks(2),
                    false
                ));
            }
        }

        Retrospective retro = new Retrospective(
            sprintId,
            LocalDate.now(),
            sortedItems,
            actions
        );

        return retroRepository.save(retro);
    }

    private void notifyManager(String sprintId, String message) {
        // Send notification to manager
        System.out.println("🚨 Alert: " + message);
    }
}
```

---

## Part 3: Team Velocity & Productivity

### Measuring Velocity

**Velocity:** Amount of work a team completes in a sprint.

**Measured in:**
- Story points (abstract complexity units)
- PRs merged
- Features shipped

**Why story points > hours:**
- Complexity is relative, not absolute
- Accounts for uncertainty
- Harder to game
- Stable across team members

**Calculating team velocity:**

```
Sprint 1: 45 points
Sprint 2: 52 points
Sprint 3: 48 points
Sprint 4: 50 points

Average velocity: (45 + 52 + 48 + 50) / 4 = 48.75 points/sprint
```

**Using velocity for planning:**
- Team capacity = recent average velocity
- Don't overcommit (causes burnout)
- Don't undercommit (wastes capacity)

### DORA Metrics

**Four Key Metrics** (from DevOps Research and Assessment):

1. **Deployment Frequency**
   - How often you deploy to production
   - Elite: Multiple times per day
   - High: Once per day to once per week
   - Medium: Once per week to once per month
   - Low: Less than once per month

2. **Lead Time for Changes**
   - Time from code committed to running in production
   - Elite: Less than 1 hour
   - High: 1 day to 1 week
   - Medium: 1 week to 1 month
   - Low: More than 1 month

3. **Mean Time to Recovery (MTTR)**
   - How quickly you restore service after incident
   - Elite: Less than 1 hour
   - High: Less than 1 day
   - Medium: 1 day to 1 week
   - Low: More than 1 week

4. **Change Failure Rate**
   - % of deployments causing failures in production
   - Elite: 0-15%
   - High: 16-30%
   - Medium: 31-45%
   - Low: 46-100%

### Productivity Killers

**Context Switching:**
- Switching between tasks costs 20-30 minutes each time
- 10 interruptions per day = 3-5 hours lost
- Solution: Block time for deep work (4-hour chunks)

**Meetings:**
- Average engineer: 10-15 hours of meetings per week
- Each meeting = 30 minutes of context switching
- Solution: No-meeting days (Tuesday/Thursday)

**Unclear Requirements:**
- Vague user stories → wasted work → rework
- Solution: Definition of Ready (DoR) checklist

**Technical Debt:**
- Slows down every feature
- Compounds over time
- Solution: Allocate 20% of sprint to tech debt

**Poor Communication:**
- Async >>> Synchronous
- Written >>> Verbal
- Solution: RFC process for major decisions

### Code Example: Velocity Tracking Service

```java
@Service
public class VelocityTrackingService {

    /**
     * Calculate team velocity trend
     */
    public record VelocityTrend(
        double currentVelocity,
        double averageVelocity,
        double trend,  // Positive = improving, negative = declining
        String status,
        String recommendation
    ) {}

    public VelocityTrend calculateVelocityTrend(String teamId, int numSprints) {
        List<Sprint> recentSprints = sprintRepository
            .findByTeamIdOrderByStartDateDesc(teamId)
            .stream()
            .filter(s -> s.status() == Sprint.SprintStatus.COMPLETED)
            .limit(numSprints)
            .toList();

        if (recentSprints.isEmpty()) {
            return new VelocityTrend(0, 0, 0, "No Data",
                "Complete at least one sprint to track velocity");
        }

        double avgVelocity = recentSprints.stream()
            .mapToDouble(Sprint::velocity)
            .average()
            .orElse(0);

        double currentVelocity = recentSprints.get(0).velocity();

        // Calculate trend (simple linear regression)
        double trend = calculateTrend(recentSprints);

        String status = getVelocityStatus(trend);
        String recommendation = generateVelocityRecommendation(trend, avgVelocity);

        return new VelocityTrend(
            currentVelocity,
            avgVelocity,
            trend,
            status,
            recommendation
        );
    }

    private double calculateTrend(List<Sprint> sprints) {
        // Simple trend: (most recent - oldest) / num sprints
        if (sprints.size() < 2) return 0;

        double oldest = sprints.get(sprints.size() - 1).velocity();
        double newest = sprints.get(0).velocity();

        return (newest - oldest) / sprints.size();
    }

    private String getVelocityStatus(double trend) {
        if (trend > 5) return "Improving";
        if (trend < -5) return "Declining";
        return "Stable";
    }

    private String generateVelocityRecommendation(double trend, double avgVelocity) {
        if (trend < -5) {
            return "⚠️ Velocity declining. Investigate: tech debt, unclear requirements, team morale.";
        } else if (trend > 10) {
            return "✅ Strong velocity growth. Ensure sustainable pace to avoid burnout.";
        } else if (avgVelocity < 20) {
            return "📊 Low baseline velocity. Consider: team size, story sizing calibration, blockers.";
        } else {
            return "✅ Healthy, stable velocity. Continue current practices.";
        }
    }

    /**
     * Calculate DORA metrics
     */
    public record DORAMetrics(
        double deploymentFrequency,      // Deploys per day
        double leadTimeHours,            // Hours from commit to prod
        double mttrHours,                // Hours to recover from incident
        double changeFailureRate,        // % of failed deployments
        String performanceTier           // Elite, High, Medium, Low
    ) {}

    public DORAMetrics calculateDORAMetrics(String teamId, LocalDate startDate, LocalDate endDate) {
        // Deployment frequency
        int deployments = deploymentRepository.countByTeamIdAndDateBetween(
            teamId, startDate, endDate
        );
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        double deployFrequency = deployments / (double) days;

        // Lead time for changes
        List<Deployment> recentDeployments = deploymentRepository
            .findByTeamIdAndDateBetween(teamId, startDate, endDate);

        double avgLeadTime = recentDeployments.stream()
            .mapToDouble(d -> ChronoUnit.HOURS.between(d.commitTime(), d.deployTime()))
            .average()
            .orElse(0);

        // MTTR
        List<Incident> incidents = incidentRepository
            .findByTeamIdAndDateBetween(teamId, startDate, endDate);

        double avgMTTR = incidents.stream()
            .mapToDouble(i -> ChronoUnit.HOURS.between(i.startTime(), i.resolvedTime()))
            .average()
            .orElse(0);

        // Change failure rate
        long failedDeployments = recentDeployments.stream()
            .filter(d -> d.status() == Deployment.Status.FAILED ||
                        d.status() == Deployment.Status.ROLLED_BACK)
            .count();

        double changeFailureRate = deployments > 0 ?
            (failedDeployments / (double) deployments) * 100 : 0;

        String tier = calculateDORATier(deployFrequency, avgLeadTime, avgMTTR, changeFailureRate);

        return new DORAMetrics(
            deployFrequency,
            avgLeadTime,
            avgMTTR,
            changeFailureRate,
            tier
        );
    }

    private String calculateDORATier(
        double deployFreq,
        double leadTime,
        double mttr,
        double changeFailure
    ) {
        int score = 0;

        // Deployment frequency
        if (deployFreq >= 1.0) score += 4;          // Multiple per day
        else if (deployFreq >= 0.14) score += 3;    // Daily
        else if (deployFreq >= 0.03) score += 2;    // Weekly
        else score += 1;

        // Lead time
        if (leadTime <= 1) score += 4;              // < 1 hour
        else if (leadTime <= 24) score += 3;        // < 1 day
        else if (leadTime <= 168) score += 2;       // < 1 week
        else score += 1;

        // MTTR
        if (mttr <= 1) score += 4;
        else if (mttr <= 24) score += 3;
        else if (mttr <= 168) score += 2;
        else score += 1;

        // Change failure rate
        if (changeFailure <= 15) score += 4;
        else if (changeFailure <= 30) score += 3;
        else if (changeFailure <= 45) score += 2;
        else score += 1;

        // Total score out of 16
        if (score >= 14) return "Elite";
        if (score >= 10) return "High";
        if (score >= 6) return "Medium";
        return "Low";
    }
}
```

---

## Part 4: Code Review & Quality

### Code Review Best Practices

**Goals:**
1. Catch bugs before production
2. Share knowledge across team
3. Maintain code quality and consistency
4. Mentor junior engineers

**Reviewer responsibilities:**
- Review within 4 hours (ideally 1 hour)
- Be constructive, not critical
- Focus on: correctness, readability, maintainability
- Nitpick separately from blocking issues

**Author responsibilities:**
- Keep PRs small (< 400 lines)
- Write clear descriptions
- Self-review before requesting review
- Address feedback promptly

**Code Review Checklist:**

✅ **Functionality**
- Does it work as intended?
- Are edge cases handled?
- Are error cases handled gracefully?

✅ **Tests**
- Are there unit tests?
- Are there integration tests?
- Do tests cover edge cases?

✅ **Performance**
- Any N+1 queries?
- Unnecessary loops?
- Memory leaks?

✅ **Security**
- SQL injection risks?
- XSS vulnerabilities?
- Authentication/authorization checked?

✅ **Readability**
- Clear variable names?
- Functions < 50 lines?
- Comments explain "why" not "what"?

### PR Size Guidelines

**Ideal PR:**
- < 200 lines of code
- Single responsibility
- Can be reviewed in < 30 minutes

**Why small PRs:**
- Faster reviews
- Easier to understand
- Less risky to deploy
- Easier to revert

**How to keep PRs small:**
- Break features into smaller tasks
- Use feature flags for incomplete work
- Refactor in separate PRs

### Code Example: Code Review Analytics

```java
@Service
public class CodeReviewAnalyticsService {

    public record PullRequest(
        String prId,
        String author,
        String title,
        int linesAdded,
        int linesDeleted,
        LocalDateTime createdAt,
        LocalDateTime mergedAt,
        List<String> reviewers,
        int commentsCount,
        PRStatus status
    ) {
        public enum PRStatus {
            OPEN,
            APPROVED,
            CHANGES_REQUESTED,
            MERGED,
            CLOSED
        }

        public long reviewTimehours() {
            if (mergedAt == null) return -1;
            return ChronoUnit.HOURS.between(createdAt, mergedAt);
        }

        public int totalLinesChanged() {
            return linesAdded + linesDeleted;
        }
    }

    /**
     * Calculate code review metrics
     */
    public record CodeReviewMetrics(
        int totalPRs,
        double avgReviewTimeHours,
        double avgPRSize,
        double approvalRate,
        Map<String, Integer> prsByAuthor,
        Map<String, Integer> reviewsByReviewer,
        List<String> slowReviews  // PRs taking > 24 hours
    ) {}

    public CodeReviewMetrics calculateMetrics(
        String teamId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        List<PullRequest> prs = prRepository
            .findByTeamIdAndCreatedAtBetween(teamId, startDate.atStartOfDay(), endDate.atTime(23, 59));

        double avgReviewTime = prs.stream()
            .filter(pr -> pr.mergedAt() != null)
            .mapToLong(PullRequest::reviewTimeHours)
            .average()
            .orElse(0);

        double avgSize = prs.stream()
            .mapToInt(PullRequest::totalLinesChanged)
            .average()
            .orElse(0);

        long approved = prs.stream()
            .filter(pr -> pr.status() == PullRequest.PRStatus.MERGED)
            .count();

        double approvalRate = prs.isEmpty() ? 0 : (approved / (double) prs.size()) * 100;

        // PRs by author
        Map<String, Long> prsByAuthorCount = prs.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                PullRequest::author,
                java.util.stream.Collectors.counting()
            ));

        Map<String, Integer> prsByAuthor = new HashMap<>();
        prsByAuthorCount.forEach((k, v) -> prsByAuthor.put(k, v.intValue()));

        // Reviews by reviewer
        Map<String, Integer> reviewsByReviewer = new HashMap<>();
        prs.forEach(pr -> {
            pr.reviewers().forEach(reviewer -> {
                reviewsByReviewer.merge(reviewer, 1, Integer::sum);
            });
        });

        // Slow reviews (> 24 hours)
        List<String> slowReviews = prs.stream()
            .filter(pr -> pr.reviewTimeHours() > 24)
            .map(pr -> pr.title() + " (" + pr.reviewTimeHours() + "h)")
            .toList();

        return new CodeReviewMetrics(
            prs.size(),
            avgReviewTime,
            avgSize,
            approvalRate,
            prsByAuthor,
            reviewsByReviewer,
            slowReviews
        );
    }

    /**
     * Identify code review bottlenecks
     */
    public record ReviewBottleneck(
        String type,
        String description,
        String recommendation
    ) {}

    public List<ReviewBottleneck> identifyBottlenecks(CodeReviewMetrics metrics) {
        List<ReviewBottleneck> bottlenecks = new ArrayList<>();

        // Slow review times
        if (metrics.avgReviewTimeHours() > 12) {
            bottlenecks.add(new ReviewBottleneck(
                "Slow Reviews",
                "Average review time is " + String.format("%.1f", metrics.avgReviewTimeHours()) + " hours",
                "Set team SLA: reviews within 4 hours. Consider rotating review duty."
            ));
        }

        // Large PRs
        if (metrics.avgPRSize() > 500) {
            bottlenecks.add(new ReviewBottleneck(
                "Large PRs",
                "Average PR size is " + String.format("%.0f", metrics.avgPRSize()) + " lines",
                "Encourage smaller PRs (< 400 lines). Break features into smaller tasks."
            ));
        }

        // Low approval rate
        if (metrics.approvalRate() < 80) {
            bottlenecks.add(new ReviewBottleneck(
                "Low Approval Rate",
                "Only " + String.format("%.0f%%", metrics.approvalRate()) + " of PRs are approved",
                "Investigate: Are requirements clear? Is tech debt blocking progress?"
            ));
        }

        // Unbalanced review load
        int maxReviews = metrics.reviewsByReviewer().values().stream()
            .max(Integer::compare)
            .orElse(0);
        int minReviews = metrics.reviewsByReviewer().values().stream()
            .min(Integer::compare)
            .orElse(0);

        if (maxReviews > minReviews * 3) {
            bottlenecks.add(new ReviewBottleneck(
                "Unbalanced Review Load",
                "Review distribution is uneven: " + minReviews + " to " + maxReviews,
                "Rotate review assignments. Use round-robin or automated assignment."
            ));
        }

        return bottlenecks;
    }
}
```

---

## Part 5: 1-on-1s & Feedback

### The Power of 1-on-1s

**1-on-1 (one-on-one):** Weekly meeting between manager and direct report.

**Purpose:**
- Build trust and psychological safety
- Career development and growth
- Surface issues early
- Give and receive feedback

**Not for:**
- ❌ Status updates (use standups)
- ❌ Project planning (use sprint planning)
- ❌ Performance reviews (separate process)

**Frequency:**
- Weekly for 30 minutes (preferred)
- Bi-weekly for 45-60 minutes (minimum)
- Never cancel (shows lack of priority)

### 1-on-1 Structure

**Opening (5 min):**
- How are you? (Really, how are you?)
- What's on your mind?
- Any urgent topics?

**Main Discussion (20 min):**
- Topics from engineer (their agenda > yours)
- Career goals and development
- Feedback (both directions)
- Obstacles and blockers

**Closing (5 min):**
- Recap action items
- Schedule next meeting
- Anything else?

### 1-on-1 Question Bank

**Career Growth:**
- What skills do you want to develop?
- Where do you see yourself in 2 years?
- What projects excite you most?
- What would you like to do more/less of?

**Feedback:**
- What's going well on the team?
- What could be better?
- How can I help you be more effective?
- Any concerns I should know about?

**Team Dynamics:**
- How's collaboration with [teammate]?
- Do you feel heard in team meetings?
- Are code reviews constructive?
- Any interpersonal issues?

**Work-Life Balance:**
- Are you working too much?
- Taking enough breaks?
- Time for family/hobbies?
- Feeling burned out?

### Giving Feedback (SBI Model)

**SBI = Situation, Behavior, Impact**

**Situation:** When and where
**Behavior:** What you observed (objective)
**Impact:** The effect it had

**Example (Positive):**
```
Situation: "In yesterday's design review..."
Behavior: "You asked clarifying questions about the API design..."
Impact: "Which helped us catch a major flaw before coding. Great catch!"
```

**Example (Constructive):**
```
Situation: "In this week's sprint planning..."
Behavior: "You committed to 20 points when the team average is 10..."
Impact: "I'm concerned you're overcommitting and may burn out or miss the deadline."
```

**Feedback best practices:**
- ✅ Specific, not vague
- ✅ Timely (within 48 hours)
- ✅ Private (1-on-1, not public)
- ✅ Actionable (what to do differently)
- ✅ Balanced (praise + constructive)

### Code Example: 1-on-1 Tracking Service

```java
@Service
public class OneOnOneService {

    public record OneOnOne(
        String meetingId,
        String managerId,
        String engineerId,
        LocalDate date,
        int durationMinutes,
        List<String> topics,
        List<String> actionItems,
        String notes,
        MeetingStatus status
    ) {
        public enum MeetingStatus {
            SCHEDULED,
            COMPLETED,
            CANCELLED,
            RESCHEDULED
        }
    }

    public record ActionItem(
        String description,
        String owner,
        LocalDate dueDate,
        boolean completed
    ) {}

    /**
     * Schedule 1-on-1
     */
    public OneOnOne schedule(
        String managerId,
        String engineerId,
        LocalDate date
    ) {
        // Check for conflicts
        Optional<OneOnOne> existing = oneOnOneRepository
            .findByManagerIdAndEngineerIdAndDate(managerId, engineerId, date);

        if (existing.isPresent()) {
            throw new IllegalStateException("1-on-1 already scheduled for this date");
        }

        OneOnOne meeting = new OneOnOne(
            UUID.randomUUID().toString(),
            managerId,
            engineerId,
            date,
            30,  // Default 30 minutes
            new ArrayList<>(),
            new ArrayList<>(),
            "",
            OneOnOne.MeetingStatus.SCHEDULED
        );

        return oneOnOneRepository.save(meeting);
    }

    /**
     * Add topics before meeting
     */
    public OneOnOne addTopics(String meetingId, List<String> topics) {
        OneOnOne meeting = oneOnOneRepository.findById(meetingId).orElseThrow();

        List<String> allTopics = new ArrayList<>(meeting.topics());
        allTopics.addAll(topics);

        OneOnOne updated = new OneOnOne(
            meeting.meetingId(),
            meeting.managerId(),
            meeting.engineerId(),
            meeting.date(),
            meeting.durationMinutes(),
            allTopics,
            meeting.actionItems(),
            meeting.notes(),
            meeting.status()
        );

        return oneOnOneRepository.save(updated);
    }

    /**
     * Complete meeting with notes
     */
    public OneOnOne completeMeeting(
        String meetingId,
        String notes,
        List<String> actionItems
    ) {
        OneOnOne meeting = oneOnOneRepository.findById(meetingId).orElseThrow();

        OneOnOne completed = new OneOnOne(
            meeting.meetingId(),
            meeting.managerId(),
            meeting.engineerId(),
            meeting.date(),
            meeting.durationMinutes(),
            meeting.topics(),
            actionItems,
            notes,
            OneOnOne.MeetingStatus.COMPLETED
        );

        return oneOnOneRepository.save(completed);
    }

    /**
     * Track 1-on-1 health
     */
    public record OneOnOneHealth(
        String engineerId,
        int meetingsScheduled,
        int meetingsCompleted,
        int meetingsCancelled,
        double completionRate,
        long daysSinceLastMeeting,
        String status,
        String recommendation
    ) {}

    public OneOnOneHealth checkHealth(String managerId, String engineerId) {
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);

        List<OneOnOne> recentMeetings = oneOnOneRepository
            .findByManagerIdAndEngineerIdAndDateAfter(managerId, engineerId, threeMonthsAgo);

        int scheduled = (int) recentMeetings.stream()
            .filter(m -> m.status() == OneOnOne.MeetingStatus.SCHEDULED)
            .count();

        int completed = (int) recentMeetings.stream()
            .filter(m -> m.status() == OneOnOne.MeetingStatus.COMPLETED)
            .count();

        int cancelled = (int) recentMeetings.stream()
            .filter(m -> m.status() == OneOnOne.MeetingStatus.CANCELLED)
            .count();

        double completionRate = (scheduled + completed + cancelled) > 0 ?
            (completed / (double) (scheduled + completed + cancelled)) * 100 : 0;

        // Days since last meeting
        Optional<LocalDate> lastMeeting = recentMeetings.stream()
            .filter(m -> m.status() == OneOnOne.MeetingStatus.COMPLETED)
            .map(OneOnOne::date)
            .max(LocalDate::compareTo);

        long daysSinceLast = lastMeeting.map(date ->
            ChronoUnit.DAYS.between(date, LocalDate.now())
        ).orElse(-1L);

        String status = getHealthStatus(completionRate, daysSinceLast);
        String recommendation = generateRecommendation(completionRate, daysSinceLast);

        return new OneOnOneHealth(
            engineerId,
            scheduled + completed,
            completed,
            cancelled,
            completionRate,
            daysSinceLast,
            status,
            recommendation
        );
    }

    private String getHealthStatus(double completionRate, long daysSinceLast) {
        if (daysSinceLast > 21) return "Critical";
        if (completionRate < 70 || daysSinceLast > 14) return "At Risk";
        return "Healthy";
    }

    private String generateRecommendation(double completionRate, long daysSinceLast) {
        if (daysSinceLast > 21) {
            return "⚠️ No 1-on-1 in 3+ weeks. Schedule immediately.";
        } else if (completionRate < 70) {
            return "📅 High cancellation rate (" + String.format("%.0f%%", 100 - completionRate) +
                "). Prioritize 1-on-1s and avoid canceling.";
        } else if (daysSinceLast > 14) {
            return "⏰ It's been " + daysSinceLast + " days. Schedule this week.";
        } else {
            return "✅ Healthy 1-on-1 cadence. Continue weekly meetings.";
        }
    }
}
```

---

## Part 6: Technical Debt Management

### What is Technical Debt?

**Technical debt:** Shortcuts taken during development that make future changes harder.

**Types of technical debt:**

1. **Deliberate debt:** Conscious decision to ship faster
   - Example: Skip unit tests to hit deadline
   - Okay if you have plan to pay it back

2. **Accidental debt:** Poor design or lack of knowledge
   - Example: Inefficient database queries
   - Should refactor when discovered

3. **Bit rot:** Code that deteriorates over time
   - Example: Dependencies with security vulnerabilities
   - Requires ongoing maintenance

### The Cost of Technical Debt

**Early:** Fast feature development, low debt interest
**Later:** Slow feature development, high debt interest

```
Feature velocity over time:

No debt:    ████████████████████████████████
Some debt:  ████████████████████
High debt:  ██████████
```

**Signs of high technical debt:**
- Features taking 2-3x longer than before
- Fear of changing code (might break things)
- Frequent production incidents
- Low test coverage
- Developer frustration

### Managing Technical Debt

**Strategy:**

1. **Track it** (debt register)
2. **Prioritize it** (impact vs effort)
3. **Budget for it** (20% of sprint capacity)
4. **Prevent it** (code reviews, standards)

**The 20% Rule:**
- 80% of sprint: new features
- 20% of sprint: tech debt, refactoring, tests

**Prioritization matrix:**

```
           High Impact  |  Low Impact
        ━━━━━━━━━━━━━━━━┿━━━━━━━━━━━━━━━━
Low     │               │
Effort  │   DO FIRST    │   DO LATER
        │   (Quick wins)│   (Nice to have)
        ┼───────────────┼────────────────
High    │   DO NEXT     │   DON'T DO
Effort  │   (Big wins)  │   (Waste of time)
        │               │
```

### Code Example: Technical Debt Tracker

```java
@Service
public class TechnicalDebtService {

    public record DebtItem(
        String debtId,
        String title,
        String description,
        DebtType type,
        DebtSeverity severity,
        int effortPoints,
        int impactScore,
        LocalDate createdAt,
        String createdBy,
        DebtStatus status,
        String relatedComponent
    ) {
        public enum DebtType {
            CODE_QUALITY,
            PERFORMANCE,
            SECURITY,
            TESTING,
            DOCUMENTATION,
            INFRASTRUCTURE
        }

        public enum DebtSeverity {
            CRITICAL,  // Blocking new work
            HIGH,      // Significantly slowing down development
            MEDIUM,    // Noticeable impact
            LOW        // Minor inconvenience
        }

        public enum DebtStatus {
            IDENTIFIED,
            PRIORITIZED,
            IN_PROGRESS,
            RESOLVED,
            WONT_FIX
        }

        public double priorityScore() {
            return impactScore / (double) effortPoints;
        }
    }

    /**
     * Create debt item
     */
    public DebtItem createDebtItem(
        String title,
        DebtItem.DebtType type,
        DebtItem.DebtSeverity severity,
        int effortPoints,
        String description
    ) {
        int impactScore = calculateImpactScore(severity);

        DebtItem debt = new DebtItem(
            UUID.randomUUID().toString(),
            title,
            description,
            type,
            severity,
            effortPoints,
            impactScore,
            LocalDate.now(),
            getCurrentUser(),
            DebtItem.DebtStatus.IDENTIFIED,
            null
        );

        return debtRepository.save(debt);
    }

    private int calculateImpactScore(DebtItem.DebtSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 100;
            case HIGH -> 75;
            case MEDIUM -> 50;
            case LOW -> 25;
        };
    }

    /**
     * Prioritize debt items
     */
    public List<DebtItem> prioritizeDebt(String teamId) {
        List<DebtItem> allDebt = debtRepository.findByStatusNot(DebtItem.DebtStatus.RESOLVED);

        return allDebt.stream()
            .sorted(Comparator.comparingDouble(DebtItem::priorityScore).reversed())
            .toList();
    }

    /**
     * Calculate debt burden
     */
    public record DebtBurden(
        int totalDebtItems,
        int criticalItems,
        int totalEffortPoints,
        Map<DebtItem.DebtType, Integer> debtByType,
        double estimatedSprintsToResolve,
        String healthStatus
    ) {}

    public DebtBurden calculateDebtBurden(String teamId) {
        List<DebtItem> activeDebt = debtRepository
            .findByStatusNot(DebtItem.DebtStatus.RESOLVED);

        long critical = activeDebt.stream()
            .filter(d -> d.severity() == DebtItem.DebtSeverity.CRITICAL)
            .count();

        int totalEffort = activeDebt.stream()
            .mapToInt(DebtItem::effortPoints)
            .sum();

        // Debt by type
        Map<DebtItem.DebtType, Long> debtByTypeCount = activeDebt.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                DebtItem::type,
                java.util.stream.Collectors.counting()
            ));

        Map<DebtItem.DebtType, Integer> debtByType = new HashMap<>();
        debtByTypeCount.forEach((k, v) -> debtByType.put(k, v.intValue()));

        // Estimate sprints to resolve (assuming 20% capacity = 10 points/sprint)
        double sprintsNeeded = totalEffort / 10.0;

        String health = getDebtHealthStatus(critical, totalEffort);

        return new DebtBurden(
            activeDebt.size(),
            (int) critical,
            totalEffort,
            debtByType,
            sprintsNeeded,
            health
        );
    }

    private String getDebtHealthStatus(long criticalCount, int totalEffort) {
        if (criticalCount > 5 || totalEffort > 200) {
            return "Critical - Debt is blocking new work";
        } else if (criticalCount > 2 || totalEffort > 100) {
            return "High - Debt is significantly impacting velocity";
        } else if (totalEffort > 50) {
            return "Medium - Manageable debt load";
        } else {
            return "Low - Healthy debt level";
        }
    }

    private String getCurrentUser() {
        // In real implementation: get from security context
        return "current-user";
    }
}
```

---

## Part 7: Incident Response & On-Call

### Incident Management

**Incident:** Unplanned interruption or reduction in quality of service.

**Severity levels:**

- **SEV1 (Critical):** Complete outage, data loss
  - Response time: Immediate
  - Example: Database down, payments failing

- **SEV2 (High):** Major functionality broken
  - Response time: < 1 hour
  - Example: Search not working, slow API

- **SEV3 (Medium):** Minor functionality broken
  - Response time: < 4 hours
  - Example: UI bug, logging issues

- **SEV4 (Low):** Cosmetic issues
  - Response time: Next business day
  - Example: Typo, color inconsistency

### On-Call Rotations

**On-call:** Engineer responsible for responding to incidents.

**Best practices:**
- ✅ Rotate weekly (not daily or monthly)
- ✅ Compensate with time off or bonus pay
- ✅ Have clear escalation path
- ✅ Document runbooks
- ✅ Limit to business hours for smaller teams

**Handoff process:**
1. Review open incidents
2. Check monitoring dashboards
3. Test alert system
4. Exchange contact info

### Incident Response Process

**1. Detect (Monitoring/Alerts)**
- Automated alerts fire
- Customer reports issue
- Engineer notices problem

**2. Respond**
- Acknowledge alert
- Assess severity
- Create incident channel (#incident-2024-01-15)
- Page additional engineers if needed

**3. Mitigate**
- Stop the bleeding (rollback, kill feature flag)
- Communicate status to stakeholders
- Fix root cause (if quick) or workaround

**4. Resolve**
- Verify fix in production
- Monitor for recurrence
- Mark incident resolved

**5. Learn (Postmortem)**
- What happened?
- What was the impact?
- What went well?
- What went poorly?
- Action items to prevent recurrence

**Blameless postmortems:**
- Focus on systems, not people
- "How did the system allow this?" not "Who caused this?"
- Goal: Learn and improve, not punish

### Code Example: Incident Management Service

```java
@Service
public class IncidentManagementService {

    public record Incident(
        String incidentId,
        String title,
        IncidentSeverity severity,
        IncidentStatus status,
        LocalDateTime startTime,
        LocalDateTime acknowledgedAt,
        LocalDateTime mitigatedAt,
        LocalDateTime resolvedAt,
        String oncallEngineer,
        List<String> affectedServices,
        String description,
        String resolution
    ) {
        public enum IncidentSeverity {
            SEV1,  // Critical
            SEV2,  // High
            SEV3,  // Medium
            SEV4   // Low
        }

        public enum IncidentStatus {
            OPEN,
            ACKNOWLEDGED,
            MITIGATED,
            RESOLVED,
            CLOSED
        }

        public long timeToAcknowledgeMinutes() {
            if (acknowledgedAt == null) return -1;
            return ChronoUnit.MINUTES.between(startTime, acknowledgedAt);
        }

        public long timeToMitigateMinutes() {
            if (mitigatedAt == null) return -1;
            return ChronoUnit.MINUTES.between(startTime, mitigatedAt);
        }

        public long timeToResolveMinutes() {
            if (resolvedAt == null) return -1;
            return ChronoUnit.MINUTES.between(startTime, resolvedAt);
        }
    }

    /**
     * Create incident
     */
    public Incident createIncident(
        String title,
        Incident.IncidentSeverity severity,
        List<String> affectedServices,
        String description
    ) {
        String oncall = getOnCallEngineer();

        Incident incident = new Incident(
            UUID.randomUUID().toString(),
            title,
            severity,
            Incident.IncidentStatus.OPEN,
            LocalDateTime.now(),
            null,
            null,
            null,
            oncall,
            affectedServices,
            description,
            null
        );

        Incident saved = incidentRepository.save(incident);

        // Page on-call engineer
        pageOnCall(saved);

        // Create incident channel
        slackService.createChannel("incident-" + saved.incidentId());

        return saved;
    }

    /**
     * Acknowledge incident
     */
    public Incident acknowledge(String incidentId) {
        Incident incident = incidentRepository.findById(incidentId).orElseThrow();

        Incident acknowledged = new Incident(
            incident.incidentId(),
            incident.title(),
            incident.severity(),
            Incident.IncidentStatus.ACKNOWLEDGED,
            incident.startTime(),
            LocalDateTime.now(),
            incident.mitigatedAt(),
            incident.resolvedAt(),
            incident.oncallEngineer(),
            incident.affectedServices(),
            incident.description(),
            incident.resolution()
        );

        return incidentRepository.save(acknowledged);
    }

    /**
     * Resolve incident
     */
    public Incident resolve(String incidentId, String resolution) {
        Incident incident = incidentRepository.findById(incidentId).orElseThrow();

        Incident resolved = new Incident(
            incident.incidentId(),
            incident.title(),
            incident.severity(),
            Incident.IncidentStatus.RESOLVED,
            incident.startTime(),
            incident.acknowledgedAt(),
            incident.mitigatedAt(),
            LocalDateTime.now(),
            incident.oncallEngineer(),
            incident.affectedServices(),
            incident.description(),
            resolution
        );

        Incident saved = incidentRepository.save(resolved);

        // Notify stakeholders
        notifyResolution(saved);

        // Schedule postmortem for SEV1/SEV2
        if (incident.severity() == Incident.IncidentSeverity.SEV1 ||
            incident.severity() == Incident.IncidentSeverity.SEV2) {
            schedulePostmortem(saved);
        }

        return saved;
    }

    /**
     * Calculate incident metrics
     */
    public record IncidentMetrics(
        int totalIncidents,
        Map<Incident.IncidentSeverity, Integer> incidentsBySeverity,
        double avgTimeToAcknowledgeMinutes,
        double avgTimeToResolveMinutes,
        double mttrMinutes,  // Mean Time To Recovery
        List<String> topAffectedServices
    ) {}

    public IncidentMetrics calculateMetrics(LocalDate startDate, LocalDate endDate) {
        List<Incident> incidents = incidentRepository
            .findByStartTimeBetween(startDate.atStartOfDay(), endDate.atTime(23, 59));

        // Incidents by severity
        Map<Incident.IncidentSeverity, Long> bySeverityCount = incidents.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                Incident::severity,
                java.util.stream.Collectors.counting()
            ));

        Map<Incident.IncidentSeverity, Integer> bySeverity = new HashMap<>();
        bySeverityCount.forEach((k, v) -> bySeverity.put(k, v.intValue()));

        // Time to acknowledge
        double avgAck = incidents.stream()
            .filter(i -> i.acknowledgedAt() != null)
            .mapToLong(Incident::timeToAcknowledgeMinutes)
            .average()
            .orElse(0);

        // Time to resolve (MTTR)
        double avgResolve = incidents.stream()
            .filter(i -> i.resolvedAt() != null)
            .mapToLong(Incident::timeToResolveMinutes)
            .average()
            .orElse(0);

        // Top affected services
        Map<String, Long> serviceCounts = incidents.stream()
            .flatMap(i -> i.affectedServices().stream())
            .collect(java.util.stream.Collectors.groupingBy(
                s -> s,
                java.util.stream.Collectors.counting()
            ));

        List<String> topServices = serviceCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .toList();

        return new IncidentMetrics(
            incidents.size(),
            bySeverity,
            avgAck,
            avgResolve,
            avgResolve,
            topServices
        );
    }

    private String getOnCallEngineer() {
        // In production: fetch from on-call schedule (PagerDuty, Opsgenie)
        return "oncall-engineer-id";
    }

    private void pageOnCall(Incident incident) {
        System.out.println("📟 Paging on-call for " + incident.severity() + ": " + incident.title());
    }

    private void notifyResolution(Incident incident) {
        System.out.println("✅ Incident resolved: " + incident.title());
    }

    private void schedulePostmortem(Incident incident) {
        System.out.println("📝 Postmortem scheduled for incident: " + incident.incidentId());
    }
}
```

---

## Part 8: Scaling Engineering Teams

### Team Topology

**Small Team (1-5 engineers):**
```
Founder/PM
    ↓
Full-stack engineers (everyone does everything)
```

**Medium Team (6-15 engineers):**
```
Engineering Manager
    ├── Frontend team (3)
    ├── Backend team (3)
    └── Infrastructure engineer (1)
```

**Large Team (16-50 engineers):**
```
VP Engineering
    ├── Engineering Manager (Frontend)
    │   └── 5-7 engineers
    ├── Engineering Manager (Backend)
    │   └── 5-7 engineers
    ├── Engineering Manager (Platform)
    │   └── 4-6 engineers
    └── QA Lead
        └── 2-3 QA engineers
```

**Very Large (50+ engineers):**
- Multiple product teams
- Platform/infrastructure teams
- Site Reliability Engineering (SRE)
- Security team
- Data team

### The Two-Pizza Rule

**Amazon's rule:** Team should be small enough to feed with two pizzas (~6-8 people).

**Why:**
- Communication overhead grows exponentially
- 3 people = 3 relationships
- 8 people = 28 relationships
- 15 people = 105 relationships

**Solution:** Split into smaller, autonomous teams.

### Conway's Law

**"Organizations design systems that mirror their communication structure."**

If you have:
- 1 team → You'll build a monolith
- 3 teams (frontend, backend, database) → You'll build a 3-tier architecture
- Many small teams → You'll build microservices

**Implication:** Design your org chart based on desired architecture.

### Hiring & Onboarding

**Hiring process:**
1. **Resume screen** (5 min per resume)
2. **Phone screen** (30 min, basic technical questions)
3. **Take-home** (2-4 hour coding exercise)
4. **Onsite** (4-6 hours):
   - Coding (2 hours)
   - System design (1 hour)
   - Behavioral (1 hour)
   - Team fit (1 hour)
5. **Offer**

**Onboarding checklist:**
- ✅ Week 1: Setup (laptop, accounts, codebase)
- ✅ Week 2: First PR (small bug fix)
- ✅ Week 3: First feature
- ✅ Week 4: Shadow on-call
- ✅ Month 2: Independent on first major project
- ✅ Month 3: Fully ramped

### Scaling Challenges

**5 → 15 engineers:**
- Challenge: Maintaining communication
- Solution: Weekly all-hands, written RFCs

**15 → 50 engineers:**
- Challenge: Coordination between teams
- Solution: Tech leads, platform teams, shared services

**50 → 150 engineers:**
- Challenge: Maintaining culture and quality
- Solution: Engineering leadership team, standards council

**Common scaling mistakes:**
- Hiring too fast (before processes in place)
- Not promoting from within (lose institutional knowledge)
- Over-engineering too early (premature optimization)
- Under-investing in tooling (slows everyone down)

### Code Example: Team Management Service

```java
@Service
public class TeamManagementService {

    public record Team(
        String teamId,
        String name,
        TeamType type,
        List<String> memberIds,
        String managerId,
        List<String> techStack,
        Map<String, Object> metrics
    ) {
        public enum TeamType {
            FRONTEND,
            BACKEND,
            FULL_STACK,
            PLATFORM,
            INFRASTRUCTURE,
            QA,
            DATA
        }

        public int size() {
            return memberIds.size();
        }

        public boolean needsSplit() {
            return size() > 8;  // Two-pizza rule
        }
    }

    public record Engineer(
        String engineerId,
        String name,
        String email,
        EngineerLevel level,
        List<String> skills,
        LocalDate hireDate,
        String managerId,
        String teamId
    ) {
        public enum EngineerLevel {
            JUNIOR,
            MID,
            SENIOR,
            STAFF,
            PRINCIPAL
        }

        public long tenureMonths() {
            return ChronoUnit.MONTHS.between(hireDate, LocalDate.now());
        }
    }

    /**
     * Calculate team health
     */
    public record TeamHealth(
        String teamId,
        int teamSize,
        boolean needsSplit,
        double avgTenureMonths,
        Map<Engineer.EngineerLevel, Integer> levelDistribution,
        double juniorRatio,
        String healthStatus,
        List<String> recommendations
    ) {}

    public TeamHealth assessTeamHealth(String teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow();
        List<Engineer> members = engineerRepository.findByTeamId(teamId);

        double avgTenure = members.stream()
            .mapToLong(Engineer::tenureMonths)
            .average()
            .orElse(0);

        // Level distribution
        Map<Engineer.EngineerLevel, Long> levelCount = members.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                Engineer::level,
                java.util.stream.Collectors.counting()
            ));

        Map<Engineer.EngineerLevel, Integer> levelDist = new HashMap<>();
        levelCount.forEach((k, v) -> levelDist.put(k, v.intValue()));

        // Junior ratio
        long juniorCount = members.stream()
            .filter(e -> e.level() == Engineer.EngineerLevel.JUNIOR)
            .count();
        double juniorRatio = members.isEmpty() ? 0 : juniorCount / (double) members.size();

        String status = determineHealthStatus(team.size(), juniorRatio, avgTenure);
        List<String> recommendations = generateRecommendations(team, juniorRatio, avgTenure);

        return new TeamHealth(
            teamId,
            team.size(),
            team.needsSplit(),
            avgTenure,
            levelDist,
            juniorRatio,
            status,
            recommendations
        );
    }

    private String determineHealthStatus(int size, double juniorRatio, double avgTenure) {
        if (size > 10) return "At Risk - Team too large";
        if (juniorRatio > 0.5) return "At Risk - Too many juniors";
        if (avgTenure < 6) return "At Risk - High turnover";
        if (size < 3) return "At Risk - Team too small";
        return "Healthy";
    }

    private List<String> generateRecommendations(Team team, double juniorRatio, double avgTenure) {
        List<String> recs = new ArrayList<>();

        if (team.needsSplit()) {
            recs.add("🔀 Split team into two (current size: " + team.size() + ")");
        }

        if (juniorRatio > 0.5) {
            recs.add("👤 Hire more senior engineers (current: " +
                String.format("%.0f%%", juniorRatio * 100) + " junior)");
        }

        if (avgTenure < 6) {
            recs.add("⏱️ Focus on retention (avg tenure: " +
                String.format("%.1f", avgTenure) + " months)");
        }

        if (team.size() < 3) {
            recs.add("➕ Grow team (minimum recommended: 3-4 engineers)");
        }

        if (recs.isEmpty()) {
            recs.add("✅ Team size and composition are healthy");
        }

        return recs;
    }

    /**
     * Onboarding progress tracking
     */
    public record OnboardingProgress(
        String engineerId,
        int daysAtCompany,
        List<OnboardingMilestone> milestones,
        double completionPercentage,
        boolean onTrack
    ) {}

    public record OnboardingMilestone(
        String title,
        int targetDay,
        boolean completed,
        LocalDate completedAt
    ) {}

    public OnboardingProgress trackOnboarding(String engineerId) {
        Engineer engineer = engineerRepository.findById(engineerId).orElseThrow();
        long daysAtCompany = ChronoUnit.DAYS.between(engineer.hireDate(), LocalDate.now());

        List<OnboardingMilestone> milestones = List.of(
            checkMilestone("Setup complete", 5, engineerId),
            checkMilestone("First PR merged", 10, engineerId),
            checkMilestone("First feature shipped", 20, engineerId),
            checkMilestone("Shadowed on-call", 25, engineerId),
            checkMilestone("Leading first project", 60, engineerId)
        );

        long completed = milestones.stream()
            .filter(OnboardingMilestone::completed)
            .count();

        double percentage = (completed / (double) milestones.size()) * 100;

        // On track if completed milestones match days at company
        boolean onTrack = true;
        for (OnboardingMilestone m : milestones) {
            if (daysAtCompany >= m.targetDay() && !m.completed()) {
                onTrack = false;
                break;
            }
        }

        return new OnboardingProgress(
            engineerId,
            (int) daysAtCompany,
            milestones,
            percentage,
            onTrack
        );
    }

    private OnboardingMilestone checkMilestone(String title, int targetDay, String engineerId) {
        // In production: query actual milestone completion records
        return new OnboardingMilestone(title, targetDay, false, null);
    }
}
```

---

## Practical Assignments

### Assignment 1: Build Sprint Management System

Implement sprint planning and tracking with:
- Sprint creation and story management
- Story point estimation
- Velocity calculation
- Sprint report generation

Test with 4 sprints and track velocity trend.

### Assignment 2: Implement Code Review Analytics

Build code review metrics dashboard:
- Track PR size, review time, approval rate
- Identify bottlenecks
- Generate recommendations

Analyze 20+ PRs and identify patterns.

### Assignment 3: Create 1-on-1 Tracker

Design 1-on-1 management system:
- Schedule recurring meetings
- Track topics and action items
- Monitor meeting completion rate
- Alert on missed meetings

Simulate 3 months of 1-on-1s for 5 engineers.

### Assignment 4: Technical Debt Register

Build technical debt tracking tool:
- Create and prioritize debt items
- Calculate debt burden
- Generate prioritization matrix
- Track resolution progress

Add 10 debt items and prioritize using impact/effort scoring.

### Assignment 5: Incident Management System

Implement incident response workflow:
- Create incidents with severity levels
- Track time to acknowledge/resolve
- Calculate MTTR
- Generate incident reports

Simulate 5 incidents and measure response metrics.

---

## Reflection Questions

1. **Management vs IC:** When should an engineer stay as IC vs become a manager?

2. **Sprint length:** Is 1-week or 2-week sprints better? Why?

3. **Code review:** Should approval be required from senior engineers only, or anyone?

4. **Technical debt:** Should you stop all new features to pay down debt, or always balance?

5. **On-call:** Should on-call be voluntary or required? How do you compensate fairly?

6. **Team size:** Is 6 engineers always the optimal size, or does it depend on the product?

7. **Remote work:** How do you manage a distributed team differently than co-located?

8. **Metrics:** Can you over-measure and kill team morale? Where's the line?

---

## Key Takeaways

### Engineering Management is Force Multiplication

- 1 engineer writes code
- 1 manager leading 6 engineers = 6x output
- Great manager improving team by 30% = 7.8x output
- Plus: removing blockers, preventing burnout, improving quality

### Agile is About Predictability

- Sprints create rhythm and predictability
- Velocity helps with capacity planning
- Retrospectives drive continuous improvement
- Small, frequent releases reduce risk

### Productivity ≠ Hours Worked

- Focus on output, not input
- Protect maker time (4-hour blocks)
- Eliminate context switching
- Measure outcomes (features shipped), not activity (lines written)

### Code Review is Knowledge Sharing

- Catch bugs before production
- Spread knowledge across team
- Mentor junior engineers
- Maintain quality standards
- Keep PRs small (< 400 lines)

### 1-on-1s Build Trust

- Weekly 30-minute meetings
- Engineer's agenda > manager's agenda
- Career development and feedback
- Surface issues early
- Never cancel

### Technical Debt is Inevitable

- Some debt is okay (deliberate trade-offs)
- Track it, prioritize it, budget for it
- 20% of sprint capacity on debt
- High debt = slow velocity

### Incidents are Learning Opportunities

- Blameless postmortems
- Focus on systems, not people
- Document and share learnings
- Action items to prevent recurrence

### Teams Scale, But Communication Doesn't

- Keep teams small (6-8 people)
- Conway's Law: org chart → architecture
- Invest in processes before scaling
- Hire for culture fit, train for skills

---

## Looking Ahead: Week 45

Next week: **Organizational Design & Culture**

You'll learn:
- Company organizational structures
- Building engineering culture
- Values and mission design
- Remote vs hybrid vs in-office
- Diversity, equity, and inclusion
- Communication patterns at scale
- Decision-making frameworks

Plus: Designing Bibby's organizational structure for growth.

---

## From Your Engineering Manager

"Management is not 'giving up coding.' It's force multiplication.

As an engineer, you impact what you build.
As a manager, you impact what your team builds.
As a leader, you impact what the entire company builds.

The best engineering managers I've known:
- Protect their team's time ruthlessly
- Give feedback often and kindly
- Remove blockers proactively
- Celebrate wins publicly, address issues privately
- Lead with empathy and data

Management is hard. It's messy. It's about people, not just processes.

But done well, it's incredibly rewarding. You're building teams, not just products.

Now go lead with intention."

—Your Engineering Manager

---

## Progress Tracker

**Week 44 of 52 complete (85% complete)**

**Semester 4 (Weeks 40-52): Execution, Revenue & Scale**
- ✅ Week 40: Revenue Architecture & Billing Systems
- ✅ Week 41: Sales Processes & Pipeline Management
- ✅ Week 42: Customer Success & Account Management
- ✅ Week 44: Engineering Management & Productivity ← **You are here**
- ⬜ Week 45: Organizational Design & Culture
- ⬜ Week 46-51: Additional topics
- ✅ Week 52: Final Capstone Project (completed!)

---

**End of Week 44**
