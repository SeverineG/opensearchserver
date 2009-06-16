/**   
 * License Agreement for Jaeksoft OpenSearchServer
 *
 * Copyright (C) 2008 Emmanuel Keller / Jaeksoft
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

package com.jaeksoft.searchlib.util;

import java.io.PrintWriter;
import java.util.Stack;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class XmlWriter {

	private TransformerHandler transformerHandler;

	private AttributesImpl elementAttributes;

	private Stack<String> startedElementStack;

	public XmlWriter(PrintWriter out, String encoding)
			throws TransformerConfigurationException, SAXException {
		StreamResult streamResult = new StreamResult(out);
		SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory
				.newInstance();
		transformerHandler = tf.newTransformerHandler();
		Transformer serializer = transformerHandler.getTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING, encoding);
		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformerHandler.setResult(streamResult);
		startedElementStack = new Stack<String>();
		transformerHandler.startDocument();
		elementAttributes = new AttributesImpl();
	}

	public void textNode(Object data) throws SAXException {
		String value = data.toString();
		int length = value.length();
		char[] chars = new char[length];
		value.getChars(0, length, chars, 0);
		transformerHandler.characters(chars, 0, length);
	}

	public void endDocument() throws SAXException {
		while (!startedElementStack.empty())
			endElement();
		transformerHandler.endDocument();
	}

	public void startElement(String name, String... attributes)
			throws SAXException {
		elementAttributes.clear();
		for (int i = 0; i < attributes.length; i++) {
			String attr = attributes[i];
			String value = attributes[++i];
			if (attr != null && value != null)
				elementAttributes.addAttribute("", "", attr, "CDATA", value);
		}
		startedElementStack.push(name);
		transformerHandler.startElement("", "", name, elementAttributes);

	}

	public void endElement() throws SAXException {
		transformerHandler.endElement("", "", startedElementStack.pop());
	}

}