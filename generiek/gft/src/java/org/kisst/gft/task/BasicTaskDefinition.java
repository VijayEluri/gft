package org.kisst.gft.task;

import java.lang.reflect.Constructor;

import org.kisst.gft.GftContainer;
import org.kisst.gft.action.Action;
import org.kisst.gft.action.ActionList;
import org.kisst.gft.filetransfer.Channel;
import org.kisst.props4j.Props;
import org.kisst.props4j.SimpleProps;
import org.kisst.util.ReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasicTaskDefinition implements TaskDefinition {
	final static Logger logger=LoggerFactory.getLogger(BasicTaskDefinition.class); 

	public final GftContainer gft;
	public final String name;
	protected final Action action;
	protected Action startAction=null;
	protected Action endAction=null;
	protected Action errorAction=null;
	public final Props props;
	private final SimpleProps context;

	private long totalCount=0;
	private long errorCount=0;

	// This constructor has a bit bogus defaultActions parameter that is needed for the other constructor
	// In future this parameter might be removed
	public BasicTaskDefinition(GftContainer gft, Props props, Action flow, String defaultActions) {
		this.gft=gft;
		context=gft.getContext().shallowClone();

		this.props=props;
		this.name=props.getLocalName();
		if (flow!=null)
			this.action=flow;
		else
			this.action=new ActionList(this, props, defaultActions);
		
		SimpleProps actprops=new SimpleProps();

		actprops.put("actions", "log_error");
		this.errorAction=new ActionList(this, actprops);

		actprops.put("actions", "log_start");
		this.startAction=new ActionList(this, actprops);
		
		actprops.put("actions", "log_completed");
		this.endAction=new ActionList(this, actprops);	
	}

	// This constructor is for backward compatibility 
	public BasicTaskDefinition(GftContainer gft, Props props, String defaultActions) {
		this(gft,props,null,defaultActions);
	}

	public String getName() { return name; }
	public SimpleProps getContext() { return context;}
	public long getTotalCount() { return totalCount; }
	public long getErrorCount() { return errorCount; }
	
	public void run(Task task) {
		try {
			totalCount++;
			if (startAction!=null)
				startAction.execute(task);
			action.execute(task);
			if (endAction!=null)
				endAction.execute(task);
		}
		catch (RuntimeException e) {
			errorCount++;
			task.setLastError(e);
			try {
				if (errorAction!=null)
					errorAction.execute(task);
			}
			catch(RuntimeException e2) { 
				logger.error("Could not perform the error actions ",e);
				// ignore this error which occurred 
			}
			throw e;
		}
	}
	
	public Action createAction(Props props) {
		try {
			return myCreateAction(props);
		}
		catch (RuntimeException e) {
			throw new RuntimeException("Error when creating action in channel "+getName(),e);
		}
	}
	
	private Action myCreateAction(Props props) {
		String classname=props.getString("class",null);
		if (classname==null)
			return null;
		if (classname.indexOf('.')<0)
			classname="org.kisst.gft.action."+classname;
		if (classname.startsWith(".")) // Prefix a class in the default package with a .
			classname=classname.substring(1);
		
		Class<?> clz;
		try {
			clz= gft.getSpecialClassLoader().loadClass(classname);
		} catch (ClassNotFoundException e) { throw new RuntimeException(e); }
		
		
		Constructor<?> c=ReflectionUtil.getConstructor(clz, new Class<?>[] {Channel.class, Props.class} );
		if (c!=null)
			return (Action) ReflectionUtil.createObject(c, new Object[] {this, props} );

		c=ReflectionUtil.getConstructor(clz, new Class<?>[] {GftContainer.class, Props.class} );
		if (c==null)
			return (Action) ReflectionUtil.createObject(classname);
		else
			return (Action) ReflectionUtil.createObject(c, new Object[] {gft, props} );
		
	}
}