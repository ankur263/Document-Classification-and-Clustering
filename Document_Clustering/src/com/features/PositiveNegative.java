package com.features;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.TreeSet;
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
 * counts the number of positive and negative words present in document
 */
public class PositiveNegative {

	TreeSet<String> positiveWords;
	TreeSet<String> negativeWords;
	String line;
	BufferedReader br;
	String word;
	Integer wordcnt,doccnt;
	Pattern pattern;
	Matcher matcher;
	boolean matches;
	int maxLength=20;
	Pair p;

	public PositiveNegative() {
		// TODO Auto-generated constructor stub
		try{
			positiveWords=new TreeSet<String>();
			negativeWords=new TreeSet<String>();
			br=new BufferedReader(new FileReader("positive.txt"));
			while ((line = br.readLine()) != null) {
				positiveWords.add(line);
			}
			br=new BufferedReader(new FileReader("negative.txt"));
			while ((line = br.readLine()) != null) {
				negativeWords.add(line);
			}
			pattern = Pattern.compile("[A-Za-z_]+");
		} catch(Exception e) {
			e.printStackTrace();
		}		
	}

	public void processAnnotations(CAS aCAS, Type aAnnotType, PrintStream aOut) {
		// get iterator over annotations
		FSIterator iter = aCAS.getAnnotationIndex(aAnnotType).iterator();

		p=MainFile.docPositiveNegative.get(MainFile.currFilename);

		// iterate
		while (iter.isValid()) {
			FeatureStructure fs = iter.get();
			word=getFS(fs, aCAS, 0, aOut);
			if(word!=null)
			{
				if(positiveWords.contains(word))
					p.setX(p.getX()+1);
				else if(negativeWords.contains(word))
					p.setY(p.getY()+1);
			}
			iter.moveToNext();
		}
	}

	public String getFS(FeatureStructure aFS, CAS aCAS, int aNestingLevel, PrintStream aOut) {
		Type stringType = aCAS.getTypeSystem().getType(CAS.TYPE_NAME_STRING);

		if(aFS.getType().getName().equalsIgnoreCase("uima.tcas.DocumentAnnotation"))
			return null;


		if (aFS instanceof AnnotationFS) {
			AnnotationFS annot = (AnnotationFS) aFS;
			String st = new String(annot.getCoveredText());
			//if(st.contains("-\n"))
			//	st=st.replace("-\n", "");

			matcher = pattern.matcher(st);
			matches = matcher.matches();
			if(matches!=true)
				return null;

			if(StopWords.sw.get(st)==null)
			{
				if(st.length()>=maxLength)
					return null;
				//st=MainFile.myStem.stem(st);
				return st;
			}
			else
				return null;
		}
		return null;
	}

	public void analyze(String[] args) {
		try {
			File taeDescriptor = new File(args[0]);

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

			// read contents of file
			String document =new String(MainFile.currentDoc.toLowerCase());

			if(document!=null)
			{
				// send doc through the AE
				cas.setDocumentText(document);
				ae.process(cas, resultSpec);

				Type annotationType = cas.getTypeSystem().getType(CAS.TYPE_NAME_ANNOTATION);
				processAnnotations(cas, annotationType, System.out);
			}

			// destroy AE
			ae.destroy();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	public void deleteData()
	{
		positiveWords.clear();
		negativeWords.clear();
	}
}
