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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.kisst.cfg4j.CompositeSetting;
import org.kisst.cfg4j.DefaultSpecification;
import org.kisst.cfg4j.IntSetting;
import org.kisst.cfg4j.LongSetting;
import org.kisst.cfg4j.StringSetting;
import org.kisst.props4j.Props;

public class HttpCaller {

    public static class Settings extends CompositeSetting {

        public final StringSetting host = new StringSetting(this, "host");
        public final LongSetting closeIdleConnections = new LongSetting(this, "closeIdleConnections", -1);
        public final IntSetting timeout = new IntSetting(this, "timeout", 30000);

        // public final StringSetting urlPostfix = new StringSetting(this, "urlPostfix", null);

        public Settings(CompositeSetting parent, String name, DefaultSpecification... args) {
            super(parent, name, args);
        }
    }

    public static final Settings settings = new Settings(null, null);

    private static final PoolingHttpClientConnectionManager connmngr = new PoolingHttpClientConnectionManager();
    private final IdleConnectionMonitorThread idleThread = new IdleConnectionMonitorThread(connmngr);//can not be static because multiple classes use this, so there are multiple instances

	private final static CredentialsProvider credsProvider = new BasicCredentialsProvider();
    private final static CloseableHttpClient client = HttpClients.custom()
    	.setDefaultCredentialsProvider(credsProvider)
    	.setConnectionManager(connmngr)
    	.build();

    {
        idleThread.setDaemon(true);
        idleThread.start();
    }
    
    protected final Props props;
    private final long closeIdleConnections;
    protected final HttpHost host;
    private final int timeout;

    // private final String urlPostfix;

    public HttpCaller(HttpHostMap hostMap, Props props) {
        this(hostMap, props, settings);
    }

    public HttpCaller(HttpHostMap hostMap, Props props, Settings settings) {
        this.props = props;
        closeIdleConnections = settings.closeIdleConnections.get(props);

        String hostname = settings.host.get(props);
        // if (hostname==null)
        // throw new RuntimeException("host config parameter should be set");
        host = hostMap.getHttpHost(hostname.trim());
        timeout = settings.timeout.get(props);
        // urlPostfix=settings.urlPostfix.get(props);
        Credentials credentials = host.getCredentials();
		if (credentials!=null) {
	        AuthScope scope;
        	if (credentials instanceof NTCredentials) 
        		scope= new AuthScope(getHostFromUrl(host.url), host.port, AuthScope.ANY_REALM, AuthSchemes.NTLM);
        	else
        		scope=new AuthScope(getHostFromUrl(host.url), host.port, AuthScope.ANY_REALM, AuthSchemes.BASIC);
    		credsProvider.setCredentials(scope, credentials);
        }
    }

    public String getCompleteUrl(String url) {
        return host.url + url;
    } // TODO: make smarter with / and ? handling

    public String httpGet(String url) {
        HttpGet method = new HttpGet(getCompleteUrl(url));
        return httpCall(method);
    }

    public String httpPost(String url, String body) {
        HttpPost method = new HttpPost(getCompleteUrl(url));
        method.setEntity(new StringEntity(body, ContentType.create("text/xml", "UTF-8")));
        return httpCall(method);
    }

    private String httpCall(final HttpRequestBase method) {
        method.setConfig(RequestConfig.custom().setSocketTimeout(timeout).build());// setStaleConnectionCheckEnabled()?
        try {
            if (closeIdleConnections >= 0) { // Hack because often some idle connections were closed which resulted in 401 errors
                connmngr.closeIdleConnections(closeIdleConnections, TimeUnit.SECONDS);
            }
            CloseableHttpResponse response = client.execute(method);
            byte[] responseBody = new byte[(int) response.getEntity().getContentLength()];
            response.getEntity().getContent().read(responseBody);
            String result = new String(responseBody, "UTF-8");
            if (response.getStatusLine().getStatusCode() >= 300) {
                throw new RuntimeException("HTTP call returned " + response.getStatusLine().getStatusCode() + "\n" + result);
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            method.releaseConnection(); // TODO: what if connection not yet borrowed?
        }
    }

    private String getHostFromUrl(String url) {
        String result = url;
        if (url.contains("http://")) {
            result = result.replaceAll("http://", "");
        }
        if (result.contains("/")) {
            result = result.substring(0, result.indexOf("/"));
        }

        return result;
    }
}
