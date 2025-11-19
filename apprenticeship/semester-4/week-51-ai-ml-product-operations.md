# Week 51: AI/ML in Product & Operations

## Overview

This week focuses on integrating artificial intelligence and machine learning into your product and operations. You'll learn when to use AI, how to build ML-powered features, and how to operationalize ML systems at scale. This is critical knowledge for modern product builders—AI is no longer optional.

**Learning Objectives:**
- Understand AI/ML fundamentals from a product perspective
- Identify high-impact AI opportunities in your product
- Build recommendation systems and personalization engines
- Implement natural language processing for search and discovery
- Deploy and monitor ML models in production
- Manage ML costs and infrastructure
- Navigate AI ethics and responsible AI practices
- Integrate modern LLMs (GPT-4, Claude) into products

**Key Concepts:**
- Machine Learning vs Deep Learning vs AI
- Supervised vs Unsupervised vs Reinforcement Learning
- Model training, evaluation, and deployment
- MLOps (ML Operations)
- Feature engineering and data pipelines
- A/B testing ML models
- Model drift and retraining
- Embedding models and vector search
- Fine-tuning vs RAG (Retrieval-Augmented Generation)
- AI product strategy

---

## Part 1: AI/ML Fundamentals for Product Leaders

### 1.1 What is Machine Learning?

Machine Learning is about building systems that learn from data rather than being explicitly programmed.

**Traditional Programming:**
```
Rules + Data → Outcomes

Example:
IF user rated book 5 stars AND book is fiction
THEN recommend similar fiction books
```

**Machine Learning:**
```
Data + Outcomes → Rules (Model)

Example:
Training data: 1M user ratings + book metadata
Model learns: Complex patterns of what users like
Result: Personalized recommendations
```

### 1.2 Types of Machine Learning

**1. Supervised Learning:**
- Learn from labeled examples
- Predict outcomes for new data

Examples:
- Book recommendation (training: user ratings → predict: what they'll like)
- Spam detection (training: emails labeled spam/not spam → predict: is new email spam?)
- Image classification (training: labeled book covers → predict: genre from cover)

**2. Unsupervised Learning:**
- Find patterns in unlabeled data
- No "correct answer" to learn from

Examples:
- User segmentation (cluster users by reading behavior)
- Anomaly detection (find unusual patterns in user behavior)
- Topic modeling (discover themes across books)

**3. Reinforcement Learning:**
- Learn through trial and error
- Optimize for long-term rewards

Examples:
- Notification timing (learn when users are most likely to engage)
- Dynamic pricing (learn optimal prices)
- Game AI (AlphaGo, chess engines)

### 1.3 When to Use AI/ML

**Good Use Cases:**
- ✅ Lots of data (thousands to millions of examples)
- ✅ Pattern recognition (image, text, time series)
- ✅ Personalization at scale
- ✅ Complex rules that are hard to write manually
- ✅ Continuous improvement from new data

**Bad Use Cases:**
- ❌ Small datasets (<1000 examples)
- ❌ Need 100% accuracy (use rules instead)
- ❌ Can't tolerate errors (medical, financial)
- ❌ Simple rules work fine (don't overcomplicate)
- ❌ No plan to maintain/retrain model

### 1.4 The ML Product Lifecycle

```
1. Problem Definition
   - What are we trying to predict?
   - How will we measure success?
   - What data do we have?
   ↓
2. Data Collection & Labeling
   - Gather training data
   - Label examples (if supervised)
   - Clean and validate data
   ↓
3. Feature Engineering
   - Transform raw data into features
   - Create derived features
   - Handle missing values
   ↓
4. Model Training
   - Choose algorithm
   - Train on training set
   - Tune hyperparameters
   ↓
5. Evaluation
   - Test on held-out data
   - Compare to baseline
   - A/B test in production
   ↓
6. Deployment
   - Serve predictions in real-time
   - Monitor performance
   - Set up alerts
   ↓
7. Monitoring & Retraining
   - Track model drift
   - Retrain on new data
   - Continuous improvement
```

---

## Part 2: AI-Powered Features for Bibby

### 2.1 Book Recommendations

The core ML feature: recommend books users will love.

**Approaches:**

**1. Collaborative Filtering:**
- "Users who liked X also liked Y"
- Learn from user behavior (ratings, saves, reads)
- No need for book metadata

**2. Content-Based Filtering:**
- Recommend books similar to what user liked
- Use book metadata (genre, author, topics, writing style)
- Works for new users with no history

**3. Hybrid Approach (Best):**
- Combine collaborative + content-based
- Use deep learning to learn both patterns
- Add contextual features (time of day, device, season)

**Example Architecture:**

```
User Features:
- Reading history (last 50 books)
- Ratings distribution
- Favorite genres
- Reading pace
- Time of day patterns

Book Features:
- Genre, subgenre
- Author, publisher
- Page count, publication year
- Average rating
- Description embeddings (semantic meaning)

Context Features:
- Current season
- User's mood (from recent activity)
- Device type
- Time since last book finished

↓

Neural Network Model
(learns complex interactions)

↓

Predicted Rating for each book
(sort by score, return top 10)
```

### 2.2 Search & Discovery

Make it easy to find books using natural language.

**Semantic Search:**
- Traditional search: exact keyword matching
- Semantic search: understand meaning and intent

**Example:**
```
Query: "books about overcoming adversity with strong female leads"

Traditional keyword search:
- Must contain "adversity" AND "female"
- Misses books that don't use exact words

Semantic search:
- Understands: resilience, challenges, women protagonists
- Finds: "Wild" by Cheryl Strayed, "Educated" by Tara Westover
- Even if they don't contain exact keywords
```

**Implementation: Embedding Models**

```
1. Convert text to vectors (embeddings)
   - Book descriptions → 768-dimensional vectors
   - User queries → 768-dimensional vectors

2. Find similar vectors
   - Cosine similarity
   - Vector database (Pinecone, Weaviate, pgvector)

3. Return most similar books
```

### 2.3 Reading Insights

Use ML to give users insights about their reading habits.

**Features:**
- Reading pace prediction ("You'll finish in 3 days")
- Genre diversity score
- Reading level analysis
- Mood tracking (sentiment analysis of books read)
- Author discovery (find new authors similar to favorites)
- Reading goals (predict if on track)

### 2.4 Smart Notifications

Use ML to send notifications at the right time.

**Prediction Model:**
```
Input Features:
- Time of day
- Day of week
- User's historical engagement
- Time since last login
- Current reading streak

Output:
- Probability of engagement (0-1)
- Only send if P(engagement) > 0.3
```

**Result:**
- Higher open rates
- Less notification fatigue
- Better user experience

### 2.5 Book Cover Recognition

Use computer vision to let users add books by taking photos.

**Implementation:**
- User takes photo of book cover
- OCR (Optical Character Recognition) extracts title + author
- Search database for match
- Confirm with user
- Add to library

**Bonus: Reading Analytics**
- Detect books in background of user photos
- Suggest books based on physical library

---

## Part 3: Building ML Systems

### 3.1 Recommendation Engine Implementation

Let's build a hybrid recommendation system for Bibby:

```java
package com.bibby.ml;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationEngine {

    private final RestTemplate restTemplate;
    private final EmbeddingService embeddingService;
    private final CollaborativeFilteringService cfService;

    public RecommendationEngine(
        RestTemplate restTemplate,
        EmbeddingService embeddingService,
        CollaborativeFilteringService cfService
    ) {
        this.restTemplate = restTemplate;
        this.embeddingService = embeddingService;
        this.cfService = cfService;
    }

    // ============= MODELS =============

    public record Book(
        String bookId,
        String title,
        String author,
        List<String> genres,
        String description,
        int pageCount,
        double averageRating,
        int ratingsCount,
        int publicationYear,
        double[] embedding  // semantic representation
    ) {}

    public record UserProfile(
        String userId,
        List<String> readBookIds,
        Map<String, Double> ratings,  // bookId -> rating
        List<String> favoriteGenres,
        double averageRating,
        int booksRead,
        double readingPace,  // pages per day
        Map<String, Integer> genreCounts
    ) {}

    public record Recommendation(
        Book book,
        double score,
        String reason,
        RecommendationType type
    ) {
        public enum RecommendationType {
            COLLABORATIVE,  // "Users like you enjoyed..."
            CONTENT_BASED,  // "Similar to books you liked..."
            TRENDING,       // "Popular this week..."
            PERSONALIZED_TRENDING,  // "Trending in genres you love..."
            SERENDIPITY     // "Try something new..."
        }
    }

    // ============= RECOMMENDATION STRATEGIES =============

    public List<Recommendation> getRecommendations(
        String userId,
        int count,
        Map<String, Object> context
    ) {
        UserProfile profile = buildUserProfile(userId);

        // Multi-strategy approach
        List<Recommendation> candidates = new ArrayList<>();

        // 1. Collaborative Filtering (40% weight)
        candidates.addAll(getCollaborativeRecommendations(profile, count * 2));

        // 2. Content-Based (30% weight)
        candidates.addAll(getContentBasedRecommendations(profile, count * 2));

        // 3. Trending in favorite genres (20% weight)
        candidates.addAll(getTrendingRecommendations(profile, count));

        // 4. Serendipity - try something new (10% weight)
        candidates.addAll(getSerendipityRecommendations(profile, count / 2));

        // Deduplicate and score
        Map<String, Recommendation> uniqueRecs = new HashMap<>();
        for (Recommendation rec : candidates) {
            String bookId = rec.book().bookId();
            if (!uniqueRecs.containsKey(bookId) ||
                uniqueRecs.get(bookId).score() < rec.score()) {
                uniqueRecs.put(bookId, rec);
            }
        }

        // Sort by score and return top N
        return uniqueRecs.values().stream()
            .filter(rec -> !profile.readBookIds().contains(rec.book().bookId()))
            .sorted((a, b) -> Double.compare(b.score(), a.score()))
            .limit(count)
            .collect(Collectors.toList());
    }

    // ============= COLLABORATIVE FILTERING =============

    private List<Recommendation> getCollaborativeRecommendations(
        UserProfile profile,
        int count
    ) {
        // Find similar users based on rating patterns
        List<String> similarUsers = cfService.findSimilarUsers(
            profile.userId(),
            50  // top 50 similar users
        );

        // Get books they liked that this user hasn't read
        Map<String, Double> bookScores = new HashMap<>();

        for (String similarUserId : similarUsers) {
            UserProfile similarProfile = buildUserProfile(similarUserId);
            double userSimilarity = cfService.calculateSimilarity(profile, similarProfile);

            for (Map.Entry<String, Double> entry : similarProfile.ratings().entrySet()) {
                String bookId = entry.getKey();
                double rating = entry.getValue();

                if (!profile.readBookIds().contains(bookId) && rating >= 4.0) {
                    bookScores.merge(
                        bookId,
                        rating * userSimilarity,
                        Double::sum
                    );
                }
            }
        }

        return bookScores.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(count)
            .map(entry -> {
                Book book = getBook(entry.getKey());
                return new Recommendation(
                    book,
                    entry.getValue(),
                    "Readers like you loved this",
                    Recommendation.RecommendationType.COLLABORATIVE
                );
            })
            .collect(Collectors.toList());
    }

    // ============= CONTENT-BASED FILTERING =============

    private List<Recommendation> getContentBasedRecommendations(
        UserProfile profile,
        int count
    ) {
        // Get user's favorite books (4+ stars)
        List<Book> favoriteBooks = profile.ratings().entrySet().stream()
            .filter(e -> e.getValue() >= 4.0)
            .map(e -> getBook(e.getKey()))
            .collect(Collectors.toList());

        if (favoriteBooks.isEmpty()) {
            return Collections.emptyList();
        }

        // Calculate average embedding of favorite books
        double[] userPreferenceVector = calculateAverageEmbedding(favoriteBooks);

        // Find books with similar embeddings
        List<Book> allBooks = getAllBooks();  // In production: query vector DB

        return allBooks.stream()
            .filter(book -> !profile.readBookIds().contains(book.bookId()))
            .map(book -> {
                double similarity = cosineSimilarity(
                    userPreferenceVector,
                    book.embedding()
                );
                return new Recommendation(
                    book,
                    similarity,
                    "Similar to books you enjoyed",
                    Recommendation.RecommendationType.CONTENT_BASED
                );
            })
            .sorted((a, b) -> Double.compare(b.score(), a.score()))
            .limit(count)
            .collect(Collectors.toList());
    }

    // ============= TRENDING RECOMMENDATIONS =============

    private List<Recommendation> getTrendingRecommendations(
        UserProfile profile,
        int count
    ) {
        // Get trending books in user's favorite genres
        List<Book> trending = new ArrayList<>();

        for (String genre : profile.favoriteGenres()) {
            // Mock: would query analytics DB for trending books
            trending.addAll(getTrendingBooksInGenre(genre, count));
        }

        return trending.stream()
            .filter(book -> !profile.readBookIds().contains(book.bookId()))
            .map(book -> new Recommendation(
                book,
                book.averageRating() * Math.log(book.ratingsCount() + 1),
                "Trending in " + book.genres().get(0),
                Recommendation.RecommendationType.PERSONALIZED_TRENDING
            ))
            .sorted((a, b) -> Double.compare(b.score(), a.score()))
            .limit(count)
            .collect(Collectors.toList());
    }

    // ============= SERENDIPITY RECOMMENDATIONS =============

    private List<Recommendation> getSerendipityRecommendations(
        UserProfile profile,
        int count
    ) {
        // Find highly-rated books in genres user hasn't explored much
        Set<String> exploredGenres = profile.genreCounts().keySet();

        List<String> unexploredGenres = getAllGenres().stream()
            .filter(genre -> !exploredGenres.contains(genre))
            .collect(Collectors.toList());

        if (unexploredGenres.isEmpty()) {
            return Collections.emptyList();
        }

        // Pick random unexplored genre
        String newGenre = unexploredGenres.get(
            new Random().nextInt(unexploredGenres.size())
        );

        return getTopBooksInGenre(newGenre, count).stream()
            .map(book -> new Recommendation(
                book,
                book.averageRating(),
                "Discover " + newGenre,
                Recommendation.RecommendationType.SERENDIPITY
            ))
            .collect(Collectors.toList());
    }

    // ============= HELPER METHODS =============

    private UserProfile buildUserProfile(String userId) {
        // Mock: would query user database
        return new UserProfile(
            userId,
            List.of("book1", "book2", "book3"),
            Map.of("book1", 5.0, "book2", 4.0, "book3", 3.0),
            List.of("Fiction", "Mystery", "Thriller"),
            4.0,
            100,
            30.0,
            Map.of("Fiction", 50, "Mystery", 30, "Thriller", 20)
        );
    }

    private double[] calculateAverageEmbedding(List<Book> books) {
        int dimensions = books.get(0).embedding().length;
        double[] average = new double[dimensions];

        for (Book book : books) {
            for (int i = 0; i < dimensions; i++) {
                average[i] += book.embedding()[i];
            }
        }

        for (int i = 0; i < dimensions; i++) {
            average[i] /= books.size();
        }

        return average;
    }

    private double cosineSimilarity(double[] a, double[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // Mock methods - would be real database queries
    private Book getBook(String bookId) { return null; }
    private List<Book> getAllBooks() { return new ArrayList<>(); }
    private List<Book> getTrendingBooksInGenre(String genre, int count) { return new ArrayList<>(); }
    private List<Book> getTopBooksInGenre(String genre, int count) { return new ArrayList<>(); }
    private List<String> getAllGenres() { return new ArrayList<>(); }
}

@Service
class CollaborativeFilteringService {

    public List<String> findSimilarUsers(String userId, int count) {
        // Use cosine similarity on user rating vectors
        // Or use pre-computed similarity matrix
        return new ArrayList<>();
    }

    public double calculateSimilarity(
        RecommendationEngine.UserProfile user1,
        RecommendationEngine.UserProfile user2
    ) {
        // Pearson correlation or cosine similarity on ratings
        Set<String> commonBooks = new HashSet<>(user1.readBookIds());
        commonBooks.retainAll(user2.readBookIds());

        if (commonBooks.isEmpty()) {
            return 0.0;
        }

        double sum1 = 0, sum2 = 0, sumProducts = 0;
        for (String bookId : commonBooks) {
            double rating1 = user1.ratings().get(bookId);
            double rating2 = user2.ratings().get(bookId);
            sum1 += rating1;
            sum2 += rating2;
            sumProducts += rating1 * rating2;
        }

        // Simplified similarity score
        return sumProducts / Math.sqrt(sum1 * sum2);
    }
}
```

### 3.2 Embedding Service (Semantic Search)

Convert text to embeddings for semantic search:

```java
package com.bibby.ml;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
public class EmbeddingService {

    private final RestTemplate restTemplate;
    private static final String OPENAI_API_URL =
        "https://api.openai.com/v1/embeddings";
    private static final String MODEL = "text-embedding-3-small";

    public EmbeddingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ============= GENERATE EMBEDDINGS =============

    public double[] generateEmbedding(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(System.getenv("OPENAI_API_KEY"));

        Map<String, Object> request = Map.of(
            "model", MODEL,
            "input", text
        );

        HttpEntity<Map<String, Object>> entity =
            new HttpEntity<>(request, headers);

        ResponseEntity<EmbeddingResponse> response = restTemplate.exchange(
            OPENAI_API_URL,
            HttpMethod.POST,
            entity,
            EmbeddingResponse.class
        );

        return response.getBody().data().get(0).embedding();
    }

    public Map<String, double[]> generateBatchEmbeddings(List<String> texts) {
        Map<String, double[]> embeddings = new HashMap<>();

        // Batch process (OpenAI supports up to 2048 inputs per request)
        int batchSize = 100;
        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(
                i,
                Math.min(i + batchSize, texts.size())
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(System.getenv("OPENAI_API_KEY"));

            Map<String, Object> request = Map.of(
                "model", MODEL,
                "input", batch
            );

            HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(request, headers);

            ResponseEntity<EmbeddingResponse> response = restTemplate.exchange(
                OPENAI_API_URL,
                HttpMethod.POST,
                entity,
                EmbeddingResponse.class
            );

            List<EmbeddingData> data = response.getBody().data();
            for (int j = 0; j < batch.size(); j++) {
                embeddings.put(batch.get(j), data.get(j).embedding());
            }
        }

        return embeddings;
    }

    record EmbeddingResponse(
        List<EmbeddingData> data,
        String model,
        Usage usage
    ) {}

    record EmbeddingData(
        double[] embedding,
        int index
    ) {}

    record Usage(
        int prompt_tokens,
        int total_tokens
    ) {}
}

@Service
class SemanticSearchService {

    private final EmbeddingService embeddingService;
    private final VectorDatabase vectorDb;

    public SemanticSearchService(
        EmbeddingService embeddingService,
        VectorDatabase vectorDb
    ) {
        this.embeddingService = embeddingService;
        this.vectorDb = vectorDb;
    }

    public record SearchResult(
        String bookId,
        String title,
        String author,
        String snippet,
        double relevanceScore
    ) {}

    public List<SearchResult> search(String query, int limit) {
        // 1. Convert query to embedding
        double[] queryEmbedding = embeddingService.generateEmbedding(query);

        // 2. Find similar book embeddings
        List<VectorMatch> matches = vectorDb.findSimilar(
            queryEmbedding,
            limit
        );

        // 3. Convert to search results
        return matches.stream()
            .map(match -> new SearchResult(
                match.id(),
                match.metadata().get("title"),
                match.metadata().get("author"),
                match.metadata().get("description"),
                match.score()
            ))
            .collect(java.util.stream.Collectors.toList());
    }

    public void indexBook(
        String bookId,
        String title,
        String author,
        String description
    ) {
        // Create searchable text
        String text = String.format(
            "Title: %s\nAuthor: %s\nDescription: %s",
            title, author, description
        );

        // Generate embedding
        double[] embedding = embeddingService.generateEmbedding(text);

        // Store in vector database
        vectorDb.upsert(
            bookId,
            embedding,
            Map.of(
                "title", title,
                "author", author,
                "description", description
            )
        );
    }
}

// Interface for vector database (Pinecone, Weaviate, pgvector, etc.)
interface VectorDatabase {
    void upsert(String id, double[] vector, Map<String, String> metadata);
    List<VectorMatch> findSimilar(double[] queryVector, int limit);

    record VectorMatch(
        String id,
        double score,
        Map<String, String> metadata
    ) {}
}
```

### 3.3 LLM Integration (GPT-4, Claude)

Integrate modern LLMs for advanced features:

```java
package com.bibby.ml;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
public class LLMService {

    private final RestTemplate restTemplate;
    private static final String ANTHROPIC_API_URL =
        "https://api.anthropic.com/v1/messages";

    public LLMService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ============= BOOK SUMMARIES =============

    public String generateBookSummary(
        String title,
        String author,
        String fullDescription
    ) {
        String prompt = String.format("""
            Generate a concise, engaging summary of this book in 2-3 sentences:

            Title: %s
            Author: %s
            Description: %s

            Focus on the main themes, plot (if fiction), and what makes it unique.
            """, title, author, fullDescription);

        return callClaude(prompt);
    }

    // ============= PERSONALIZED READING LIST DESCRIPTIONS =============

    public String generateReadingListDescription(
        String listName,
        List<String> bookTitles
    ) {
        String prompt = String.format("""
            Create a compelling 2-sentence description for this reading list:

            List Name: %s
            Books: %s

            Make it engaging and highlight the common theme.
            """, listName, String.join(", ", bookTitles));

        return callClaude(prompt);
    }

    // ============= BOOK RECOMMENDATIONS WITH EXPLANATIONS =============

    public String explainRecommendation(
        String userName,
        List<String> likedBooks,
        String recommendedBook
    ) {
        String prompt = String.format("""
            Explain why %s might enjoy "%s" based on their reading history:

            Books they loved:
            %s

            Write a personalized 2-sentence explanation.
            """, userName,
            recommendedBook,
            String.join("\n", likedBooks));

        return callClaude(prompt);
    }

    // ============= BOOK COMPARISONS =============

    public String compareBooks(String book1, String book2) {
        String prompt = String.format("""
            Compare these two books in 3-4 sentences:

            1. %s
            2. %s

            Highlight similarities, differences, and who might prefer each.
            """, book1, book2);

        return callClaude(prompt);
    }

    // ============= READING LEVEL ANALYSIS =============

    public ReadingLevelAnalysis analyzeReadingLevel(String bookText) {
        String prompt = String.format("""
            Analyze the reading level of this book excerpt:

            "%s"

            Provide:
            1. Estimated grade level (e.g., "8th grade", "College")
            2. Complexity score (1-10)
            3. Key characteristics (vocabulary, sentence structure, themes)

            Format as JSON.
            """, bookText.substring(0, Math.min(bookText.length(), 1000)));

        String response = callClaude(prompt);

        // Parse JSON response (simplified)
        return new ReadingLevelAnalysis(
            "10th grade",
            7.5,
            "Moderate vocabulary complexity, varied sentence structure"
        );
    }

    public record ReadingLevelAnalysis(
        String gradeLevel,
        double complexityScore,
        String characteristics
    ) {}

    // ============= THEMATIC ANALYSIS =============

    public List<String> extractThemes(String bookDescription) {
        String prompt = String.format("""
            Extract 3-5 main themes from this book description:

            "%s"

            Return only the theme keywords, comma-separated.
            """, bookDescription);

        String response = callClaude(prompt);
        return Arrays.asList(response.split(",\\s*"));
    }

    // ============= DISCUSSION QUESTIONS =============

    public List<String> generateDiscussionQuestions(
        String title,
        String author,
        String summary
    ) {
        String prompt = String.format("""
            Generate 5 thought-provoking discussion questions for a book club reading:

            Title: %s
            Author: %s
            Summary: %s

            Make them open-ended and engaging.
            Number each question.
            """, title, author, summary);

        String response = callClaude(prompt);

        // Parse numbered list
        return Arrays.stream(response.split("\n"))
            .filter(line -> line.matches("^\\d+\\..*"))
            .map(line -> line.replaceFirst("^\\d+\\.\\s*", ""))
            .toList();
    }

    // ============= CLAUDE API CALL =============

    private String callClaude(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", System.getenv("ANTHROPIC_API_KEY"));
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> request = Map.of(
            "model", "claude-3-5-sonnet-20241022",
            "max_tokens", 1024,
            "messages", List.of(
                Map.of(
                    "role", "user",
                    "content", prompt
                )
            )
        );

        HttpEntity<Map<String, Object>> entity =
            new HttpEntity<>(request, headers);

        try {
            ResponseEntity<ClaudeResponse> response = restTemplate.exchange(
                ANTHROPIC_API_URL,
                HttpMethod.POST,
                entity,
                ClaudeResponse.class
            );

            return response.getBody()
                .content()
                .get(0)
                .text();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Claude API", e);
        }
    }

    record ClaudeResponse(
        String id,
        String type,
        String role,
        List<ContentBlock> content,
        String model,
        String stop_reason,
        Usage usage
    ) {}

    record ContentBlock(
        String type,
        String text
    ) {}

    record Usage(
        int input_tokens,
        int output_tokens
    ) {}
}
```

### 3.4 Image Recognition (Book Cover OCR)

Use computer vision to extract book information from photos:

```java
package com.bibby.ml;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class BookCoverRecognitionService {

    private final LLMService llmService;

    public BookCoverRecognitionService(LLMService llmService) {
        this.llmService = llmService;
    }

    public record BookRecognitionResult(
        String title,
        String author,
        double confidence,
        String isbn,
        List<BookMatch> potentialMatches
    ) {}

    public record BookMatch(
        String bookId,
        String title,
        String author,
        String isbn,
        double matchScore
    ) {}

    // ============= RECOGNIZE BOOK FROM IMAGE =============

    public BookRecognitionResult recognizeBookCover(MultipartFile image)
        throws IOException {

        // 1. Use Claude Vision to extract text from image
        String extractedText = extractTextFromCover(image);

        // 2. Parse title and author
        BookInfo info = parseBookInfo(extractedText);

        // 3. Search database for matches
        List<BookMatch> matches = searchForBook(info.title(), info.author());

        if (matches.isEmpty()) {
            return new BookRecognitionResult(
                info.title(),
                info.author(),
                0.0,
                null,
                Collections.emptyList()
            );
        }

        BookMatch bestMatch = matches.get(0);
        return new BookRecognitionResult(
            bestMatch.title(),
            bestMatch.author(),
            bestMatch.matchScore(),
            bestMatch.isbn(),
            matches
        );
    }

    private String extractTextFromCover(MultipartFile image) throws IOException {
        // Use Claude Vision API
        byte[] imageBytes = image.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Call Claude with vision
        String prompt = """
            Extract the book title and author from this book cover image.

            Return in this format:
            Title: [book title]
            Author: [author name]

            If you can't find either, write "Unknown".
            """;

        // This would use Claude's vision API
        // For now, mock response
        return "Title: The Great Gatsby\nAuthor: F. Scott Fitzgerald";
    }

    private record BookInfo(String title, String author) {}

    private BookInfo parseBookInfo(String extractedText) {
        Map<String, String> fields = new HashMap<>();

        for (String line : extractedText.split("\n")) {
            if (line.startsWith("Title:")) {
                fields.put("title", line.substring(6).trim());
            } else if (line.startsWith("Author:")) {
                fields.put("author", line.substring(7).trim());
            }
        }

        return new BookInfo(
            fields.getOrDefault("title", "Unknown"),
            fields.getOrDefault("author", "Unknown")
        );
    }

    private List<BookMatch> searchForBook(String title, String author) {
        // Search database with fuzzy matching
        // Return top matches sorted by relevance

        // Mock implementation
        return List.of(
            new BookMatch(
                "book123",
                "The Great Gatsby",
                "F. Scott Fitzgerald",
                "9780743273565",
                0.98
            )
        );
    }

    // ============= BATCH PROCESSING =============

    public List<BookRecognitionResult> recognizeBookshelf(
        List<MultipartFile> images
    ) throws IOException {
        List<BookRecognitionResult> results = new ArrayList<>();

        for (MultipartFile image : images) {
            try {
                results.add(recognizeBookCover(image));
            } catch (Exception e) {
                // Log error, continue with next image
                System.err.println("Failed to process image: " + e.getMessage());
            }
        }

        return results;
    }
}
```

---

## Part 4: MLOps - Operating ML in Production

### 4.1 Model Deployment

**Deployment Options:**

**1. Real-Time Serving:**
- REST API endpoint
- Low latency (<100ms)
- Use for: recommendations on page load, search

**2. Batch Processing:**
- Precompute predictions
- Store in database/cache
- Use for: daily recommendation emails, trends

**3. Edge Deployment:**
- Model runs on device (mobile app)
- Ultra-low latency
- Works offline
- Use for: on-device book cover recognition

### 4.2 Model Monitoring

Track model performance in production:

```java
package com.bibby.ml;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ModelMonitoringService {

    public record Prediction(
        String predictionId,
        String modelVersion,
        String userId,
        Map<String, Object> features,
        double predictedScore,
        String predictedClass,
        LocalDateTime timestamp,
        boolean wasShown,      // Was recommendation shown to user?
        boolean wasClicked,    // Did user click?
        boolean wasConverted   // Did user add book to library?
    ) {}

    public record ModelMetrics(
        String modelVersion,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        int totalPredictions,
        double averageScore,
        double clickThroughRate,
        double conversionRate,
        double precision,
        double recall,
        double ndcg,  // Normalized Discounted Cumulative Gain
        Map<String, Double> featureImportance
    ) {}

    // ============= TRACK PREDICTIONS =============

    public void logPrediction(Prediction prediction) {
        // Store in time-series database (InfluxDB, TimescaleDB)
        // Index by modelVersion, userId, timestamp
    }

    // ============= CALCULATE METRICS =============

    public ModelMetrics calculateMetrics(
        String modelVersion,
        LocalDateTime start,
        LocalDateTime end
    ) {
        // Query predictions from database
        List<Prediction> predictions = getPredictions(modelVersion, start, end);

        int total = predictions.size();
        double avgScore = predictions.stream()
            .mapToDouble(Prediction::predictedScore)
            .average()
            .orElse(0.0);

        long clicked = predictions.stream()
            .filter(Prediction::wasClicked)
            .count();

        long converted = predictions.stream()
            .filter(Prediction::wasConverted)
            .count();

        double ctr = total > 0 ? (clicked / (double) total) * 100 : 0;
        double cvr = total > 0 ? (converted / (double) total) * 100 : 0;

        return new ModelMetrics(
            modelVersion,
            start,
            end,
            total,
            avgScore,
            ctr,
            cvr,
            0.0,  // Would calculate from labeled data
            0.0,
            0.0,
            Map.of()
        );
    }

    // ============= DETECT MODEL DRIFT =============

    public record DriftDetection(
        boolean isDrifting,
        double driftScore,
        String affectedFeature,
        String recommendation
    ) {}

    public DriftDetection detectDrift(
        String modelVersion,
        int daysToCompare
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(daysToCompare);

        // Compare feature distributions
        Map<String, FeatureStats> currentStats =
            getFeatureStats(modelVersion, weekAgo, now);

        Map<String, FeatureStats> baselineStats =
            getFeatureStats(modelVersion, now.minusDays(daysToCompare * 2), weekAgo);

        // Calculate KL divergence or PSI (Population Stability Index)
        double maxDrift = 0.0;
        String driftingFeature = null;

        for (String feature : currentStats.keySet()) {
            double drift = calculatePSI(
                baselineStats.get(feature),
                currentStats.get(feature)
            );

            if (drift > maxDrift) {
                maxDrift = drift;
                driftingFeature = feature;
            }
        }

        boolean isDrifting = maxDrift > 0.2;  // threshold

        String recommendation = isDrifting ?
            "Retrain model - significant drift detected in " + driftingFeature :
            "Model performing normally";

        return new DriftDetection(
            isDrifting,
            maxDrift,
            driftingFeature,
            recommendation
        );
    }

    private record FeatureStats(
        double mean,
        double stdDev,
        Map<String, Double> distribution
    ) {}

    private double calculatePSI(FeatureStats baseline, FeatureStats current) {
        // Population Stability Index
        double psi = 0.0;

        for (String bucket : baseline.distribution().keySet()) {
            double baselinePercent = baseline.distribution().get(bucket);
            double currentPercent = current.distribution().getOrDefault(bucket, 0.001);

            psi += (currentPercent - baselinePercent) *
                   Math.log(currentPercent / baselinePercent);
        }

        return psi;
    }

    // Mock methods
    private List<Prediction> getPredictions(
        String modelVersion,
        LocalDateTime start,
        LocalDateTime end
    ) {
        return new ArrayList<>();
    }

    private Map<String, FeatureStats> getFeatureStats(
        String modelVersion,
        LocalDateTime start,
        LocalDateTime end
    ) {
        return new HashMap<>();
    }
}
```

### 4.3 A/B Testing ML Models

Test new models against production baseline:

```java
package com.bibby.ml;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ModelExperimentService {

    public record Experiment(
        String experimentId,
        String name,
        String controlModelVersion,
        String treatmentModelVersion,
        int trafficPercent,  // % of users in treatment
        ExperimentStatus status,
        Map<String, Double> metrics
    ) {
        public enum ExperimentStatus {
            DRAFT, RUNNING, PAUSED, COMPLETED, WINNER_DECLARED
        }
    }

    // ============= RUN EXPERIMENT =============

    public String selectModel(String userId, String experimentId) {
        Experiment experiment = getExperiment(experimentId);

        if (experiment.status() != Experiment.ExperimentStatus.RUNNING) {
            return experiment.controlModelVersion();
        }

        // Consistent hash assignment (same user always sees same variant)
        int hash = Math.abs(userId.hashCode() % 100);

        if (hash < experiment.trafficPercent()) {
            return experiment.treatmentModelVersion();  // Treatment
        } else {
            return experiment.controlModelVersion();    // Control
        }
    }

    // ============= ANALYZE RESULTS =============

    public record ExperimentResults(
        String experimentId,
        int controlUsers,
        int treatmentUsers,
        Map<String, MetricComparison> metrics,
        String recommendation
    ) {}

    public record MetricComparison(
        String metricName,
        double controlValue,
        double treatmentValue,
        double lift,  // % improvement
        double pValue,
        boolean isSignificant
    ) {}

    public ExperimentResults analyzeExperiment(String experimentId) {
        Experiment experiment = getExperiment(experimentId);

        // Get metrics for both groups
        Map<String, Double> controlMetrics =
            getMetrics(experiment.controlModelVersion());
        Map<String, Double> treatmentMetrics =
            getMetrics(experiment.treatmentModelVersion());

        Map<String, MetricComparison> comparisons = new HashMap<>();

        for (String metric : controlMetrics.keySet()) {
            double controlValue = controlMetrics.get(metric);
            double treatmentValue = treatmentMetrics.get(metric);
            double lift = ((treatmentValue - controlValue) / controlValue) * 100;

            // Statistical significance test (simplified)
            double pValue = calculatePValue(controlValue, treatmentValue);
            boolean isSignificant = pValue < 0.05;

            comparisons.put(metric, new MetricComparison(
                metric,
                controlValue,
                treatmentValue,
                lift,
                pValue,
                isSignificant
            ));
        }

        // Decision logic
        MetricComparison ctrComparison = comparisons.get("clickThroughRate");
        String recommendation;

        if (ctrComparison.isSignificant() && ctrComparison.lift() > 5) {
            recommendation = "Ship treatment - significant improvement";
        } else if (ctrComparison.isSignificant() && ctrComparison.lift() < -5) {
            recommendation = "Keep control - treatment performed worse";
        } else {
            recommendation = "Inconclusive - continue experiment";
        }

        return new ExperimentResults(
            experimentId,
            1000,  // mock user counts
            1000,
            comparisons,
            recommendation
        );
    }

    private double calculatePValue(double control, double treatment) {
        // Simplified - would use proper statistical test
        return Math.random() < 0.7 ? 0.01 : 0.10;
    }

    private Experiment getExperiment(String experimentId) {
        return new Experiment(
            experimentId,
            "Recommendation Algorithm v2",
            "v1.0",
            "v2.0",
            50,
            Experiment.ExperimentStatus.RUNNING,
            Map.of()
        );
    }

    private Map<String, Double> getMetrics(String modelVersion) {
        return Map.of(
            "clickThroughRate", 12.5,
            "conversionRate", 3.2
        );
    }
}
```

---

## Part 5: AI Product Strategy

### 5.1 Identifying AI Opportunities

**Framework: RICE for AI Features**

**Reach:** How many users benefit?
**Impact:** How much better is the experience?
**Confidence:** How confident are we in model performance?
**Effort:** Cost to build, deploy, and maintain

**Example: Bibby AI Features**

| Feature | Reach | Impact | Confidence | Effort | Score |
|---------|-------|--------|------------|--------|-------|
| Book recommendations | 100% | High | High | Medium | 8.0 |
| Semantic search | 80% | High | High | Low | 9.0 |
| Reading insights | 60% | Medium | Medium | Low | 6.0 |
| Discussion questions | 20% | Medium | High | Low | 4.0 |
| Book cover OCR | 40% | High | Medium | High | 4.5 |

**Prioritization: Build semantic search first, then recommendations.**

### 5.2 Build vs Buy

**When to build:**
- ✅ Core differentiation (recommendations for your domain)
- ✅ Unique data advantage
- ✅ Need full control over model
- ✅ Cost at scale favors building

**When to buy/use APIs:**
- ✅ Commodity features (OCR, translation)
- ✅ Don't have ML expertise
- ✅ Need to ship fast
- ✅ Small scale (APIs are cheaper)

**Example: Bibby**
- **Build:** Book recommendations (unique data, core feature)
- **Buy:** Embeddings (OpenAI), OCR (Google Vision), LLM (Claude)

### 5.3 AI Ethics & Responsible AI

**Key Principles:**

**1. Transparency:**
- Tell users when AI is making decisions
- Explain recommendations ("Because you liked X")
- Allow users to provide feedback

**2. Fairness:**
- Avoid bias in recommendations
- Don't create filter bubbles
- Test across user segments

**3. Privacy:**
- Don't train on sensitive user data
- Allow users to opt out
- Be transparent about data usage

**4. Control:**
- Users can edit/delete their data
- Users can control AI features
- Don't force AI on users

**5. Safety:**
- Content moderation
- Detect and block harmful content
- Monitor for misuse

### 5.4 AI Cost Management

AI can get expensive. Optimize costs:

**1. Model Selection:**
- Use smaller models when possible
- GPT-3.5 vs GPT-4 (10x cheaper)
- Open source models (Llama, Mistral)

**2. Caching:**
- Cache embeddings (don't regenerate)
- Cache LLM responses for common queries
- Precompute batch predictions

**3. Rate Limiting:**
- Limit AI features per user
- Throttle expensive operations
- Queue batch jobs

**4. Monitoring:**
- Track token usage
- Alert on unusual spikes
- Optimize prompts (fewer tokens)

**Example Budget for Bibby (10K users):**
```
Embeddings (OpenAI):
- 100K books × $0.02/1M tokens = $2
- One-time cost

Recommendations:
- 10K users × 10 recs/day × 30 days = 3M predictions
- Precomputed in batch: $0
- Real-time serving: $100/month (infrastructure)

LLM Features (Claude):
- 1K summaries/month × $0.01 = $10
- 5K discussion questions × $0.02 = $100

Total: ~$200/month for AI features
```

---

## Part 6: Assignments

### Assignment 1: Recommendation Strategy

Design a recommendation strategy for Bibby that balances:
- Accuracy (show books users will love)
- Discovery (introduce new genres/authors)
- Diversity (don't only show similar books)
- Engagement (drive reads and ratings)

**Include:**
- Mix of collaborative filtering, content-based, and trending
- Specific weights for each approach
- How you'd measure success
- Cold start strategy (new users with no history)

### Assignment 2: Build Reading Insights

Extend the RecommendationEngine to generate weekly reading insights:

```java
public record ReadingInsights(
    String userId,
    LocalDate weekStart,
    int booksRead,
    int pagesRead,
    double averageRating,
    List<String> topGenres,
    String readingPaceComparison,  // "faster than 80% of readers"
    List<String> achievements,      // "Read 3 books this week!"
    String prediction,              // "At this pace, you'll read 156 books this year"
    List<Recommendation> suggestedNext
);

public ReadingInsights generateWeeklyInsights(String userId);
```

**Requirements:**
- Calculate reading pace (pages/day, books/week)
- Compare to historical average and other users
- Generate personalized achievements
- Predict annual reading based on current pace
- Suggest next books based on recent reads

### Assignment 3: Semantic Search

Implement semantic search using embeddings:

**Requirements:**
- Index all books with embeddings
- Support natural language queries
- Return ranked results
- Handle typos and synonyms
- Add filters (genre, publication year, rating)

**Bonus:**
- Hybrid search (combine semantic + keyword)
- Re-ranking based on popularity
- Query expansion (add related terms)

### Assignment 4: Model Monitoring Dashboard

Design a monitoring dashboard for the recommendation model:

**Include:**
- Real-time metrics (CTR, conversion rate)
- Model performance over time
- Drift detection alerts
- Feature importance
- A/B test results
- Cost tracking

**Implement key metrics:**
```java
public record ModelHealth(
    String status,  // "healthy", "degraded", "critical"
    Map<String, Double> currentMetrics,
    List<Alert> alerts,
    DriftDetection driftStatus,
    Map<String, Double> costByFeature
);
```

### Assignment 5: AI Ethics Framework

Create an AI ethics framework for Bibby:

**Address:**
1. **Transparency:** How will you explain recommendations?
2. **Bias:** How will you detect and mitigate bias?
3. **Privacy:** What user data will you use for ML?
4. **Filter Bubbles:** How will you prevent echo chambers?
5. **Opt-Out:** Can users disable AI features?

**Deliverable:**
- Written policy (1-2 pages)
- UI mockups for transparency features
- Metrics to track fairness

---

## Part 7: Reflection Questions

1. **When to Use AI:**
   - Which Bibby features actually need ML vs simple rules?
   - What's the risk of over-using AI?
   - How do you know if AI is working?

2. **Model Performance:**
   - What metrics matter most for recommendations?
   - How do you balance precision vs diversity?
   - When should you retrain your model?

3. **Cost vs Value:**
   - How much would you spend on AI features?
   - Build vs buy decision framework?
   - How do you optimize costs without hurting UX?

4. **Ethics:**
   - How do you prevent filter bubbles in recommendations?
   - Should users know they're interacting with AI?
   - What are the risks of personalization?

5. **Future of AI:**
   - How will LLMs change product development?
   - What AI features will be table stakes in 3 years?
   - How do you stay competitive as AI democratizes?

---

## Part 8: Additional Resources

### Books
- **"Designing Data-Intensive Applications"** by Martin Kleppmann
- **"Machine Learning Design Patterns"** by Valliappa Lakshmanan
- **"Building Machine Learning Powered Applications"** by Emmanuel Ameisen
- **"Designing Machine Learning Systems"** by Chip Huyen
- **"AI Product Management"** by Irene Bratsis

### Courses
- **Andrew Ng's ML Course** (Coursera)
- **Fast.ai Practical Deep Learning**
- **Stanford CS229: Machine Learning**
- **DeepLearning.AI MLOps Specialization**

### Tools & Platforms
- **Embeddings:** OpenAI, Cohere, Sentence Transformers
- **Vector DBs:** Pinecone, Weaviate, Qdrant, pgvector
- **LLMs:** OpenAI GPT-4, Anthropic Claude, Google Gemini
- **MLOps:** Weights & Biases, MLflow, Kubeflow
- **Monitoring:** Arize AI, Fiddler, WhyLabs

### Papers
- "Attention Is All You Need" (Transformers)
- "BERT: Pre-training of Deep Bidirectional Transformers"
- "GPT-3: Language Models are Few-Shot Learners"
- "Recommender Systems Handbook" (various papers)

---

## Summary

This week you learned:

1. **AI/ML Fundamentals:**
   - Types of ML (supervised, unsupervised, reinforcement)
   - When to use ML vs rules
   - ML product lifecycle

2. **Recommendation Systems:**
   - Collaborative filtering
   - Content-based filtering
   - Hybrid approaches
   - Embedding-based recommendations

3. **Modern AI Features:**
   - Semantic search with embeddings
   - LLM integration (summaries, explanations)
   - Computer vision (book cover recognition)
   - Personalization at scale

4. **MLOps:**
   - Model deployment strategies
   - Monitoring and drift detection
   - A/B testing ML models
   - Cost optimization

5. **AI Product Strategy:**
   - Identifying high-impact AI opportunities
   - Build vs buy decisions
   - AI ethics and responsible AI
   - Cost management

**Key Insight:** AI is a tool, not a goal. Use it to solve real user problems, not because it's trendy. The best AI features are invisible—they just make the product work better.

**Next Week Preview:** Week 52 is your Final Capstone Project—you'll integrate everything from all 52 weeks into a complete, executable business plan for Bibby, from technical architecture to go-to-market to financial projections.

---

**Week 51 Complete!** You now understand how to integrate AI/ML into your product strategically, build recommendation systems, and operationalize ML at scale. These skills are essential for modern product builders—AI is no longer optional.
