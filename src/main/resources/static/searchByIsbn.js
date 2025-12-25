
document.getElementById("the-form").addEventListener(
    "submit", async (e) => {
        e.preventDefault(); // stops refresh/navigation

        const isbn = document.getElementById("isbn-input").value.trim();
        if (!isbn) return;
        // e.target.reset();
        await searchByIsbn(isbn).then(
            (book) => {
                const resultDiv = document.getElementById("result-content");
                resultDiv.innerHTML = ""; // Clear previous results
                resultDiv.innerText = book ? JSON.stringify(book, null, 2) : "Book not found.";
            });
    }
)

async function searchByIsbn(isbn) {
    try {
        const res = await fetch(`/api/v1/books/search/${encodeURIComponent(isbn)}`);

        if (res.status === 404) {
            console.log("Book not found");
            return null;
        }
        if (!res.ok) throw new Error(`HTTP ${res.status}`);

        const book = await res.json();
        console.log("Found:", book);

        return book;
    } catch (err) {
        console.error("Search failed:", err);
    }
}

