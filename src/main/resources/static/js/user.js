async function validate() {
  return (
    validateText("username", "Please enter a Username.")
  )
}

async function validateImport() {
  return (
    validateAny(["goodreads", "google-books", "isbn", "library-thing", "open-library"])
  )
}

async function validateCreate() {
  return (
    validateSelect("format", "Please select a Format.") &&
    validateSelect("publisher-id", "Please select a Publisher.") &&
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
      imageUrl: formData["image-url"].trim() || null,
      username: formData["username"].trim(),
    };

    const response = await submitRequest("/api/users", "POST", body);
    if (response !== null) {
      form.reset();
      window.location = `/users/${response.id}`;
    }
  }

  removeLoading(caller);
}

async function submitUpdate(userId) {
  const caller = "update-button";
  addLoading(caller);

  if (await validate()) {
    const form = document.getElementById("update-form");
    const formData = Object.fromEntries(new FormData(form));
    const body = {
      imageUrl: formData["image-url"].trim() || null,
      username: formData["username"].trim(),
    };

    const response = await submitRequest(`/api/users/${userId}`, "PUT", body);
    if (response !== null) {
      form.reset();
      window.location = `/users/${userId}`;
    }
  }

  removeLoading(caller);
}

async function submitDelete(userId) {
  const caller = "delete-button";
  addLoading(caller);

  const response = await submitRequest(`/api/users/${userId}`, "DELETE");
  if (response !== null)
    window.location = "/users";

  removeLoading(caller);
}

async function submitCreateBook(userId) {
  const caller = "create-book-button";
  addLoading(caller);

  if (await validateCreate()) {
    const form = document.getElementById("create-book-form");
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
      title: formData["title"].trim(),
    };

    const response = await submitRequest("/api/books", "POST", body);
    if (response !== null) {
      form.reset();
      window.location = document.referrer;
    }
  }

  removeLoading(caller);
}

async function submitImportBook(userId) {
  const caller = "import-book-button";
  addLoading(caller);

  if (await validateImport()) {
    const form = document.getElementById("import-book-form");
    const formData = Object.fromEntries(new FormData(form));
    let body = {
      goodreadsId: formData["goodreads"].trim() || null,
      googleBooksId: formData["google-books"].trim() || null,
      isbn: formData["isbn"].trim() || null,
      libraryThingId: formData["library-thing"].trim() || null,
      openLibraryId: formData["open-library"].trim() || null,
    };

    let response = await submitRequest("/api/books/import", "POST", body);
    if (response !== null) {
      body = {
        id: response.body.id
      };

      response = await submitRequest(`/api/users/${userId}/wished`, "POST", body);
      if (response !== null) {
        form.reset();
        window.location = document.referrer;
      }
    }
  } else {
    alert("Atleast 1 field must be filled.");
  }

  removeLoading(caller);
}
