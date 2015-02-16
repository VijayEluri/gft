package org.kisst.gft.filetransfer;

import java.io.PrintWriter;

import org.kisst.gft.GftContainer;
import org.kisst.gft.RetryableException;
import org.kisst.gft.TaskStarter;
import org.kisst.gft.action.Action;
import org.kisst.gft.action.ActionList;
import org.kisst.gft.admin.WritesHtml;
import org.kisst.gft.filetransfer.action.DestinationFile;
import org.kisst.gft.filetransfer.action.SourceFile;
import org.kisst.gft.task.BasicTaskDefinition;
import org.kisst.gft.task.Task;
import org.kisst.props4j.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Channel extends BasicTaskDefinition  implements SourceFile, DestinationFile, WritesHtml, TaskStarter.JmsTaskCreator {
	final static Logger logger=LoggerFactory.getLogger(Channel.class); 

	private final FileLocation src; 
	private final FileLocation dest; 
	//public final SshFileServer src;
	//public final SshFileServer dest;
	//public final String srcdir;
	//public final String destdir;
	public final String mode;
	private final Action flow;

	public Channel(GftContainer gft, Props props) {
		super(gft, props);
		this.flow= ActionList.createAction(this, null);
		this.src=new FileLocation(gft.getFileServer(props.getString("src.host")), props.getString("src.dir",  ""));
		this.dest=new FileLocation(gft.getFileServer(props.getString("dest.host")), props.getString("dest.dir",  ""));

		this.mode=props.getString("mode", "push");
		if (!("pull".equals(mode) || "push".equals(mode)))
			throw new RuntimeException("mode should be push or pull, not "+mode);
		
	}

	@Override public FileLocation getSourceFile() { return src; }
	@Override public FileLocation getDestinationFile() { return dest; }

	@Override public Action getFlow() { return this.flow;} 
	public String getSrcDescription() {	return src.getShortString(); } // TODO:remove
	public String getDestDescription() { return dest.getShortString(); }

	public String toString() { return this.getClass().getSimpleName()+"("+name+" from "+getSrcDescription()+" to "+getDestDescription()+")";}
	public void checkSystemsAvailable(FileTransferTask ft) {
		if (! src.getFileServer().isAvailable())
			throw new RetryableException("Source system "+src.getFileServer()+" is not available to transfer file "+ft.filename+" for channel "+name);
		if (! dest.getFileServer().isAvailable())
			throw new RetryableException("Destination system "+dest.getFileServer()+" is not available to transfer file "+ft.filename+" for channel "+name);
	}

	@Override public void run(Task task) {
		FileTransferTask ft= (FileTransferTask) task;
		checkSystemsAvailable(ft);
		super.run(task);
	}
	
	
	@Override protected String getLogDetails(Task task) {
		if (task instanceof FileTransferTask) {
			FileTransferTask ft = (FileTransferTask) task;
			return  "bestand: "+ft.filename+ ", van: "+ft.getSourceFile()+" naar: "+ft.getDestinationFile();
		}
		else
			return task.toString();
	}
	
	@Override public void writeHtml(PrintWriter out) {
		writeHtmlHeader(out);
		out.println("<h2>Directories</h2>");
		out.println("<ul>");
		out.println("<li>FROM: <a href=\"/dir/"+src.getShortString()+"\">"+src.getShortString()+"</a>");
		out.println("<li>TO:   <a href=\"/dir/"+dest.getShortString()+"\">"+dest.getShortString()+"</a>");
		out.println("</ul>");
	
		writeHtmlFooter(out);
	}
}
