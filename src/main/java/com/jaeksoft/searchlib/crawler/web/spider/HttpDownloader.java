/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2008-2012 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of OpenSearchServer.
 *
 * OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.crawler.web.spider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;

import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.crawler.web.database.CookieItem;
import com.jaeksoft.searchlib.crawler.web.database.CredentialItem;

public class HttpDownloader extends HttpAbstract {

	public HttpDownloader(String userAgent, boolean bFollowRedirect,
			ProxyHandler proxyHandler) {
		super(userAgent, bFollowRedirect, proxyHandler);
	}

	private void addHeader(HttpUriRequest httpUriRequest,
			List<Header> additionalHeaders) {
		if (additionalHeaders == null)
			return;
		for (Header header : additionalHeaders)
			httpUriRequest.addHeader(header);
	}

	private DownloadItem getDownloadItem(URI uri) throws IllegalStateException,
			IOException, SearchLibException {
		DownloadItem downloadItem = new DownloadItem(uri);
		downloadItem.setRedirectLocation(getRedirectLocation());
		downloadItem.setContentLength(getContentLength());
		downloadItem.setLastModified(getLastModified());
		downloadItem
				.setContentDispositionFilename(getContentDispositionFilename());
		downloadItem.setContentBaseType(getContentBaseType());
		downloadItem.setContentEncoding(getContentEncoding());
		downloadItem.setContentLocation(getContentLocation());
		downloadItem.setContentTypeCharset(getContentTypeCharset());
		downloadItem.setStatusCode(getStatusCode());
		downloadItem.setContentInputStream(getContent());
		downloadItem.setHeaders(getHeaders());
		return downloadItem;
	}

	public DownloadItem get(URI uri, CredentialItem credentialItem,
			List<Header> additionalHeaders, List<CookieItem> cookies)
			throws ClientProtocolException, IOException, IllegalStateException,
			SearchLibException, URISyntaxException {
		synchronized (this) {
			reset();
			HttpGet httpGet = new HttpGet(uri);
			addHeader(httpGet, additionalHeaders);
			execute(httpGet, credentialItem, cookies);
			return getDownloadItem(uri);
		}
	}

	public DownloadItem get(URI uri, CredentialItem credentialItem)
			throws ClientProtocolException, IOException, IllegalStateException,
			SearchLibException, URISyntaxException {
		return get(uri, credentialItem, null, null);
	}

	public DownloadItem head(URI uri, CredentialItem credentialItem,
			List<Header> additionalHeaders, List<CookieItem> cookies)
			throws ClientProtocolException, IOException, IllegalStateException,
			SearchLibException, URISyntaxException {
		synchronized (this) {
			reset();
			HttpHead httpHead = new HttpHead(uri);
			addHeader(httpHead, additionalHeaders);
			execute(httpHead, credentialItem, cookies);
			return getDownloadItem(uri);
		}
	}

	public DownloadItem head(URI uri, CredentialItem credentialItem)
			throws ClientProtocolException, IOException, IllegalStateException,
			SearchLibException, URISyntaxException {
		return head(uri, credentialItem, null, null);
	}

	public DownloadItem post(URI uri, CredentialItem credentialItem,
			List<CookieItem> cookies, HttpEntity entity)
			throws ClientProtocolException, IOException, IllegalStateException,
			SearchLibException, URISyntaxException {
		synchronized (this) {
			reset();
			HttpPost httpPost = new HttpPost(uri);
			if (entity != null)
				httpPost.setEntity(entity);
			execute(httpPost, credentialItem, cookies);
			return getDownloadItem(uri);
		}
	}

	public DownloadItem delete(URI uri, CredentialItem credentialItem,
			List<CookieItem> cookies) throws ClientProtocolException,
			IOException, IllegalStateException, SearchLibException,
			URISyntaxException {
		synchronized (this) {
			reset();
			HttpDelete httpDelete = new HttpDelete(uri);
			execute(httpDelete, credentialItem, cookies);
			return getDownloadItem(uri);
		}
	}

	public DownloadItem put(URI uri, CredentialItem credentialItem,
			List<CookieItem> cookies, HttpEntity entity)
			throws ClientProtocolException, IOException, IllegalStateException,
			SearchLibException, URISyntaxException {
		synchronized (this) {
			reset();
			HttpPut httpPut = new HttpPut(uri);
			if (entity != null)
				httpPut.setEntity(entity);
			execute(httpPut, credentialItem, cookies);
			return getDownloadItem(uri);
		}

	}
}
