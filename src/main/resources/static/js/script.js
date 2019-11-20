function loadNewest() {
	for (let i = 0; i < 5; i++) {
		let column = document.createElement('div');
		column.className = 'column is-one-fifth';
		column.innerHTML = '<div class="card">' +
			'<div class="card-image">' +
			'<figure class="image is-3by4">' +
			'<img src="https://bulma.io/images/placeholders/480x640.png" alt="Placeholder">' +
			'</figure>' +
			'</div>' +
			'<div class="card-content">' +
			'<div class="media">' +
			'<div class="media-content">' +
			'<p class="title is-4">Book Title</p>' +
			'<p class="subtitle is-6">Subtitle</p>' +
			'</div>' +
			'</div>' +
			'<div class="content">Book Description</div>' +
			'</div>' +
			'</div>';

		document.getElementById('newest-grid').appendChild(column)
	}
}

function loadRead() {
	for (let i = 0; i < 5; i++) {
		let column = document.createElement('div');
		column.className = 'column is-one-fifth';
		column.innerHTML = '<div class="card">' +
			'<div class="card-image">' +
			'<figure class="image is-3by4">' +
			'<img src="https://bulma.io/images/placeholders/480x640.png" alt="Placeholder">' +
			'</figure>' +
			'</div>' +
			'<div class="card-content">' +
			'<div class="media">' +
			'<div class="media-content">' +
			'<p class="title is-4">Book Title</p>' +
			'<p class="subtitle is-6">Subtitle</p>' +
			'</div>' +
			'</div>' +
			'<div class="content">Book Description</div>' +
			'</div>' +
			'</div>';

		document.getElementById('read-grid').appendChild(column)
	}
}

function loadCollection() {
	$.ajax({
		async: false,
		url: '/api/books/collection',
		type: 'GET',
		headers: {
			accept: 'application/json',
			contentType: 'application/json'
		},
		dataType: 'json',
		success: function (data) {
			for (let book of data) {
				let column = bookToHTML(book);
				document.getElementById('collection-grid').appendChild(column);
			}
		},
		error: function (xhr, status, error) {
			alert("#ERR: xhr.status=" + xhr.status + ", xhr.statusText=" + xhr.statusText + "\nstatus=" + status + ", error=" + error);
		}
	});
}

function addToCollection() {
	document.getElementById("collectionAddButton").className = "ui loading inverted blue button";
	$.ajax({
		async: true,
		url: '/api/books/collection',
		type: 'POST',
		contentType: 'application/json',
		data: JSON.stringify({
			"isbn": document.getElementById('collection_isbn').value.toString(),
			"format": document.getElementById('collection_format').value
		}),
		dataType: 'json',
		success: function (data) {
			console.log("Response: " + data);
			document.getElementById("collectionAddButton").className = "ui inverted blue button";
			document.getElementById("collection_isbn").value = '';
			document.getElementById("collection_format").value = '';
			location.reload();
		},
		error: function (xhr, status, error) {
			alert("#ERR: xhr.status=" + xhr.status + ", xhr.statusText=" + xhr.statusText + "\nstatus=" + status + ", error=" + error);
			document.getElementById("collectionAddButton").className = "ui inverted blue button";
		}
	});
}

function loadWishlist() {
	$.ajax({
		async: false,
		url: '/api/books/wishlist',
		type: 'GET',
		headers: {
			accept: 'application/json',
			contentType: 'application/json'
		},
		dataType: 'json',
		success: function (data) {
			for (let book of data) {
				let column = bookToHTML(book);
				document.getElementById('wishlist-grid').appendChild(column);
			}
		},
		error: function (xhr, status, error) {
			alert("#ERR: xhr.status=" + xhr.status + ", xhr.statusText=" + xhr.statusText + "\nstatus=" + status + ", error=" + error);
		}
	});
}

function addToWishlist() {
	document.getElementById("wishlistAddButton").className = "ui loading inverted blue button";
	$.ajax({
		async: true,
		url: '/api/books/wishlist',
		type: 'POST',
		contentType: 'application/json',
		data: JSON.stringify({
			"isbn": document.getElementById('wishlist_isbn').value.toString(),
			"format": document.getElementById('wishlist_format').value
		}),
		dataType: 'json',
		success: function (data) {
			console.log("Response: " + data);
			document.getElementById("wishlistAddButton").className = "ui inverted blue button";
			document.getElementById("wishlist_isbn").value = '';
			document.getElementById("wishlist_format").value = '';
			location.reload();
		},
		error: function (xhr, status, error) {
			alert("#ERR: xhr.status=" + xhr.status + ", xhr.statusText=" + xhr.statusText + "\nstatus=" + status + ", error=" + error);
			document.getElementById("wishlistAddButton").className = "ui inverted blue button";
		}
	});
}

function bookToHTML(book) {
	let column = document.createElement('div');
	column.className = 'column is-2';
	column.innerHTML = '<div class="card">' +
		'<div class="card-image">' +
		'<figure class="image is-3by4">' +
		`<img src="${book.Book.Images.Medium}" alt="Placeholder">` +
		'</figure>' +
		'</div>' +
		'<div class="card-content">' +
		'<div class="media">' +
		'<div class="media-content">' +
		`<p class="title is-4">${book.Book.Title}</p>` +
		`<p class="subtitle is-6">${book.Book.Subtitle}</p>` +
		'</div>' +
		'</div>' +
		'<div class="content">Book Description</div>' +
		'</div>' +
		'</div>';
	return column
}

function loadLoans() {
	for (var i = 0; i < 10; i++) {
		let column = document.createElement('div');
		column.className = 'column is-2';
		column.innerHTML = '<div class="card">' +
			'<div class="card-image">' +
			'<figure class="image is-3by4">' +
			'<img src="https://bulma.io/images/placeholders/480x640.png" alt="Placeholder">' +
			'</figure>' +
			'</div>' +
			'<div class="card-content">' +
			'<div class="media">' +
			'<div class="media-content">' +
			'<p class="title is-4">Book Title</p>' +
			'<p class="subtitle is-6">Subtitle</p>' +
			'</div>' +
			'</div>' +
			'<div class="content">Book Description</div>' +
			'</div>' +
			'</div>';

		document.getElementById('loan-grid').appendChild(column)
	}
}