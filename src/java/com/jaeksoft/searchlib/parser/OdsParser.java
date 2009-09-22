/**   
 * License Agreement for Jaeksoft OpenSearchServer
 *
 * Copyright (C) 2008-2009 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of Jaeksoft OpenSearchServer.
 *
 * Jaeksoft OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Jaeksoft OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Jaeksoft OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.parser;

import java.io.IOException;

import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.office.OdfOfficeSpreadsheet;
import org.w3c.dom.Document;

/**
 * 
 * @author Emmanuel Gosse (philCube)
 * 
 */
public class OdsParser extends OOParser {
	StringBuffer textBuffer;

	public OdsParser() {
		super();
	}

	@Override
	protected void parseContent(LimitInputStream inputStream) {
		try {
			super.parseContent(inputStream);

			// Load file
			OdfSpreadsheetDocument odf = (OdfSpreadsheetDocument) OdfSpreadsheetDocument
					.loadDocument(inputStream);

			// get root of all content of a text document
			OdfOfficeSpreadsheet officeText = odf.getContentRoot();

			scanNodes(officeText.getChildNodes(), ParserFieldEnum.content);

			Document docOwner = officeText.getOwnerDocument();
			if (docOwner != null)
				scanNodes(docOwner.getChildNodes(), ParserFieldEnum.author);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void parseContent(LimitReader reader) throws IOException {
		throw new IOException("Unsupported");
	}

}