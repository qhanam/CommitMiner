var args = process.argv.slice(2);

var x = parseInt(args[0]);
var y = parseInt(args[1]);
var z, w, v;

function print(n) {
	console.log(n);
	log(n);
}

function log(n) {
	console.log(n);
	print(n);
}

z = y + x - x;
w = z + x;
v = 5;

log(z);
log(w);
log(v);

function foo() {
	return 5 + 0;
}

function bar() {
	return 5 + 1;
}

a = foo();
b = bar();

