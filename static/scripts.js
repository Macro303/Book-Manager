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

function performRequest(caller, url, method, body = {}){
  addLoading(caller);
  fetch(url, {
    method: method,
    headers: headers,
    body: JSON.stringify(body),
  }).then((response) => {
    if (response.ok){
      return response.json();
    }
    return Promise.reject(response);
  }).then((data) => window.location.reload())
  .catch((response) => response.json().then((msg) => {
    alert(`${response.status} ${response.statusText}: ${msg.details}`);
  })).finally(() => removeLoading(caller));
}

function collectBook(bookId){
  performRequest(
    caller="collect-book-button",
    url=`/api/v0/books/${bookId}/collect`,
    method="POST",
  );
}

function readBook(caller, bookId, userId){
  performRequest(
    caller=caller,
    url=`/api/v0/books/${bookId}/read`,
    method="POST",
    body={
      "user_id": userId
    },
  );
}

function saveBook(bookId, userId){
  const caller = "save-book-button";
  const editForm = document.getElementById("edit-form");
  let details = Object.fromEntries(new FormData(editForm));
  console.log(details);

  addLoading(caller);
  fetch(`/api/v0/books/${bookId}`, {
    method: "PATCH",
    headers: headers,
    body: JSON.stringify({
      "title": details["title"],
      "subtitle": details["subtitle"],
      "format": details["format"],
      "description": details["description"],
      "publisher_id": details["publisher"],
      "goodreads_id": details["goodreads-id"],
      "library_thing_id": details["library-thing-id"],
      "open_library_id": details["open-library-id"],
    }),
  }).then((response) => {
    if (response.ok){
      return response.json();
    }
    return Promise.reject(response);
  }).then((data) => window.location = `/${userId}/books/${bookId}`)
  .catch((response) => response.json().then((msg) => {
    alert(`${response.status} ${response.statusText}: ${msg.details}`);
  })).finally(() => removeLoading(caller));
}

function resetBook(bookId){
  const caller = "reset-book-button"

  addLoading(caller);
  fetch(`/api/v0/books/${bookId}`, {
    method: "PUT",
    headers: headers,
  }).then((response) => {
    if (response.ok){
      return response.json();
    }
    return Promise.reject(response);
  }).then((data) => window.location = `/${userId}/books/${bookId}`)
  .catch((response) => response.json().then((msg) => {
    alert(`${response.status} ${response.statusText}: ${msg.details}`);
  })).finally(() => removeLoading(caller));
}

function deleteBook(bookId){
  const caller = "delete-book-button"

  addLoading(caller);
  fetch(`/api/v0/books/${bookId}`, {
    method: "DELETE",
    headers: headers,
  }).then((response) => {
    if (response.ok){
      return response.json();
    }
    return Promise.reject(response);
  }).then((data) => window.location = `/${userId}/collection`)
  .catch((response) => response.json().then((msg) => {
    alert(`${response.status} ${response.statusText}: ${msg.details}`);
  })).finally(() => removeLoading(caller));
}
