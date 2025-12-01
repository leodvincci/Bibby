const { BrowserMultiFormatReader } = ZXingBrowser;

const videoElement = document.getElementById("video");
const resultDiv = document.getElementById("result");
const statusDiv = document.getElementById("status"); // add a <div id="status">

const codeReader = new BrowserMultiFormatReader();

// 👉 change this to whatever your Bibby endpoint is
const API_URL = "/import/books";

// simple dedupe so we don't spam the API
let lastIsbn = "";
let lastSentAt = 0;
const DEDUPE_WINDOW_MS = 2000; // 2 seconds

async function sendToApi(isbn) {
    const payload = { isbn }; // later you can add title/authors if you want

    try {
        statusDiv.textContent = "Sending to Bibby…";

        const response = await fetch(API_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const bodyText = await response.text().catch(() => "");
            console.error("API error:", response.status, bodyText);
            statusDiv.textContent = "❌ Bibby rejected the book (" + response.status + ")";
            return;
        }

        statusDiv.textContent = "✅ Sent to Bibby: " + isbn;
        console.log("Sent to API:", isbn);
    } catch (err) {
        console.error("Network/API error:", err);
        statusDiv.textContent = "⚠️ Network error sending to Bibby";
    }
}

function handleScan(isbn) {
    const now = Date.now();

    // ignore rapid duplicates of the same code
    if (isbn === lastIsbn && now - lastSentAt < DEDUPE_WINDOW_MS) {
        return;
    }

    lastIsbn = isbn;
    lastSentAt = now;

    resultDiv.textContent = "Scanned: " + isbn;
    console.log("Decoded:", isbn);

    // fire off the API call
    sendToApi(isbn);
}

codeReader.decodeFromVideoDevice(
    null,
    videoElement,
    (result, err) => {
        if (result) {
            const text = result.getText();
            handleScan(text);
        }
        // you can ignore err here; ZXing calls this a lot with "no barcode" etc.
    }
);

