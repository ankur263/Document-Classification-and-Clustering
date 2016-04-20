package com.features;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import com.extraction.DocInfo;
import com.extraction.MainFile;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/*
 * Gets all POS Tag from document using stanford POS Tagger
 */
public class POS {

	public void analyze(String[] args) {
		try {
			DocInfo doc;
			Integer wordcnt;
			//System.out.println(args[1]);
			MaxentTagger tagger = new MaxentTagger(args[0]);
			TokenizerFactory<CoreLabel> ptbTokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(),
					"untokenizable=noneKeep");		
			BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(args[1])));
			DocumentPreprocessor documentPreprocessor = new DocumentPreprocessor(r);
			documentPreprocessor.setTokenizerFactory(ptbTokenizerFactory);
			doc=MainFile.docPOS.get(MainFile.currFilename);
			for (List<HasWord> sentence : documentPreprocessor) {
				List<TaggedWord> taggedSent = tagger.tagSentence(sentence);
				for (TaggedWord tw : taggedSent) {
					//tw.word() gives word related with tag
					wordcnt=doc.wordCount.get(tw.tag());
					doc.totalWords=doc.totalWords+1;
					if(wordcnt==null)
					{
						doc.wordCount.put(tw.tag(), 1);
						if(!(MainFile.uniqPOS.contains(tw.tag())))
						{
							MainFile.uniqPOS.add(tw.tag());
							MainFile.totalFeatures=MainFile.totalFeatures+1;
						}
					}
					else
						doc.wordCount.put(tw.tag(), wordcnt+1);
				}
			}
			r.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

}
