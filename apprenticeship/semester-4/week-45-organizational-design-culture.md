# Week 45: Organizational Design & Culture

## Overview

This week focuses on designing organizational structures that scale and building company cultures that attract top talent and drive performance. You'll learn how to structure teams, define values, and create systems that preserve culture as you grow from 5 to 500 people.

**Learning Objectives:**
- Design effective organizational structures for different stages
- Build and maintain strong company culture
- Create decision-making frameworks that scale
- Implement communication systems for distributed teams
- Define and live company values
- Measure and improve organizational health
- Navigate remote, hybrid, and in-office work models
- Scale culture without losing what makes you special

**Key Concepts:**
- Functional vs divisional vs matrix organizations
- Span of control and layers of management
- Conway's Law (org structure mirrors product architecture)
- Culture as competitive advantage
- Values vs virtues
- Transparency and information sharing
- Remote-first vs remote-friendly
- Organizational debt

---

## Part 1: Organizational Design Fundamentals

### 1.1 Why Org Design Matters

Poor organizational design leads to:
- Slow decision-making
- Duplicated work
- Unclear ownership
- Talent leaving
- Product shipping delays
- Cultural erosion

**Good org design enables:**
- Clear accountability
- Fast execution
- Career growth paths
- Efficient communication
- Scalable processes

### 1.2 Organizational Structures

**1. Functional Structure (Early Stage: 1-20 people)**

```
        CEO
         |
    _____|_____
   |     |     |
  Eng  Product Marketing
```

**Pros:**
- Clear expertise areas
- Easy to understand
- Efficient resource use

**Cons:**
- Silos between functions
- Slow cross-functional work
- Hard to scale beyond ~50 people

**When to use:** Pre-product/market fit, single product

**2. Divisional Structure (Growth Stage: 50-200 people)**

```
           CEO
            |
     _______|_______
    |       |       |
  Team A  Team B  Team C
    |       |       |
  E/P/D   E/P/D   E/P/D

E = Engineering
P = Product
D = Design
```

**Pros:**
- Teams own outcomes end-to-end
- Fast execution
- Clear accountability

**Cons:**
- Duplicated functions
- Less knowledge sharing
- Resource inefficiency

**When to use:** Multiple products, post-PMF, scaling phase

**3. Matrix Structure (Scale Stage: 200+ people)**

```
           CEO
            |
    _______|_______
   |       |       |
  Eng    Product  Design
   |       |       |
  [Product Teams have dotted-line reporting]
   |       |       |
  Team A  Team B  Team C
```

**Pros:**
- Balance expertise and execution
- Resource efficiency
- Knowledge sharing

**Cons:**
- Complex reporting
- Potential conflicts
- Requires mature processes

**When to use:** Large organizations, multiple products, need for specialization

### 1.3 Conway's Law

"Organizations design systems that mirror their communication structure."

**Example:**

```
Org Structure:
- Separate iOS and Android teams
- Separate backend team

Product Result:
- iOS and Android apps feel different
- Mobile and web out of sync
- APIs don't match mobile needs

Better Structure:
- Cross-functional feature teams
- Platform team supports all

Better Product:
- Consistent experience
- Unified roadmap
- APIs designed for clients
```

**Takeaway:** Design your org to reflect your desired product architecture.

### 1.4 Span of Control

**Span of Control:** Number of direct reports per manager

**Narrow Span (3-5 reports):**
- More hands-on management
- Better for inexperienced teams
- More layers of hierarchy
- Slower decision-making

**Wide Span (8-12 reports):**
- More autonomous teams
- Fewer layers
- Faster decisions
- Requires experienced reports

**Recommended:**
- Early stage: 5-7 reports
- Growth stage: 6-8 reports
- Scale stage: 7-10 reports

### 1.5 The Rule of 40/150

**Rule of 40:**
- Teams larger than ~40 people need structure
- Sub-teams of 5-8 people
- Clear leads for each sub-team

**Dunbar's Number (150):**
- Humans can maintain ~150 meaningful relationships
- Beyond 150 people, you need formal systems
- Can't rely on "everyone knows everyone"

**Organizational Phases:**

```
1-10: Everyone does everything
10-40: Function specialists emerge
40-150: Teams form, managers needed
150-500: Departments, directors, VPs
500+: Business units, executive team
```

---

## Part 2: Building Company Culture

### 2.1 What is Culture?

Culture is:
- How decisions are made when no one is watching
- What behaviors get rewarded vs punished
- The unwritten rules of "how we do things here"

Culture is NOT:
- ❌ Ping pong tables and free snacks
- ❌ Mission statement on the wall
- ❌ What founders say in all-hands

**Culture = Values × Behaviors × Systems**

### 2.2 Defining Values

**Bad Values:**
- Generic ("Innovation," "Excellence")
- Not actionable
- Not differentiated
- Not lived

**Good Values:**
- Specific and memorable
- Behavioral (can observe)
- Differentiated (not every company)
- Used in decisions

**Example: Netflix Culture**

```
Bad: "We value teamwork"
Good: "We are a team, not a family"
→ Actionable: We give feedback directly
→ Differentiated: High performers only
→ Lived: We have "keeper tests" for managers
```

**Example: Amazon Leadership Principles**

```
"Customer Obsession"
→ Start with customer, work backwards
→ Used in every decision and review

"Bias for Action"
→ Speed matters, be decisive
→ Don't wait for perfect information
```

**Creating Values for Bibby:**

```
1. Readers First
   - Every decision starts with: "Is this better for readers?"
   - We ship features readers love, not what's easiest to build

2. Move Fast, Build Trust
   - Ship weekly, iterate based on feedback
   - Speed without quality is recklessness

3. Default to Transparency
   - Share context, decisions, and metrics openly
   - Trust the team with information

4. Compound Learning
   - Every project makes you better
   - Share knowledge, write docs, teach others

5. Own the Outcome
   - We own results, not tasks
   - Disagree and commit
```

### 2.3 Living Your Values

**How to embed values:**

**1. Hiring:**
- Screen for values fit in interviews
- Ask behavioral questions that test values
- Reject candidates who don't match

**2. Performance Reviews:**
- Evaluate on values, not just results
- Reward value-aligned behavior
- Let go of values mis-matches

**3. Recognition:**
- Call out examples of values in action
- Peer recognition programs
- Values-based awards

**4. Decision-Making:**
- Reference values in meetings
- Use values to break ties
- Document how values influenced decisions

**5. Storytelling:**
- Share stories of values in action
- Founders model values publicly
- Celebrate values wins

### 2.4 Remote vs Hybrid vs In-Office

**Remote-First (Bibby's Approach):**

**Principles:**
- Default to async communication
- Document everything
- Optimize for distributed collaboration
- In-person time is strategic

**Benefits:**
- Access global talent
- Lower costs (no office)
- Flexibility for employees
- Forced documentation

**Challenges:**
- Harder to build relationships
- Timezone coordination
- Onboarding complexity
- Less spontaneous collaboration

**Best Practices:**

```
Communication:
- Async by default (Slack, email, docs)
- Sync for brainstorming, decisions, bonding
- Record meetings for async viewing
- Over-communicate context

Collaboration:
- Shared docs (Notion, Google Docs)
- Design tools (Figma for async feedback)
- Code reviews (GitHub)
- Daily standups (async or 15-min sync)

Connection:
- Weekly team meetings
- Monthly all-hands
- Quarterly offsites (in-person)
- Virtual coffee chats
- Donut pairings (random 1:1s)

Boundaries:
- Respect timezones
- No expectation of instant response
- Core hours (e.g., 10am-2pm PT overlap)
- Encourage time off
```

### 2.5 Communication Frameworks

**1. Levels of Communication**

```
Level 1: FYI
- Info sharing, no response needed
- Use: Email, Slack channels, docs

Level 2: Input Wanted
- Seeking feedback
- Use: Slack threads, doc comments, meetings

Level 3: Decision Needed
- Clear decision point
- Use: Meetings, decision docs, polls

Level 4: Urgent
- Needs immediate attention
- Use: Phone, DM, @mention
```

**2. RACI Matrix (Decision Rights)**

For each decision, assign:
- **R**esponsible: Does the work
- **A**ccountable: Final decision-maker (only 1 person)
- **C**onsulted: Input before decision
- **I**nformed: Told after decision

**Example: Launching New Feature**

| Role | R | A | C | I |
|------|---|---|---|---|
| Product Manager | ✓ | ✓ | | |
| Engineer | ✓ | | | |
| Designer | ✓ | | | |
| Marketing | | | ✓ | |
| CEO | | | ✓ | |
| All Employees | | | | ✓ |

**3. DRI (Directly Responsible Individual)**

Every project has exactly ONE DRI:
- Makes final decisions
- Unblocks issues
- Communicates status
- Accountable for outcome

Not a dictator—gathers input, but decides.

### 2.6 Scaling Culture

**Challenges as you grow:**

```
1-10: Culture = Founders' personalities
10-50: Need to codify values
50-150: Need systems to reinforce culture
150-500: Need dedicated culture team
500+: Need to preserve across locations/divisions
```

**How culture dilutes:**
- New hires bring different norms
- Remote offices develop sub-cultures
- Acquisitions import foreign cultures
- Growth prioritized over culture fit
- Founders less involved in daily work

**How to preserve culture:**

**1. Intentional Hiring:**
- Values screening in every interview
- "Culture add" not "culture fit"
- Reject bad values fits, even strong skills

**2. Strong Onboarding:**
- Week 1: Culture deep dive
- Assign culture buddy
- Founders meet every new hire
- Share company stories

**3. Reinforcement Systems:**
- Values in performance reviews
- Peer recognition
- All-hands storytelling
- Promotion criteria includes values

**4. Transparency:**
- Share metrics, decisions, strategy
- Open Q&A in all-hands
- Written decision docs
- Public roadmaps

**5. Rituals:**
- Weekly demos
- Monthly all-hands
- Quarterly offsites
- Annual retreats

---

## Part 3: Decision-Making Frameworks

### 3.1 Decision-Making Models

**1. Consensus (Everyone Agrees)**
- Slow, frustrating
- Leads to mediocre decisions
- ❌ Don't use for most decisions

**2. Democracy (Vote)**
- Faster than consensus
- Can create factions
- ⚠️ Use sparingly (team lunches, office location)

**3. Consultative (Gather Input, DRI Decides)**
- Fast
- Informed decisions
- Clear accountability
- ✅ Best for most decisions

**4. Command (Leader Decides)**
- Fastest
- Low buy-in
- ⚠️ Use only in crisis or for trivial decisions

**Bibby's Approach: Consultative by Default**

```
Process:
1. DRI writes decision doc
2. Gather input (async + meeting)
3. DRI decides
4. Communicate decision + reasoning
5. Team commits (even if they disagree)
```

### 3.2 Disagree and Commit

**Principle:** Once a decision is made, everyone commits fully—even those who disagreed.

**Why it matters:**
- Prevents endless debates
- Ensures aligned execution
- Builds trust (your opinion was heard)

**How to practice:**

```
Bad:
"I told you this wouldn't work."
→ Undermines decision, divides team

Good:
"I disagreed, but we decided to try X. I'm committed to making it succeed."
→ Supports decision, unites team
```

**Leader's role:**
- Actively seek dissenting opinions
- Explain reasoning clearly
- Give people space to disagree
- Expect commitment once decided

### 3.3 One-Way vs Two-Way Doors

**Amazon's framework for decision speed:**

**Two-Way Doors (Reversible):**
- Easy to undo
- Low cost to reverse
- Examples: UI changes, pricing tests, marketing campaigns
- **Decision process:** Fast, low-level DRI, minimal review

**One-Way Doors (Irreversible):**
- Hard/expensive to undo
- High impact
- Examples: Acquisitions, major pivots, layoffs
- **Decision process:** Slow, senior leadership, extensive review

**Bias:** Treat more decisions as two-way doors. Most are reversible.

---

## Part 4: Practical Implementation

### 4.1 Organization Management System

Track organizational structure and health:

```java
package com.bibby.org;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrganizationService {

    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;

    public OrganizationService(
        EmployeeRepository employeeRepository,
        TeamRepository teamRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.teamRepository = teamRepository;
    }

    // ============= MODELS =============

    @Document(collection = "employees")
    public record Employee(
        @Id String employeeId,
        String firstName,
        String lastName,
        String email,
        String role,
        String level,  // IC1, IC2, M1, M2, etc.
        String department,
        String teamId,
        String managerId,
        LocalDate startDate,
        EmploymentStatus status,
        Map<String, Object> metadata
    ) {
        public enum EmploymentStatus {
            ACTIVE, ON_LEAVE, TERMINATED
        }

        public String fullName() {
            return firstName + " " + lastName;
        }

        public boolean isManager() {
            return level != null && level.startsWith("M");
        }

        public int yearsAtCompany() {
            return java.time.Period.between(startDate, LocalDate.now()).getYears();
        }
    }

    @Document(collection = "teams")
    public record Team(
        @Id String teamId,
        String name,
        String mission,
        TeamType type,
        String leaderId,
        List<String> memberIds,
        String parentTeamId,
        Map<String, String> metrics
    ) {
        public enum TeamType {
            ENGINEERING,
            PRODUCT,
            DESIGN,
            MARKETING,
            SALES,
            OPERATIONS,
            CROSS_FUNCTIONAL
        }

        public int size() {
            return memberIds.size();
        }
    }

    // ============= ORGANIZATIONAL METRICS =============

    public record OrgMetrics(
        int totalEmployees,
        int activeEmployees,
        Map<String, Integer> byDepartment,
        Map<String, Integer> byLevel,
        double averageTenure,
        int managersCount,
        double averageSpanOfControl,
        int layersOfHierarchy,
        Map<String, TeamHealth> teamHealth
    ) {}

    public record TeamHealth(
        String teamId,
        String teamName,
        int size,
        double avgTenure,
        int spanOfControl,
        boolean hasOpenHeadcount,
        double retentionRate
    ) {}

    public OrgMetrics calculateOrgMetrics() {
        List<Employee> allEmployees = employeeRepository.findAll();
        List<Employee> active = allEmployees.stream()
            .filter(e -> e.status() == Employee.EmploymentStatus.ACTIVE)
            .toList();

        // By department
        Map<String, Integer> byDept = active.stream()
            .collect(Collectors.groupingBy(
                Employee::department,
                Collectors.summingInt(e -> 1)
            ));

        // By level
        Map<String, Integer> byLevel = active.stream()
            .collect(Collectors.groupingBy(
                Employee::level,
                Collectors.summingInt(e -> 1)
            ));

        // Average tenure
        double avgTenure = active.stream()
            .mapToDouble(Employee::yearsAtCompany)
            .average()
            .orElse(0.0);

        // Managers
        int managersCount = (int) active.stream()
            .filter(Employee::isManager)
            .count();

        // Span of control
        Map<String, Long> directReports = active.stream()
            .filter(e -> e.managerId() != null)
            .collect(Collectors.groupingBy(
                Employee::managerId,
                Collectors.counting()
            ));

        double avgSpan = directReports.values().stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);

        // Layers of hierarchy
        int layers = calculateHierarchyDepth();

        // Team health
        Map<String, TeamHealth> teamHealth = calculateTeamHealth();

        return new OrgMetrics(
            allEmployees.size(),
            active.size(),
            byDept,
            byLevel,
            avgTenure,
            managersCount,
            avgSpan,
            layers,
            teamHealth
        );
    }

    private int calculateHierarchyDepth() {
        // Find CEO (employee with no manager)
        Optional<Employee> ceo = employeeRepository.findAll().stream()
            .filter(e -> e.managerId() == null)
            .findFirst();

        if (ceo.isEmpty()) return 0;

        return calculateDepth(ceo.get().employeeId(), 1);
    }

    private int calculateDepth(String employeeId, int currentDepth) {
        List<Employee> reports = employeeRepository.findByManagerId(employeeId);

        if (reports.isEmpty()) {
            return currentDepth;
        }

        return reports.stream()
            .mapToInt(r -> calculateDepth(r.employeeId(), currentDepth + 1))
            .max()
            .orElse(currentDepth);
    }

    private Map<String, TeamHealth> calculateTeamHealth() {
        List<Team> allTeams = teamRepository.findAll();
        Map<String, TeamHealth> health = new HashMap<>();

        for (Team team : allTeams) {
            List<Employee> members = employeeRepository
                .findAllById(team.memberIds());

            double avgTenure = members.stream()
                .mapToDouble(Employee::yearsAtCompany)
                .average()
                .orElse(0.0);

            Employee leader = employeeRepository
                .findById(team.leaderId())
                .orElse(null);

            int spanOfControl = leader != null ?
                employeeRepository.findByManagerId(leader.employeeId()).size() : 0;

            health.put(team.teamId(), new TeamHealth(
                team.teamId(),
                team.name(),
                team.size(),
                avgTenure,
                spanOfControl,
                false,  // Would check actual headcount
                0.95    // Mock retention rate
            ));
        }

        return health;
    }

    // ============= ORG CHART GENERATION =============

    public record OrgChartNode(
        String employeeId,
        String name,
        String role,
        String level,
        List<OrgChartNode> directReports
    ) {}

    public OrgChartNode generateOrgChart() {
        // Find CEO (no manager)
        Optional<Employee> ceo = employeeRepository.findAll().stream()
            .filter(e -> e.managerId() == null)
            .findFirst();

        if (ceo.isEmpty()) {
            throw new RuntimeException("No CEO found");
        }

        return buildOrgChartNode(ceo.get());
    }

    private OrgChartNode buildOrgChartNode(Employee employee) {
        List<Employee> reports = employeeRepository
            .findByManagerId(employee.employeeId());

        List<OrgChartNode> reportNodes = reports.stream()
            .map(this::buildOrgChartNode)
            .collect(Collectors.toList());

        return new OrgChartNode(
            employee.employeeId(),
            employee.fullName(),
            employee.role(),
            employee.level(),
            reportNodes
        );
    }

    // ============= SPAN OF CONTROL ANALYSIS =============

    public record SpanAnalysis(
        String managerId,
        String managerName,
        int directReports,
        SpanStatus status,
        String recommendation
    ) {
        public enum SpanStatus {
            HEALTHY,      // 5-8 reports
            TOO_NARROW,   // <5 reports
            TOO_WIDE      // >10 reports
        }
    }

    public List<SpanAnalysis> analyzeSpanOfControl() {
        List<Employee> managers = employeeRepository.findAll().stream()
            .filter(Employee::isManager)
            .toList();

        return managers.stream()
            .map(manager -> {
                int reports = employeeRepository
                    .findByManagerId(manager.employeeId())
                    .size();

                SpanAnalysis.SpanStatus status;
                String recommendation;

                if (reports < 5) {
                    status = SpanAnalysis.SpanStatus.TOO_NARROW;
                    recommendation = "Consider consolidating teams or adding more reports";
                } else if (reports > 10) {
                    status = SpanAnalysis.SpanStatus.TOO_WIDE;
                    recommendation = "Consider adding a layer or splitting team";
                } else {
                    status = SpanAnalysis.SpanStatus.HEALTHY;
                    recommendation = "Span of control is healthy";
                }

                return new SpanAnalysis(
                    manager.employeeId(),
                    manager.fullName(),
                    reports,
                    status,
                    recommendation
                );
            })
            .collect(Collectors.toList());
    }
}

@Repository
interface EmployeeRepository extends MongoRepository<OrganizationService.Employee, String> {
    List<OrganizationService.Employee> findByManagerId(String managerId);
    List<OrganizationService.Employee> findByDepartment(String department);
    List<OrganizationService.Employee> findByTeamId(String teamId);
    List<OrganizationService.Employee> findByStatus(
        OrganizationService.Employee.EmploymentStatus status);
}

@Repository
interface TeamRepository extends MongoRepository<OrganizationService.Team, String> {
    List<OrganizationService.Team> findByType(OrganizationService.Team.TeamType type);
    List<OrganizationService.Team> findByLeaderId(String leaderId);
}
```

### 4.2 Culture Survey System

Measure and track culture health:

```java
package com.bibby.org;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CultureSurveyService {

    private final SurveyResponseRepository responseRepository;

    public CultureSurveyService(SurveyResponseRepository responseRepository) {
        this.responseRepository = responseRepository;
    }

    @Document(collection = "survey_responses")
    public record SurveyResponse(
        @Id String responseId,
        String employeeId,
        LocalDate surveyDate,
        SurveyType type,
        Map<String, Integer> scores,  // question -> score (1-5)
        Map<String, String> openEnded,
        boolean anonymous
    ) {
        public enum SurveyType {
            QUARTERLY_PULSE,
            ONBOARDING,
            EXIT,
            VALUES_ALIGNMENT,
            MANAGER_FEEDBACK
        }

        public double averageScore() {
            return scores.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        }
    }

    // ============= CULTURE METRICS =============

    public record CultureHealth(
        LocalDate asOfDate,
        double overallScore,
        Map<String, Double> dimensionScores,
        Map<String, Double> trendVsPrevious,
        List<String> strengths,
        List<String> concerns,
        int responseRate,
        Map<String, Double> scoresByDepartment
    ) {}

    public CultureHealth calculateCultureHealth(LocalDate asOfDate) {
        LocalDate previousQuarter = asOfDate.minusMonths(3);

        List<SurveyResponse> currentResponses = responseRepository
            .findByTypeAndSurveyDateBetween(
                SurveyResponse.SurveyType.QUARTERLY_PULSE,
                asOfDate.minusMonths(1),
                asOfDate
            );

        List<SurveyResponse> previousResponses = responseRepository
            .findByTypeAndSurveyDateBetween(
                SurveyResponse.SurveyType.QUARTERLY_PULSE,
                previousQuarter.minusMonths(1),
                previousQuarter
            );

        // Overall score
        double overallScore = currentResponses.stream()
            .mapToDouble(SurveyResponse::averageScore)
            .average()
            .orElse(0.0);

        // Dimension scores
        Map<String, Double> dimensionScores = calculateDimensionScores(currentResponses);

        // Trend
        Map<String, Double> previousDimensionScores =
            calculateDimensionScores(previousResponses);

        Map<String, Double> trends = new HashMap<>();
        for (String dimension : dimensionScores.keySet()) {
            double current = dimensionScores.get(dimension);
            double previous = previousDimensionScores.getOrDefault(dimension, current);
            trends.put(dimension, current - previous);
        }

        // Identify strengths and concerns
        List<String> strengths = dimensionScores.entrySet().stream()
            .filter(e -> e.getValue() >= 4.0)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        List<String> concerns = dimensionScores.entrySet().stream()
            .filter(e -> e.getValue() < 3.5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Response rate (mock)
        int responseRate = 87;

        // By department (mock)
        Map<String, Double> byDept = Map.of(
            "Engineering", 4.2,
            "Product", 4.1,
            "Design", 4.3
        );

        return new CultureHealth(
            asOfDate,
            overallScore,
            dimensionScores,
            trends,
            strengths,
            concerns,
            responseRate,
            byDept
        );
    }

    private Map<String, Double> calculateDimensionScores(
        List<SurveyResponse> responses
    ) {
        // Group questions by dimension
        Map<String, List<String>> dimensionQuestions = Map.of(
            "Mission & Purpose", List.of("mission_clear", "mission_inspiring", "impact_visible"),
            "Growth & Development", List.of("learning_opportunities", "career_growth", "feedback_quality"),
            "Work-Life Balance", List.of("workload_reasonable", "flexibility", "burnout_prevention"),
            "Collaboration", List.of("team_trust", "cross_team_collaboration", "psychological_safety"),
            "Leadership", List.of("leadership_trust", "decision_transparency", "values_alignment"),
            "Recognition", List.of("recognition_frequency", "recognition_meaningful", "compensation_fair")
        );

        Map<String, Double> dimensionScores = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : dimensionQuestions.entrySet()) {
            String dimension = entry.getKey();
            List<String> questions = entry.getValue();

            double avgScore = responses.stream()
                .flatMap(r -> questions.stream()
                    .map(q -> r.scores().getOrDefault(q, 0)))
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

            dimensionScores.put(dimension, avgScore);
        }

        return dimensionScores;
    }

    // ============= ENPS (Employee NET PROMOTER SCORE) =============

    public record ENPSScore(
        int promoters,      // 9-10
        int passives,       // 7-8
        int detractors,     // 0-6
        double enpsScore,   // (promoters% - detractors%) * 100
        String status       // "Excellent", "Good", "Needs Improvement"
    ) {}

    public ENPSScore calculateENPS(LocalDate asOfDate) {
        List<SurveyResponse> responses = responseRepository
            .findByTypeAndSurveyDateBetween(
                SurveyResponse.SurveyType.QUARTERLY_PULSE,
                asOfDate.minusMonths(1),
                asOfDate
            );

        int total = responses.size();
        int promoters = 0;
        int passives = 0;
        int detractors = 0;

        for (SurveyResponse response : responses) {
            Integer score = response.scores().get("recommend_company");
            if (score != null) {
                if (score >= 9) {
                    promoters++;
                } else if (score >= 7) {
                    passives++;
                } else {
                    detractors++;
                }
            }
        }

        double promoterPct = (promoters / (double) total) * 100;
        double detractorPct = (detractors / (double) total) * 100;
        double enps = promoterPct - detractorPct;

        String status;
        if (enps > 50) {
            status = "Excellent";
        } else if (enps > 30) {
            status = "Good";
        } else if (enps > 0) {
            status = "Fair";
        } else {
            status = "Needs Improvement";
        }

        return new ENPSScore(promoters, passives, detractors, enps, status);
    }

    // ============= VALUES ALIGNMENT TRACKING =============

    public record ValuesAlignment(
        String valueName,
        double alignmentScore,  // How well employees feel company lives this value
        double importanceScore, // How important employees think this value is
        double gap,             // importance - alignment (positive = underdelivering)
        List<String> exampleBehaviors,
        List<String> concerns
    ) {}

    public List<ValuesAlignment> analyzeValuesAlignment() {
        List<SurveyResponse> responses = responseRepository
            .findByType(SurveyResponse.SurveyType.VALUES_ALIGNMENT);

        List<String> companyValues = List.of(
            "Readers First",
            "Move Fast, Build Trust",
            "Default to Transparency",
            "Compound Learning",
            "Own the Outcome"
        );

        return companyValues.stream()
            .map(value -> {
                String alignmentKey = value.toLowerCase().replaceAll("\\s+", "_") + "_alignment";
                String importanceKey = value.toLowerCase().replaceAll("\\s+", "_") + "_importance";

                double alignment = responses.stream()
                    .mapToInt(r -> r.scores().getOrDefault(alignmentKey, 0))
                    .average()
                    .orElse(0.0);

                double importance = responses.stream()
                    .mapToInt(r -> r.scores().getOrDefault(importanceKey, 0))
                    .average()
                    .orElse(0.0);

                double gap = importance - alignment;

                // Extract open-ended examples and concerns
                List<String> examples = responses.stream()
                    .map(r -> r.openEnded().get(value + "_example"))
                    .filter(Objects::nonNull)
                    .limit(3)
                    .collect(Collectors.toList());

                List<String> concerns = responses.stream()
                    .map(r -> r.openEnded().get(value + "_concern"))
                    .filter(Objects::nonNull)
                    .limit(3)
                    .collect(Collectors.toList());

                return new ValuesAlignment(
                    value,
                    alignment,
                    importance,
                    gap,
                    examples,
                    concerns
                );
            })
            .sorted((a, b) -> Double.compare(b.gap(), a.gap()))  // Sort by biggest gaps
            .collect(Collectors.toList());
    }
}

@org.springframework.stereotype.Repository
interface SurveyResponseRepository extends MongoRepository<CultureSurveyService.SurveyResponse, String> {
    List<CultureSurveyService.SurveyResponse> findByType(
        CultureSurveyService.SurveyResponse.SurveyType type);
    List<CultureSurveyService.SurveyResponse> findByTypeAndSurveyDateBetween(
        CultureSurveyService.SurveyResponse.SurveyType type,
        LocalDate start,
        LocalDate end);
    List<CultureSurveyService.SurveyResponse> findByEmployeeId(String employeeId);
}
```

---

## Part 5: Assignments

### Assignment 1: Design Bibby's Org Structure

Design organizational structures for Bibby at three stages:

**Stage 1: 10 people (Year 1)**
- List roles and reporting structure
- Justify your design

**Stage 2: 40 people (Year 2)**
- How does structure evolve?
- What new roles/teams emerge?

**Stage 3: 150 people (Year 4)**
- Full org chart
- Departments and teams
- Spans of control

### Assignment 2: Define Company Values

Create 5 company values for Bibby:

**Requirements:**
- Specific and actionable
- Include behavioral examples
- Explain how to use in hiring
- Explain how to use in performance reviews

### Assignment 3: Remote Work Playbook

Write a remote work playbook covering:
- Communication norms
- Meeting guidelines
- Async collaboration tools
- Building team connection
- Onboarding remote employees

### Assignment 4: Build Organizational Health Dashboard

Implement a dashboard showing:
- Team sizes and span of control
- Org chart visualization
- Headcount by department
- Tenure distribution
- Open headcount
- Red flags (too narrow/wide span, high attrition teams)

### Assignment 5: Culture Survey

Design a quarterly culture survey:

**Include:**
- 15-20 questions (Likert scale 1-5)
- Cover: mission, growth, balance, collaboration, leadership
- 3-5 open-ended questions
- eNPS question

---

## Part 6: Reflection Questions

1. **Organizational Structure:**
   - When should you reorganize?
   - What are the costs of reorganizing too often?
   - How do you balance autonomy and alignment?

2. **Culture:**
   - Can culture be designed or does it just emerge?
   - How do you change culture that's already set?
   - What's the risk of over-engineering culture?

3. **Remote Work:**
   - What's lost in remote-first?
   - What's gained?
   - How do you build trust remotely?

4. **Decision-Making:**
   - When should you seek consensus vs decide unilaterally?
   - How do you know if you're moving too fast/slow on decisions?
   - What's the role of intuition vs data in decisions?

5. **Scaling:**
   - What should change as you scale?
   - What should never change?
   - How do you prevent bureaucracy?

---

## Part 7: Additional Resources

### Books
- **"An Elegant Puzzle: Systems of Engineering Management"** by Will Larson
- **"The Five Dysfunctions of a Team"** by Patrick Lencioni
- **"Powerful: Building a Culture of Freedom and Responsibility"** by Patty McCord (Netflix)
- **"No Rules Rules"** by Reed Hastings (Netflix culture)
- **"The Culture Code"** by Daniel Coyle
- **"Team Topologies"** by Matthew Skelton

### Articles
- **Netflix Culture Memo** (classic)
- **Valve Employee Handbook** (flat organization)
- **GitLab Remote Work Guide** (comprehensive)
- **Amazon Leadership Principles**

### Tools
- **Org Charts:** Lucidchart, OrgWeaver, ChartHop
- **Surveys:** Lattice, Culture Amp, Officevibe
- **Communication:** Slack, Notion, Linear
- **Recognition:** Bonusly, Kudos

---

## Summary

This week you learned:

1. **Organizational Design:**
   - Functional, divisional, and matrix structures
   - Span of control and hierarchy depth
   - Conway's Law and org-product alignment
   - When and how to reorganize

2. **Company Culture:**
   - Defining meaningful values
   - Living values through systems
   - Remote-first culture building
   - Scaling culture without dilution

3. **Decision-Making:**
   - RACI and DRI frameworks
   - Consultative decision-making
   - Disagree and commit
   - One-way vs two-way doors

4. **Communication:**
   - Async-first for remote teams
   - Levels of communication
   - Transparency defaults
   - Connection rituals

5. **Measuring Health:**
   - Organizational metrics
   - Culture surveys
   - eNPS tracking
   - Values alignment

**Key Insight:** Culture eats strategy for breakfast. You can have the best product and strategy, but without the right culture and organizational design, you won't execute. Invest early in defining values, building systems, and measuring health.

**Next Week Preview:** Week 46 will cover Fundraising & Investor Relations—how to raise capital, pitch investors, manage your board, and maintain relationships that fuel your growth.

---

**Week 45 Complete!** You now understand how to design organizations that scale and build cultures that attract top talent and drive exceptional performance.
