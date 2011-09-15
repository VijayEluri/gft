package org.kisst.gft.action;

import org.kisst.gft.filetransfer.FileTransferTask;
import org.kisst.gft.ssh.SshHost;
import org.kisst.gft.task.Task;

public class FixPermissions implements Action {

	public boolean safeToRetry() { return true; }

	public Object execute(Task task) {
		FileTransferTask ft= (FileTransferTask) task;
		String destdir=ft.destpath.substring(0,ft.destpath.lastIndexOf('/'));

		SshHost dest = (SshHost) ft.channel.dest;
		String s=dest.call("system dspaut \"obj('"+destdir+"/')\"");
		int pos=s.indexOf("Lijst van machtigingen");
		if (pos<=0)
			throw new RuntimeException("Kan geen lijst van machtingen vinden voor directory "+destdir);
		pos=s.indexOf(":", pos);
		int pos2 = s.indexOf("\n",pos);
		if (pos<=0 || pos2<=0 || pos>pos2)
			throw new RuntimeException("Problem parsing dspaut output: "+s);
		String autlist=s.substring(pos+1, pos2).trim();
		dest.call("system chgaut \"obj('"+ft.destpath+"') autl("+autlist+")\"");
		return null;
	}

}
