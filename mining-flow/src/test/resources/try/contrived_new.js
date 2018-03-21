function print(message) {
	console.log("Hello " + message);
}

function gaf() {
	for(var i = 0; i < 10; i++) {
		try {
			console.log(i);
		} catch (e) { }
	}
}

function bar() {
	try {
		console.log("Hello World!");
		console.log("Hello Console!");
	} catch (e) { 
		console.log(e);	
	} finally {
		close();
	}
	gaf();
	try {
		console.log("Goodbye World!");
	} catch (e) { }
}

function foo() {
	try {
		bar();
		bar();
	} catch (e) { }
}

try {
	foo();
} catch (e) { }
