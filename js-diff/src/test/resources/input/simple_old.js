var args = process.argv.slice(2);

var x = parseInt(args[0]);
var y = parseInt(args[1]);
var z, w;
var a, b;

function print(n) {
	console.log(n);
	log(n);
}

function log(n) {
	console.log(n);
	print(n);
}

z = y + x;
w = z;

log(z);
log(w);

function foo() {
	return 5;
}

function bar() {
	return 5;
}

a = foo();
b = bar();

