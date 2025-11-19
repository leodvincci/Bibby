# Section 29: Technical Interviews

**Part V: Interview Mastery — Week 29**

**Overview:**
You've built your network, optimized your online presence, and started conducting informational interviews. Now it's time to prepare for the technical interview - the coding challenge that stands between you and your target backend engineering role.

**What Is a Technical Interview?**
A technical interview is a 45-60 minute session where you solve coding problems in real-time while explaining your thought process. You'll typically solve 1-2 problems ranging from easy to medium difficulty, focusing on data structures, algorithms, and problem-solving skills.

**Why This Matters:**
- **Gate-keeper to backend roles:** Most companies require 1-3 technical interviews as part of the hiring process
- **Not just about getting the right answer:** Communication, problem-solving approach, and code quality matter as much as correctness
- **Learnable skill:** Technical interviewing is a distinct skill from day-to-day engineering - it requires specific preparation
- **Confidence builder:** Strong technical interview skills reduce anxiety and improve performance

**Week 29 Goals:**
1. Understand technical interview format and expectations
2. Master core data structures and algorithms relevant to backend engineering
3. Learn and practice problem-solving framework
4. Complete 10-15 LeetCode problems (easy to medium)
5. Conduct 1-2 mock technical interviews
6. Build sustainable practice routine for ongoing preparation

**Time Commitment:** 15-20 hours (Week 29) + ongoing 5-10 hours/week until interviews

---

## Part 1: Understanding the Technical Interview Landscape

### What to Expect

**Format:**
- **Duration:** 45-60 minutes
- **Number of problems:** 1-2 (typically 1 medium or 2 easy-medium)
- **Platform:** CoderPad, HackerRank, or company-specific platform (shared coding environment)
- **Communication:** Video call while coding, expected to explain your thinking
- **Interviewer role:** Observes, asks clarifying questions, provides hints if stuck

**What They're Evaluating:**

**1. Problem-Solving Approach (35%)**
- Can you break down a complex problem into smaller parts?
- Do you ask clarifying questions?
- Do you consider edge cases?
- Do you test your solution?

**2. Technical Competence (30%)**
- Do you know appropriate data structures?
- Can you analyze time and space complexity?
- Is your code correct?
- Do you handle edge cases?

**3. Communication (20%)**
- Can you explain your thinking clearly?
- Do you articulate trade-offs?
- Can you discuss alternative approaches?
- Do you respond well to hints and feedback?

**4. Code Quality (15%)**
- Is your code readable?
- Do you use meaningful variable names?
- Is your code organized?
- Do you follow best practices?

**What They're NOT Evaluating:**
- Memorization of obscure algorithms (unless you're interviewing at Google/Meta for algorithms-heavy roles)
- Speed (within reason - finishing is good, but rushing and making mistakes is bad)
- Trick questions (most companies have moved away from these)

### Company-Specific Expectations

**For Leo's Target Companies:**

**OSIsoft, Rockwell Automation, Honeywell (Industrial Software):**
- **Focus:** Practical problems, data processing, real-time systems
- **Difficulty:** Medium (not typically LeetCode hard)
- **Style:** More emphasis on correctness and robustness than algorithmic cleverness
- **Example problems:** Processing time-series data, rate limiting, caching strategies

**Uptake (Startup in Industrial Space):**
- **Focus:** Backend systems, data pipelines, algorithms
- **Difficulty:** Medium to medium-hard
- **Style:** Mix of algorithmic and practical
- **Example problems:** Graph algorithms (sensor networks), data aggregation, API design

**Splunk, Datadog, PagerDuty (Observability/Monitoring):**
- **Focus:** Data structures, string processing, log parsing
- **Difficulty:** Medium
- **Style:** Practical problems with real-world context
- **Example problems:** Log parsing, metric aggregation, search algorithms

**General Backend Engineering:**
- **Common topics:** Arrays, strings, hash maps, trees, graphs, sorting, searching
- **Less common:** Dynamic programming (unless senior+ role), advanced graph algorithms
- **Very rare:** Math-heavy problems, bit manipulation (unless specific to role)

---

## Part 2: Core Data Structures and Algorithms (Backend Focus)

### Must-Know Data Structures

**Priority 1: Essential (Master These First)**

**1. Arrays and Strings**
- **Why:** 40-50% of backend interview questions
- **Key operations:** Traversal, two-pointer technique, sliding window
- **Time complexity:** Access O(1), search O(n), insert/delete O(n)
- **Common problems:** Two sum, valid palindrome, longest substring, merge intervals

**Example - Two Pointer Technique:**
```java
// Problem: Remove duplicates from sorted array
public int removeDuplicates(int[] nums) {
    if (nums.length == 0) return 0;

    int writeIndex = 1; // Pointer for next unique position

    for (int readIndex = 1; readIndex < nums.length; readIndex++) {
        // If current element is different from previous, it's unique
        if (nums[readIndex] != nums[readIndex - 1]) {
            nums[writeIndex] = nums[readIndex];
            writeIndex++;
        }
    }

    return writeIndex; // Length of unique elements
}
```

**2. Hash Maps (HashMap in Java)**
- **Why:** Most versatile data structure for backend problems
- **Key operations:** Insert O(1), lookup O(1), delete O(1)
- **Use cases:** Counting, caching, deduplication, lookup tables
- **Common problems:** Two sum, group anagrams, LRU cache

**Example - Using HashMap for Counting:**
```java
// Problem: First unique character in string
public int firstUniqChar(String s) {
    // Count frequency of each character
    Map<Character, Integer> count = new HashMap<>();
    for (char c : s.toCharArray()) {
        count.put(c, count.getOrDefault(c, 0) + 1);
    }

    // Find first character with count 1
    for (int i = 0; i < s.length(); i++) {
        if (count.get(s.charAt(i)) == 1) {
            return i;
        }
    }

    return -1;
}
```

**3. Lists (ArrayList and LinkedList in Java)**
- **Why:** Common in backend data processing
- **ArrayList:** Dynamic array, O(1) access, O(n) insert/delete
- **LinkedList:** O(1) insert/delete at ends, O(n) access
- **Common problems:** Reverse list, detect cycle, merge sorted lists

**Example - LinkedList Manipulation:**
```java
// Problem: Reverse a linked list
public ListNode reverseList(ListNode head) {
    ListNode prev = null;
    ListNode current = head;

    while (current != null) {
        ListNode next = current.next; // Save next node
        current.next = prev;           // Reverse pointer
        prev = current;                // Move prev forward
        current = next;                // Move current forward
    }

    return prev; // New head
}
```

**4. Stacks and Queues**
- **Why:** Common in parsing, BFS/DFS, and backend processing
- **Stack:** LIFO, O(1) push/pop (use ArrayDeque in Java)
- **Queue:** FIFO, O(1) enqueue/dequeue (use LinkedList or ArrayDeque)
- **Common problems:** Valid parentheses, min stack, level-order traversal

**Example - Stack for Parsing:**
```java
// Problem: Valid parentheses
public boolean isValid(String s) {
    Stack<Character> stack = new Stack<>();
    Map<Character, Character> pairs = Map.of(')', '(', '}', '{', ']', '[');

    for (char c : s.toCharArray()) {
        if (pairs.containsValue(c)) {
            // Opening bracket - push to stack
            stack.push(c);
        } else {
            // Closing bracket - check if matches top of stack
            if (stack.isEmpty() || stack.pop() != pairs.get(c)) {
                return false;
            }
        }
    }

    return stack.isEmpty(); // All brackets should be matched
}
```

**Priority 2: Important (Learn After Mastering Priority 1)**

**5. Trees (Binary Trees, Binary Search Trees)**
- **Why:** 15-20% of backend interviews
- **Key operations:** Traversal (inorder, preorder, postorder, level-order), search
- **Common problems:** Validate BST, lowest common ancestor, tree depth, serialize/deserialize

**Example - Tree Traversal:**
```java
// Problem: Binary tree level order traversal
public List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) return result;

    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);

    while (!queue.isEmpty()) {
        int levelSize = queue.size();
        List<Integer> currentLevel = new ArrayList<>();

        for (int i = 0; i < levelSize; i++) {
            TreeNode node = queue.poll();
            currentLevel.add(node.val);

            if (node.left != null) queue.offer(node.left);
            if (node.right != null) queue.offer(node.right);
        }

        result.add(currentLevel);
    }

    return result;
}
```

**6. Graphs**
- **Why:** 10-15% of backend interviews, important for system relationships
- **Representations:** Adjacency list (most common), adjacency matrix
- **Key algorithms:** BFS, DFS
- **Common problems:** Number of islands, course schedule, clone graph

**Example - Graph BFS:**
```java
// Problem: Number of islands (2D grid)
public int numIslands(char[][] grid) {
    if (grid == null || grid.length == 0) return 0;

    int count = 0;
    for (int i = 0; i < grid.length; i++) {
        for (int j = 0; j < grid[0].length; j++) {
            if (grid[i][j] == '1') {
                bfs(grid, i, j);
                count++;
            }
        }
    }
    return count;
}

private void bfs(char[][] grid, int row, int col) {
    Queue<int[]> queue = new LinkedList<>();
    queue.offer(new int[]{row, col});
    grid[row][col] = '0'; // Mark as visited

    int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};

    while (!queue.isEmpty()) {
        int[] cell = queue.poll();
        for (int[] dir : directions) {
            int newRow = cell[0] + dir[0];
            int newCol = cell[1] + dir[1];

            if (newRow >= 0 && newRow < grid.length &&
                newCol >= 0 && newCol < grid[0].length &&
                grid[newRow][newCol] == '1') {
                queue.offer(new int[]{newRow, newCol});
                grid[newRow][newCol] = '0';
            }
        }
    }
}
```

**Priority 3: Optional (If Time Permits or Company-Specific)**

**7. Heaps (PriorityQueue in Java)**
- **Why:** Less common but useful for specific problems
- **Use cases:** Top K elements, merge K sorted lists, median finding
- **Common problems:** Kth largest element, merge K sorted lists

**8. Trie (Prefix Tree)**
- **Why:** Rare in general backend interviews
- **Use cases:** Autocomplete, word search, prefix matching

---

### Must-Know Algorithms

**1. Sorting and Searching**
- **Know:** Binary search, merge sort, quicksort (concepts, not implementation from scratch)
- **Time complexity:** Binary search O(log n), merge sort O(n log n), quicksort O(n log n) average
- **Common problems:** Search in rotated sorted array, find peak element, merge intervals

**2. Two Pointers**
- **Pattern:** Use two pointers moving through array/list
- **Use cases:** Palindrome check, remove duplicates, container with most water
- **Why useful:** Often reduces O(n²) brute force to O(n)

**3. Sliding Window**
- **Pattern:** Maintain window of elements that satisfy condition
- **Use cases:** Longest substring without repeating characters, maximum sum subarray
- **Backend relevance:** Rate limiting, log analysis, stream processing

**4. BFS and DFS**
- **BFS:** Level-by-level exploration (use queue)
- **DFS:** Depth-first exploration (use recursion or stack)
- **Use cases:** Tree/graph traversal, shortest path, connected components

**5. Recursion and Backtracking**
- **Know:** Base case, recursive case, call stack
- **Common problems:** Fibonacci, tree problems, permutations
- **Less common in backend interviews:** Complex backtracking (like N-Queens)

---

## Part 3: The Problem-Solving Framework (UMPIRE Method)

### Step-by-Step Approach for Every Problem

**U - Understand**
**M - Match**
**P - Plan**
**I - Implement**
**R - Review**
**E - Evaluate**

### Step 1: Understand (5-8 minutes)

**Goal:** Fully understand the problem before writing any code.

**What to Do:**
1. **Read the problem carefully** (twice if needed)
2. **Ask clarifying questions:**
   - What are the input constraints? (size, range, null/empty cases)
   - What should I return if input is invalid?
   - Can I assume the input is valid/sorted/unique?
   - Are there performance requirements?
3. **Restate the problem in your own words**
4. **Work through 2-3 examples manually**
   - Include edge cases (empty, single element, duplicates)

**Example - Two Sum Problem:**

**Problem:** Given an array of integers and a target, return indices of two numbers that add up to target.

**Clarifying Questions You Should Ask:**
```
Interviewer presents problem: "Given an array of integers nums and an integer target,
return indices of the two numbers such that they add up to target."

You: "Thanks for the problem. A few clarifying questions:
1. Can I assume there's exactly one solution, or should I handle the case where there's no solution?
2. Can I use the same element twice? For example, if nums = [3] and target = 6?
3. What should I return if there's no solution - empty array or null?
4. Are there any constraints on the array size or integer values?
5. Does the order of the returned indices matter?"

Interviewer: "Good questions. There's exactly one solution, you can't use the same element twice,
array can be up to 10,000 elements, and order doesn't matter."

You: "Great, let me restate to make sure I understand: I need to find two different indices i and j
where nums[i] + nums[j] equals target, and return [i, j]. And there's guaranteed to be exactly one
such pair."

Interviewer: "Exactly right."
```

### Step 2: Match (2-3 minutes)

**Goal:** Identify what data structure or algorithm pattern applies.

**Ask Yourself:**
- Have I seen a similar problem before?
- What data structure would make this easier?
- Is there a common pattern here? (two pointers, sliding window, BFS/DFS, etc.)

**Common Pattern Recognition:**

| Problem Characteristics | Likely Pattern/Data Structure |
|------------------------|-------------------------------|
| Find pair with sum/difference | Hash map or two pointers |
| Substring problems | Sliding window |
| Parentheses/brackets | Stack |
| Tree traversal | BFS (queue) or DFS (recursion) |
| Graph connectivity | BFS/DFS |
| Top K elements | Heap |
| Recent N items | Queue or deque |
| Counting frequencies | Hash map |

**Example - Two Sum:**
```
You: "This problem asks me to find two numbers that sum to a target. I need to check if a
complement exists. This seems like a good use case for a hash map - I can store numbers I've
seen and check if (target - current number) exists in constant time."
```

### Step 3: Plan (3-5 minutes)

**Goal:** Outline your approach before coding.

**What to Do:**
1. **Describe your approach in plain English**
2. **Outline algorithm steps**
3. **Discuss time and space complexity**
4. **Consider alternative approaches and trade-offs**
5. **Get interviewer buy-in before coding**

**Example - Two Sum:**
```
You: "Here's my approach:

1. I'll use a hash map to store numbers I've seen and their indices
2. For each number in the array:
   - Calculate complement = target - current number
   - Check if complement exists in hash map
   - If yes, return [complement's index, current index]
   - If no, add current number and index to hash map
3. Return result

Time complexity: O(n) - single pass through array
Space complexity: O(n) - hash map can store up to n elements

An alternative would be brute force with two nested loops checking all pairs, but that
would be O(n²) time. The hash map approach is more efficient.

Does this approach sound good to you?"

Interviewer: "Yes, that sounds great. Go ahead and implement it."
```

**Important:** Don't skip this step. Spending 3-5 minutes planning saves 10-15 minutes of debugging.

### Step 4: Implement (15-20 minutes)

**Goal:** Write clean, correct code while explaining your thinking.

**What to Do:**
1. **Talk while you code** - explain what you're doing
2. **Write clean code:**
   - Meaningful variable names (not `i`, `j`, `temp` everywhere)
   - Proper indentation
   - Clear logic flow
3. **Handle edge cases as you go**
4. **Don't rush** - accuracy > speed

**Example - Two Sum Implementation:**
```java
public int[] twoSum(int[] nums, int target) {
    // Hash map to store number -> index mapping
    Map<Integer, Integer> seen = new HashMap<>();

    // Single pass through array
    for (int i = 0; i < nums.length; i++) {
        int complement = target - nums[i];

        // Check if complement exists in our map
        if (seen.containsKey(complement)) {
            // Found our pair - return indices
            return new int[]{seen.get(complement), i};
        }

        // Add current number and its index to map
        seen.put(nums[i], i);
    }

    // This shouldn't happen based on problem guarantee
    return new int[]{};
}
```

**While Coding, Say Things Like:**
- "I'm creating a hash map to store the numbers we've seen so far..."
- "Now I'll iterate through the array..."
- "For each number, I calculate the complement..."
- "If the complement exists in the map, we've found our answer..."

### Step 5: Review (3-5 minutes)

**Goal:** Test your code and fix any bugs.

**What to Do:**
1. **Trace through with example:** Walk through your code with the sample input
2. **Check edge cases:**
   - Empty input
   - Single element
   - Duplicates
   - Minimum/maximum values
3. **Look for common bugs:**
   - Off-by-one errors
   - Null pointer exceptions
   - Integer overflow
   - Infinite loops

**Example - Two Sum Review:**
```
You: "Let me trace through with the example: nums = [2, 7, 11, 15], target = 9

- i = 0, nums[0] = 2, complement = 7
  - seen is empty, so add {2: 0}
- i = 1, nums[1] = 7, complement = 2
  - seen contains 2 at index 0
  - return [0, 1] ✓

Let me also check edge cases:
- What if nums = [3, 3], target = 6?
  - i = 0: complement = 3, seen is empty, add {3: 0}
  - i = 1: complement = 3, seen contains 3 at index 0, return [0, 1] ✓
- This handles duplicates correctly.

The code looks good to me."
```

### Step 6: Evaluate (2-3 minutes)

**Goal:** Analyze complexity and discuss optimizations.

**What to Do:**
1. **State time complexity** with explanation
2. **State space complexity** with explanation
3. **Discuss possible optimizations** (if any)
4. **Discuss trade-offs**

**Example - Two Sum Evaluation:**
```
You: "Time complexity is O(n) because we make a single pass through the array, and hash map
operations (put and containsKey) are O(1) on average.

Space complexity is O(n) in the worst case, where we might store almost all n elements in
the hash map before finding the pair.

For optimization: If the array were sorted, we could use a two-pointer approach with O(1)
space, but sorting would take O(n log n) time. Since the problem doesn't guarantee sorted
input, the hash map approach is optimal for unsorted arrays.

The trade-off is space for time - we use O(n) space to achieve O(n) time instead of O(1)
space with O(n²) time in the brute force approach."
```

---

## Part 4: Practice Strategy for Week 29

### The 10-15 Problem Starter List

**For Week 29, focus on these problems in order:**

**Easy Problems (Days 1-3): Build Foundation**

1. **Two Sum** (Array + Hash Map)
   - LeetCode #1
   - Pattern: Hash map for lookup
   - Time: 30-45 min first time

2. **Valid Parentheses** (Stack)
   - LeetCode #20
   - Pattern: Stack for matching pairs
   - Time: 30-45 min

3. **Merge Two Sorted Lists** (LinkedList)
   - LeetCode #21
   - Pattern: Two pointers, list manipulation
   - Time: 45-60 min

4. **Best Time to Buy and Sell Stock** (Array)
   - LeetCode #121
   - Pattern: Single pass, track minimum
   - Time: 30-45 min

5. **Valid Palindrome** (Two Pointers)
   - LeetCode #125
   - Pattern: Two pointers from ends
   - Time: 30 min

**Medium Problems (Days 4-7): Build Competence**

6. **Longest Substring Without Repeating Characters** (Sliding Window)
   - LeetCode #3
   - Pattern: Sliding window + hash map
   - Time: 60-90 min
   - **Backend relevance:** Stream processing, caching

7. **3Sum** (Two Pointers)
   - LeetCode #15
   - Pattern: Sorting + two pointers
   - Time: 60-90 min

8. **Group Anagrams** (Hash Map)
   - LeetCode #49
   - Pattern: Hash map with sorted string as key
   - Time: 45-60 min

9. **Binary Tree Level Order Traversal** (BFS)
   - LeetCode #102
   - Pattern: BFS with queue
   - Time: 45-60 min

10. **Number of Islands** (Graph DFS/BFS)
    - LeetCode #200
    - Pattern: DFS or BFS on 2D grid
    - Time: 60-90 min

**Additional Medium Problems (If Time Permits):**

11. **LRU Cache** (Hash Map + Doubly Linked List)
    - LeetCode #146
    - **Highly relevant for backend roles**
    - Time: 90-120 min

12. **Product of Array Except Self** (Array)
    - LeetCode #238
    - Pattern: Prefix/suffix products
    - Time: 45-60 min

13. **Top K Frequent Elements** (Heap)
    - LeetCode #347
    - Pattern: Hash map + heap
    - Time: 60 min

14. **Course Schedule** (Graph)
    - LeetCode #207
    - Pattern: Topological sort, cycle detection
    - Time: 60-90 min

15. **Validate Binary Search Tree** (Tree)
    - LeetCode #98
    - Pattern: Tree traversal with constraints
    - Time: 45-60 min

### Daily Practice Schedule

**Week 29 Practice Plan:**

**Day 1 (Monday): 3-4 hours**
- Watch: "How to approach coding problems" (YouTube - 30 min)
- Solve: Two Sum (45 min)
- Solve: Valid Parentheses (45 min)
- Review: UMPIRE framework (30 min)
- Notes: Document patterns learned (30 min)

**Day 2 (Tuesday): 2-3 hours**
- Solve: Merge Two Sorted Lists (60 min)
- Solve: Best Time to Buy and Sell Stock (45 min)
- Review: Revisit Two Sum without looking (30 min)
- Notes: Update pattern recognition doc (15 min)

**Day 3 (Wednesday): 2-3 hours**
- Solve: Valid Palindrome (30 min)
- Solve: Longest Substring Without Repeating Characters (90 min)
- Review solutions and discuss complexity (30 min)

**Day 4 (Thursday): 3-4 hours**
- Solve: 3Sum (90 min)
- Solve: Group Anagrams (60 min)
- Review: Revisit Longest Substring problem (30 min)
- Mock interview prep (30 min)

**Day 5 (Friday): 3-4 hours**
- Solve: Binary Tree Level Order Traversal (60 min)
- Solve: Number of Islands (90 min)
- Review: All tree/graph problems this week (30 min)

**Day 6 (Saturday): 3-4 hours**
- Solve: LRU Cache (120 min)
- Review: All problems from week (60 min)
- **Mock interview #1** (60 min) - with friend or Pramp

**Day 7 (Sunday): 2-3 hours**
- Solve: 1-2 additional medium problems (120 min)
- **Mock interview #2** (60 min)
- Week review and notes (30 min)

**Total: 18-25 hours**

---

## Part 5: How to Practice Effectively

### The Right Way to Practice

**DO:**

**1. Use the UMPIRE Framework Every Time**
- Don't jump into coding immediately
- Spend 5-8 minutes understanding and planning
- Practice the framework until it's automatic

**2. Time Yourself (But Don't Stress About Speed)**
- Set timer for 45-60 minutes per problem
- Goal is to finish, but it's okay if you don't at first
- Speed will come with practice

**3. Actually Write Code (Don't Just Read Solutions)**
- Type it out, don't just think through it
- Use a proper editor (VS Code, IntelliJ)
- Run and test your code

**4. Review Solutions Even When You Solve It**
- There might be a better approach
- Learn from others' clean code
- Read LeetCode discussions

**5. Redo Problems**
- Revisit problems after 2-3 days
- Redo without looking at your previous solution
- Should be able to solve in 20-30 min on second attempt

**6. Focus on Understanding, Not Memorizing**
- Understand WHY a solution works
- Recognize patterns that apply to multiple problems
- Learn to derive solutions, not memorize them

**DON'T:**

**1. Don't Look at Solution Immediately**
- Struggle for at least 30-45 minutes
- Try different approaches
- Looking too early prevents learning

**2. Don't Just Read Solutions**
- Reading ≠ understanding
- You must code it yourself

**3. Don't Skip Easy Problems**
- Easy problems build foundation
- They're what you'll likely see in initial interviews
- Practice framework on easy problems

**4. Don't Only Practice Medium/Hard**
- Diminishing returns
- Backend roles rarely ask LeetCode hard
- Better to nail medium problems than struggle with hard

**5. Don't Practice in Silence**
- Always explain out loud (even to yourself)
- This is critical for live interviews
- Practice articulating your thinking

### When You're Stuck

**After 15-20 minutes stuck:**
- Read problem again carefully
- Try a simpler example
- Try brute force approach first
- Ask yourself: "What data structure would help here?"

**After 30-45 minutes stuck:**
- Look at hints (if available)
- Look at approach (not full solution)
- Try to implement based on high-level approach

**After 60 minutes stuck:**
- Read full solution
- Understand it thoroughly
- Close solution and implement yourself
- Redo this problem in 2-3 days

---

## Part 6: Live Coding Tips

### During the Actual Interview

**Before Writing Any Code:**

1. **Ask clarifying questions** (even if you think you understand)
   - Shows you're thorough
   - Prevents misunderstanding
   - Buys you thinking time

2. **Work through example manually**
   - Use whiteboard or shared doc
   - Show your thinking process
   - Helps you understand the problem

3. **Explain your approach**
   - Describe algorithm in plain English
   - Discuss time/space complexity
   - Get interviewer buy-in

**While Writing Code:**

4. **Talk through your code**
   - "I'm creating a hash map to store..."
   - "Now I'll iterate through the array..."
   - Silence is bad - interviewer can't read your mind

5. **Write clean code first**
   - Use meaningful variable names
   - Add comments for complex logic
   - Format properly (indentation, spacing)

6. **Think out loud about edge cases**
   - "I should handle the empty array case..."
   - "What if there are duplicates..."
   - Shows thoroughness

**If You Get Stuck:**

7. **Communicate that you're stuck**
   - "I'm thinking through the best way to handle..."
   - "I'm considering a few different approaches..."
   - Don't sit in silence

8. **Respond well to hints**
   - "Oh, that's a great point!"
   - "Let me think about that approach..."
   - Interviewers want to help you

9. **Consider brute force first**
   - "The brute force approach would be..."
   - "But that's O(n²), which we can improve..."
   - Shows you can find a working solution

**After Writing Code:**

10. **Test your code**
    - Walk through with example
    - Check edge cases
    - Find and fix bugs

11. **Analyze complexity**
    - Time and space complexity
    - Explain why

12. **Discuss optimizations**
    - "We could reduce space by..."
    - "If the input were sorted, we could..."

### Communication Examples

**Good:**
```
"I'm going to use a hash map here to store the numbers I've seen. This will give me O(1)
lookup time when I need to check if the complement exists. I'm calling it 'seen' because
it tracks which numbers we've already processed."
```

**Bad (silence):**
```
*types code without saying anything*
```

**Good (when stuck):**
```
"I'm thinking about how to handle the case where we have duplicates. Let me work through
an example... if we have [3, 3] and target is 6, we want to return [0, 1]. So my current
approach should handle that... let me trace through..."
```

**Bad (when stuck):**
```
*sits in silence for 5 minutes*
```

**Good (responding to hint):**
```
Interviewer: "What if you sorted the array first?"
You: "Ah, that's interesting! If we sort the array, we could use two pointers from both
ends. That would give us O(n log n) for sorting plus O(n) for the two-pointer pass.
The trade-off is we'd lose the original indices though. Let me think if that matters
for this problem..."
```

---

## Part 7: Language Choice - Java for Backend Roles

### Why Java for Leo

**Advantages:**
- **Target companies use Java:** Spring Boot, backend infrastructure
- **Familiar syntax:** You're already working in Java for Bibby
- **Rich standard library:** Built-in data structures (HashMap, ArrayList, etc.)
- **Interviewer familiarity:** Most backend engineers know Java

**Key Java Syntax for Interviews:**

**Arrays:**
```java
int[] arr = new int[]{1, 2, 3};
int[] arr2 = new int[10]; // Size 10, all elements initialized to 0
arr.length; // Length property
```

**Lists:**
```java
List<Integer> list = new ArrayList<>();
list.add(5);
list.get(0);
list.size();
list.remove(0); // Remove by index
```

**Maps:**
```java
Map<String, Integer> map = new HashMap<>();
map.put("key", 1);
map.get("key");
map.getOrDefault("key", 0);
map.containsKey("key");
map.keySet(); // Set of keys
map.values(); // Collection of values
```

**Sets:**
```java
Set<Integer> set = new HashSet<>();
set.add(5);
set.contains(5);
set.remove(5);
```

**Stack:**
```java
Stack<Integer> stack = new Stack<>();
stack.push(5);
stack.pop();
stack.peek();
stack.isEmpty();
```

**Queue:**
```java
Queue<Integer> queue = new LinkedList<>();
queue.offer(5); // Add to queue
queue.poll();   // Remove from queue
queue.peek();   // Look at front
```

**Sorting:**
```java
Arrays.sort(arr); // Sort array
Collections.sort(list); // Sort list
Collections.sort(list, Collections.reverseOrder()); // Sort descending
```

**String Manipulation:**
```java
String s = "hello";
s.length();
s.charAt(0);
s.substring(0, 3); // "hel"
s.split(",");
String.join(",", list);
s.toCharArray();
new String(charArray);
```

---

## Part 8: Mock Interviews

### Why Mock Interviews Matter

**Benefits:**
- Practice talking while coding (different from solo practice)
- Get comfortable with pressure
- Receive feedback on communication
- Identify weak areas

**Goal for Week 29:** 2 mock interviews

### Where to Find Mock Interview Partners

**1. Pramp (pramp.com)**
- **Free** peer-to-peer mock interviews
- Matched with another person preparing
- Both of you take turns interviewing each other
- Good for: Realistic practice, free option

**2. Interviewing.io**
- Anonymous mock interviews with engineers from top companies
- Free basic tier
- Paid tier for guaranteed interview times
- Good for: High-quality feedback

**3. Friends/Colleagues**
- Ask bootcamp classmates or engineer friends
- Trade mock interviews
- Good for: Comfortable environment, scheduling flexibility

**4. Preplaced, LeetCode Mock**
- Platform-specific mock interview features
- Timed problems in interview format
- Good for: Solo practice in interview conditions

### How to Conduct a Mock Interview

**Setup:**
1. Schedule 60 minutes
2. Use video call (Zoom, Google Meet)
3. Use CoderPad or shared Google Doc
4. One person is interviewer, one is candidate (switch halfway if peer-to-peer)

**Format:**
- **5 min:** Introductions and problem presentation
- **40 min:** Candidate solves problem
- **15 min:** Feedback and discussion

**As Candidate:**
- Treat it like a real interview
- Use UMPIRE framework
- Talk through your thinking
- Ask clarifying questions

**As Interviewer:**
- Choose medium LeetCode problem you've solved
- Present problem clearly
- Provide hints if candidate stuck for 10+ minutes
- Take notes on: communication, approach, code quality
- Give constructive feedback

### What to Focus On in Feedback

**Ask Your Mock Interviewer:**
1. Did I communicate clearly throughout?
2. Was my code readable and organized?
3. Did I ask good clarifying questions?
4. How was my problem-solving approach?
5. What should I improve?

---

## Part 9: Week 29 Action Plan

### Day-by-Day Breakdown

**Monday (3-4 hours):**
- [ ] Watch "Technical Interview Tips" video (30 min)
- [ ] Read UMPIRE framework again (30 min)
- [ ] Solve: Two Sum (45 min)
- [ ] Solve: Valid Parentheses (45 min)
- [ ] Document patterns learned (30 min)

**Tuesday (2-3 hours):**
- [ ] Solve: Merge Two Sorted Lists (60 min)
- [ ] Solve: Best Time to Buy and Sell Stock (45 min)
- [ ] Redo: Two Sum without looking at solution (30 min)

**Wednesday (2-3 hours):**
- [ ] Solve: Valid Palindrome (30 min)
- [ ] Solve: Longest Substring Without Repeating Characters (90 min)
- [ ] Review and analyze time complexity (30 min)

**Thursday (3-4 hours):**
- [ ] Solve: 3Sum (90 min)
- [ ] Solve: Group Anagrams (60 min)
- [ ] Redo: Valid Parentheses and Best Time to Buy/Sell Stock (60 min)

**Friday (3-4 hours):**
- [ ] Solve: Binary Tree Level Order Traversal (60 min)
- [ ] Solve: Number of Islands (90 min)
- [ ] Review all tree/graph patterns (30 min)

**Saturday (3-4 hours):**
- [ ] Solve: LRU Cache (120 min)
- [ ] Review all Week 29 problems (60 min)
- [ ] **Mock Interview #1** - Pramp or friend (60 min)

**Sunday (2-3 hours):**
- [ ] Solve: 1-2 additional problems from list (120 min)
- [ ] **Mock Interview #2** (60 min)
- [ ] Week review and plan for Week 30+ (30 min)

**Total: 18-25 hours**

---

## Part 10: Common Mistakes and How to Avoid Them

### Mistake 1: Jumping Straight to Code

**What it looks like:**
- Reading problem and immediately start typing
- No clarifying questions
- No examples worked through
- No algorithm discussion

**Why it's bad:**
- Often solve wrong problem
- Miss edge cases
- Inefficient solution
- Looks unprepared

**How to avoid:**
- Force yourself to spend 5-8 minutes on Understand + Match + Plan
- Don't touch keyboard until you've explained approach
- Practice UMPIRE framework

### Mistake 2: Coding in Silence

**What it looks like:**
- Typing code without explaining
- Interviewer has no idea what you're thinking
- Long periods of silence

**Why it's bad:**
- Interviewer can't help if stuck
- Seems like poor communication
- No insight into problem-solving process

**How to avoid:**
- Practice talking out loud while coding (even when alone)
- Narrate what you're doing: "I'm creating a hash map..."
- Explain why: "...because I need O(1) lookup"

### Mistake 3: Ignoring Edge Cases

**What it looks like:**
- Only testing happy path example
- Not considering empty input, single element, duplicates
- Code fails on edge cases

**Why it's bad:**
- Shows lack of thoroughness
- Real production code must handle edge cases

**How to avoid:**
- Always ask: "What if input is empty? What if there's only one element?"
- Test edge cases after implementing
- Think about null, empty, minimum, maximum

### Mistake 4: Not Testing Code

**What it looks like:**
- Writing code and saying "looks good!"
- Not walking through with example
- Not catching obvious bugs

**Why it's bad:**
- Shows carelessness
- Misses bugs that would be caught with simple test

**How to avoid:**
- Always trace through code with example
- Use different example than the one given (to catch assumptions)
- Check your edge cases

### Mistake 5: Giving Up Too Easily

**What it looks like:**
- Getting stuck and immediately asking for answer
- Not trying different approaches
- Saying "I don't know" without thinking

**Why it's bad:**
- Shows lack of persistence
- Misses opportunity to demonstrate problem-solving

**How to avoid:**
- Struggle for at least 15-20 minutes before hints
- Try brute force if stuck on optimal solution
- Talk through what you've tried: "I considered X but that won't work because..."

---

## Part 11: Long-Term Practice Plan (Weeks 30+)

### After Week 29

**Weeks 30-32 (During Interview Prep):**
- **Daily:** 1-2 problems per day (1-2 hours)
- **Weekly:** 1 mock interview
- **Focus:** Problems similar to target company patterns

**Month 2-3 (Ongoing Practice):**
- **3-4 problems per week** (maintain skills)
- **1 mock interview per week**
- **Review previously solved problems**

**Before Each Real Interview:**
- **Week before:** Solve 5-7 problems in that company's style
- **Day before:** Review 2-3 favorite problems (confidence boost)
- **Day of:** Don't practice - rest and review framework

### Measuring Progress

**Week 29 Goals:**
- [ ] Complete 10-15 problems (mix of easy and medium)
- [ ] 2 mock interviews completed
- [ ] Can explain UMPIRE framework from memory
- [ ] Comfortable with hash maps, arrays, strings, stacks, queues
- [ ] Attempted at least one tree and one graph problem
- [ ] Can solve easy problems in 20-30 minutes

**Success Metrics:**
- **Easy problems:** Solve in 20-30 minutes with minimal hints
- **Medium problems:** Solve in 45-60 minutes (60-90 for complex ones)
- **Communication:** Can talk while coding without pausing
- **Code quality:** Clean, readable code with good variable names
- **Confidence:** Feel prepared for technical phone screen

---

## Part 12: Integration with Other Sections

**This Section Builds On:**
- **Sections 1-16 (Foundation + Building):** Technical skills learned (Java, Spring Boot, algorithms) are being refined for interview context
- **Section 24 (Flagship Story):** You'll introduce yourself in technical interviews using your story
- **Sections 25-28 (Networking):** Informational interviews and referrals lead to technical interviews

**This Section Prepares For:**
- **Section 30 (System Design Interviews):** Technical coding is often first round; system design comes next
- **Section 31 (Behavioral Interviews):** Often combined with technical or system design rounds
- **Section 32 (Job Search):** Technical interview skills determine if you get offers

---

## Part 13: Leo-Specific Considerations

### Leveraging Your Operations Background

**In Technical Interviews, You Can:**

**1. Use Operations Examples for Context**
When discussing approaches:
```
"This reminds me of when I was monitoring pipeline sensors - we needed O(1) lookup for
sensor IDs to quickly retrieve real-time data. A hash map would be perfect here for the
same reason."
```

**2. Demonstrate Real-World Thinking**
When discussing edge cases:
```
"From my experience managing SCADA systems, I know real-world data can be messy. I should
handle the empty input case and null values."
```

**3. Connect Algorithms to Operations**
When explaining solutions:
```
"This BFS approach is similar to how we'd detect if a leak in one part of the pipeline
network could affect other parts - we explore connected segments level by level."
```

### Your Unique Strengths

**You have advantages most bootcamp grads don't:**

1. **Systems thinking:** 9 years managing complex systems translates to understanding how code fits into larger systems
2. **Reliability mindset:** You think about edge cases and failure modes naturally
3. **Real-world constraints:** You understand performance and uptime matter
4. **Domain expertise:** For industrial software companies, your operations background is differentiating

**Use these in interviews when appropriate** (but don't force it).

---

## Part 14: Resources

### Online Platforms

**Primary:**
- **LeetCode** (leetcode.com) - Most popular, best for practice
- **Free tier** is sufficient for Week 29

**Supplementary:**
- **NeetCode** (neetcode.io) - Curated list of 150 essential problems
- **AlgoExpert** ($99/year) - Video explanations, good for learning
- **Pramp** (pramp.com) - Free mock interviews

### Books (Optional)

- **"Cracking the Coding Interview"** by Gayle McDowell - Bible of technical interviews
- **"Elements of Programming Interviews in Java"** - More advanced

### YouTube Channels

- **NeetCode** - Clear problem explanations
- **Back to Back SWE** - In-depth algorithm explanations
- **Kevin Naughton Jr.** - LeetCode problem walkthroughs

---

## Final Thoughts

**Technical interviewing is a learnable skill**, separate from being a good engineer. The engineers who succeed at technical interviews are not necessarily the smartest - they're the ones who:

1. **Practice deliberately** (not just grinding problems)
2. **Use a framework** (UMPIRE or similar)
3. **Communicate clearly** (talk while coding)
4. **Stay calm under pressure** (mock interviews help)

**By the end of Week 29, you should:**
- Have solved 10-15 LeetCode problems
- Be comfortable with arrays, strings, hash maps, stacks, queues
- Have attempted tree and graph problems
- Completed 2 mock interviews
- Have a practice routine for Weeks 30+

**Remember:**
- **Backend interviews are less algorithmic** than frontend or general SWE roles
- **Medium problems are your target** - you don't need LeetCode hard
- **Communication matters as much as correctness**
- **Your operations background is an asset** - use it when relevant

**You have the technical foundation (CS degree + bootcamp + Bibby project) and the real-world experience (9 years operations).** Now it's about translating that into interview performance.

**15-20 hours this week will build the foundation. Then 1-2 hours daily until interviews will maintain and improve.**

**Next:** Section 30 will cover system design interviews - where your operational experience with SCADA systems and large-scale infrastructure will really shine.

---

**End of Section 29**
