function gaf() {
	for(var i = 0; i < 10; i++) {
		console.log(i);
	}
}

function bar() {
	try {
		console.log("Hello World!");
	} catch (e) { }
	gaf();
	console.log("Goodbye World!");
}

function foo() {
	try {
		bar();
	} catch (e) { }
}

foo();
