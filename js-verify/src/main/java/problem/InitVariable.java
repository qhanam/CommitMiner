package problem;

public class InitVariable {
	public String name ;
	public Type type;

	public InitVariable(String name, Type type) {
		this.name = name; 
		this.type = type;
	}
	
	public enum Type {
		INT,
		STRING,
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this) return true;
		if(o instanceof InitVariable) {
			InitVariable v = (InitVariable)o;
			if(name.equals(v.name)) return true;
		}
		if(o instanceof String) {
			String v = (String)o;
			if(name.equals(v)) return true;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
}
