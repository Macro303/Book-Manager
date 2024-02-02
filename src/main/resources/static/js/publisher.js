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
      imageUrl: formData["image-url"].trim() || null,
      summary: formData["summary"].trim() || null,
      title: formData["title"].trim(),
    };

    const response = await submitRequest("/api/publishers", "POST", body);
    if (response !== null) {
      form.reset();
      window.location = `/publishers/${response.body.id}`;
    }
  }

  removeLoading(caller);
}

async function submitUpdate(publisherId) {
  const caller = "update-button";
  addLoading(caller);

  if (await validate()) {
    const form = document.getElementById("update-form");
    const formData = Object.fromEntries(new FormData(form));
    const body = {
      imageUrl: formData["image-url"].trim() || null,
      summary: formData["summary"].trim() || null,
      title: formData["title"].trim(),
    };

    const response = await submitRequest(`/api/publishers/${publisherId}`, "PUT", body);
    if (response !== null) {
      form.reset();
      window.location = `/publishers/${publisherId}`;
    }
  }

  removeLoading(caller);
}

async function submitDelete(publisherId) {
  const caller = "delete-button";
  addLoading(caller);

  const response = await submitRequest(`/api/publishers/${publisherId}`, "DELETE");
  if (response !== null)
    window.location = "/publishers";

  removeLoading(caller);
}
