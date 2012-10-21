/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2008-2011 Emmanuel Keller / Jaeksoft
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

package com.jaeksoft.searchlib.index;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.analysis.Analyzer;
import com.jaeksoft.searchlib.analysis.CompiledAnalyzer;
import com.jaeksoft.searchlib.analysis.LanguageEnum;
import com.jaeksoft.searchlib.analysis.TokenStream;
import com.jaeksoft.searchlib.index.osse.OsseIndex;
import com.jaeksoft.searchlib.index.osse.OsseLibrary;
import com.jaeksoft.searchlib.index.osse.OsseTransaction;
import com.jaeksoft.searchlib.request.SearchRequest;
import com.jaeksoft.searchlib.schema.FieldValueItem;
import com.jaeksoft.searchlib.schema.Schema;
import com.jaeksoft.searchlib.schema.SchemaField;
import com.jaeksoft.searchlib.schema.SchemaFieldList;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;

public class WriterNativeOSSE extends WriterAbstract {

	private OsseIndex index;

	protected WriterNativeOSSE(OsseIndex index, IndexConfig indexConfig) {
		super(indexConfig);
		this.index = index;
	}

	@Override
	public boolean deleteDocument(Schema schema, String uniqueField) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int deleteDocuments(Schema schema, Collection<String> uniqueFields) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void optimize() {

	}

	private void checkFieldCreation(OsseTransaction transaction,
			SchemaField schemaField) throws SearchLibException {
		String fieldName = schemaField.getName();
		WString wFieldName = new WString(fieldName);
		Pointer indexField = OsseLibrary.INSTANCE.OSSCLib_Transact_GetField(
				transaction.getPointer(), wFieldName,
				transaction.getErrorPointer());
		if (indexField != null)
			return;
		int flag = 0;
		switch (schemaField.getTermVector()) {
		case YES:
			flag += OsseLibrary.OSSCLIB_FIELD_UI32FIELDFLAGS_VSM1;
			break;
		case POSITIONS_OFFSETS:
			flag = OsseLibrary.OSSCLIB_FIELD_UI32FIELDFLAGS_VSM1
					| OsseLibrary.OSSCLIB_FIELD_UI32FIELDFLAGS_OFFSET
					| OsseLibrary.OSSCLIB_FIELD_UI32FIELDFLAGS_POSITION;
			break;
		default:
			break;
		}
		if (OsseLibrary.INSTANCE.OSSCLib_Transact_CreateField(
				transaction.getPointer(), new WString(fieldName),
				OsseLibrary.OSSCLIB_FIELD_UI32FIELDTYPE_STRING, flag, null,
				transaction.getErrorPointer()) == null)
			transaction.throwError();
		System.out.println("FIELD CREATED: " + fieldName);
	}

	private void checkFieldDeletion(OsseTransaction transaction,
			String fieldName) throws SearchLibException {
		WString[] wTerms = new WString[] { new WString(fieldName) };
		if (OsseLibrary.INSTANCE.OSSCLib_Transact_DeleteFields(
				transaction.getPointer(), wTerms, 1,
				transaction.getErrorPointer()) != 1)
			transaction.throwError();
		System.out.println("FIELD DELETE: " + fieldName);
	}

	public void checkSchemaFieldList(SchemaFieldList schemaFieldList)
			throws SearchLibException {
		OsseTransaction transaction = null;
		try {
			transaction = new OsseTransaction(index);
			for (SchemaField schemaField : schemaFieldList)
				checkFieldCreation(transaction, schemaField);
			int nField = OsseLibrary.INSTANCE.OSSCLib_Index_GetListOfFields(
					index.getPointer(), null, 0, transaction.getErrorPointer());
			if (nField > 0) {
				List<String> toDelete = new ArrayList<String>(0);
				Pointer[] hFieldArray = new Pointer[nField];
				OsseLibrary.INSTANCE.OSSCLib_Index_GetListOfFields(
						index.getPointer(), hFieldArray, nField,
						transaction.getErrorPointer());
				for (Pointer hField : hFieldArray) {
					IntByReference fieldId = new IntByReference();
					IntByReference fieldType = new IntByReference();
					IntByReference fieldFlags = new IntByReference();
					Pointer hFieldName = OsseLibrary.INSTANCE
							.OSSCLib_Index_GetFieldNameAndProperties(
									index.getPointer(), hField, fieldId,
									fieldType, fieldFlags,
									transaction.getErrorPointer());
					String fieldName = hFieldName.getString(0, true);
					if (schemaFieldList.get(fieldName) == null)
						toDelete.add(fieldName);
					OsseLibrary.INSTANCE
							.OSSCLib_Index_GetFieldNameAndProperties_Free(hFieldName);

				}
				for (String fieldName : toDelete)
					checkFieldDeletion(transaction, fieldName);
			}
			transaction.commit();
		} finally {
			if (transaction != null)
				transaction.release();
		}
	}

	final private void updateTerms(OsseTransaction transaction,
			Pointer documentPtr, Pointer fieldPtr, WString[] terms, int length)
			throws SearchLibException {
		int res = OsseLibrary.INSTANCE
				.OSSCLib_Transact_Document_AddStringTerms(
						transaction.getPointer(), documentPtr, fieldPtr, terms,
						null, null, null, length, transaction.getErrorPointer());
		System.out.println("OSSCLib_Transact_Document_AddStringTerms returns "
				+ res + " / " + length);
		if (res != length)
			transaction.throwError();
	}

	final private void updateTerm(OsseTransaction transaction,
			Pointer documentPtr, Pointer fieldPtr, String value)
			throws SearchLibException {
		if (value == null || value.length() == 0)
			return;
		WString[] terms = new WString[] { new WString(value) };
		updateTerms(transaction, documentPtr, fieldPtr, terms, 1);
	}

	final private void updateTerms(OsseTransaction transaction,
			Pointer documentPtr, Pointer fieldPtr,
			CompiledAnalyzer compiledAnalyzer, String value)
			throws IOException, SearchLibException {
		StringReader stringReader = null;
		try {
			stringReader = new StringReader(value);
			TokenStream tokenStream = compiledAnalyzer
					.tokenStream(stringReader);
			WString[] terms = new WString[100];
			int length = 0;
			while (tokenStream.incrementToken()) {
				terms[length++] = new WString(tokenStream.getCurrentTerm());
				if (length == terms.length) {
					updateTerms(transaction, documentPtr, fieldPtr, terms,
							length);
					length = 0;
				}
			}
			if (length > 0)
				updateTerms(transaction, documentPtr, fieldPtr, terms, length);
		} finally {
			if (stringReader != null)
				IOUtils.closeQuietly(stringReader);
		}
	}

	@Override
	public boolean updateDocument(Schema schema, IndexDocument document)
			throws SearchLibException {
		OsseTransaction transaction = null;
		try {
			transaction = new OsseTransaction(index);
			Pointer documentPtr = OsseLibrary.INSTANCE
					.OSSCLib_Transact_Document_New(transaction.getPointer(),
							transaction.getErrorPointer());
			LanguageEnum lang = document.getLang();
			for (FieldContent fieldContent : document) {
				SchemaField schemaField = schema.getFieldList().get(
						fieldContent.getField());
				if (schemaField != null) {
					Analyzer analyzer = schema.getAnalyzer(schemaField, lang);
					CompiledAnalyzer compiledAnalyzer = null;
					if (analyzer != null)
						compiledAnalyzer = analyzer.getIndexAnalyzer();
					WString wFieldName = new WString(fieldContent.getField());
					Pointer fieldPtr = OsseLibrary.INSTANCE
							.OSSCLib_Transact_GetField(
									transaction.getPointer(), wFieldName,
									transaction.getErrorPointer());
					if (fieldPtr == null)
						transaction.throwError();
					for (FieldValueItem valueItem : fieldContent.getValues()) {
						String value = valueItem.getValue();
						if (compiledAnalyzer != null)
							updateTerms(transaction, documentPtr, fieldPtr,
									compiledAnalyzer, value);
						else
							updateTerm(transaction, documentPtr, fieldPtr,
									value);
					}
				}
			}
			transaction.commit();
		} catch (IOException e) {
			throw new SearchLibException(e);
		} finally {
			if (transaction != null)
				transaction.release();
		}
		return true;
	}

	@Override
	public int updateDocuments(Schema schema,
			Collection<IndexDocument> documents) throws SearchLibException {
		int i = 0;
		for (IndexDocument document : documents)
			if (updateDocument(schema, document))
				i++;
		return i;
	}

	@Override
	public int deleteDocuments(SearchRequest query) {
		throw new RuntimeException("Not yet implemented");

	}

}
