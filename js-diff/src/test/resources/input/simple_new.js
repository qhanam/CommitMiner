var fs = require('fs');

function foo() {
	fs.existsSync('my_file.txt');
}

foo();
