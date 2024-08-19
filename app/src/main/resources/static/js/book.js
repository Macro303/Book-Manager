async function validate() {
  return (
    validateSelect("format", "Please select a Format.") &&
    validateText("title", "Please enter a Title.")
  )
}

async function submitCreate() {
  const caller = "create-button";
  addLoading(caller);

  if (await validate()) {
    const form = document.getElementById("create-form");
    const formData = Object.fromEntries(new FormData(form));
    const body = {
      format: formData["format"] || "PAPERBACK",
      goodreadsId: formData["goodreads"].trim() || null,
      googleBooksId: formData["google-books"].trim() || null,
      imageUrl: formData["image-url"].trim() || null,
      isbn: formData["isbn"].trim() || null,
      isCollected: true,
      libraryThingId: formData["library-thing"].trim() || null,
      openLibraryId: formData["open-library"].trim() || null,
      publishDate: formData["publish-date"].trim() || null,
      publisherId: formData["publisher-id"] || null,
      subtitle: formData["subtitle"].trim() || null,
      summary: formData["summary"].trim() || null,
      title: formData["title"].trim(),
    };

    const response = await submitRequest("/api/books", "POST", body);
    if (response !== null) {
      form.reset();
      window.location = `/books/${response.body.id}`;
    }
  }

  removeLoading(caller);
}

async function submitUpdate(bookId) {
  const caller = "update-button";
  addLoading(caller);

  if (await validate()) {
    const form = document.getElementById("update-form");
    const formData = Object.fromEntries(new FormData(form));
    const body = {
      format: formData["format"] || "PAPERBACK",
      goodreadsId: formData["goodreads"].trim() || null,
      googleBooksId: formData["google-books"].trim() || null,
      imageUrl: formData["image-url"].trim() || null,
      isbn: formData["isbn"].trim() || null,
      libraryThingId: formData["library-thing"].trim() || null,
      openLibraryId: formData["open-library"].trim() || null,
      publishDate: formData["publish-date"].trim() || null,
      publisherId: formData["publisher-id"] || null,
      subtitle: formData["subtitle"].trim() || null,
      summary: formData["summary"].trim() || null,
      title: formData["title"].trim(),
    };

    const response = await submitRequest(`/api/books/${bookId}`, "PUT", body);
    if (response !== null) {
      form.reset();
      window.location = `/books/${bookId}`;
    }
  }

  removeLoading(caller);
}

async function submitDelete(bookId) {
  const caller = "delete-button";
  addLoading(caller);

  const response = await submitRequest(`/api/books/${bookId}`, "DELETE");
  if (response !== null)
    window.location = "/books";

  removeLoading(caller);
}

