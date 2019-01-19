$(document).ready(function () {

	// remote
	// ------

	var products = new Bloodhound({
		datumTokenizer: function (datum) {
			return Bloodhound.tokenizers.whitespace;
		},
		queryTokenizer: Bloodhound.tokenizers.whitespace,
		remote: {
			wildcard: '%QUERY',
			url: 'http://_ENDPOINT/api/fetchProducts?name=%QUERY',
			transform: function (response) {
				// Map the remote source JSON array to a JavaScript object array
				return $.map(response, function (suggestion) {
					return {
						value: suggestion
					};
				});
			}
		}
	});

	// Instantiate the Typeahead UI
	$('#remote .typeahead').typeahead({
		hint: true,
		highlight: true,
		minLength: 2
	}, {
			display: 'value',
			source: products,
			limit: 10,
			templates: {
				empty: [
					'<div class="empty-message">',
					'Unable to find any products that match the current query',
					'</div>'
				].join('\n')
			}
		}
	);


	$.ajax({
		url: "http://_ENDPOINT/api/buildInfo"
	}).then(function (data) {
		$('#footer').append(data);
	});

});