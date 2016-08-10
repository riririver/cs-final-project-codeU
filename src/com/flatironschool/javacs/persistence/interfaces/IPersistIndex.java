package com.flatironschool.javacs.persistence.interfaces;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.flatironschool.javacs.TermCounter;

public interface IPersistIndex {

	 public boolean isIndexed (String url);
	 
	 public Set<String> getUrls (String term);
	 
	 public Map<String, Double> getUrlToTermCount (String term);
	 
	 public double getTotalTermCount (String url);
	 
	 public Integer getCount (String url, String term);
	 
	 public void pushTermCounter (TermCounter tc);
	 
	 public Set<String> urlSetKeys();
	 
	 public void deleteURLSets();
	 
	 public void deleteTermCounters();
	 
	 public void deleteAllKeys();
	 
	 public Set<String> termCounterKeys();
	 
	 	 
}
