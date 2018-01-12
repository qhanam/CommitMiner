package multidiffplus.jsanalysis.abstractdomain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ScriptNode;

import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CFGNode;
import multidiffplus.jsanalysis.factories.StoreFactory;
import multidiffplus.jsanalysis.flow.Analysis;
import multidiffplus.jsanalysis.flow.StackFrame;
import multidiffplus.jsanalysis.trace.Trace;
import multidiffplus.jsanalysis.transfer.Helpers;
import multidiffplus.jsanalysis.transfer.StateComparator;
import multidiffplus.jsanalysis.visitors.FunctionLiftVisitor;
import multidiffplus.jsanalysis.visitors.VariableLiftVisitor;

/**
 * The abstract domain for function closures.
 */
public class FunctionClosure extends Closure {

	/** The function. **/
	public CFG cfg;

	/** The closure environment? **/
	public Environment environment;

	/**
	 * @param cfg The control flow graph for the function.
	 * @param environment The environment of the parent closure. Does not yet
	 * 					  contain local variables of this function.
	 */
	public FunctionClosure(CFG cfg, Environment environment) {
		this.cfg = cfg;
		this.environment = environment;
	}

	@Override
	public State run(
			Address selfAddr, Store store,
			Scratchpad scratchpad, Trace trace, Control control,
			Analysis analysis) {

		/* Advance the trace. */
		trace = trace.update(environment, store, selfAddr, 
							 (ScriptNode)cfg.getEntryNode().getStatement());
		
		/* Create the initial state if needed. */
		State newState = null;
		State oldState = (State) cfg.getEntryNode().getBeforeState();
		State primeState = initState(selfAddr, store, scratchpad, trace, control, analysis);
		State exitState = null;

		/* Have we hit a timeout? Return the current exit state. This creates
		* unsoundness, but is better than running forever. */
		if(cfg.hasTimedOut()) {
			System.err.println("FunctionClosure::run -- WARNING -- aborting function analysis because of timeout.");
			for(CFGNode exitNode : cfg.getExitNodes()) {
				if(exitState == null) exitState = (State)exitNode.getBeforeState();
				else exitState = exitState.join((State)exitNode.getBeforeState());
			}
			return exitState;
		}
		
		if(oldState == null) {
			/* Create the initial state for the function call by lifting local 
			 * vars and functions into the environment. */
			newState = primeState;
		}
		else {
			/* If newState does not change initState, we do not need to re-analyze the function. */
			newState = oldState.join(primeState);
	
			StateComparator comparator = new StateComparator(oldState, newState);

			if(comparator.isEqual()) {

				exitState = null;

				for(CFGNode exitNode : cfg.getExitNodes()) {
					/* Merge all exit states, because we can only return one. */
					State s = (State)exitNode.getAfterState();
					if(exitState == null) exitState = s;
					else if(s != null) exitState = exitState.join(s);
				}

				if(exitState != null) {
					/* Finally, merge the store from the exit state with the
					 * store from the entry state. */
					exitState.store = exitState.store.join(store);
					return exitState;
				}
				
				/* We must be in a recursive loop. Don't update the state. */
				return newState;
				
			}
			else {
				/* We have a new initial state for the function. */
				cfg.getEntryNode().setBeforeState(newState);
			}
			
		}
		
		/* Get the set of local vars to search for unanalyzed functions. */
		Set<String> localVars = new HashSet<String>();
		List<Name> localVarNames = VariableLiftVisitor.getVariableDeclarations((ScriptNode)cfg.getEntryNode().getStatement());
		for(Name localVarName : localVarNames) localVars.add(localVarName.toSource());

		/* Get the set of local functions to search for unanalyzed functions. */
		List<FunctionNode> localFunctions = FunctionLiftVisitor.getFunctionDeclarations((ScriptNode)cfg.getEntryNode().getStatement());
		for(FunctionNode localFunction : localFunctions) {
			Name name = localFunction.getFunctionName();
			if(name != null) localVars.add(name.toSource());
		}

		/* Perform the initial analysis and get the publicly accessible methods. */
		analysis.pushFunctionCall(new StackFrame(cfg, newState));
//		exitState = Helpers.run(cfg, newState);

		/* Analyze the publicly accessible methods that weren't analyzed in
		 * the main analysis. */
//		Helpers.analyzeEnvReachable(exitState, exitState.env.environment, exitState.selfAddr, cfgs, new HashSet<Address>(), localVars);

		return exitState;

	}
	
	/**
	 * Lift local variables and function declarations into the environment and 
	 * create the initial state for the function call.
	 * @return The environment for the closure, including parameters and {@code this}.
	 */
	private State initState(
			Address selfAddr, Store store,
			Scratchpad scratchpad, Trace trace, Control control,
			Analysis analysis) {

		Environment env = this.environment.clone();

		/* Match parameters with arguments. */
		if(this.cfg.getEntryNode().getStatement() instanceof FunctionNode) {
			FunctionNode function = (FunctionNode)this.cfg.getEntryNode().getStatement();

			/* Create the arguments object. */
			Map<String, Property> ext = new HashMap<String, Property>();
			int i = 0;
			for(BValue argVal : scratchpad.applyArgs()) {

				store = Helpers.addProp(function.getID(), String.valueOf(i), argVal,
								ext, store, trace);
				i++;
			}

			InternalObjectProperties internal = new InternalObjectProperties(
					Address.inject(StoreFactory.Arguments_Addr, Change.u(), DefinerIDs.bottom()), JSClass.CObject);
			Obj argObj = new Obj(ext, internal);

			/* Put the argument object on the store. */
			Address argAddr = trace.makeAddr(function.getID(), "");
			store = store.alloc(argAddr, argObj);

			i = 0;
			for(AstNode param : function.getParams()) {
				if(param instanceof Name) {

					Name paramName = (Name) param;
					Property prop = argObj.externalProperties.get(String.valueOf(i));

					if(prop == null) {

						/* No argument was given for this parameter. Create a
						 * dummy value. */

						/* Add the argument address to the argument object. */
						BValue argVal = BValue.top(Change.convU(param));
						store = Helpers.addProp(param.getID(), String.valueOf(i), argVal,
										  argObj.externalProperties, store, trace);
						prop = argObj.externalProperties.get(String.valueOf(i));

						/* Add or update the argument object to the store. */
						argAddr = trace.makeAddr(param.getID(), String.valueOf(i));
						store = store.alloc(argAddr, argObj);

					}
					Variable identity = new Variable(paramName.getID(), paramName.toSource(), Change.conv(paramName), new Addresses(prop.address));
					env = env.strongUpdate(paramName.toSource(), identity);
				}
				i++;
			}
		}

		/* Lift local variables and function declarations into the environment. 
		 * This has to happen after the parameters are added to the environment
		 * so that the parameters are available in the closure of functions 
		 * declared within this function. */
		store = Helpers.lift(env, store,
							 (ScriptNode)cfg.getEntryNode().getStatement(),
							 analysis.cfgs, trace);
		
		/* Add 'this' to environment (points to caller's object or new object). */
		env = env.strongUpdate("this", new Variable(cfg.getEntryNode().getId(), "this", Change.u(), new Addresses(selfAddr)));
		
		/* Create the initial state for the function call. */
		return new State(store, env, scratchpad, trace, control, selfAddr);
		
	}
	
	@Override
	public String toString() {
		return this.cfg.getEntryNode().getStatement().toString();
	}
	
}
