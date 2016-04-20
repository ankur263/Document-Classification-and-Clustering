package com.features;

import java.util.List;

import com.extraction.DocInfo;
import com.extraction.MainFile;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.Triple;

/*
 * Gets all Name entities from document using stanford named entity recognizer api
 */
public class NameEntityRecognizer {	

	public void analyze(String[] args) {
		try {
			String serializedClassifier=args[0];
			AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifier(serializedClassifier);

			DocInfo doc;
			Integer wordcnt;

			String document=new String(MainFile.currentDoc);

			//removes different encoding other than UTF-8
			document = document.replaceAll( "([\\ud800-\\udbff\\udc00-\\udfff])", "");
			doc=MainFile.docNE.get(MainFile.currFilename);
			List<Triple<String,Integer,Integer>> triples = classifier.classifyToCharacterOffsets(document);
			for (Triple<String,Integer,Integer> trip : triples) {
				//System.out.printf("%s over character offsets [%d, %d) in sentence.%n",trip.first(), trip.second(), trip.third);
				//System.out.println(document.substring(trip.second(), trip.third));
				if(trip.first()!=null)
				{
					wordcnt=doc.wordCount.get(trip.first());
					doc.totalWords=doc.totalWords+1;
					if(wordcnt==null)
					{
						doc.wordCount.put(trip.first(), 1);
						if(!(MainFile.uniqNE.contains(trip.first())))
						{
							MainFile.uniqNE.add(trip.first());
							MainFile.totalFeatures=MainFile.totalFeatures+1;
						}
					}
					else
						doc.wordCount.put(trip.first(), wordcnt+1);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
