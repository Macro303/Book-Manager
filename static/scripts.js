function addUser(){
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
      window.location = "/Book-Manager/collection?username=" + username;
    },
    error: function(xhr){
      alert('Request Status: ' + xhr.status + ' Status Text: ' + xhr.statusText + ' ' + xhr.responseText);
    },
  });
}

function selectUser(){
  let username = document.getElementById("usernameEntry").value;
  $.ajax({
    url: "/api/v0/users/" + username,
    type: "GET",
    dataType: "json",
    contentType: "application/json; charset=UTF-8",
    success: function(){
      window.location = "/Book-Manager/collection?username=" + username;
    },
    error: function(xhr){
      alert('Request Status: ' + xhr.status + ' Status Text: ' + xhr.statusText + ' ' + xhr.responseText);
    },
  });
}

function addBook(){
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
      "wished": username
    }),
    success: function(){
      window.location = "/Book-Manager/wishlist?username=" + username;
    },
    error: function(xhr){
      alert('Request Status: ' + xhr.status + ' Status Text: ' + xhr.statusText + ' ' + xhr.responseText);
    },
  });
}

function removeBook(){
  let isbn = document.getElementById("isbnEntry").value;
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

function readBook(isbn, existing_readers){
  let params = new URLSearchParams(window.location.search);
  let username = params.get("username");
  existing_readers.push(username)
  $.ajax({
    url: "/api/v0/books/" + isbn,
    type: "PUT",
    dataType: "json",
    contentType: "application/json; charset=UTF-8",
    data: JSON.stringify({
      "wished": "",
      "read": existing_readers
    }),
    success: function(){
      window.location.reload();
    },
    error: function(xhr){
      alert('Request Status: ' + xhr.status + ' Status Text: ' + xhr.statusText + ' ' + xhr.responseText);
    },
  });
}

function unreadBook(isbn, existing_readers){
  let params = new URLSearchParams(window.location.search);
  let username = params.get("username");
  existing_readers = existing_readers.filter(e => e !== username)
  $.ajax({
    url: "/api/v0/books/" + isbn,
    type: "PUT",
    dataType: "json",
    contentType: "application/json; charset=UTF-8",
    data: JSON.stringify({
      "wished": "",
      "read": existing_readers
    }),
    success: function(){
      window.location.reload();
    },
    error: function(xhr){
      alert('Request Status: ' + xhr.status + ' Status Text: ' + xhr.statusText + ' ' + xhr.responseText);
    },
  });
}

function acquiredBook(isbn){
  let params = new URLSearchParams(window.location.search);
  let username = params.get("username");
  $.ajax({
    url: "/api/v0/books/" + isbn,
    type: "PUT",
    dataType: "json",
    contentType: "application/json; charset=UTF-8",
    data: JSON.stringify({
      "wished": "",
      "read": []
    }),
    success: function(){
      window.location = "/Book-Manager/collection?username=" + username;
    },
    error: function(xhr){
      alert('Request Status: ' + xhr.status + ' Status Text: ' + xhr.statusText + ' ' + xhr.responseText);
    },
  });
}
