(function() {
    let title = "";
    let content = "";
    let nextUrl = "";

    if (window.location.hostname.includes("ncode.syosetu.com")) {
        let titleElement = document.querySelector(".p-novel__title.p-novel__title--rensai, .p-novel__subtitle");
        let contentElement = document.querySelector(".p-novel__body, .js-novel-text.p-novel__text");
        let nextChapterElement = document.querySelector(".c-pager__item.c-pager__item--next");

        if (titleElement) title = titleElement.innerText.trim();
        if (contentElement) content = contentElement.innerText.trim();
        if (nextChapterElement) nextUrl = nextChapterElement.href;
    }
    else if (window.location.hostname.includes("booktoki468.com")) {
        let titleElement = document.querySelector(".toon-title");
        let contentElement = document.querySelector("#novel_content");
        let nextChapterElement = document.querySelector("#goNextBtn");

        if (titleElement) title = titleElement.innerText.trim();
        if (contentElement) content = contentElement.innerText.trim();
        if (nextChapterElement) nextUrl = nextChapterElement.href;
    }

    if (title && content) {
        let fullText = "Translate this novel chapter to English\\n\\n" + title + "\\n\\n" + content;
        return JSON.stringify({ text: fullText, nextUrl: nextUrl });
    } else {
        return JSON.stringify({ error: "Error: Title or Content not found", nextUrl: "" });
    }
})();
