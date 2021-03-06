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

package com.jaeksoft.searchlib.crawler.file.process;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import com.jaeksoft.searchlib.Logging;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.config.Config;
import com.jaeksoft.searchlib.crawler.common.database.FetchStatus;
import com.jaeksoft.searchlib.crawler.common.database.IndexStatus;
import com.jaeksoft.searchlib.crawler.common.database.ParserStatus;
import com.jaeksoft.searchlib.crawler.common.process.CrawlStatistics;
import com.jaeksoft.searchlib.crawler.common.process.CrawlStatus;
import com.jaeksoft.searchlib.crawler.common.process.CrawlThreadAbstract;
import com.jaeksoft.searchlib.crawler.file.database.FileCrawlQueue;
import com.jaeksoft.searchlib.crawler.file.database.FileInfo;
import com.jaeksoft.searchlib.crawler.file.database.FileItem;
import com.jaeksoft.searchlib.crawler.file.database.FileManager;
import com.jaeksoft.searchlib.crawler.file.database.FilePathItem;
import com.jaeksoft.searchlib.crawler.file.database.FileTypeEnum;
import com.jaeksoft.searchlib.crawler.file.spider.CrawlFile;

public class CrawlFileThread extends
		CrawlThreadAbstract<CrawlFileThread, CrawlFileMaster> {

	private FileItem currentFileItem;
	private FileManager fileManager;
	private long delayBetweenAccesses;
	private FilePathItem filePathItem;
	private long nextTimeTarget;

	protected CrawlFileThread(Config config, CrawlFileMaster crawlMaster,
			CrawlStatistics sessionStats, FilePathItem filePathItem)
			throws SearchLibException {
		super(config, crawlMaster, null);
		this.fileManager = config.getFileManager();
		currentStats = new CrawlStatistics(sessionStats);
		delayBetweenAccesses = filePathItem.getDelay();
		nextTimeTarget = 0;
		this.filePathItem = filePathItem;
	}

	private void sleepInterval(long max) {
		long c = System.currentTimeMillis();
		long ms = nextTimeTarget - c;
		nextTimeTarget = c + delayBetweenAccesses;
		if (ms < 0)
			return;
		if (ms > max)
			ms = max;
		sleepMs(ms);
	}

	@Override
	public void runner() throws Exception {

		CrawlFileMaster crawlMaster = (CrawlFileMaster) getThreadMaster();
		FileCrawlQueue crawlQueue = (FileCrawlQueue) crawlMaster
				.getCrawlQueue();

		FilePathItemIterator filePathIterator = new FilePathItemIterator(
				filePathItem);

		ItemIterator itemIterator;

		while ((itemIterator = filePathIterator.next()) != null) {

			if (isAborted() || crawlMaster.isAborted())
				break;

			FileInstanceAbstract fileInstance = itemIterator.getFileInstance();
			currentFileItem = fileManager.getNewFileItem(fileInstance);

			FileTypeEnum type = currentFileItem.getFileType();
			if (type == FileTypeEnum.directory) {
				if (!checkDirectory((ItemDirectoryIterator) itemIterator,
						crawlQueue))
					continue;
			} else if (type == FileTypeEnum.file) {
				if (!checkFile())
					continue;
			}

			CrawlFile crawl = crawl(fileInstance);
			if (crawl != null)
				crawlQueue.add(currentStats, crawl);

			setStatus(CrawlStatus.INDEXATION);
			crawlQueue.index(false);

		}
		crawlQueue.index(!crawlMaster.isRunning());
	}

	private CrawlFile crawl(FileInstanceAbstract fileInstance)
			throws SearchLibException {

		sleepInterval(60000);

		setStatus(CrawlStatus.CRAWL);
		currentStats.incUrlCount();

		CrawlFile crawl = new CrawlFile(fileInstance, currentFileItem,
				getConfig(), currentStats, fileManager.getFileItemFieldEnum());

		// Fetch started
		currentStats.incFetchedCount();

		crawl.download();

		if (currentFileItem.getFetchStatus() == FetchStatus.FETCHED
				&& currentFileItem.getParserStatus() == ParserStatus.PARSED
				&& currentFileItem.getIndexStatus() != IndexStatus.META_NOINDEX) {
			crawl.getFileItem().setIndexStatus(IndexStatus.INDEXED);
			currentFileItem.setIndexStatus(IndexStatus.INDEXED);
			currentStats.incParsedCount();
		} else
			currentStats.incIgnoredCount();

		return crawl;
	}

	final private void smartDelete(FileCrawlQueue crawlQueue, FileInfo fileInfo)
			throws SearchLibException {
		crawlQueue.delete(currentStats, fileInfo.getUri());
		if (fileInfo.getFileType() != FileTypeEnum.directory)
			return;
		HashMap<String, FileInfo> indexFileMap = new HashMap<String, FileInfo>();
		try {
			fileManager.getFileInfoList(new URI(fileInfo.getUri()),
					indexFileMap);
			for (FileInfo fi : indexFileMap.values())
				smartDelete(crawlQueue, fi);
		} catch (UnsupportedEncodingException e) {
			Logging.warn(e);
		} catch (URISyntaxException e) {
			Logging.warn(e);
		}
	}

	private boolean checkDirectory(ItemDirectoryIterator itemDirectory,
			FileCrawlQueue crawlQueue) throws UnsupportedEncodingException,
			SearchLibException, URISyntaxException {

		// Load directory from Index
		FileInstanceAbstract fileInstance = itemDirectory.getFileInstance();
		FilePathItem filePathItem = fileInstance.getFilePathItem();
		HashMap<String, FileInfo> indexFileMap = new HashMap<String, FileInfo>();
		fileManager.getFileInfoList(fileInstance.getURI(), indexFileMap);

		// If the filePathItem does not support subdir
		if (!filePathItem.isWithSubDir())
			for (FileInfo fileInfo : indexFileMap.values())
				if (fileInfo.getFileType() == FileTypeEnum.directory)
					smartDelete(crawlQueue, fileInfo);

		// Remove existing files from the map
		FileInstanceAbstract[] files = itemDirectory.getFiles();
		if (files != null)
			for (FileInstanceAbstract file : files)
				indexFileMap.remove(file.getURI().toASCIIString());

		// The file that remain in the map can be removed
		if (indexFileMap.size() > 0)
			for (FileInfo fileInfo : indexFileMap.values())
				smartDelete(crawlQueue, fileInfo);

		return checkFile();
	}

	private boolean checkFile() throws UnsupportedEncodingException,
			SearchLibException, URISyntaxException {
		FileInfo oldFileInfo = fileManager
				.getFileInfo(currentFileItem.getUri());
		// The file is a new file
		if (oldFileInfo == null) {
			return true;
		}
		// The file has been modified
		if (oldFileInfo.isNewCrawlNeeded(currentFileItem))
			return true;
		// The file has not changed, we don't need to craw it
		currentStats.incIgnoredCount();
		return false;
	}

	public FileItem getCurrentFileItem() {
		synchronized (this) {
			return currentFileItem;
		}
	}

	public void setCurrentFileItem(FileItem item) {
		synchronized (this) {
			currentFileItem = item;
		}
	}

	@Override
	public String getCurrentInfo() {
		if (currentFileItem != null)
			return currentFileItem.getDirectory();
		return "";
	}

}