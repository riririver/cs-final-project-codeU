package com.flatironschool.javacs.persistence.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.flatironschool.javacs.TermCounter;
import com.flatironschool.javacs.persistence.interfaces.IPersistIndex;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

public class JedisIndexer implements IPersistIndex {

	private Jedis jedis;

	public JedisIndexer(Jedis jedis) {
		this.jedis = jedis;
	}

	@Override
	public boolean isIndexed(String url) {
		String redisKey = termCounterKey(url);
		return jedis.exists(redisKey);
	}

	@Override
	public Set<String> getUrls(String term) {
		Set<String> set = jedis.smembers(urlSetKey(term));
		return set;
	}

	@Override
	public Map<String, Double> getUrlToTermCount(String term) {
		// convert the set of strings to a list so we get the
		// same traversal order every time
		List<String> urls = new ArrayList<String>();
		urls.addAll(getUrls(term));

		// construct a transaction to perform all lookups
		Transaction t = jedis.multi();
		for (String url : urls) {
			String redisKey = termCounterKey(url);
			t.hget(redisKey, term);
		}
		List<Object> res = t.exec();

		// iterate the results and make the map
		Map<String, Double> map = new HashMap<String, Double>();
		int i = 0;
		for (String url : urls) {
			Double count = new Double((String) res.get(i++));

			map.put(url, count);

		}

		return map;
	}

	@Override
	public Integer getCount(String url, String term) {
		String redisKey = termCounterKey(url);
		String count = jedis.hget(redisKey, term);
		return new Integer(count);
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

	@Override
	public void pushTermCounter(TermCounter tc) {
	Transaction t = jedis.multi();
		
		String url = tc.getLabel();
		String hashname = termCounterKey(url);
		
		// if this page has already been indexed; delete the old hash
		t.del(hashname);

		// for each term, add an entry in the termcounter and a new
		// member of the index
		for (String term: tc.keySet()) {
			Integer count = tc.get(term);
			t.hset(hashname, term, count.toString());
			t.sadd(urlSetKey(term), url);
		}
		t.exec();
		
		
	}

	@Override
	public void deleteURLSets() {
		Set<String> keys = urlSetKeys();
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
		
	}
	
	public Set<String> urlSetKeys() {
		return jedis.keys("URLSet:*");
	}
	
	public Set<String> termCounterKeys() {
		return jedis.keys("TermCounter:*");
	}
	
	public void deleteTermCounters() {
		Set<String> keys = termCounterKeys();
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
	}
	
	public void deleteAllKeys() {
		Set<String> keys = jedis.keys("*");
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
	}

	@Override
	public double getTotalTermCount(String url) {
		String redisKey = termCounterKey(url);
		Map<String, String> termToCount = jedis.hgetAll(redisKey);
		
	
		Set<String> termKeys  = termToCount.keySet();
		double totalCountOfAllTerms = 0;
		for (String termKey : termKeys)
		{
			Integer count = new Integer (termToCount.get(termKey));
			totalCountOfAllTerms += count;
		}
		
		return totalCountOfAllTerms;
	}

}
