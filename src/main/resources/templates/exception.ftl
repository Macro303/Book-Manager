<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta content="Macro303" name="author">
	<title>Book Manager</title>
	<link href="https://cdn.jsdelivr.net/npm/semantic-ui@2.4.2/dist/semantic.min.css" rel="stylesheet">
	<link href="/css/style.css" rel="stylesheet"/>
</head>
<body>
<div id="navbar"></div>
<div class="ui pushable segment basic">
	<div class="ui sidebar vertical menu fixed">
		<h4 class="ui orange inverted header item">Book Manager</h4>
		<a class="item" href="/"><i class="home icon"></i>Home</a>
		<a class="item" href="/collection.html"><i class="book icon"></i>Collection</a>
		<a class="item" href="/wishlist.html"><i class="clipboard list icon"></i>Wishlist</a>
		<a class="item" href="/loan.html"><i class="address book icon"></i>Loans</a>
		<a class="item" href="/about.html"><i class="info circle icon"></i>About</a>
	</div>
	<div class="pusher">
		<div class="ui basic segment" id="content">
			<div class="ui one column stackable center aligned page grid">
				<div class="column twelve wide">
					<div class="ui red big message opacity compact">
						<div class="header">${code}: ${request}</div>
						<p>${message}</p>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<script src="https://code.jquery.com/jquery-3.1.1.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/semantic-ui@2.4.2/dist/semantic.min.js"></script>
<script src="/js/script.js"></script>
<script type="text/javascript">
	$(document).ready(function () {
		$("#navbar").load("/navbar.html");
	});
</script>
</body>
</html>