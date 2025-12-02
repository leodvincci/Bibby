const { BrowserMultiFormatReader } = ZXingBrowser;

const videoElement = document.getElementById("video");
const resultDiv = document.getElementById("result");
const statusDiv = document.getElementById("status");
const bookDetails = document.getElementById("book-details");
const bookTitle = document.getElementById("book-title");
const bookAuthors = document.getElementById("book-authors");
const bookPublisher = document.getElementById("book-publisher");
const bookDescription = document.getElementById("book-description");
const emptyState = document.getElementById("empty-state");
const shelfSection = document.getElementById("shelf-section");
const shelfSelect = document.getElementById("shelf-select");
const shelfHelper = document.getElementById("shelf-helper");
const shelfEmpty = document.getElementById("shelf-empty");
const placeButton = document.getElementById("place-button");

const codeReader = new BrowserMultiFormatReader();

const IMPORT_URL = "/import/books";
const SHELF_OPTIONS_URL = "/api/v1/shelves/options";
const PLACE_URL = (bookId) => `/api/v1/books/${bookId}/shelf`;

let lastIsbn = "";
let lastSentAt = 0;
const DEDUPE_WINDOW_MS = 2000;
let currentBook = null;
let shelfOptions = [];

function formatAuthors(authors) {
    if (!authors || authors.length === 0) {
        return "Unknown author";
    }
    return authors.map((a) => `<span class="pill">${a}</span>`).join("");
}

function showBookDetails(book) {
    currentBook = book;
    emptyState.classList.add("hidden");
    bookDetails.classList.remove("hidden");
    bookTitle.textContent = `${book.title} (${book.isbn})`;
    bookAuthors.innerHTML = formatAuthors(book.authors);
    bookPublisher.textContent = book.publisher ? `Publisher: ${book.publisher}` : "";
    bookDescription.textContent = book.description || "";
}

async function fetchShelfOptions() {
    const response = await fetch(SHELF_OPTIONS_URL);
    if (!response.ok) {
        throw new Error("Unable to load shelf options");
    }
    shelfOptions = await response.json();
    renderShelves();
}

function renderShelves() {
    if (!shelfOptions || shelfOptions.length === 0) {
        shelfSection.classList.add("hidden");
        shelfEmpty.classList.remove("hidden");
        shelfEmpty.textContent = "No shelves available yet.";
        return;
    }

    shelfEmpty.classList.add("hidden");
    shelfSection.classList.remove("hidden");

    shelfSelect.innerHTML = shelfOptions.map((shelf) => {
        const usage = `${shelf.bookCount}/${shelf.bookCapacity}`;
        const label = `${shelf.bookcaseLabel} • ${shelf.shelfLabel} (${usage})`;
        const disabled = shelf.hasSpace ? "" : "disabled";
        return `<option value="${shelf.shelfId}" ${disabled}>${label}</option>`;
    }).join("");

    const first = shelfOptions.find((s) => s.hasSpace);
    if (first) {
        shelfSelect.value = first.shelfId;
        updateShelfHelper(first);
        placeButton.disabled = false;
    } else {
        shelfHelper.textContent = "All shelves are full — create space to continue.";
        placeButton.disabled = true;
    }
}

function updateShelfHelper(option) {
    const usage = `${option.bookCount}/${option.bookCapacity} filled`;
    const available = option.bookCapacity - option.bookCount;
    const availability = available > 0 ? `${available} open slot(s)` : "No space left";
    shelfHelper.textContent = `${option.bookcaseLabel} → ${option.shelfLabel} • ${usage} • ${availability}`;
}

async function importBook(isbn) {
    statusDiv.textContent = "Fetching book details and saving to your library…";
    const response = await fetch(IMPORT_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ isbn })
    });

    if (!response.ok) {
        const errorText = await response.text().catch(() => "");
        throw new Error(errorText || `Import failed (${response.status})`);
    }

    const data = await response.json();
    showBookDetails(data);
    statusDiv.textContent = `Saved “${data.title}”. Pick a shelf to finish.`;
    await fetchShelfOptions();
}

async function placeOnShelf() {
    if (!currentBook) {
        return;
    }
    const selectedId = Number(shelfSelect.value);
    const option = shelfOptions.find((s) => s.shelfId === selectedId);
    if (!option) {
        statusDiv.textContent = "Pick a valid shelf before placing the book.";
        return;
    }

    placeButton.disabled = true;
    statusDiv.textContent = "Placing book on shelf…";

    const response = await fetch(PLACE_URL(currentBook.bookId), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ shelfId: selectedId })
    });

    placeButton.disabled = false;

    if (!response.ok) {
        const errorText = await response.text().catch(() => "");
        statusDiv.textContent = errorText || `Shelf update failed (${response.status})`;
        return;
    }

    const data = await response.json();
    statusDiv.textContent = `Book placed on ${data.bookcaseLabel} / ${data.shelfLabel}.`;
    resultDiv.textContent = `Ready for next scan.`;
}

async function handleScan(isbn) {
    const now = Date.now();
    if (isbn === lastIsbn && now - lastSentAt < DEDUPE_WINDOW_MS) {
        return;
    }
    lastIsbn = isbn;
    lastSentAt = now;

    resultDiv.textContent = "Scanned: " + isbn;
    try {
        await importBook(isbn);
    } catch (err) {
        console.error(err);
        statusDiv.textContent = err.message || "Something went wrong while importing.";
    }
}

codeReader.decodeFromVideoDevice(
    null,
    videoElement,
    (result) => {
        if (result) {
            const text = result.getText();
            handleScan(text);
        }
    }
);

shelfSelect.addEventListener("change", (event) => {
    const option = shelfOptions.find((s) => s.shelfId === Number(event.target.value));
    if (option) {
        updateShelfHelper(option);
    }
});

placeButton.addEventListener("click", () => {
    placeOnShelf().catch((err) => {
        statusDiv.textContent = err.message || "Unable to place book on shelf.";
    });
});

fetchShelfOptions().catch(() => {
    // it's fine if this fails pre-scan; we'll retry after import
});
