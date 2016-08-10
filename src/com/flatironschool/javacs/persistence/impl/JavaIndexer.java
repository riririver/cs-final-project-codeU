package com.flatironschool.javacs.persistence.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.flatironschool.javacs.TermCounter;
import com.flatironschool.javacs.persistence.interfaces.IPersistIndex;

import redis.clients.jedis.Transaction;

public class JavaIndexer implements IPersistIndex {

	
	Map<String, Map<String, Integer>> urlToTerms;
	Map<String, Set<String>> termToUrl;
	
	
	
	public JavaIndexer() {
		super();
		urlToTerms = new HashMap<>();
		termToUrl = new HashMap<>();
		
	}

	@Override
	public boolean isIndexed(String url) {
		String redisKey = termCounterKey(url);
		return urlToTerms.containsKey(redisKey);
	}

	@Override
	public Set<String> getUrls(String term) {
		return termToUrl.get(urlSetKey(term));
	}

	@Override
	public Map<String, Double> getUrlToTermCount(String term) {
		
		//get the urls where the term is present
		Set<String> urls = getUrls (term);
		
		//in each url find the count of this term
		Map<String, Double> map = new HashMap<String, Double>();
		for (String url : urls)
		{
			Map<String, Integer> termToCount = urlToTerms.get(termCounterKey(url));
			if (termToCount != null)
			{
				Integer count = termToCount.get(term);
				map.put(url, new Double (count));
			}
		}
		return map;
	}

	@Override
	public double getTotalTermCount(String url) {
		String redisKey = termCounterKey(url);
		Map<String, Integer> termToCount = urlToTerms.get(redisKey);
		
	
		Set<String> termKeys  = termToCount.keySet();
		double totalCountOfAllTerms = 0;
		for (String termKey : termKeys)
		{
			Integer count = new Integer (termToCount.get(termKey));
			totalCountOfAllTerms += count;
		}
		
		return totalCountOfAllTerms;
	}

	@Override
	public Integer getCount(String url, String term) {
		String redisKey = termCounterKey(url);
		Map<String, Integer> termToCount = urlToTerms.get(redisKey);
		Integer count = null;
		if (termToCount != null)
		{
			count = termToCount.get(term);
		}
		return count;
	}

	@Override
	public void pushTermCounter(TermCounter tc) {
		
		String url = tc.getLabel();
		String hashname = termCounterKey(url);
		
		
		// for each term, add an entry in the termcounter and a new
		// member of the index
		for (String term: tc.keySet()) {
			Integer count = tc.get(term);
			Map<String, Integer> termToCount = urlToTerms.get(hashname);
			if (termToCount == null)
			{
				termToCount = new HashMap<>();
				urlToTerms.put(hashname, termToCount);
			}
			termToCount.put(term, count);
			
			Set<String> urls = this.termToUrl.get(urlSetKey(term));
			if (urls == null)
			{
				urls = new HashSet<>();
				this.termToUrl.put(urlSetKey(term), urls);
			}
			urls.add(url);
			
		}
		return;
	}

	@Override
	public Set<String> urlSetKeys() {
		return this.termToUrl.keySet();
	}

	@Override
	public void deleteURLSets() {
		this.termToUrl.clear();

	}

	@Override
	public void deleteTermCounters() {
		this.urlToTerms.clear();

	}

	@Override
	public void deleteAllKeys() {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<String> termCounterKeys() {
		return this.urlToTerms.keySet();
	}
	
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

}
