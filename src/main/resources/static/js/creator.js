async function validate() {
  return (
    validateText("name", "Please enter a Name.")
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
      name: formData["name"].trim(),
    };

    const response = await submitRequest("/api/creators", "POST", body);
    if (response !== null) {
      form.reset();
      window.location = `/creators/${response.body.id}`;
    }
  }

  removeLoading(caller);
}

async function submitUpdate(creatorId) {
  const caller = "update-button";
  addLoading(caller);

  if (await validate()) {
    const form = document.getElementById("update-form");
    const formData = Object.fromEntries(new FormData(form));
    const body = {
      imageUrl: formData["image-url"].trim() || null,
      summary: formData["summary"].trim() || null,
      name: formData["name"].trim(),
    };

    const response = await submitRequest(`/api/creators/${creatorId}`, "PUT", body);
    if (response !== null) {
      form.reset();
      window.location = `/creators/${creatorId}`;
    }
  }

  removeLoading(caller);
}

async function submitDelete(creatorId) {
  const caller = "delete-button";
  addLoading(caller);

  const response = await submitRequest(`/api/creators/${creatorId}`, "DELETE");
  if (response !== null)
    window.location = "/creators";

  removeLoading(caller);
}
