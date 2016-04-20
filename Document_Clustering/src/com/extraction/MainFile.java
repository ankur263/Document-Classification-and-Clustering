/*
 * This package mainly contains core java files which extract the features from different documents and
 * classify based on those feature values
 * */
package com.extraction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.tika.*;
import org.apache.uima.util.FileUtils;

import com.features.*;


import weka.clusterers.SimpleKMeans;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class MainFile {

	//All objects to store data temporary to extract features from documents 
	//The object starting with 'uniq', maintains state of features across document corpus (this is applicable for each below objects)
	//The object starting with 'doc', maintains state of features for each particular document (this is applicable for each below objects)
	public static TreeMap<String, Integer> uniqUnigrams;  
	public static HashMap<String, DocInfo> docUnigrams;   
	public static TreeMap<String, Integer> uniqBigrams;	
	public static HashMap<String, DocInfo> docBigrams;
	public static TreeMap<String, Integer> uniqTrigrams;
	public static HashMap<String, DocInfo> docTrigrams;
	public static TreeMap<String, Integer> uniqCapitals;
	public static HashMap<String, DocInfo> docCapitals;
	public static TreeSet<String> uniqPunct;
	public static HashMap<String, DocInfo> docPunct;
	public static TreeSet<String> uniqPOS;		//Around 45 POS tags
	public static HashMap<String, DocInfo> docPOS; 
	public static HashMap<String, Integer> docSentences;
	public static TreeSet<String> uniqNE;		//Named Entity Labels: PERSON, ORGANIZATION, LOCATION
	public static HashMap<String, DocInfo> docNE;
	public static HashMap<String, Pair> docPositiveNegative;
	public static TreeMap<String, Integer> uniqURLs;
	public static HashMap<String, DocInfo> docURLs;
	public static String currentDoc;		//Text of current document under process of feature extraction
	public static ArrayList<Integer> featureChoice;	//contains choice of features to cluster given document corpus
	public static Integer totalFeatures=0;	//Specifies total features based on which clustering will be done
	public static PorterStemmer myStem;
	public static String currFilename;		//Name of current document under process of feature extraction
	public static ArrayList<ArrayList<Integer>> cluster;	//contains info of each cluster like which documents are in which clusters
	public static int no_cluster=4;		//Specifies no of clusters for document clustering
	public static Integer totalDocs=0;  //Specifies total documents in given corpus
	int diffrentFeatures=10;			//Specifies different type of features like unigrams,bigrams etc.
	File inputDir,file,outputDir,tempDir;		
	File[] files;		//file pointers of each documents in corpus
	BufferedWriter bw; 
	String st;
	DocInfo doc;	
	Unigram objUnigram;
	Bigram objBigram;
	Trigram objTrigram;
	Capital objCapital;
	Sentence objSentence;
	Punctuation objPunctuation;
	POS objPOS;
	NameEntityRecognizer objNE;
	PositiveNegative objPN;
	URL objURL;
	int i;
	StringBuilder sb;	
	String destFolder,dataset,raw_data,processed,feature_data,descriptor,tagger,neTrained,tempCluster;
	
	/*
	 * Main function from where execution of program will start.
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		long lStartTime = new Date().getTime();
		MainFile obj;
		obj=new MainFile();
		obj.check();	
		obj.init();
		obj.process();
		long lEndTime = new Date().getTime();
		long difference = lEndTime - lStartTime;
		System.out.println("Elapsed milliseconds: " + difference);  //Measures total execution time of project
	}
	
	/*
	 * This function initializes every objects and also gives choice of features for document clustering
	 */
	public void init()
	{
		uniqUnigrams=new TreeMap<String, Integer>();
		docUnigrams=new HashMap<String, DocInfo>();
		uniqBigrams=new TreeMap<String, Integer>();
		docBigrams=new HashMap<String, DocInfo>();
		uniqTrigrams=new TreeMap<String, Integer>();
		docTrigrams=new HashMap<String, DocInfo>();
		uniqCapitals=new TreeMap<String, Integer>();
		docCapitals=new HashMap<String, DocInfo>();
		uniqPOS=new TreeSet<String>();
		docPOS=new HashMap<String, DocInfo>();
		uniqPunct=new TreeSet<String>();
		docPunct=new HashMap<String, DocInfo>();
		docSentences=new HashMap<String, Integer>();
		docPositiveNegative=new HashMap<String, Pair>();
		uniqURLs=new TreeMap<String, Integer>();
		docURLs=new HashMap<String, DocInfo>();
		myStem=new PorterStemmer();
		uniqNE=new TreeSet<String>();
		docNE=new HashMap<String, DocInfo>();
		featureChoice=new ArrayList<Integer>(diffrentFeatures);	//user can enter their choice of features and it is specified in this object
		
		System.out.println("Feature list:");
		System.out.println("1. Unigram");
		System.out.println("2. Bigram");
		System.out.println("3. Trigram");
		System.out.println("4. #Sentence");
		System.out.println("5. POS Tags");
		System.out.println("6. Punctuation");
		System.out.println("7. Capitalization");
		System.out.println("8. Named Entities");
		System.out.println("9. Positive and Negative words");
		System.out.println("10. URLs");
		int choice;
		Scanner in = new Scanner(System.in);
		System.out.println("Select your features:(to stop enter -1 at last)");
		while(true)
		{
			choice=Integer.parseInt(in.nextLine());
			if(choice==-1)
				break;
			if(choice>10 || choice <=0)
				continue;
			if(!(featureChoice.contains(choice-1)))
				featureChoice.add(choice-1);
		}
		//featureChoice.add(5);
		//featureChoice.add(4);
		//featureChoice.add(2);
		//featureChoice.add(6);
	}
	
	/*
	 * This function iterates through each document, extracts features and stores in different objects.
	 */
	public void process()
	{
		try{
			String[] para = new String[2];
			totalDocs=files.length;

			// process each document one by one and extract each specified feature from documents
			for (i = 0; i < files.length; i++) {
				//Text file for each documents which does not contains multimedia information
				st=outputDir.getPath()+File.separator+files[i].getName().replaceFirst("[.][^.]+$", "")+".txt";
				file = new File(files[i].getPath());				//input file name
				bw = new BufferedWriter(new FileWriter(st));		//output file name
				currFilename=file.getName();
				processRawData();	
				bw.close();
				// read contents of file in string
				currentDoc = FileUtils.file2String(new File(st));
				para[1]=st;
				//iterates and extract all specified features from document
				for(int c=0;c<featureChoice.size();c++)
				{
					switch(featureChoice.get(c)){
						case 0: docUnigrams.put(currFilename,new DocInfo());	//Unigrams
								objUnigram=new Unigram();
								para[0]=descriptor+File.separator+"Unigram.xml"; //Name of descriptor file(in xml format)
								objUnigram.analyze(para);
								break;
						case 1: docBigrams.put(currFilename,new DocInfo());		//Bigrams
								objBigram=new Bigram();
								para[0]=descriptor+File.separator+"Bigram.xml";
								objBigram.analyze(para);
								break;
						case 2: docTrigrams.put(currFilename,new DocInfo());	//Trigrams
								objTrigram=new Trigram();
								para[0]=descriptor+File.separator+"Trigram.xml";
								objTrigram.analyze(para);
								break;
						case 3: docSentences.put(currFilename, 0);		//no of sentences
								objSentence=new Sentence();
								para[0]=descriptor+File.separator+"Sentence.xml";
								objSentence.analyze(para);
								break;
						case 4: docPOS.put(currFilename, new DocInfo());	//POS Tag
								objPOS=new POS();
								para[0]=tagger+File.separator+"english-left3words-distsim.tagger";	//model file for POS tagging
								objPOS.analyze(para);
								break;
						case 5: docPunct.put(currFilename,new DocInfo());	//No of Punctuation
								objPunctuation=new Punctuation();
								para[0]=descriptor+File.separator+"Punctuation.xml"; 
								objPunctuation.analyze(para);
								break;
						case 6: docCapitals.put(currFilename,new DocInfo());	//Capital words
								objCapital=new Capital();
								para[0]=descriptor+File.separator+"Unigram.xml"; 
								objCapital.analyze(para);
								break;
						case 7: docNE.put(currFilename, new DocInfo());		//Named Entity
								objNE=new NameEntityRecognizer();
								para[0]=neTrained+File.separator+"english.all.3class.distsim.crf.ser.gz";	//model file for named entity
								objNE.analyze(para);
								break;
						case 8: docPositiveNegative.put(currFilename, new Pair());	//Positive and Negative words
								objPN=new PositiveNegative();
								para[0]=descriptor+File.separator+"Unigram.xml";
								objPN.analyze(para);
								break;
						case 9: docURLs.put(currFilename,new DocInfo());	//URLs
								objURL=new URL();
								para[0]=descriptor+File.separator+"URL.xml"; 
								objURL.analyze(para);
								break;
					}
				}

				/*for(Map.Entry<String,Integer> entry : MainFile.uniqUnigrams.entrySet()) {
					  String key = entry.getKey();
					  Integer value = entry.getValue();
					  System.out.println(key + " => " + value);
					}*/

				/*for(Map.Entry<String, DocInfo> entry : MainFile.docPOS.entrySet()) {
					  String key = entry.getKey();
					  DocInfo value = entry.getValue();
					  System.out.println(key + " ===========================> "+value.totalWords);
					  for(Map.Entry<String, Integer> entry2 : value.wordCount.entrySet()) {
						  String key1 = entry2.getKey();
						  Integer value1 = entry2.getValue();
						  System.out.println(key1 + " ## " + value1);
					  }
					}*/
			}
			if(featureChoice.contains(3))
				totalFeatures=totalFeatures+1;
			if(featureChoice.contains(8))
			{
				totalFeatures=totalFeatures+2;
				objPN.deleteData();
			}
			if(totalFeatures!=0)
			{
				datasetCreation();
				clusterData();
//				Scanner in=new Scanner(System.in);
				//System.out.println("Enter desired feature and cluster based on previously selected features for bar graph:");
//				String feature=in.nextLine();
//				String cluster=in.nextLine();
//				drawBarChart(feature,cluster);
//				
//				System.out.println("Enter desired cluster for word cloud:");
//				cluster=in.nextLine();
//				drawWordCloud(cluster);				
			}
			else
			{
				//Zero extracted features from given dataset
				System.out.println("Not a proper dataset");
				System.out.println("Program terminated");
				System.exit(0);
			}

		} catch(Exception e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*void drawBarChart(String feature, String cluster)
	{
		String[] args=new String[3];
		args[0]=(Integer.parseInt(feature)-1)+"";	//Feature selection to draw bar chart
		args[1]=(Integer.parseInt(cluster)-1)+"";	//Cluster selection to draw bar chart
		args[2]=feature_data;	//location of features files
		BarGraph app = new BarGraph();
		app.caller(args);	//draws bar chart according to given arguments and stored in 'chart.png' file in current project folder
	}
	
	void drawWordCloud(String cluster)
	{
		String[] args=new String[3];
		args[0]=(Integer.parseInt(cluster)-1)+"";	//Cluster selection
		args[1]=tempCluster;	//location of clustered text files
	    WordCloud wc=new WordCloud();	
		wc.draw(args);	//draws word cloud based on given arguments and stored in wordcloud_<cluster_no>.png file in current project folder
	}*/
	
	/*
	 * This function converts heterogeneous documents into pure text documents. 
	 * This function uses Apache Tika to do this. 
	 */
	public void processRawData()
	{
		Tika obj=new Tika();
		Reader r;
		Boolean fg=true;	//specifies previous character is not space
		char dataChar;
		int data;
		try {
			//Pre-processing before converting into text file like removing extra spaces from document
			//Now document is only "bag of related words"
			r=obj.parse(file);
			data = r.read();
			while(data!=-1 && (data==' ' || data=='\t' || data=='\n' || data=='-'))
			{
				data = r.read();
			}
			while(data != -1){
				dataChar = (char) data;

				if(fg && dataChar=='-')
				{
					while(data != -1 && (dataChar=='-' || dataChar=='\n'))
					{
						data = r.read();
						dataChar = (char) data;
					}
				}
				if(dataChar==' ' || dataChar=='\t' || dataChar=='\n')
				{
					if(fg)
						bw.write(dataChar);
					fg=false;
				}
				else
				{
					bw.write(dataChar);
					fg=true;
				}
				data = r.read();				
			}
			bw.write(" ");
			r.close();
			//String st=obj.parseToString(file);
			//String filetype=obj.detect(file);
			//System.out.println(st);
			//System.out.println("================================");
			//System.out.println(filetype);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * This function clusters data based on different feature values of documents and stores documents in different cluster folder
	 * KMeans algorithm of Weka inbuilt library is used for clustering. 
	 */
	public void clusterData()
	{	
		try {
			File tfile;
			int seedVal=2*no_cluster;
			DataSource source = new DataSource(dataset);	//data file("data.arff") in weka specified format 
			cluster=new ArrayList<ArrayList<Integer>>(no_cluster);
			for(i=0;i<no_cluster;i++)
				cluster.add(new ArrayList<Integer>());
			Instances data = source.getDataSet();
			SimpleKMeans model = new SimpleKMeans();
			model.setNumClusters(no_cluster);
			model.setSeed(seedVal);
			
			model.buildClusterer(data);
			file=new File(destFolder);
			tfile=new File(tempCluster);
			if (file.exists()) 
				//delete older files recursively
				recursiveDelete(file);
			if (tfile.exists()) 
				recursiveDelete(tfile);
			
			if (!(file.mkdir()) || !(tfile.mkdir())) 
			{
				System.out.println("Failed to create cluster directory!!!");
				System.exit(0);
			}

			//Separates files in different cluster based on result generated by KMeans algo.
			for(i=0;i<no_cluster;i++)
			{
				file=new File(destFolder+File.separator+i);
				tfile=new File(tempCluster+File.separator+i);
				if (!(file.mkdir()) || !(tfile.mkdir())) 
				{
					System.out.println("Failed to create cluster directory!");
					System.exit(0);
				}
			}

			i=0;
			int no;
			for (Instance instance : data) {
				no=model.clusterInstance(instance);
								
				Files.copy(new File(raw_data+File.separator+files[i].getName()).toPath(),(new File(destFolder+"/"+no+"/"+files[i].getName())).toPath(), StandardCopyOption.REPLACE_EXISTING);
				Files.copy(new File(processed+File.separator+files[i].getName().replaceFirst("[.][^.]+$", "")+".txt").toPath(),(new File(tempCluster+"/"+no+"/"+files[i].getName().replaceFirst("[.][^.]+$", "")+".txt")).toPath(), StandardCopyOption.REPLACE_EXISTING);
				//System.out.println(processed+"/"+files[i].getName().replaceFirst("[.][^.]+$", "")+".txt"+"-->"+no);
				cluster.get(no).add(i);
				i++;
			}
			//	System.out.println(i);
			//	System.out.println(model);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * This function creates weka specific data file("data.arff") for clustering data
	 * Calculates feature values based on objects created in process() function like tf-idf,frequency etc.
	 */
	public void datasetCreation()
	{		
		try {
			int c;
			BufferedWriter tpbw;
			ArrayList<BufferedWriter> bw_tf=new ArrayList<BufferedWriter>(diffrentFeatures);
			ArrayList<BufferedWriter> bw_idf=new ArrayList<BufferedWriter>(diffrentFeatures);
			String key;
			Integer value,tf;
			Double tf_idf,idf;
			Iterator<String> iterator;
			//file for clustering in required format
			bw = new BufferedWriter(new FileWriter(dataset));
			bw.write("@relation document\n\n");
			
			for(i=0;i<diffrentFeatures;i++)
			{
				bw_tf.add(null);
				bw_idf.add(null);
			}
			
			//iterates through each features and specifies attribute for each different features
			//stores each feature in separate temporary files
			for(c=0;c<featureChoice.size();c++)
			{
				switch(featureChoice.get(c)){
					case 0: tpbw=new BufferedWriter(new FileWriter(feature_data+File.separator+"unigrams.txt"));
							i=0;
							for(Map.Entry<String,Integer> entry : uniqUnigrams.entrySet()) {
								key = entry.getKey();
								bw.write("@attribute uni"+i+" numeric\n");
								tpbw.write(key+"\n");
								i++;								
							}
							tpbw.close();
							bw_tf.set(c,new BufferedWriter(new FileWriter(feature_data+File.separator+"unigram_tf.txt",true)));
							bw_idf.set(c,new BufferedWriter(new FileWriter(feature_data+File.separator+"unigram_tf_idf.txt",true)));
							break;
					case 1: tpbw=new BufferedWriter(new FileWriter(feature_data+File.separator+"bigrams.txt"));
							i=0;
							for(Map.Entry<String,Integer> entry : uniqBigrams.entrySet()) {
								key = entry.getKey();
								bw.write("@attribute bi"+i+" numeric\n");
								tpbw.write(key+"\n");
								i++;								
							}
							tpbw.close();
							bw_tf.set(c,new BufferedWriter(new FileWriter(feature_data+File.separator+"bigram_tf.txt",true)));
							bw_idf.set(c,new BufferedWriter(new FileWriter(feature_data+File.separator+"bigram_tf_idf.txt",true)));
							break;
					case 2: tpbw=new BufferedWriter(new FileWriter(feature_data+File.separator+"trigrams.txt"));
							i=0;
							for(Map.Entry<String,Integer> entry : uniqTrigrams.entrySet()) {
								key = entry.getKey();
								bw.write("@attribute tri"+i+" numeric\n");
								tpbw.write(key+"\n");
								i++;								
							}
							tpbw.close();
							bw_tf.set(c,new BufferedWriter(new FileWriter(feature_data+File.separator+"trigram_tf.txt",true)));
							bw_idf.set(c,new BufferedWriter(new FileWriter(feature_data+File.separator+"trigram_tf_idf.txt",true)));
							break;
					case 3: tpbw=new BufferedWriter(new FileWriter(feature_data+File.separator+"sentences.txt"));
							bw.write("@attribute #Sentences numeric\n");
							tpbw.write("Total Sentences\n");
							tpbw.close();
							bw_tf.set(c,new BufferedWriter(new FileWriter(feature_data+File.separator+"sentence_tf.txt",true)));
							break;
					case 4: tpbw=new BufferedWriter(new FileWriter(feature_data+File.separator+"POS.txt"));
							i=0;
							iterator = uniqPOS.iterator();
							while (iterator.hasNext()){
								key = iterator.next();
								bw.write("@attribute POS"+i+" numeric\n");
								tpbw.write(key+"\n");
								i++;								
							}
							tpbw.close();
							bw_tf.set(c,new BufferedWriter(new FileWriter(feature_data+File.separator+"POS_tf.txt",true)));
							break;
					case 5: tpbw=new BufferedWriter(new FileWriter(feature_data+File.separator+"punctuations.txt"));
							i=0;
							iterator = uniqPunct.iterator();
							while (iterator.hasNext()){
								key = iterator.next();
								bw.write("@attribute punct"+i+" numeric\n");
								tpbw.write(key+"\n");
								i++;								
							}
							tpbw.close();
							bw_tf.set(c,new BufferedWriter(new FileWriter(feature_data+File.separator+"punctuation_tf.txt",true)));
							break;
					case 6: tpbw=new BufferedWriter(new FileWriter(feature_data+File.separator+"capitals.txt"));
							i=0;
							for(Map.Entry<String,Integer> entry : uniqCapitals.entrySet()) {
								key = entry.getKey();
								bw.write("@attribute capital"+i+" numeric\n");
								tpbw.write(key+"\n");
								i++;								
							}
							tpbw.close();
							bw_tf.set(c,new BufferedWriter(new FileWriter(feature_data+File.separator+"capital_tf.txt",true)));
							bw_idf.set(c,new BufferedWriter(new FileWriter(feature_data+File.separator+"capital_tf_idf.txt",true)));
							break;
					case 7: tpbw=new BufferedWriter(new FileWriter(feature_data+File.separator+"NE.txt"));
							i=0;
							iterator = uniqNE.iterator();
							while (iterator.hasNext()){
								key = iterator.next();
								bw.write("@attribute NE"+i+" numeric\n");
								tpbw.write(key+"\n");
								i++;								
							}
							tpbw.close();
							bw_tf.set(c,new BufferedWriter(new FileWriter(feature_data+File.separator+"NE_tf.txt",true)));
							break;
					case 8: tpbw=new BufferedWriter(new FileWriter(feature_data+File.separator+"PN.txt"));
							bw.write("@attribute positive_words numeric\n");
							bw.write("@attribute negative_words numeric\n");
							tpbw.write("Total Positive words\nTotal Negative words\n");
							tpbw.close();
							bw_tf.set(c,new BufferedWriter(new FileWriter(feature_data+File.separator+"PN_tf.txt",true)));
							break;
					case 9: tpbw=new BufferedWriter(new FileWriter(feature_data+File.separator+"urls.txt"));
							i=0;
							for(Map.Entry<String,Integer> entry : uniqURLs.entrySet()) {
								key = entry.getKey();
								bw.write("@attribute url"+i+" numeric\n");
								tpbw.write(key+"\n");
								i++;								
							}
							tpbw.close();
							bw_tf.set(c,new BufferedWriter(new FileWriter(feature_data+File.separator+"url_tf.txt",true)));
							bw_idf.set(c,new BufferedWriter(new FileWriter(feature_data+File.separator+"url_tf_idf.txt",true)));
							break;
				}					
			}
			bw.write("\n@data\n");

			sb=new StringBuilder(1000);
			//iterates through every docs and writes features values in vector like format in one line for each document
			for (i = 0; i < files.length; i++) {
				sb.setLength(0);
				st=files[i].getName();
				//iterates through every specified features and write values of features in comma separated values			
				for(c=0;c<featureChoice.size();c++){
					switch(featureChoice.get(c)){
						case 0: doc=docUnigrams.get(st);
								for(Map.Entry<String,Integer> entry : MainFile.uniqUnigrams.entrySet()) {
									key = entry.getKey();
									value = entry.getValue();
									tf=doc.wordCount.get(key);
									if(tf!=null)
									{
										idf=Math.log10(totalDocs*1.0/value);	//Calculation of tf-idf score
										tf_idf=tf*idf;
										sb.append(tf_idf+",");
										bw_idf.get(c).write(tf_idf+",");
										bw_tf.get(c).write(tf+",");
									}
									else
									{
										sb.append("0,");	
										bw_idf.get(c).write("0,");
										bw_tf.get(c).write("0,");
									}									
								}	
								bw_idf.get(c).write("\n");
								bw_tf.get(c).write("\n");
								break;
						case 1: doc=docBigrams.get(st);
								for(Map.Entry<String,Integer> entry : MainFile.uniqBigrams.entrySet()) {
									key = entry.getKey();
									value = entry.getValue();
									tf=doc.wordCount.get(key);
									if(tf!=null)
									{
										idf=Math.log10(totalDocs*1.0/value);	//Calculation of tf-idf score
										tf_idf=tf*idf;
										sb.append(tf_idf+",");
										bw_idf.get(c).write(tf_idf+",");
										bw_tf.get(c).write(tf+",");
									}
									else
									{
										sb.append("0,");
										bw_idf.get(c).write("0,");
										bw_tf.get(c).write("0,");
									}
								}
								bw_idf.get(c).write("\n");
								bw_tf.get(c).write("\n");
								break;
						case 2: doc=docTrigrams.get(st);
								for(Map.Entry<String,Integer> entry : MainFile.uniqTrigrams.entrySet()) {
									key = entry.getKey();
									value = entry.getValue();
									tf=doc.wordCount.get(key);
									if(tf!=null)
									{
										idf=Math.log10(totalDocs*1.0/value);	//Calculation of tf-idf score
										tf_idf=tf*idf;
										sb.append(tf_idf+",");
										bw_idf.get(c).write(tf_idf+",");
										bw_tf.get(c).write(tf+",");
									}
									else
									{
										sb.append("0,");
										bw_idf.get(c).write("0,");
										bw_tf.get(c).write("0,");
									}
								}
								bw_idf.get(c).write("\n");
								bw_tf.get(c).write("\n");
								break;
						case 3: sb.append(docSentences.get(st)+",");
								bw_tf.get(c).write(docSentences.get(st)+"\n");
								break;
						case 4: doc=docPOS.get(st);
								iterator = uniqPOS.iterator();
								while (iterator.hasNext()){
									tf=doc.wordCount.get(iterator.next());
									if(tf!=null)
									{
										tf_idf= (double)tf/doc.totalWords;	//Relatively calculates no of POS Tags
										sb.append(tf_idf+",");
										bw_tf.get(c).write(tf_idf+" ");
									}
									else
									{
										sb.append("0,");
										bw_tf.get(c).write("0 ");
									}
								}
								bw_tf.get(c).write("\n");
								break;
						case 5: doc=docPunct.get(st);
								iterator = uniqPunct.iterator();
								while (iterator.hasNext()){
									tf=doc.wordCount.get(iterator.next());
									if(tf!=null)
									{
										tf_idf= (double)tf/doc.totalWords;	//Relatively calculates no of Punctuations
										sb.append(tf_idf+",");
										bw_tf.get(c).write(tf_idf+" ");
									}
									else
									{
										sb.append("0,");
										bw_tf.get(c).write("0 ");
									}
								}
								bw_tf.get(c).write("\n");
								break;
						case 6: doc=docCapitals.get(st);
								for(Map.Entry<String,Integer> entry : MainFile.uniqCapitals.entrySet()) {
									key = entry.getKey();
									value = entry.getValue();
									tf=doc.wordCount.get(key);
									if(tf!=null)
									{
										idf=Math.log10(totalDocs*1.0/value);	//Calculation of tf-idf score
										tf_idf=tf*idf;
										sb.append(tf_idf+",");
										bw_idf.get(c).write(tf_idf+",");
										bw_tf.get(c).write(tf+",");
									}
									else
									{
										sb.append("0,");
										bw_idf.get(c).write("0,");
										bw_tf.get(c).write("0,");
									}
								}
								bw_idf.get(c).write("\n");
								bw_tf.get(c).write("\n");
								break;
						case 7: doc=docNE.get(st);
								iterator = uniqNE.iterator();
								while (iterator.hasNext()){
									tf=doc.wordCount.get(iterator.next());
									if(tf!=null)
									{
										tf_idf= (double)tf/doc.totalWords;	//Relatively calculates no of Named entities: Person,Organization or Location
										sb.append(tf_idf+",");
										bw_tf.get(c).write(tf_idf+",");
									}
									else
									{
										sb.append("0,");
										bw_tf.get(c).write("0,");
									}
								}
								bw_tf.get(c).write("\n");
								break;
						case 8: sb.append(docPositiveNegative.get(st).getX()+","+docPositiveNegative.get(st).getY()+",");
								bw_tf.get(c).write(docPositiveNegative.get(st).getX()+","+docPositiveNegative.get(st).getY()+"\n");
								break;
						case 9: doc=docURLs.get(st);
								for(Map.Entry<String,Integer> entry : MainFile.uniqURLs.entrySet()) {
									key = entry.getKey();
									value = entry.getValue();
									tf=doc.wordCount.get(key);
									if(tf!=null)
									{
										idf=Math.log10(totalDocs*1.0/value);	//Calculation of tf-idf score
										tf_idf=tf*idf;
										sb.append(tf_idf+",");
										bw_idf.get(c).write(tf_idf+",");
										bw_tf.get(c).write(tf+",");
									}
									else
									{
										sb.append("0,");
										bw_idf.get(c).write("0,");
										bw_tf.get(c).write("0,");
									}
								}
								bw_idf.get(c).write("\n");
								bw_tf.get(c).write("\n");
								break;
					}
				}
				sb.setCharAt(sb.length()-1,'\n');
				bw.write(sb.toString());
			}
			//clear all static objects because now we have all info in file format 
			//so need to store unnecessary objects in main memory
			uniqUnigrams.clear();
			docUnigrams.clear();
			uniqBigrams.clear();
			docBigrams.clear();
			uniqTrigrams.clear();
			docTrigrams.clear();
			docSentences.clear();
			uniqPOS.clear();
			docPOS.clear();
			uniqCapitals.clear();
			uniqPunct.clear();
			docCapitals.clear();
			docPunct.clear();
			uniqNE.clear();
			docNE.clear();
			docPositiveNegative.clear();
			bw.close();
			for(i=0;i<diffrentFeatures;i++)
			{
				if(bw_idf.get(i)!=null)
					bw_idf.get(i).close();
				if(bw_tf.get(i)!=null)
					bw_tf.get(i).close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * This functions check for structure of input document corpus.
	 * Dataset must be in folder "resources/raw_data".
	 * If folder format is not in proper format than application will terminate with message.
	 * It also creates extra temporary folder for application to store temp data.
	 */
	void check()
	{
		destFolder="resources"+File.separator+"clustered";
		dataset="resources"+File.separator+"data.arff";	
		raw_data="resources"+File.separator+"raw_data";
		processed="resources"+File.separator+"data";
		feature_data="resources"+File.separator+"temp";
		tempCluster="resources"+File.separator+"temp"+File.separator+"clustered";
		descriptor="descriptor";
		tagger="tagger";
		neTrained="classifiers";
		
		inputDir=new File(raw_data);
		if(!inputDir.exists() || !inputDir.isDirectory())
		{
			System.out.println("It seems input files are not present or input data structure is not in appropriate format");
			System.out.println("Create directory structure in current project folder \"resources/raw_data\" and put all input documents int that folder.");
			System.out.println("Program terminated");
			System.exit(0);
		}
		// get all files in the input directory
		files = inputDir.listFiles();
		if(files==null)
		{
			System.out.println("Input documents are not present");
			System.out.println("Program terminated");
			System.exit(0);
		}
		for (i=0;i<files.length;i++) {
			if(files[i].length()==0)
			{
				System.out.println("Some input documents are empty. Delete those documents before running program");
				System.out.println("Program terminated");
				System.exit(0);
			}
			if(files[i].isDirectory())
			{
				System.out.println("Input folder should contain documents only");
				System.out.println("Delete folders in input folder before running program");
				System.out.println("Program terminated");
				System.exit(0);
			}
		}
		
		outputDir=new File(processed);
		if(outputDir.exists())
		{
			if(!outputDir.isDirectory())
			{
				if(!(outputDir.delete())){
					System.out.println("Failed to delete files");
					System.out.println("Program terminated");
					System.exit(0);
				}
			}
			else
			{
				//delete older files in processed directory
				recursiveDelete(outputDir);
			}
		}
		boolean status = outputDir.mkdirs();
		if(!status)
		{
			System.out.println("Failed to create directory");
			System.out.println("Program terminated");
			System.exit(0);
		}	
		
		tempDir=new File(feature_data);
		if(tempDir.exists())
		{
			if(!tempDir.isDirectory())
			{
				if(!(tempDir.delete())){
					System.out.println("Failed to delete files");
					System.out.println("Program terminated");
					System.exit(0);
				}
			}
			else
			{
				//delete older files in processed directory
				recursiveDelete(tempDir);
			}
		}
		status = tempDir.mkdirs();
		if(!status)
		{
			System.out.println("Failed to create directory");
			System.out.println("Program terminated");
			System.exit(0);
		}		
		tempDir=new File(tempCluster);
		status = tempDir.mkdirs();
		if(!status)
		{
			System.out.println("Failed to create directory");
			System.out.println("Program terminated");
			System.exit(0);
		}	
	}
	
	/*
	 * Recursively delete all files and folder in given path 
	 */
	public boolean recursiveDelete(File file) {
		//to end the recursive loop
		if (!file.exists())
			return true;

		//if directory, go inside and call recursively
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				//call recursively
				recursiveDelete(f);
			}
		}
		//call delete to delete files and empty directory
		if(!file.delete())
			return false;
		return true;
	}
}
