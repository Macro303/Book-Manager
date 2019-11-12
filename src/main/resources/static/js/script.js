function loadNewest() {
	for (var i = 0; i < 5; i++) {
		var column = document.createElement('div');
		column.className = 'column';
		var segment = document.createElement('div');
		segment.className = 'ui placeholder segment inverted';
		column.appendChild(segment);
		document.getElementById('newest-grid').appendChild(column)
	}
}

function loadRead() {
	for (var i = 0; i < 5; i++) {
		var column = document.createElement('div');
		column.className = 'column';
		var segment = document.createElement('div');
		segment.className = 'ui placeholder segment inverted';
		column.appendChild(segment);
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
		success: function(data){
			for(let book of data){
				let column = document.createElement('div');
				column.className = 'column';
				let segment = document.createElement('div');
				segment.className = 'ui segment inverted';
				let imageDiv = document.createElement('div');
				imageDiv.className = 'ui small image';
				let label = null;
				if (book.Count !== 1) {
					label = document.createElement('a');
					label.className = 'ui small teal right ribbon label';
					label.textContent = book.Count;
				}
				let image = document.createElement('img');
				image.className = 'ui small image';
				image.src = book.Book.Images.Medium;
				let header = document.createElement('div');
				header.className = 'ui center aligned small inverted header';
				header.textContent = book.Book.Title;
				let subheader = document.createElement('div');
				subheader.className = 'sub header';
				subheader.textContent = book.Book.Subtitle;

				header.appendChild(subheader);
				imageDiv.appendChild(image);
				if (book.Count !== 1) {
					imageDiv.appendChild(label);
				}
				segment.appendChild(imageDiv);
				segment.appendChild(header);
				column.appendChild(segment);
				document.getElementById('collection-grid').appendChild(column);
			}
		},
		error: function(xhr, status, error){
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
			location.reload();
		},
		error: function(xhr, status, error){
			alert("#ERR: xhr.status=" + xhr.status + ", xhr.statusText=" + xhr.statusText + "\nstatus=" + status + ", error=" + error);
			document.getElementById("collectionAddButton").className = "ui inverted blue button";
			location.reload();
		}
	});
}

function loadWishlist() {
	for (var i = 0; i < 18; i++) {
		var column = document.createElement('div');
		column.className = 'column';
		var segment = document.createElement('div');
		segment.className = 'ui placeholder segment inverted';
		column.appendChild(segment);
		document.getElementById('wishlist-grid').appendChild(column)
	}
}

function loadLoans() {
	for (var i = 0; i < 10; i++) {
		var column = document.createElement('div');
		column.className = 'column';
		var segment = document.createElement('div');
		segment.className = 'ui placeholder segment inverted';
		column.appendChild(segment);
		document.getElementById('loan-grid').appendChild(column)
	}
}