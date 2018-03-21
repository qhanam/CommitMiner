function print(message) {
	console.log(message);
}

function gaf() {
	for(var i = 0; i < 10; i++) {
		console.log(i);
	}

	function noise() {
		var a = "I'm in the way";
	}
}

function bar() {
	try {
		console.log("Hello World!");
		console.log("Hello Console!");
	} catch (e) { 
		console.log("Oh no!");	
	} finally {
		close();
	}
	gaf();
	console.log("Goodbye World!");
}

function foo() {
	try {
		bar();
	} catch (e) { }
}

foo();
