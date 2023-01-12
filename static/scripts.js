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

function addBook(caller, userId){
  addLoading(caller);
  let isbn = document.getElementById("isbn-entry").value;
  let openLibraryId = document.getElementById("open-library-entry").value;
  fetch("/api/v0/books", {
    method: "POST",
    headers,
    body: JSON.stringify({
      "isbn": isbn,
      "open_library_id": openLibraryId,
      "wisher_id": userId
    }),
  })
  .then((response) => {
    if (response.ok) {
      window.location = `/book-catalogue/${userId}/wishlist`;
    }
    return Promise.reject(response);
  })
  .catch((response) => response.json().then((msg) => {
    alert(`${response.status} ${response.statusText}: ${msg.details}`);
  }))
  .finally(() => removeLoading(caller));
}

function refreshAllBooks(caller){
  addLoading(caller);
  fetch("/api/v0/books/refresh", {
    method: "PUT",
    headers,
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
    headers,
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
    headers,
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
    headers,
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
    headers,
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

function filterBooks(caller, userId){
  addLoading(caller);
  let url = new URL(window.location);

  let titleFilter = document.getElementById("title-filter-input").value;
  if (titleFilter == "") {
    url.searchParams.delete("title");
  } else {
    url.searchParams.set("title", titleFilter);
  }

  let authorFilter = document.getElementById("author-filter-select").value;
  if (authorFilter == -1) {
    url.searchParams.delete("author_id");
  } else {
    url.searchParams.set("author_id", authorFilter);
  }

  let formatFilter = document.getElementById("format-filter-select").value;
  if (formatFilter == -1) {
    url.searchParams.delete("format");
  } else {
    url.searchParams.set("format", formatFilter);
  }

  let seriesFilter = document.getElementById("series-filter-select").value;
  if (seriesFilter == -1) {
    url.searchParams.delete("series_id");
  } else {
    url.searchParams.set("series_id", seriesFilter);
  }

  let publisherFilter = document.getElementById("publisher-filter-select").value;
  if (publisherFilter == -1) {
    url.searchParams.delete("publisher_id");
  } else {
    url.searchParams.set("publisher_id", publisherFilter);
  }

  window.location = url;
}
