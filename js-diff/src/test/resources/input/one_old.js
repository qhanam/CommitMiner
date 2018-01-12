var args = process.argv.slice(2);
var x = parseInt(args[0]);
var z;

function log(n) {
	if(n === undefined) n = n;
	else n = "A";
	print(n);
}

z = x;

log(z);
