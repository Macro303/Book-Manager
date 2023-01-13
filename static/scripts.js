document.addEventListener('DOMContentLoaded', () => {
  function openModal($el) {
    $el.classList.add('is-active');
  }
  function closeModal($el) {
    $el.classList.remove('is-active');
  }
  function closeAllModals() {
    (document.querySelectorAll('.modal') || []).forEach(($modal) => {
      closeModal($modal);
    });
  }
  (document.querySelectorAll('.modal-trigger') || []).forEach(($trigger) => {
    const modal = $trigger.dataset.target;
    const $target = document.getElementById(modal);
    $trigger.addEventListener('click', () => {
      openModal($target);
    });
  });
  (document.querySelectorAll('.modal-background, .modal-close, .modal-card-head .delete, .modal-card-foot .button') || []).forEach(($close) => {
    const $target = $close.closest('.modal');
    $close.addEventListener('click', () => {
      closeModal($target);
    });
  });
  document.addEventListener('keydown', (event) => {
    const e = event || window.event;
    if (e.keyCode === 27) { // Escape key
      closeAllModals();
    }
  });
});

const headers = {
  "Accept": "application/json; charset=UTF-8",
  "Content-Type": "application/json; charset=UTF-8",
};

function addLoading(caller){
  let element = document.getElementById(caller);
  element.classList.add("is-loading");
}

function removeLoading(caller){
  let element = document.getElementById(caller);
  element.classList.remove("is-loading");
}

let addForm = document.getElementById("add-form");
addForm.addEventListener('submit', e => {
  addLoading("add-form-button");
  let details = Object.fromEntries(new FormData(addForm));
  console.log(details);

  fetch("/api/v0/books", {
    method: "POST",
    headers: {
      "Accept": "application/json; charset=UTF-8",
      "Content-Type": "application/json; charset=UTF-8",
    },
    body: JSON.stringify({
      "isbn": details["isbn"],
      "open_library_id": "",
      "wisher_id": details["wisher-id"]
    }),
  })
  .then((response) => {
    if(response.ok){
      window.location = `/${details["wisher-id"]}/wishlist`;
    }
    return Promise.reject(response);
  })
  .catch((response) => response.json().then((msg) => {
    alert(`${response.status} ${response.statusText}: ${msg.details}`)
  }))
  .finally(() => removeLoading("add-form-button"));

  e.preventDefault();
});

function createUser(caller){
  addLoading(caller);
  let username = document.getElementById("username-entry").value;
  fetch("/api/v0/users", {
    method: "POST",
    headers,
    body: JSON.stringify({
      "username": username
    }),
  })
  .then((response) => {
    if (response.ok) {
      return response.json();
    }
    return Promise.reject(response);
  })
  .then((data) => window.location = `/book-catalogue/${data.user_id}`)
  .catch((response) => response.json().then((msg) => {
    alert(`${response.status} ${response.statusText}: ${msg.details}`);
  }))
  .finally(() => removeLoading(caller));
}

function loginUser(caller){
  addLoading(caller);
  let username = document.getElementById("username-entry").value;
  fetch(`/api/v0/users/${username}`, {
    method: "GET",
    headers,
  })
  .then((response) => {
    if (response.ok) {
      return response.json();
    }
    return Promise.reject(response);
  })
  .then((data) => window.location = `/book-catalogue/${data.user_id}`)
  .catch((response) => response.json().then((msg) => {
    alert(`${response.status} ${response.statusText}: ${msg.details}`);
  }))
  .finally(() => removeLoading(caller));
}

function refreshAllBooks(caller){
  addLoading(caller);
  fetch("/api/v0/books/refresh", {
    method: "PUT",
    headers: {
      "Accept": "application/json; charset=UTF-8",
      "Content-Type": "application/json; charset=UTF-8",
    },
  })
  .then((response) => {
    if (response.ok) {
      window.location.reload();
    }
    return Promise.reject(response);
  })
  .catch((response) => response.json().then((msg) => {
    alert(`${response.status} ${response.statusText}: ${msg.details}`);
  }))
  .finally(() => removeLoading(caller));
}

function refreshBook(caller, bookId){
  addLoading(caller);
  fetch(`/api/v0/books/${bookId}/refresh`, {
    method: "PUT",
    headers: {
      "Accept": "application/json; charset=UTF-8",
      "Content-Type": "application/json; charset=UTF-8",
    },
  })
  .then((response) => {
    if (response.ok) {
      window.location.reload();
    }
    return Promise.reject(response);
  })
  .catch((response) => response.json().then((msg) => {
    alert(`${response.status} ${response.statusText}: ${msg.details}`);
  }))
  .finally(() => removeLoading(caller));
}

function updateBookReaders(caller, bookId, userId){
  addLoading(caller);
  fetch(`/api/v0/books/${bookId}/readers`, {
    method: "POST",
    headers: {
      "Accept": "application/json; charset=UTF-8",
      "Content-Type": "application/json; charset=UTF-8",
    },
    body: JSON.stringify({
      "user_id": userId
    }),
  })
  .then((response) => {
    if (response.ok) {
      window.location.reload();
    }
    return Promise.reject(response);
  })
  .catch((response) => response.json().then((msg) => {
    alert(`${response.status} ${response.statusText}: ${msg.details}`);
  }))
  .finally(() => removeLoading(caller));
}

function collectBook(caller, bookId, userId){
  addLoading(caller);
  fetch(`/api/v0/books/${bookId}`, {
    method: "PUT",
    headers: {
      "Accept": "application/json; charset=UTF-8",
      "Content-Type": "application/json; charset=UTF-8",
    },
  })
  .then((response) => {
    if (response.ok) {
      window.location.reload();
    }
    return Promise.reject(response);
  })
  .catch((response) => response.json().then((msg) => {
    alert(`${response.status} ${response.statusText}: ${msg.details}`);
  }))
  .finally(() => removeLoading(caller));
}

function removeBook(caller, bookId){
  addLoading(caller);
  fetch(`/api/v0/books/${bookId}`, {
    method: "DELETE",
    headers: {
      "Accept": "application/json; charset=UTF-8",
      "Content-Type": "application/json; charset=UTF-8",
    },
  })
  .then((response) => {
    if (response.ok) {
      window.location.reload();
    }
    return Promise.reject(response);
  })
  .catch((response) => response.json().then((msg) => {
    alert(`${response.status} ${response.statusText}: ${msg.details}`);
  }))
  .finally(() => removeLoading(caller));
}
