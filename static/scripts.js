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
      window.location.reload();
      return response.json();
    }
    return Promise.reject(response);
  }).catch((response) => response.json().then((msg) => {
    alert(`${response.status} ${response.statusText}: ${msg.details}`);
  })).finally(() => removeLoading(caller));
}

let addForm = document.getElementById("add-form");
addForm.addEventListener('submit', e => {
  let details = Object.fromEntries(new FormData(addForm));
  console.log(details);

  performRequest(
    caller="add-form-button",
    url="/api/v0/books",
    method="POST",
    body={
      "isbn": details["isbn"],
      "open_library_id": "",
      "wisher_id": details["wisher-id"],
    },
  );

  e.preventDefault();
});

function deleteBook(caller, bookId){
  performRequest(
    caller=caller,
    url=`/api/v0/books/${bookId}`,
    method="DELETE",
  );
}

function refreshBook(caller, bookId){
  performRequest(
    caller=caller,
    url=`/api/v0/books/${bookId}`,
    method="PUT",
  );
}

function collectBook(caller, bookId){
  performRequest(
    caller=caller,
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
