function setLoading(caller){
  let element = document.getElementById(caller);
  element.classList.add("is-loading");
}

function addUser(caller){
  setLoading(caller);
  let username = document.getElementById("usernameEntry").value;
  $.ajax({
    url: "/api/v0/users",
    type: "POST",
    dataType: "json",
    contentType: "application/json; charset=UTF-8",
    data: JSON.stringify({
      "username": username
    }),
    success: function(){
      window.location = "/book-catalogue/collection?username=" + username;
    },
    error: function(xhr){
      alert('Request Status: ' + xhr.status + ' Status Text: ' + xhr.statusText + ' ' + xhr.responseText);
    },
  });
}

function selectUser(caller){
  setLoading(caller);
  let username = document.getElementById("usernameEntry").value;
  $.ajax({
    url: "/api/v0/users/" + username,
    type: "GET",
    dataType: "json",
    contentType: "application/json; charset=UTF-8",
    success: function(){
      window.location = "/book-catalogue/collection?username=" + username;
    },
    error: function(xhr){
      alert('Request Status: ' + xhr.status + ' Status Text: ' + xhr.statusText + ' ' + xhr.responseText);
    },
  });
}

function addBook(caller){
  setLoading(caller);
  let params = new URLSearchParams(window.location.search);
  let username = params.get("username");
  let isbn = document.getElementById("isbnEntry").value;
  $.ajax({
    url: "/api/v0/books",
    type: "POST",
    dataType: "json",
    contentType: "application/json; charset=UTF-8",
    data: JSON.stringify({
      "isbn": isbn,
      "wisher": username
    }),
    success: function(){
      window.location = "/book-catalogue/wishlist?username=" + username;
    },
    error: function(xhr){
      alert('Request Status: ' + xhr.status + ' Status Text: ' + xhr.statusText + ' ' + xhr.responseText);
    },
  });
}

function refreshBook(caller, isbn){
  setLoading(caller);
  $.ajax({
    url: "/api/v0/books/" + isbn,
    type: "POST",
    dataType: "json",
    contentType: "application/json; charset=UTF-8",
    success: function(){
      window.location.reload();
    },
    error: function(xhr){
      alert('Request Status: ' + xhr.status + ' Status Text: ' + xhr.statusText + ' ' + xhr.responseText);
    },
  });
}

function removeBook(caller, isbn){
  setLoading(caller);
  $.ajax({
    url: "/api/v0/books/" + isbn,
    type: "DELETE",
    dataType: "json",
    contentType: "application/json; charset=UTF-8",
    success: function(){
      window.location.reload();
    },
    error: function(xhr){
      alert('Request Status: ' + xhr.status + ' Status Text: ' + xhr.statusText + ' ' + xhr.responseText);
    },
  });
}

function readBook(caller, isbn, readers){
  setLoading(caller);
  let params = new URLSearchParams(window.location.search);
  let username = params.get("username");
  readers.push(username);
  $.ajax({
    url: "/api/v0/books/" + isbn,
    type: "PUT",
    dataType: "json",
    contentType: "application/json; charset=UTF-8",
    data: JSON.stringify({
      "wisher": "",
      "readers": readers
    }),
    success: function(){
      window.location.reload();
    },
    error: function(xhr){
      alert('Request Status: ' + xhr.status + ' Status Text: ' + xhr.statusText + ' ' + xhr.responseText);
    },
  });
}

function unreadBook(caller, isbn, readers){
  setLoading(caller);
  let params = new URLSearchParams(window.location.search);
  let username = params.get("username");
  readers = readers.filter(e => e !== username);
  $.ajax({
    url: "/api/v0/books/" + isbn,
    type: "PUT",
    dataType: "json",
    contentType: "application/json; charset=UTF-8",
    data: JSON.stringify({
      "wisher": "",
      "readers": readers
    }),
    success: function(){
      window.location.reload();
    },
    error: function(xhr){
      alert('Request Status: ' + xhr.status + ' Status Text: ' + xhr.statusText + ' ' + xhr.responseText);
    },
  });
}

function acquiredBook(caller, isbn){
  setLoading(caller);
  let params = new URLSearchParams(window.location.search);
  let username = params.get("username");
  $.ajax({
    url: "/api/v0/books/" + isbn,
    type: "PUT",
    dataType: "json",
    contentType: "application/json; charset=UTF-8",
    data: JSON.stringify({
      "wisher": "",
      "readers": []
    }),
    success: function(){
      window.location = "/book-catalogue/collection?username=" + username;
    },
    error: function(xhr){
      alert('Request Status: ' + xhr.status + ' Status Text: ' + xhr.statusText + ' ' + xhr.responseText);
    },
  });
}
