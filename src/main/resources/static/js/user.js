async function validate() {
  return (
    validateText("username", "Please enter a Username.")
  )
}

async function submitCreate() {
  const caller = "create-button";
  addLoading(caller);

  if (await validate()) {
    const form = document.getElementById("create-form");
    const formData = Object.fromEntries(new FormData(form));
    const body = {
      image: formData["image"].trim() || null,
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

async function submitDelete(userId) {
  const caller = "delete-button";
  addLoading(caller);

  const response = await submitRequest(`/api/users/${userId}`, "DELETE");
  if (response !== null)
    window.location = document.referrer;

  removeLoading(caller);
}
