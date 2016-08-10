package com.flatironschool.javacs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jsoup.select.Elements;

import com.flatironschool.javacs.persistence.impl.JedisIndexer;
import com.flatironschool.javacs.persistence.interfaces.IPersistIndex;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

/**
 * Represents a Redis-backed web search index.
 * 
 */
public class Indexer {

	private IPersistIndex persistance;
	private Set<String> stopWordsList;

	/**
	 * Constructor.
	 * 
	 * @param jedis
	 */
	public Indexer(IPersistIndex persistance) {
		this.persistance = persistance;
		populateStopWords ();
	}
	
	/**
	 * Returns the Redis key for a given search term.
	 * 
	 * @return Redis key.
	 */
	private String urlSetKey(String term) {
		return "URLSet:" + term;
	}
	
	/**
	 * Returns the Redis key for a URL's TermCounter.
	 * 
	 * @return Redis key.
	 */
	private String termCounterKey(String url) {
		return "TermCounter:" + url;
	}

	/**
	 * Checks whether we have a TermCounter for a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public boolean isIndexed(String url) {
		return this.persistance.isIndexed(url);
	}
	
	/**
	 * Adds a URL to the set associated with `term`.
	 * 
	 * @param term
	 * @param tc
	 */
	/*public void add(String term, TermCounter tc) {
		jedis.sadd(urlSetKey(term), tc.getLabel());
	}*/

	/**
	 * Looks up a search term and returns a set of URLs.
	 * 
	 * @param term
	 * @return Set of URLs.
	 */
	public Set<String> getURLs(String term) {
		
		return this.persistance.getUrls(term);
	/*	Set<String> set = jedis.smembers(urlSetKey(term));
		return set;*/
	}

	/**
	 * Looks up a term and returns a map from URL to count.
	 * 
	 * @param term
	 * @return Map from URL to count.
	 */
	public Map<String, Integer> getCounts(String term) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		Set<String> urls = getURLs(term);
		for (String url: urls) {
			Integer count = getCount(url, term);
			map.put(url, count);
		}
		return map;
	}

	/**
	 * Looks up a term and returns a map from URL to count.
	 * 
	 * @param term
	 * @return Map from URL to count.
	 */
	public Map<String, Double> getCountsFaster(String term) {
			
		Map<String, Double> map = this.persistance.getUrlToTermCount(term);
		
		map.entrySet().stream()
	    .map(e->e.getKey() + " has the term " + term)
	    .sorted()
	    .forEach(e->System.out.println(e));
		return map;
	}
	

	/**
	 * Returns the number of times the given term appears at the given URL.
	 * 
	 * @param url
	 * @param term
	 * @return
	 */
	public Integer getCount(String url, String term) {
		return this.persistance.getCount(url, term);
	}
	
	/**
	 * 
	 * gets all the terms found in a doc and their count
	 * @param url
	 * @return
	 */
	
	public Map<String, Double> getCountWithRelevance(String term, Ranker ranker) {
		
		//get the term count per url
		Map<String, Double> urlToCount = getCountsFaster (term);
		
		if (urlToCount == null || urlToCount.size() == 0)
			return null;
		
		//iterate over the map and find relevance per url
		Set<String> urls = urlToCount.keySet();
		
				
		//get the total number of docs indexed
		Set<String> indexedUrls = termCounterKeys();
		
		double idf = Math.log(indexedUrls.size()/(double)urls.size());
		
			
		for (String url: urls)
		{
			Double termCount = urlToCount.get(url);
			
						
			double totalCountOfAllTerms = this.persistance.getTotalTermCount(url);
			
			double tf = termCount/totalCountOfAllTerms;
			double tfidf = round(tf*idf, 1);
			
			//add the ranking of this relevance
			if (ranker != null)
			{
				Integer linkedCount = ranker.getLinkedCount(url);
				tfidf  = tfidf + linkedCount;
			}
					
			urlToCount.put(url, tfidf);
			
			
		}
		
		
		
		return urlToCount;
	}

	
	
	/**
	 * Add a page to the index.
	 * 
	 * @param url         URL of the page.
	 * @param paragraphs  Collection of elements that should be indexed.
	 */
	public void indexPage(String url, Elements paragraphs) {
				
		// make a TermCounter and count the terms in the paragraphs
		TermCounter tc = new TermCounter(url);
		tc.processElements(paragraphs, this.stopWordsList);
		
		// push the contents of the TermCounter to Redis
		pushTermCounterToRedis(tc);
					
	}

	/**
	 * Pushes the contents of the TermCounter to Redis.
	 * 
	 * @param tc
	 * @return List of return values from Redis.
	 */
	public void pushTermCounterToRedis(TermCounter tc) {
		this.persistance.pushTermCounter(tc);
	}

	/**
	 * Prints the contents of the index.
	 * 
	 * Should be used for development and testing, not production.
	 */
	public void printIndex() {
		// loop through the search terms
		for (String term: termSet()) {
			System.out.println(term);
			
			// for each term, print the pages where it appears
			Set<String> urls = getURLs(term);
			for (String url: urls) {
				Integer count = getCount(url, term);
				System.out.println("    " + url + " " + count);
			}
		}
	}

	/**
	 * Returns the set of terms that have been indexed.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public Set<String> termSet() {
		Set<String> keys = urlSetKeys();
		Set<String> terms = new HashSet<String>();
		for (String key: keys) {
			String[] array = key.split(":");
			if (array.length < 2) {
				terms.add("");
			} else {
				terms.add(array[1]);
			}
		}
		return terms;
	}

	/**
	 * Returns URLSet keys for the terms that have been indexed.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public Set<String> urlSetKeys() {
		return this.persistance.urlSetKeys();
	//	return jedis.keys("URLSet:*");
	}

	/**
	 * Returns TermCounter keys for the URLS that have been indexed.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public Set<String> termCounterKeys() {
		//return jedis.keys("TermCounter:*");
		return this.persistance.termCounterKeys();
	}

	/**
	 * Deletes all URLSet objects from the database.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public void deleteURLSets() {
		this.persistance.deleteURLSets();
	}

	/**
	 * Deletes all URLSet objects from the database.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public void deleteTermCounters() {
		this.persistance.deleteTermCounters();
		
	}

	/**
	 * Deletes all keys from the database.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public void deleteAllKeys() {
		this.persistance.deleteAllKeys();
	
	}
	
	/**
	 * Stores two pages in the index for testing purposes.
	 * 
	 * @return
	 * @throws IOException
	 */
	private static void loadIndex(Indexer index) throws IOException {
		WikiFetcher wf = new WikiFetcher();

		String url = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		Elements paragraphs = wf.readWikipedia(url);
		index.indexPage(url, paragraphs);
		
		url = "https://en.wikipedia.org/wiki/Programming_language";
		paragraphs = wf.readWikipedia(url);
		index.indexPage(url, paragraphs);
	}
	
	private void populateStopWords() {
		
		this.stopWordsList = new TreeSet<>();
		
		String slash = File.separator;
		String filename = "resources" + slash + "stopWords.txt";
		
	    

	    Scanner scan = null;

	    try {
	    	
	    	// read the file
	    	
	    	InputStream stream = Indexer.class.getClassLoader().getResourceAsStream(filename);
			if (stream == null)
			{
				System.out.println("ERROR READING STOP WORDS");
				return;
			}
	        scan = new Scanner(stream);

	        while (scan.hasNextLine()) {
	            String line = scan.nextLine();
	            String[] lineArray = line.split(",");
	            
	            for (int i = 0; i < lineArray.length; i++)
	            {
	            	this.stopWordsList.add(lineArray[i]);
	            }
	            // do something with lineArray, such as instantiate an object
	        }
	  
	    } finally {
	        scan.close();
	    }
	}
	
	private static double round (double value, int precision) {
	    int scale = (int) Math.pow(10, precision);
	    return (double) Math.round(value * scale) / scale;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Jedis jedis = JedisMaker.make();
		
		JedisIndexer jedisIndexer = new JedisIndexer (jedis);
		Indexer index = new Indexer(jedisIndexer);
		
		//index.deleteTermCounters();
		//index.deleteURLSets();
		//index.deleteAllKeys();
		loadIndex(index);
		
		Map<String, Double> map = index.getCountsFaster("the");
		for (Entry<String, Double> entry: map.entrySet()) {
			System.out.println(entry);
		}
	}

	
}
