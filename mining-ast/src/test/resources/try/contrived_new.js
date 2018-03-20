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
	} catch (e) { 
		console.log(e);	
	}
	gaf();
	try {
		console.log("Goodbye World!");
	} catch (e) { }
}

function foo() {
	try {
		bar();
	} catch (e) { }
}

try {
	foo();
} catch (e) { }
