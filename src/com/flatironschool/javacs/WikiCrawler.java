package com.flatironschool.javacs;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;

public class WikiCrawler {
	// keeps track of where we started
	private final String source;

	// the index where the results go
	private Indexer index;

	// queue of URLs to be indexed
	private Queue<Link> queue = new LinkedList<Link>();

	// fetcher used to get pages from Wikipedia
	final static WikiFetcher wf = new WikiFetcher();

	// ranks each link
	Ranker ranker;

	/**
	 * Constructor.
	 * 
	 * @param source
	 * @param index
	 */
	public WikiCrawler(String source, Indexer index, Ranker ranker) {
		this.source = source;
		this.index = index;
		this.ranker = ranker;

		Link base = new Link(source, 0);
		queue.offer(base);
	}

	/**
	 * Returns the number of URLs in the queue.
	 * 
	 * @return
	 */
	public int queueSize() {
		return queue.size();
	}

	/**
	 * Gets a URL from the queue and indexes it.
	 * 
	 * @param b
	 * 
	 * @return Number of pages indexed.
	 * @throws IOException
	 */
	public String crawl(boolean testing) throws IOException {
		// pop the first link from the queue

		Link urlLink = queue.poll();

		if (!testing && index.isIndexed(urlLink.getUrl()))
			return null;

		Elements paragraphs = wf.readWikipedia(urlLink.getUrl());
		index.indexPage(urlLink.getUrl(), paragraphs);

		queueInternalLinks(paragraphs, 0);

		return urlLink.getUrl();

	}

	public void crawlAll(int depth) throws IOException {
		// pop the first link from the queue
		
		boolean readFromResource = true;

		while (!queue.isEmpty()) {
			Link urlLink = queue.poll();

			if (ranker != null)
				ranker.increment(urlLink.getUrl());

			if (index.isIndexed(urlLink.getUrl())) {
				//System.out.println("Skipping URL " + urlLink.getUrl() + " as already indexed");
				continue;
			}

			
		   Elements paragraphs = wf.readDocument(urlLink.getUrl(), readFromResource); 
		   if  (paragraphs == null) { 
			   if (urlLink.getUrl().equals(this.source))
			   {
				   System.out.println ("Source not found in resources, so trying to load from internet");
				   readFromResource = false;
				   paragraphs = wf.readDocument(urlLink.getUrl(), readFromResource); 
			   }
		   }
		   
		   if (paragraphs == null)
			   continue;
			
			System.out.println("Indexing url " + urlLink.getUrl() + " depth = " + urlLink.depth);
			index.indexPage(urlLink.getUrl(), paragraphs);

			// do not add links from this page if we are already at the depth
			if ((urlLink.depth + 1) <= depth) {
				queueInternalLinks(paragraphs, urlLink.depth + 1);
			
			}
		}

	}

	/**
	 * Parses paragraphs and adds internal links to the queue.
	 * 
	 * @param paragraphs
	 */
	// NOTE: absence of access level modifier means package-level
	void queueInternalLinks(Elements paragraphs, int depth) {

		for (Element para : paragraphs) {
			addLinks(para, depth);

		}

	}

	
	private void addLinksEx(Element para, int depth) throws IOException {

		Iterable<Node> iter = new WikiNodeIterable(para);

		for (Node node : iter) {

			if (node instanceof Element) {
				Element element = (Element) node;
				if (element.tagName().equals("a")) {
					String href = element.attr("href");

					if (queue.contains(href)) {
						System.out.println("Getting into a loop");
						continue;
					} else if (href.startsWith("/wiki")) {

						String completeUrl = "https://en.wikipedia.org" + href;
						// if (depth > 0)
						// System.out.println ("Adding link to queue " +
						// completeUrl + " at depth " + depth);
						queue.add(new Link(completeUrl, depth));

					}

				}

			}

		}

	}

	private void addLinks(Element paragraph, int depth) {

		// get the links
		Elements elts = paragraph.select("a[href]");
		for (Element elt : elts) {
			String relURL = elt.attr("href");

			//get only internal links
		//	if (relURL.startsWith("/wiki/")) {
			if (relURL.startsWith("/")) {
				String absURL = elt.attr("abs:href");
				queue.offer(new Link(absURL, depth));
			}
		}

	}

	

	
}
