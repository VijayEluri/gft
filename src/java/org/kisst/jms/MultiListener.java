package org.kisst.jms;

import org.kisst.gft.admin.rest.Representable;
import org.kisst.props4j.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiListener implements Representable {
	private final static Logger logger=LoggerFactory.getLogger(MultiListener.class); 

	private final String name;
	private final Props props;
	public final JmsListener[] listeners;

	public MultiListener(JmsSystem system, MessageHandler handler, Props props, Object context) {
		this.name=props.getLocalName();
		this.props=props;
		int nrofThreads = props.getInt("nrofThreads",2);
		this.listeners =new JmsListener[nrofThreads];
		for (int i=0; i<nrofThreads; i++)
			listeners[i]=new JmsListener(system, handler, props, context);
	}
	
	public int getNrofListeners() { return listeners.length; }
	public int getNrofActiveListeners() {
		int count = 0;
		for (JmsListener l: listeners) {
			if (l.isActive())
				count++;
		}
		return count;
	}
	public boolean listening() { return listeners!=null; }
	public String getQueue() { return listeners[0].queue; }
	public String getErrorQueue() { return listeners[0].errorqueue; }
	public String getRetryQueue() { return listeners[0].retryqueue; }
	public String getRepresentation() { return props.toString(); }

	public void stop() {
		logger.info("Stopping MultiListener {}", name);
		for (JmsListener t:listeners)
			t.stop();
	}
	public void start()  {
		logger.info("Starting MultiListener {}", name);
		for (JmsListener t:listeners)
			t.start();
	}
}
