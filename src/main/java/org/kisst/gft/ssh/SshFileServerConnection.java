package org.kisst.gft.ssh;

import com.jcraft.jsch.*;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import org.kisst.gft.filetransfer.FileCouldNotBeMovedException;
import org.kisst.gft.filetransfer.FileServerConnection;
import org.kisst.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedHashMap;
import java.util.Vector;


public class SshFileServerConnection implements FileServerConnection {
	private static final Logger logger = LoggerFactory.getLogger(SshFileServerConnection.class);
	private final Session session;
	private final ChannelSftp sftp;
	private final SshFileServer fileserver;
	private final SshHost host;

	public SshFileServerConnection(SshFileServer fileserver) {
		this.fileserver = fileserver;
		this.host=fileserver.getSshHost();
		session = Ssh.openSession(host);
		logger.info("Opening session on host: {}",host);
		try {
			sftp = (ChannelSftp) session.openChannel("sftp");
			sftp.connect();
		} catch (JSchException e) { throw new RuntimeException(e);}
	}
	public ChannelSftp getSftpChannel() { return sftp; }
	
	@Override
	public void close() {
		logger.info("Closing session on host: {}",host);
		sftp.disconnect();
		session.disconnect();
	}

	public boolean fileExists(String path) { 
		path=fileserver.unixPath(path);
		try {
			SftpATTRS result = sftp.lstat(path);
			return result != null;
		}
		catch (SftpException e) {
			if (e.id==2) // && e.getMessage().equals("SfsStatusCode.NoSuchFile")) {
				return false;
			throw new RuntimeException(e+" for file "+path,e); }
	}

	public void deleteFile(String path) { 
		path=fileserver.unixPath(path);
		try {
			sftp.rm(path);
		} catch (SftpException e) { throw new RuntimeException(e+" for file "+path,e); }
	}
	public long fileSize(String path) { return getFileAttributes(path).size; }
	public long lastModified(String path) { return getFileAttributes(path).modifyTimeMilliSecs; }
	public boolean isDirectory(String path) { return getFileAttributes(path).isDirectory; }
	public boolean isLocked(String path) { 
		path=fileserver.unixPath(path);
		try {
			sftp.rename(path, path);
			return false;
		}
		catch (SftpException e) { e.printStackTrace(); return true; } // TODO: check specific Exception
	}
	
	public FileAttributes getFileAttributes(String path) {
		path=fileserver.unixPath(path);
		try {
			SftpATTRS attr = sftp.lstat(path);
			return new FileAttributes(1000L*attr.getATime(), 1000L*attr.getMTime(), attr.isDir(), attr.getSize());
		} catch (SftpException e) { throw new RuntimeException(e+" for file "+path,e); }
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public LinkedHashMap<String, FileAttributes> getDirectoryEntries(String path) {
		try {
			path=fileserver.unixPath(path);
			logger.info("getting remote diretory: {}",path);
			Vector<LsEntry> vv = sftp.ls(path);
			logger.info("found {} entries",vv.size());
			LinkedHashMap<String,FileAttributes> result = new LinkedHashMap<String,FileAttributes>();
			for (LsEntry entry: vv) {
				logger.debug("found entry {} - {}",entry.getFilename(), entry.getLongname());
				SftpATTRS attr = entry.getAttrs();
                result.put(entry.getFilename(),
                	new FileAttributes(attr.getATime(), attr.getMTime(), attr.isDir(), attr.getSize()));
			}
			return result;
		} 
		catch (SftpException e) { throw new RuntimeException(e+" for directory "+path,e); }
	}

	public void move(String path, String newpath) {
		path=fileserver.unixPath(path);
		newpath=fileserver.unixPath(newpath);
		try {
			sftp.rename(path, newpath);
		}
		catch (SftpException e) { throw new FileCouldNotBeMovedException(path, e); }
	}

	public void getToLocalFile(String remotepath, String localpath) {
		remotepath=fileserver.unixPath(remotepath);
		try {
			logger.info("copy file from remote {} to local {}",remotepath,localpath);
			sftp.get(remotepath, localpath);
		} 
		catch (SftpException e) { throw new RuntimeException(e+" for file "+remotepath,e); }
	}

	public String getFileContentAsString(String remotepath) {
		remotepath=fileserver.unixPath(remotepath);
		InputStreamReader reader = null;
		try {
			logger.info("get file from remote {} ",remotepath);
			reader = new InputStreamReader(sftp.get(remotepath));
			return FileUtil.loadString(reader);
		} 
		catch (SftpException e) { throw new RuntimeException(e+" for file "+remotepath,e); }
		finally {
			if (reader!=null) {
				try {
					reader.close();
				} 
				catch (IOException e) { throw new RuntimeException(e);}
			}
		}
	}

	public void putStringAsFileContent(String remotepath, String content) {
		remotepath=fileserver.unixPath(remotepath);
		OutputStreamWriter writer = null;
		try {
			logger.info("put content to remote {} ",remotepath);
			writer = new OutputStreamWriter(sftp.put(remotepath));
			writer.write(content);
		} 
		catch (SftpException e) { throw new RuntimeException(e+" for file "+remotepath,e); }
		catch (IOException e) { throw new RuntimeException(e+" for file "+remotepath,e); }
		finally {
			if (writer!=null) {
				try {
					writer.close();
				} 
				catch (IOException e) { throw new RuntimeException(e+" for file "+remotepath,e);}
			}
		}
	}
	
	@Override
	public void putFromLocalFile(String localpath, String remotepath) {
		remotepath=fileserver.unixPath(remotepath);
		try {
			logger.info("copy file from local {} to remote {}",localpath,remotepath);
			sftp.put(localpath, remotepath);
		} 
		catch (SftpException e) { throw new RuntimeException(e+" for file "+remotepath,e); }
	}

}