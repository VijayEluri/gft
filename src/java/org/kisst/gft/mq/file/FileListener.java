package org.kisst.gft.mq.file;

import java.util.List;

import org.kisst.cfg4j.Props;
import org.kisst.gft.mq.LockedBySomeoneElseException;
import org.kisst.gft.mq.MessageHandler;
import org.kisst.gft.mq.MqMessage;
import org.kisst.gft.mq.QueueListener;

public class FileListener implements QueueListener, Runnable {
	private final FileQueueSystem system;
	private final Props props;
	private final FileQueue queue;
	private MessageHandler handler;
	
	FileListener(FileQueueSystem system, Props props) {
		this.system=system;
		this.props=props;
		this.queue=system.getQueue(props.getString("queue"));
	}
	@Override public String toString() { return "FileListener("+queue+")"; }
	public FileQueueSystem getSystem() { return system; }

	public void pollTillEmpty() {
		List<MqMessage> messages;
		do {
			messages= queue.getAllMessages();
			for (MqMessage msg:messages) {
				handle(msg);
				if (! running)
					return;
			}
		}
		while (messages!=null && messages.size()>0);
	}

	
	private void handle(MqMessage msg) {
		try {
			msg.lock();
		} catch (LockedBySomeoneElseException e) {
			System.out.println("Could not lock "+msg);
			e.printStackTrace();
			return;
		}
		System.out.println(queue+" handling "+msg);
		handler.handle(msg);
		msg.done();
	}
	
	private boolean running=false;
	public void run() {
		long delay=props.getLong("delay",1000);
		running=true;
		while (running) {
			//System.out.println("polling");
			pollTillEmpty();
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}
	}

	public void stopListening() { running=false; }
	public void listen(MessageHandler handler) {
		this.handler=handler;
		new Thread(this).start();
	}
	
}
