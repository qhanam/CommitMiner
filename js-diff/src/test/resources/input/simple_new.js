function foo() {
	var x = 5;
  try {
		x;
		return x;
  } catch (e) { 
		x;
		return x;
	} finally {
		x = 7;
	}
}
var y = foo();
