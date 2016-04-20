package com.extraction;

import java.util.HashMap;

/*
 * This class contains informations for each documents like word counts, total words etc based on specified feature values
 */
public class DocInfo
{
	public HashMap<String, Integer> wordCount;
	public Integer totalWords;
	public DocInfo()
	{
		this.wordCount=new HashMap<String, Integer>();
		this.totalWords=0;
	}
}
