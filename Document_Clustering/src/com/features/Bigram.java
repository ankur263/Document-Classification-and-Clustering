/*
 * This package contains all files which extracts different features from documents,  pre-processes
 * those features before storing and removes irrelevant features
 */
package com.features;

/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;

import com.extraction.*;

/*
 * Gets all two consecutive words from document based on related descriptor file and discards bad bigrams 
 * like containing stopwords and stem at last before storing
 */
public class Bigram {

	Byte present;
	String word;
	Integer wordcnt,doccnt;
	Pattern pattern;
	Matcher matcher;
	boolean matches;
	int maxLength=20;
	HashMap<String, Byte> tempCheck;
	DocInfo doc;

	public Bigram()
	{
		pattern = Pattern.compile("[A-Za-z]+");
		tempCheck=new HashMap<String, Byte>(1000);
	}

	public void processAnnotations(CAS aCAS, Type aAnnotType, PrintStream aOut) {
		// get iterator over annotations
		FSIterator iter = aCAS.getAnnotationIndex(aAnnotType).iterator();

		doc=MainFile.docBigrams.get(MainFile.currFilename);
		// iterate
		while (iter.isValid()) {
			FeatureStructure fs = iter.get();
			word=getFS(fs, aCAS, 0, aOut);
			if(word!=null)
			{
				present=tempCheck.get(word);
				if(present!=null)
				{
					doc.totalWords=doc.totalWords+1;
					wordcnt=doc.wordCount.get(word);
					if(wordcnt==null)
					{
						doc.wordCount.put(word, 2);
						doccnt=MainFile.uniqBigrams.get(word);
						if(doccnt==null)
						{
							MainFile.uniqBigrams.put(word, 2);
							MainFile.totalFeatures=MainFile.totalFeatures+1;
						}
						else
						{
							doccnt=doccnt+1;
							MainFile.uniqBigrams.put(word, doccnt);
						}    			  
					}
					else
					{
						wordcnt=wordcnt+1;
						doc.wordCount.put(word, wordcnt);
					}
				}
				else
				{
					tempCheck.put(word, (byte)1);
				}
			}
			iter.moveToNext();
		}
	}

	public String getFS(FeatureStructure aFS, CAS aCAS, int aNestingLevel, PrintStream aOut) {
		Type stringType = aCAS.getTypeSystem().getType(CAS.TYPE_NAME_STRING);
		String[] part;
		int g;

		if(aFS.getType().getName().equalsIgnoreCase("uima.tcas.DocumentAnnotation"))
			return null;


		if (aFS instanceof AnnotationFS) {
			AnnotationFS annot = (AnnotationFS) aFS;
			String st = new String(annot.getCoveredText());
			//	if(st.contains("-\n"))
			//		st=st.replaceAll("-\n", "");

			part=st.split("\\s+");
			if(part.length<=1)
				return null;

			matcher = pattern.matcher(part[0]);
			matches = matcher.matches();
			if(matches!=true)
			{
				if(part[0].charAt(0)=='"' || part[0].charAt(0)=='\'' || part[0].charAt(0)=='(' || part[0].charAt(0)=='{' || part[0].charAt(0)=='[')
					part[0]=part[0].substring(1, part[0].length());
				else
					return null;

				if(part[0]==null)
					return null;

				matcher = pattern.matcher(part[0]);
				matches = matcher.matches();
				if(matches!=true)
					return null;
			}

			matcher = pattern.matcher(part[1]);
			matches = matcher.matches();
			if(matches!=true)
			{
				for (g = 0; g<part[1].length(); g++){
					if(!(Character.isLetter(part[1].charAt(g))))
						break;
				}
				for(int t=g;t<part[1].length();t++)
				{
					if(Character.isLetter(part[1].charAt(t)))
						return null;
				}
				part[1]=part[1].substring(0, g);
				if(part[1]==null)
					return null;
				matcher = pattern.matcher(part[1]);
				matches = matcher.matches();
				if(matches!=true)
					return null;
			}

			if(StopWords.sw.get(part[0])!=null)
				return null;
			else if(part[0].length()>=maxLength)
				return null;

			if(StopWords.sw.get(part[1])!=null)
				return null;
			else if(part[1].length()>=maxLength)
				return null;

			st=part[0]+" "+part[1];
			st=MainFile.myStem.stem(st);
			return st;
		}
		return null;
	}

	/**
	 * Main program for testing this class. There are two required arguments - the path to the XML
	 * descriptor for the TAE to run and an input file. Additional arguments are Type or Feature names
	 * to be included in the ResultSpecification passed to the TAE.
	 */
	public void analyze(String[] args) {
		try {
			File taeDescriptor = new File(args[0]);

			// read contents of file
			String document =new String(MainFile.currentDoc.toLowerCase());

			int p=0;
			int gram=2;			//2 for bigram
			for(int k=0;k<gram;k++)		
			{
				p=0;
				if(k!=0)
				{
					while(p<document.length() && (document.charAt(p)!=' ' && document.charAt(p)!='\n'))
						p++;
					if(p<document.length())
						document=document.substring(p+1);
					else
						document=null;
				}

				if(document!=null)
				{

					// get Resource Specifier from XML file or TEAR
					XMLInputSource in = new XMLInputSource(taeDescriptor);
					ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);

					// create Analysis Engine
					AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(specifier);
					// create a CAS
					CAS cas = ae.newCAS();

					// build ResultSpec if Type and Feature names were specified on commandline
					ResultSpecification resultSpec = null;
					if (args.length > 2) {
						resultSpec = ae.createResultSpecification(cas.getTypeSystem());
						for (int i = 2; i < args.length; i++) {
							if (args[i].indexOf(':') > 0) // feature name
							{
								resultSpec.addResultFeature(args[i]);
							} else {
								resultSpec.addResultType(args[i], false);
							}
						}
					}

					// send doc through the AE
					cas.setDocumentText(document);
					ae.process(cas, resultSpec);

					Type annotationType = cas.getTypeSystem().getType(CAS.TYPE_NAME_ANNOTATION);
					processAnnotations(cas, annotationType, System.out);

					// destroy AE
					ae.destroy();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

