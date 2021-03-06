/**
Copyright 2008, 2009 Mark Hooijkaas

This file is part of the RelayConnector framework.

The RelayConnector framework is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

The RelayConnector framework is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with the RelayConnector framework.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.kisst.http4j;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.kisst.cfg4j.CompositeSetting;
import org.kisst.cfg4j.IntSetting;
import org.kisst.cfg4j.StringSetting;
import org.kisst.props4j.Props;
import org.kisst.util.CryptoUtil;

public class HttpHost {
	public static class Settings extends CompositeSetting {
		public final StringSetting url = new StringSetting(this, "url", null); // TODO: mandatory?; 
		public final IntSetting port = new IntSetting(this, "port", 80); // TODO: mandatory?; 
		public final StringSetting username = new StringSetting(this, "username", null);  
		public final StringSetting password = new StringSetting(this, "password", null);
		public final StringSetting encryptedPassword = new StringSetting(this, "encryptedPassword", null);
		public final StringSetting ntlmhost   = new StringSetting(this, "ntlmhost", null); // TODO: is this necessary 
		public final StringSetting ntlmdomain = new StringSetting(this, "ntlmdomain", null); 

		public Settings(CompositeSetting parent, String name) { super(parent, name); }
	}
	
	public final String url; 
	public final int port; 
	public final String username;  
	public final String password;
	public final String ntlmhost; 
	public final String ntlmdomain; 

	private static Settings unconnectedSettings = new Settings(null, null);
	
	public HttpHost(Props props) { this(unconnectedSettings, props); }

	public HttpHost(Settings settings, Props props) {
		url=settings.url.get(props);
		port=settings.port.get(props);
		username=settings.username.get(props);
		if (username==null)
			password=null;
		else if (settings.password.get(props)!=null)
			password=settings.password.get(props);
		else {
			String encryptedPassword = settings.encryptedPassword.get(props);
			if (encryptedPassword==null)
				throw new RuntimeException("Could not find a password or encrypted Password for http "+props.getFullName()+" with url "+url+" and with username "+username);
			password=CryptoUtil.decrypt(encryptedPassword); 
		}
		ntlmhost=settings.ntlmhost.get(props);
		ntlmdomain=settings.ntlmdomain.get(props);
	}
	
	public String toString() { return "HttpHost("+username+","+url+")"; }
	public  Credentials getCredentials(){
		if (username==null)
			return null;
		else if (ntlmdomain==null)
			return new UsernamePasswordCredentials(username, password);
		else
			return new NTCredentials(username, password, ntlmhost, ntlmdomain);
	}
}