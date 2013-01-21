package org.kisst.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;

public class JarLoader {
	public static class ModuleInfo {
		public final File file;
		public final String mainClassname;
		public final String version;
		public File getFile() { return file;}
		public String getVersion() { return version; }
		public String getMainClassname() { return mainClassname; }
		public ModuleInfo(File f) {
			this.file=f;
			String tmpversion;
			try {
				URL url=new URL("jar:"+f.toURL()+"!/META-INF/MANIFEST.MF");
				Manifest manifest = new Manifest(url.openStream());
				this.mainClassname =  manifest.getMainAttributes().getValue("Main-Class");
			} catch (IOException e) { throw new RuntimeException("Could not find module Main-Class in Manifest of "+f.getName(),e);}
			try {
				URL url=new URL("jar:"+f.toURL()+"!/version.properties");
				tmpversion = FileUtil.loadString(new InputStreamReader(url.openStream())).trim();
			}
			catch (Exception e) {
				e.printStackTrace();
				tmpversion=null;
			}
			this.version=tmpversion;
		}
	}
	
	private final File dir;
	private final ArrayList<ModuleInfo> modules=new ArrayList<ModuleInfo>();
	private final URLClassLoader loader; 

	public JarLoader(String filename) {
		this.dir=new File(filename);
		if (! dir.isDirectory())
			throw new IllegalArgumentException(filename+" should be a directory");
		for (File f:dir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(".jar"))
				modules.add(new ModuleInfo(f));
		}
		int i=0;
		URL[] urls = new URL[modules.size()];
		for (ModuleInfo m: modules) {
			try {
				urls[i++]=m.file.toURL();
			} 
			catch (MalformedURLException e) { throw new RuntimeException(e); }
		}
		loader = new URLClassLoader(urls);
	}
	
	public List<ModuleInfo> getModuleInfo() { return Collections.unmodifiableList(modules); }
	public ClassLoader getClassLoader() { return loader; }
	
	public Class<?> getClass(String name) {
		try {
			return Class.forName(name, true, loader);
		} 
		catch (ClassNotFoundException e) { throw new RuntimeException(e); }
	}
	
	public List<Class<?>> getMainClasses() {
		ArrayList<Class<?>> result=new ArrayList<Class<?>>();
		for (ModuleInfo m: modules) {
			if (m.mainClassname!=null) {
				Class<?> c = getClass(m.mainClassname);
				result.add(c);
				System.out.println("Found "+c.getSimpleName()+"\tfrom file "+m.file+"\tversion "+m.version);
			}
		}
		return result;
	}
}
