async function validate() {
  return (
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
      summary: formData["summary"].trim() || null,
      title: formData["title"].trim(),
    };

    const response = await submitRequest("/api/genres", "POST", body);
    if (response !== null) {
      form.reset();
      window.location = `/genres/${response.body.id}`;
    }
  }

  removeLoading(caller);
}

async function submitUpdate(genreId) {
  const caller = "update-button";
  addLoading(caller);

  if (await validate()) {
    const form = document.getElementById("update-form");
    const formData = Object.fromEntries(new FormData(form));
    const body = {
      summary: formData["summary"].trim() || null,
      title: formData["title"].trim(),
    };

    const response = await submitRequest(`/api/genres/${genreId}`, "PUT", body);
    if (response !== null) {
      form.reset();
      window.location = `/genres/${genreId}`;
    }
  }

  removeLoading(caller);
}

async function submitDelete(genreId) {
  const caller = "delete-button";
  addLoading(caller);

  const response = await submitRequest(`/api/genres/${genreId}`, "DELETE");
  if (response !== null)
    window.location = "/genres";

  removeLoading(caller);
}
