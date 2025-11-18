# Section 06: Algorithms & Problem Solving II

## Building on the Foundation

You've mastered the foundational patterns: Two Pointers, Frequency Counter, and Sliding Window. These handle most array and string problems in backend interviews.

Now we level up to **hierarchical and graph-based problems**—patterns that directly mirror industrial systems like organizational structures, pipeline networks, and equipment dependency chains.

This section covers:
1. **Tree BFS (Breadth-First Search)** - Level-by-level traversal
2. **Tree DFS (Depth-First Search)** - Go deep before going wide
3. **Graph Traversal** - Connected components and path finding
4. **Dynamic Programming (Introduction)** - Optimization problems
5. **Backtracking** - Exploring all possibilities

These patterns are more relevant to backend engineering than you might think. Your Bibby hierarchy (Bookcase → Shelf → Book) is a tree. Pipeline networks at Kinder Morgan are graphs. Resource optimization is dynamic programming.

## Pattern 4: Tree BFS (Breadth-First Search)

### The Concept

BFS traverses a tree level by level, visiting all nodes at depth d before moving to depth d+1. Uses a **queue** (FIFO) to track nodes to visit.

**When to Use:**
- Level-order traversal
- Finding tree depth/height
- Finding nodes at a specific level
- Shortest path in unweighted tree
- Level-wise processing (like organizational hierarchies)

**Time Complexity:** O(n) - visit each node once
**Space Complexity:** O(w) - w is maximum width of tree (queue size)

### Industrial Analogy

**From Your Operations Experience:**

"At Kinder Morgan, organizational communication often flows level-by-level. A directive from regional management goes to all facility managers first (level 1), then facility managers communicate to all shift supervisors (level 2), then supervisors to operators (level 3).

You don't skip levels—you ensure everyone at each level is informed before proceeding to the next level.

BFS works the same way: process all nodes at current level, then move to the next level. This is how you'd visualize organizational hierarchy or equipment dependency chains."

### Template Code

**Basic BFS:**

```java
public List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) return result;

    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);

    while (!queue.isEmpty()) {
        int levelSize = queue.size();
        List<Integer> currentLevel = new ArrayList<>();

        // Process all nodes at current level
        for (int i = 0; i < levelSize; i++) {
            TreeNode node = queue.poll();
            currentLevel.add(node.val);

            // Add children to queue for next level
            if (node.left != null) queue.offer(node.left);
            if (node.right != null) queue.offer(node.right);
        }

        result.add(currentLevel);
    }

    return result;
}
```

**TreeNode definition:**
```java
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) {
        this.val = val;
    }
}
```

### Problem 1: Binary Tree Level Order Traversal (Medium)

**LeetCode #102**

**Problem:**
Return the level-order traversal of a binary tree (as list of lists, each inner list is one level).

**Example:**
```
Input:     3
          / \
         9  20
           /  \
          15   7

Output: [[3], [9,20], [15,7]]
```

**Solution:**

```java
public List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) {
        return result;
    }

    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);

    while (!queue.isEmpty()) {
        int levelSize = queue.size();
        List<Integer> currentLevel = new ArrayList<>();

        // Process all nodes at this level
        for (int i = 0; i < levelSize; i++) {
            TreeNode node = queue.poll();
            currentLevel.add(node.val);

            // Queue children for next level
            if (node.left != null) {
                queue.offer(node.left);
            }
            if (node.right != null) {
                queue.offer(node.right);
            }
        }

        result.add(currentLevel);
    }

    return result;
}
```

**Time Complexity:** O(n) - visit each node once
**Space Complexity:** O(w) - worst case queue holds all nodes at widest level

**Industrial Connection:**
"Like generating organizational charts—list all employees at each management level. Regional managers (level 1), facility managers (level 2), supervisors (level 3), operators (level 4)."

### Problem 2: Maximum Depth of Binary Tree (Easy)

**LeetCode #104**

**Problem:**
Find the maximum depth (number of nodes along longest path from root to leaf).

**Example:**
```
Input:     3
          / \
         9  20
           /  \
          15   7

Output: 3
```

**Solution (BFS):**

```java
public int maxDepth(TreeNode root) {
    if (root == null) {
        return 0;
    }

    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);
    int depth = 0;

    while (!queue.isEmpty()) {
        int levelSize = queue.size();
        depth++;  // Increment depth for each level

        for (int i = 0; i < levelSize; i++) {
            TreeNode node = queue.poll();

            if (node.left != null) {
                queue.offer(node.left);
            }
            if (node.right != null) {
                queue.offer(node.right);
            }
        }
    }

    return depth;
}
```

**Alternative (DFS - often cleaner for this problem):**

```java
public int maxDepth(TreeNode root) {
    if (root == null) {
        return 0;
    }

    int leftDepth = maxDepth(root.left);
    int rightDepth = maxDepth(root.right);

    return Math.max(leftDepth, rightDepth) + 1;
}
```

**Industrial Connection:**
"Like determining organizational hierarchy depth—how many management levels from CEO to front-line operator. Helps understand command chain complexity."

### Problem 3: Zigzag Level Order Traversal (Medium)

**LeetCode #103**

**Problem:**
Return level order traversal in zigzag pattern (left-to-right, then right-to-left alternating).

**Example:**
```
Input:     3
          / \
         9  20
           /  \
          15   7

Output: [[3], [20,9], [15,7]]
```

**Solution:**

```java
public List<List<Integer>> zigzagLevelOrder(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) {
        return result;
    }

    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);
    boolean leftToRight = true;

    while (!queue.isEmpty()) {
        int levelSize = queue.size();
        List<Integer> currentLevel = new ArrayList<>();

        for (int i = 0; i < levelSize; i++) {
            TreeNode node = queue.poll();
            currentLevel.add(node.val);

            if (node.left != null) {
                queue.offer(node.left);
            }
            if (node.right != null) {
                queue.offer(node.right);
            }
        }

        // Reverse list if right-to-left level
        if (!leftToRight) {
            Collections.reverse(currentLevel);
        }

        result.add(currentLevel);
        leftToRight = !leftToRight;  // Toggle direction
    }

    return result;
}
```

**Industrial Connection:**
"Like alternating inspection patterns—inspect left-to-right on even floors, right-to-left on odd floors for systematic coverage."

### Bibby Implementation: Bookcase Hierarchy Level-Order Listing

**Feature:** List all bookcases and their shelves level-by-level

```java
@Service
public class BookcaseAnalyticsService {

    /**
     * Generates level-order listing of bookcase hierarchy.
     *
     * Level 0: All bookcases
     * Level 1: All shelves grouped by bookcase
     * Level 2: All books grouped by shelf
     *
     * Industrial parallel: Organizational structure reports—
     * list all facilities, then all departments per facility,
     * then all equipment per department.
     */
    public Map<Integer, List<String>> generateHierarchyReport() {
        Map<Integer, List<String>> levels = new HashMap<>();

        // Level 0: Bookcases
        List<BookcaseEntity> bookcases = bookcaseRepository.findAll();
        levels.put(0, bookcases.stream()
            .map(BookcaseEntity::getBookcaseLabel)
            .collect(Collectors.toList()));

        // Level 1: Shelves (grouped by bookcase)
        List<String> shelfLabels = new ArrayList<>();
        for (BookcaseEntity bookcase : bookcases) {
            List<ShelfEntity> shelves = shelfRepository.findByBookcaseId(bookcase.getBookcaseId());
            shelfLabels.addAll(shelves.stream()
                .map(shelf -> String.format("%s/%s",
                    bookcase.getBookcaseLabel(),
                    shelf.getShelfLabel()))
                .collect(Collectors.toList()));
        }
        levels.put(1, shelfLabels);

        // Level 2: Books (could be extensive)
        // Implementation similar to above

        return levels;
    }

    /**
     * Calculates maximum depth of bookcase hierarchy.
     *
     * Useful for understanding organizational complexity.
     * Deep hierarchies may indicate need for restructuring.
     */
    public int calculateHierarchyDepth() {
        // In current model: Bookcase → Shelf → Book = depth of 3
        // Could be dynamic if hierarchy is more complex

        List<BookcaseEntity> bookcases = bookcaseRepository.findAll();
        if (bookcases.isEmpty()) return 0;

        int maxDepth = 1;  // At least one level (bookcases exist)

        for (BookcaseEntity bookcase : bookcases) {
            List<ShelfEntity> shelves = shelfRepository.findByBookcaseId(bookcase.getBookcaseId());
            if (!shelves.isEmpty()) {
                maxDepth = Math.max(maxDepth, 2);  // Shelves exist

                for (ShelfEntity shelf : shelves) {
                    List<BookEntity> books = bookRepository.findByShelfId(shelf.getShelfId());
                    if (!books.isEmpty()) {
                        maxDepth = Math.max(maxDepth, 3);  // Books exist
                        break;
                    }
                }
            }
        }

        return maxDepth;
    }
}
```

**In an interview:**
"I used BFS thinking in Bibby to generate hierarchical reports of the library structure—bookcases, then shelves, then books. This level-by-level processing mirrors how we'd generate organizational reports at Kinder Morgan: facilities first, then departments, then equipment."

## Pattern 5: Tree DFS (Depth-First Search)

### The Concept

DFS explores as far down one branch as possible before backtracking. Can be implemented recursively (elegant) or iteratively with a stack.

**Three DFS Orders:**
1. **Pre-order:** Process node, then left, then right (root first)
2. **In-order:** Process left, then node, then right (sorted for BST)
3. **Post-order:** Process left, then right, then node (children first)

**When to Use:**
- Path problems (root to leaf)
- Tree validation (BST checking)
- Calculating tree properties (diameter, height)
- Problems requiring exploring all paths
- When recursion makes sense

**Time Complexity:** O(n) - visit each node once
**Space Complexity:** O(h) - h is height (recursion stack or explicit stack)

### Industrial Analogy

**From Your Operations Experience:**

"At Kinder Morgan, when investigating a pipeline incident, we'd trace the problem down one specific branch of the system until we reached the root cause, then backtrack if needed.

We wouldn't jump between different branches—we'd follow one path completely: 'Problem in Region A → Facility 5 → Pipeline Segment 23 → Valve V-47.' Found the issue? Great. If not, backtrack and try another branch.

DFS works the same way: follow one path to its end before exploring other paths. It's depth-first exploration."

### Template Code

**Recursive DFS (Pre-order):**

```java
public void dfs(TreeNode root) {
    if (root == null) {
        return;
    }

    // Process current node (pre-order)
    System.out.println(root.val);

    // Recurse on children
    dfs(root.left);
    dfs(root.right);
}
```

**Iterative DFS (using stack):**

```java
public void dfsIterative(TreeNode root) {
    if (root == null) return;

    Stack<TreeNode> stack = new Stack<>();
    stack.push(root);

    while (!stack.isEmpty()) {
        TreeNode node = stack.pop();
        System.out.println(node.val);

        // Push right first so left is processed first
        if (node.right != null) stack.push(node.right);
        if (node.left != null) stack.push(node.left);
    }
}
```

### Problem 1: Path Sum (Easy)

**LeetCode #112**

**Problem:**
Given a tree and target sum, determine if there's a root-to-leaf path that sums to target.

**Example:**
```
Input:     5
          / \
         4   8
        /   / \
       11  13  4
      /  \      \
     7    2      1

Target: 22
Output: true (5→4→11→2 = 22)
```

**Solution:**

```java
public boolean hasPathSum(TreeNode root, int targetSum) {
    // Base case: empty tree
    if (root == null) {
        return false;
    }

    // Base case: leaf node
    if (root.left == null && root.right == null) {
        return root.val == targetSum;
    }

    // Recursive case: check left and right subtrees
    int remainingSum = targetSum - root.val;
    return hasPathSum(root.left, remainingSum) ||
           hasPathSum(root.right, remainingSum);
}
```

**Time Complexity:** O(n) - might visit all nodes
**Space Complexity:** O(h) - recursion depth (h = height)

**Industrial Connection:**
"Like tracing resource allocation paths—does any path from headquarters to field equipment total the target budget? Follow each path to completion."

### Problem 2: Validate Binary Search Tree (Medium)

**LeetCode #98**

**Problem:**
Determine if a binary tree is a valid BST (left < node < right for all nodes).

**Example:**
```
Input:     2
          / \
         1   3

Output: true

Input:     5
          / \
         1   4
            / \
           3   6

Output: false (4 is in right subtree of 5, but 3 < 5)
```

**Solution:**

```java
public boolean isValidBST(TreeNode root) {
    return validate(root, Long.MIN_VALUE, Long.MAX_VALUE);
}

private boolean validate(TreeNode node, long min, long max) {
    // Base case: null nodes are valid
    if (node == null) {
        return true;
    }

    // Check if current node violates BST property
    if (node.val <= min || node.val >= max) {
        return false;
    }

    // Validate left subtree (all values must be < node.val)
    // Validate right subtree (all values must be > node.val)
    return validate(node.left, min, node.val) &&
           validate(node.right, node.val, max);
}
```

**Time Complexity:** O(n)
**Space Complexity:** O(h)

**Key Insight:**
Each node must satisfy: `min < node.val < max`
- Left child inherits min, gets node.val as new max
- Right child inherits max, gets node.val as new min

**Industrial Connection:**
"Like validating equipment hierarchy constraints—child equipment must have specifications within parent's operating range. Every level must satisfy constraints inherited from above."

### Problem 3: Diameter of Binary Tree (Easy)

**LeetCode #543**

**Problem:**
Find the length of the longest path between any two nodes (diameter doesn't have to pass through root).

**Example:**
```
Input:     1
          / \
         2   3
        / \
       4   5

Output: 3 (path 4→2→1→3 or 5→2→1→3)
```

**Solution:**

```java
class Solution {
    private int maxDiameter = 0;

    public int diameterOfBinaryTree(TreeNode root) {
        calculateHeight(root);
        return maxDiameter;
    }

    private int calculateHeight(TreeNode node) {
        if (node == null) {
            return 0;
        }

        // Get heights of left and right subtrees
        int leftHeight = calculateHeight(node.left);
        int rightHeight = calculateHeight(node.right);

        // Diameter through this node is left + right heights
        int currentDiameter = leftHeight + rightHeight;
        maxDiameter = Math.max(maxDiameter, currentDiameter);

        // Return height of this subtree
        return Math.max(leftHeight, rightHeight) + 1;
    }
}
```

**Time Complexity:** O(n)
**Space Complexity:** O(h)

**Industrial Connection:**
"Like finding the longest communication path in an organization—doesn't necessarily go through the CEO, might be lateral across departments. Track the longest path as you traverse the hierarchy."

### Bibby Implementation: Calculate Total Books in Hierarchy

**Feature:** Count total books using DFS through bookcase structure

```java
@Service
public class BookcaseAnalyticsService {

    /**
     * Calculates total capacity across entire bookcase hierarchy.
     * Uses DFS-style recursive aggregation.
     *
     * Industrial parallel: Calculating total equipment capacity across
     * all facilities—recurse through organizational tree summing capacity.
     */
    public int calculateTotalCapacity() {
        List<BookcaseEntity> bookcases = bookcaseRepository.findAll();
        int totalCapacity = 0;

        for (BookcaseEntity bookcase : bookcases) {
            totalCapacity += calculateBookcaseCapacity(bookcase);
        }

        return totalCapacity;
    }

    private int calculateBookcaseCapacity(BookcaseEntity bookcase) {
        // Get all shelves for this bookcase
        List<ShelfEntity> shelves = shelfRepository.findByBookcaseId(bookcase.getBookcaseId());

        int bookcaseTotal = 0;
        for (ShelfEntity shelf : shelves) {
            // Could extend to calculate shelf capacity
            List<BookEntity> books = bookRepository.findByShelfId(shelf.getShelfId());
            bookcaseTotal += books.size();
        }

        return bookcaseTotal;
    }

    /**
     * Validates hierarchical constraints.
     * E.g., ensures no shelf exceeds bookcase capacity.
     *
     * Like validating BST—each child must satisfy constraints
     * inherited from parent.
     */
    public boolean validateHierarchyConstraints() {
        List<BookcaseEntity> bookcases = bookcaseRepository.findAll();

        for (BookcaseEntity bookcase : bookcases) {
            List<ShelfEntity> shelves = shelfRepository.findByBookcaseId(bookcase.getBookcaseId());

            // Check: number of shelves doesn't exceed bookcase capacity
            if (shelves.size() > bookcase.getShelfCapacity()) {
                return false;  // Constraint violated
            }

            // Could add more constraints per shelf
            for (ShelfEntity shelf : shelves) {
                // E.g., max books per shelf
                List<BookEntity> books = bookRepository.findByShelfId(shelf.getShelfId());
                // Validation logic...
            }
        }

        return true;
    }
}
```

**In an interview:**
"I used DFS-style recursion in Bibby to calculate total capacity across the hierarchy—sum books per shelf, sum shelves per bookcase, sum all bookcases. At Kinder Morgan, we did similar recursive aggregation for equipment capacity across facilities."

## Pattern 6: Graph Traversal

### The Concept

Graphs represent relationships between entities. Unlike trees, graphs can have:
- **Cycles** (A → B → C → A)
- **Multiple paths** between nodes
- **Disconnected components**

**Graph Representations:**
1. **Adjacency List:** Map of node → list of neighbors (most common)
2. **Adjacency Matrix:** 2D array where matrix[i][j] = edge exists

**Common Algorithms:**
- **BFS:** Shortest path in unweighted graph
- **DFS:** Detect cycles, find connected components
- **Union-Find:** Check connectivity

**When to Use:**
- Network problems (pipeline networks, org charts)
- Dependency problems (task scheduling)
- Connected components (islands, groups)
- Path finding

### Industrial Analogy

**From Your Operations Experience:**

"Pipeline networks at Kinder Morgan are graphs. Facilities are nodes, pipelines are edges.

When planning maintenance, we need to know: 'If we shut down pipeline P-12, which facilities lose connectivity?' That's finding connected components.

When routing product, we need: 'What's the shortest path from source to destination?' That's BFS on a graph.

When validating safety interlocks, we check: 'Are there circular dependencies?' That's cycle detection with DFS."

### Template Code

**Graph BFS:**

```java
public void bfs(Map<Integer, List<Integer>> graph, int start) {
    Set<Integer> visited = new HashSet<>();
    Queue<Integer> queue = new LinkedList<>();

    queue.offer(start);
    visited.add(start);

    while (!queue.isEmpty()) {
        int node = queue.poll();
        System.out.println(node);  // Process node

        // Visit neighbors
        for (int neighbor : graph.getOrDefault(node, new ArrayList<>())) {
            if (!visited.contains(neighbor)) {
                visited.add(neighbor);
                queue.offer(neighbor);
            }
        }
    }
}
```

**Graph DFS (recursive):**

```java
public void dfs(Map<Integer, List<Integer>> graph, int node, Set<Integer> visited) {
    visited.add(node);
    System.out.println(node);  // Process node

    for (int neighbor : graph.getOrDefault(node, new ArrayList<>())) {
        if (!visited.contains(neighbor)) {
            dfs(graph, neighbor, visited);
        }
    }
}
```

### Problem 1: Number of Islands (Medium)

**LeetCode #200**

**Problem:**
Given a 2D grid of '1's (land) and '0's (water), count the number of islands. An island is surrounded by water and formed by connecting adjacent lands horizontally or vertically.

**Example:**
```
Input: grid = [
  ['1','1','0','0','0'],
  ['1','1','0','0','0'],
  ['0','0','1','0','0'],
  ['0','0','0','1','1']
]
Output: 3
```

**Solution (DFS):**

```java
public int numIslands(char[][] grid) {
    if (grid == null || grid.length == 0) {
        return 0;
    }

    int rows = grid.length;
    int cols = grid[0].length;
    int islandCount = 0;

    for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
            if (grid[r][c] == '1') {
                islandCount++;
                // Mark entire island as visited via DFS
                markIsland(grid, r, c);
            }
        }
    }

    return islandCount;
}

private void markIsland(char[][] grid, int r, int c) {
    // Boundary check and water check
    if (r < 0 || r >= grid.length || c < 0 || c >= grid[0].length || grid[r][c] == '0') {
        return;
    }

    // Mark as visited
    grid[r][c] = '0';

    // DFS in 4 directions
    markIsland(grid, r + 1, c);  // Down
    markIsland(grid, r - 1, c);  // Up
    markIsland(grid, r, c + 1);  // Right
    markIsland(grid, r, c - 1);  // Left
}
```

**Time Complexity:** O(m × n) - visit each cell once
**Space Complexity:** O(m × n) - worst case recursion depth

**Industrial Connection:**
"Like identifying isolated facility clusters—which facilities are connected via pipelines, which are isolated? Each connected cluster is an 'island' in the network graph."

### Problem 2: Clone Graph (Medium)

**LeetCode #133**

**Problem:**
Deep clone an undirected graph. Each node contains a value and a list of neighbors.

**Solution:**

```java
class Node {
    public int val;
    public List<Node> neighbors;

    public Node(int val) {
        this.val = val;
        this.neighbors = new ArrayList<>();
    }
}

public Node cloneGraph(Node node) {
    if (node == null) {
        return null;
    }

    // Map original node -> cloned node
    Map<Node, Node> cloned = new HashMap<>();
    return cloneNode(node, cloned);
}

private Node cloneNode(Node node, Map<Node, Node> cloned) {
    // If already cloned, return the clone
    if (cloned.containsKey(node)) {
        return cloned.get(node);
    }

    // Create new node
    Node copy = new Node(node.val);
    cloned.put(node, copy);

    // Clone all neighbors recursively
    for (Node neighbor : node.neighbors) {
        copy.neighbors.add(cloneNode(neighbor, cloned));
    }

    return copy;
}
```

**Industrial Connection:**
"Like duplicating network configurations—create a complete copy of a pipeline network structure for disaster recovery or testing scenarios."

### Bibby Implementation: Book Recommendation Graph

**Feature:** Recommend books based on co-checkout patterns (graph of books)

```java
@Service
public class BookRecommendationService {

    /**
     * Builds a graph where books are nodes, edges connect books
     * frequently checked out together.
     *
     * Industrial parallel: Equipment usage correlation—which equipment
     * pieces are often used together? Build dependency graphs for
     * maintenance planning.
     */
    public Map<Long, List<Long>> buildCoCheckoutGraph() {
        // Graph: book ID -> list of related book IDs
        Map<Long, List<Long>> graph = new HashMap<>();

        // Get all checkout records (hypothetical CheckoutHistory entity)
        // Group by user or time window
        // For each pair of books checked out together, add edge

        // Simplified example:
        List<CheckoutPair> pairs = findCoCheckoutPairs();
        for (CheckoutPair pair : pairs) {
            graph.computeIfAbsent(pair.book1Id, k -> new ArrayList<>()).add(pair.book2Id);
            graph.computeIfAbsent(pair.book2Id, k -> new ArrayList<>()).add(pair.book1Id);
        }

        return graph;
    }

    /**
     * Finds recommended books using BFS from a seed book.
     * Explores books within n hops of the seed.
     */
    public List<Long> findRecommendations(Long seedBookId, int maxHops) {
        Map<Long, List<Long>> graph = buildCoCheckoutGraph();
        Set<Long> visited = new HashSet<>();
        List<Long> recommendations = new ArrayList<>();

        Queue<QueueItem> queue = new LinkedList<>();
        queue.offer(new QueueItem(seedBookId, 0));
        visited.add(seedBookId);

        while (!queue.isEmpty()) {
            QueueItem item = queue.poll();

            if (item.hops > maxHops) {
                continue;
            }

            // Don't recommend the seed book itself
            if (item.hops > 0) {
                recommendations.add(item.bookId);
            }

            // Explore neighbors
            for (Long neighborId : graph.getOrDefault(item.bookId, new ArrayList<>())) {
                if (!visited.contains(neighborId)) {
                    visited.add(neighborId);
                    queue.offer(new QueueItem(neighborId, item.hops + 1));
                }
            }
        }

        return recommendations;
    }

    private static class QueueItem {
        Long bookId;
        int hops;

        QueueItem(Long bookId, int hops) {
            this.bookId = bookId;
            this.hops = hops;
        }
    }

    private static class CheckoutPair {
        Long book1Id;
        Long book2Id;
    }

    private List<CheckoutPair> findCoCheckoutPairs() {
        // Implementation would query checkout history
        return new ArrayList<>();
    }
}
```

**In an interview:**
"I implemented graph traversal in Bibby for book recommendations—books that are frequently checked out together form a graph. BFS from a seed book finds recommendations within n hops. At Kinder Morgan, we used similar correlation graphs for equipment—which equipment pieces are used together, helping predict maintenance needs."

## Pattern 7: Dynamic Programming (Introduction)

### The Concept

Dynamic Programming (DP) solves optimization problems by breaking them into overlapping subproblems and storing results to avoid redundant computation.

**Key Characteristics:**
1. **Optimal Substructure:** Optimal solution contains optimal solutions to subproblems
2. **Overlapping Subproblems:** Same subproblems computed multiple times

**Approaches:**
1. **Top-Down (Memoization):** Recursive with caching
2. **Bottom-Up (Tabulation):** Iterative, build table

**When to Use:**
- Optimization problems (min/max)
- Counting problems (how many ways)
- Decision problems (is it possible)

### Industrial Analogy

**From Your Operations Experience:**

"At Kinder Morgan, route optimization for product delivery involved finding the most cost-effective path through the pipeline network.

We didn't recalculate the optimal route from scratch every time—we stored solutions to subproblems: 'Optimal route from A to B,' 'Optimal route from B to C,' then combined them.

Dynamic programming works the same: solve small problems once, store the answers, build up to the big problem without recalculating."

### Problem 1: Climbing Stairs (Easy)

**LeetCode #70**

**Problem:**
You're climbing a staircase with n steps. Each time you can climb 1 or 2 steps. How many distinct ways can you reach the top?

**Example:**
```
Input: n = 3
Output: 3
Explanation: Three ways: 1+1+1, 1+2, 2+1
```

**Solution (Top-Down Memoization):**

```java
public int climbStairs(int n) {
    Map<Integer, Integer> memo = new HashMap<>();
    return climb(n, memo);
}

private int climb(int n, Map<Integer, Integer> memo) {
    if (n <= 2) {
        return n;  // 1 step: 1 way, 2 steps: 2 ways
    }

    if (memo.containsKey(n)) {
        return memo.get(n);  // Already calculated
    }

    int ways = climb(n - 1, memo) + climb(n - 2, memo);
    memo.put(n, ways);

    return ways;
}
```

**Solution (Bottom-Up):**

```java
public int climbStairs(int n) {
    if (n <= 2) {
        return n;
    }

    int[] dp = new int[n + 1];
    dp[1] = 1;  // 1 way to reach step 1
    dp[2] = 2;  // 2 ways to reach step 2

    for (int i = 3; i <= n; i++) {
        dp[i] = dp[i - 1] + dp[i - 2];
    }

    return dp[n];
}
```

**Optimized (O(1) space):**

```java
public int climbStairs(int n) {
    if (n <= 2) {
        return n;
    }

    int prev2 = 1;  // dp[i-2]
    int prev1 = 2;  // dp[i-1]

    for (int i = 3; i <= n; i++) {
        int current = prev1 + prev2;
        prev2 = prev1;
        prev1 = current;
    }

    return prev1;
}
```

**Time Complexity:** O(n)
**Space Complexity:** O(1) in optimized version

**Pattern Recognition:**
This is actually Fibonacci! `ways(n) = ways(n-1) + ways(n-2)`

**Industrial Connection:**
"Like calculating equipment replacement schedules—optimal strategy at step n depends on optimal strategies at previous steps. Store solutions to avoid recalculating."

### Problem 2: House Robber (Medium)

**LeetCode #198**

**Problem:**
You're a robber planning to rob houses along a street. Each house has money. You can't rob adjacent houses (alarms). Maximize the amount you can rob.

**Example:**
```
Input: nums = [2,7,9,3,1]
Output: 12
Explanation: Rob house 1 (2) + house 3 (9) + house 5 (1) = 12
```

**Solution:**

```java
public int rob(int[] nums) {
    if (nums.length == 0) return 0;
    if (nums.length == 1) return nums[0];

    int prev2 = 0;           // max money at house i-2
    int prev1 = nums[0];     // max money at house i-1

    for (int i = 1; i < nums.length; i++) {
        // Either rob current house + prev2, or skip (take prev1)
        int current = Math.max(nums[i] + prev2, prev1);
        prev2 = prev1;
        prev1 = current;
    }

    return prev1;
}
```

**DP Relation:**
```
dp[i] = max(nums[i] + dp[i-2], dp[i-1])
```

Either rob house i (add to max from i-2) or skip it (take max from i-1).

**Time Complexity:** O(n)
**Space Complexity:** O(1)

**Industrial Connection:**
"Like scheduling maintenance windows—if you service equipment A, you can't service adjacent equipment B (they're in same system). Maximize total equipment serviced while respecting constraints."

### Bibby Implementation: Optimal Book Selection

**Feature:** Given limited shelf space, select books to maximize value

```java
@Service
public class BookSelectionService {

    /**
     * Knapsack problem: Given shelf capacity, select books
     * to maximize total "value" (could be popularity score,
     * checkout frequency, etc.)
     *
     * Industrial parallel: Resource allocation—given limited
     * budget or capacity, select equipment to maximize
     * operational value.
     */
    public List<BookEntity> selectOptimalBooks(
            List<BookEntity> availableBooks,
            int shelfCapacity,
            Function<BookEntity, Integer> valueFunction) {

        // Simplified: assume each book takes 1 unit of space
        // Value is determined by provided function

        if (availableBooks.size() <= shelfCapacity) {
            return availableBooks;  // All fit
        }

        // Sort by value descending (greedy approach for simple case)
        // For true knapsack, would use DP
        return availableBooks.stream()
            .sorted((a, b) -> valueFunction.apply(b).compareTo(valueFunction.apply(a)))
            .limit(shelfCapacity)
            .collect(Collectors.toList());
    }

    /**
     * Calculates maximum checkout frequency achievable
     * with optimal book selection (DP approach).
     */
    public int maxCheckoutFrequency(List<Integer> bookFrequencies, int capacity) {
        // DP: dp[i] = max frequency achievable with i capacity

        int[] dp = new int[capacity + 1];

        for (int freq : bookFrequencies) {
            for (int c = capacity; c >= 1; c--) {
                // Each book takes 1 unit, has frequency value
                dp[c] = Math.max(dp[c], dp[c - 1] + freq);
            }
        }

        return dp[capacity];
    }
}
```

**In an interview:**
"I applied DP thinking in Bibby for optimal book selection—given limited shelf space, which books maximize checkout frequency? It's a knapsack variant. At Kinder Morgan, we used similar optimization for equipment allocation—maximize operational capacity within budget constraints."

## Pattern 8: Backtracking

### The Concept

Backtracking explores all possible solutions by building candidates incrementally and abandoning candidates ("backtracking") when they violate constraints.

**Template:**
```
1. Choose (add to current solution)
2. Explore (recurse)
3. Unchoose (remove from current solution - backtrack)
```

**When to Use:**
- Generating permutations/combinations
- Sudoku solvers
- N-Queens problem
- Finding all paths
- Constraint satisfaction problems

### Industrial Analogy

"At Kinder Morgan, when planning incident response procedures, we'd consider different response strategies: 'Try approach A. If that fails, backtrack and try approach B.'

We'd explore each branch fully before moving to the next option. Backtracking in code works the same: try an option, explore where it leads, if it doesn't work, undo (backtrack) and try the next option."

### Problem: Subsets (Medium)

**LeetCode #78**

**Problem:**
Given an array of unique integers, return all possible subsets.

**Example:**
```
Input: nums = [1,2,3]
Output: [[],[1],[2],[1,2],[3],[1,3],[2,3],[1,2,3]]
```

**Solution:**

```java
public List<List<Integer>> subsets(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    backtrack(nums, 0, new ArrayList<>(), result);
    return result;
}

private void backtrack(int[] nums, int start, List<Integer> current, List<List<Integer>> result) {
    // Add current subset to result
    result.add(new ArrayList<>(current));

    // Explore adding each remaining element
    for (int i = start; i < nums.length; i++) {
        // Choose
        current.add(nums[i]);

        // Explore
        backtrack(nums, i + 1, current, result);

        // Unchoose (backtrack)
        current.remove(current.size() - 1);
    }
}
```

**Time Complexity:** O(2^n) - 2^n possible subsets
**Space Complexity:** O(n) - recursion depth

## Exercises for This Section

### Exercise 1: Pattern Mastery (12-15 hours over Week 7)

**Days 1-2: Tree BFS**
- Problems: Level Order Traversal, Max Depth, Zigzag
- Implement hierarchy report in Bibby
- Anki cards

**Days 3-4: Tree DFS**
- Problems: Path Sum, Validate BST, Diameter
- Implement capacity calculation in Bibby
- Journal entry

**Days 5-6: Graph Traversal**
- Problems: Number of Islands, Clone Graph
- Implement book recommendation graph
- Practice explaining

**Day 7: DP & Backtracking**
- Problems: Climbing Stairs, House Robber, Subsets
- Review all patterns
- Update Bibby README

**Deliverable:** 10+ problems, 3 Bibby features

### Exercise 2: Pattern Application Matrix (2 hours)

Create comprehensive reference connecting all patterns to industrial applications.

### Exercise 3: Mock System Design (2 hours)

Design a simple system using these patterns:
- "Design a library recommendation system"
- Use graphs for book relationships
- Use trees for category hierarchy
- Use DP for optimal selection

**Deliverable:** Design document with pattern justifications

## Action Items for Week 7

### Critical
1. ✅ Complete Exercise 1: 10+ problems across 4 patterns
2. ✅ Implement 3 Bibby features (hierarchy report, capacity calc, recommendations)
3. ✅ Create Anki cards for all patterns
4. ✅ Daily practice: 1-2 problems

### Important
5. ⬜ Complete Exercise 2: Pattern application matrix
6. ⬜ Complete Exercise 3: Mock system design
7. ⬜ Update learning journal
8. ⬜ Can explain all patterns without reference

## Key Takeaways

1. **Trees mirror hierarchies.** BFS for level-by-level, DFS for deep exploration.

2. **Graphs mirror networks.** Pipeline networks, equipment dependencies, organizational relationships.

3. **DP optimizes.** Break into subproblems, store solutions, build up.

4. **Backtracking explores.** Try options systematically, backtrack when constraints violated.

5. **Every pattern has industrial parallels.** Use your operational experience to understand algorithms.

## What's Next

Section 07: **Clean Code, Domain Modeling & Architecture Thinking**
- Bibby entity relationships deep dive
- JPA best practices
- Domain-driven design basics
- SOLID principles in practice
- Architecture decision records

---

**Word Count:** ~7,400 words

**Time Investment Week 7:** 18-22 hours
- Pattern study: 12-15 hours
- Bibby implementation: 4-5 hours
- System design: 2 hours

**Success Metrics:**
- 10+ problems solved
- Pattern recognition: 85%+
- All Bibby features working
- Can explain industrial connections clearly

---

*Advanced patterns aren't just interview prep—they're how you think about industrial systems. Trees are hierarchies. Graphs are networks. DP is optimization. Connect the dots between algorithms and operations.*
