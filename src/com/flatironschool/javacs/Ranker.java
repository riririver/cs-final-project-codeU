package com.flatironschool.javacs;

import java.util.HashMap;
import java.util.Map;

public class Ranker {

	Map<String, Integer> ranks;
	
	public Ranker ()
	{
		ranks = new HashMap<>();
	}
	
	public void increment (String url)
	{
		Integer linkedCount = ranks.get(url);
		if (linkedCount == null)
		{
			ranks.put(url, 1);
		}
		else
		{
			ranks.put(url, linkedCount+1);
		}
	}
	
	public Integer getLinkedCount (String url)
	{
		return ranks.get(url);
	}
}
