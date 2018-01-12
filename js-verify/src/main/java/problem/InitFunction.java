package problem;

import java.util.Arrays;

import problem.InitVariable.Type;

public class InitFunction {
	public String name;
	public Type returnType;
	public Type[] argTypes;
	
	public InitFunction(String name, Type returnType, Type[] argTypes) {
		this.name = name;
		this.returnType =returnType;
		this.argTypes = argTypes;
	}
	
	public String getTypeString() {
		String ret = "";
		boolean firstArg = true;
		ret += "(";
		for(Type arg: argTypes) {
			if(firstArg)
				firstArg = false;
			else
				ret += ",";
			ret += arg.name();
		}
		ret += ")";
		ret += "->";
		ret += returnType.name();
		return ret;
	}
	
	@Override
	public String toString() {
		String ret = "";
		ret = name + " : " + this.getTypeString();
		return ret;
	}
}
