package com.flatironschool.javacs;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.flatironschool.javacs.persistence.impl.JedisIndexer;

import java.util.Scanner;
import java.util.Set;

import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import redis.clients.jedis.Jedis;
import sun.net.dns.ResolverConfiguration.Options;

/**
 * Represents the results of a search query.
 *
 */
public class WikiSearch {

	public static enum Operation {
		AND, OR
	};

	// map from URLs that contain the term(s) to relevance score
	private Map<String, Double> map;

	/**
	 * Constructor.
	 * 
	 * @param map
	 */
	public WikiSearch(Map<String, Double> map) {
		this.map = map;
	}

	/**
	 * Looks up the relevance of a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public Double getRelevance(String url) {
		Double relevance = map.get(url);
		return relevance == null ? 0 : relevance;
	}

	/**
	 * Prints the contents in order of term frequency.
	 * 
	 * @param map
	 */
	void print() {
		List<Entry<String, Double>> entries = sort();
		
		Collections.reverse(entries);
		
		System.out.println ("********************************************************************************");
		System.out.println ("Here the URLs in order of relevance:");
		for (Entry<String, Double> entry : entries) {
			System.out.println(entry);
		}
	}

	/**
	 * Computes the union of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch or(WikiSearch that) {

		Map<String, Double> unionMap = new HashMap<String, Double>();

		unionMap.putAll(this.map);

		Set<String> thatKeys = that.map.keySet();

		Iterator<String> ii = thatKeys.iterator();

		while (ii.hasNext()) {
			String thatKey = ii.next();
			Double thatRelevance = that.map.get(thatKey);
			Double thisRelevance = unionMap.get(thatKey);

			if (thisRelevance != null)
				thatRelevance = thatRelevance + thisRelevance;

			unionMap.put(thatKey, thatRelevance);
		}

		return new WikiSearch(unionMap);
	}

	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch and(WikiSearch that) {
		Map<String, Double> andMap = new HashMap<String, Double>();

		Set<String> thisKeys = this.map.keySet();

		Iterator<String> ii = thisKeys.iterator();

		while (ii.hasNext()) {
			String thisKey = ii.next();
			Double thatRelevance = that.map.get(thisKey);

			if (thatRelevance != null) {
				Double thisRelevance = this.map.get(thisKey);
				thisRelevance = thatRelevance + thisRelevance;
				andMap.put(thisKey, thisRelevance);
			}

		}

		return new WikiSearch(andMap);
	}

	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch minus(WikiSearch that) {
		Map<String, Double> minusMap = new HashMap<String, Double>();

		Set<String> thisKeys = this.map.keySet();

		Iterator<String> ii = thisKeys.iterator();

		while (ii.hasNext()) {
			String thisKey = ii.next();
			Double thatRelevance = that.map.get(thisKey);

			if (thatRelevance == null) {
				Double thisRelevance = this.map.get(thisKey);
				minusMap.put(thisKey, thisRelevance);
			}

		}

		return new WikiSearch(minusMap);
	}

	/**
	 * Computes the relevance of a search with multiple terms.
	 * 
	 * @param rel1:
	 *            relevance score for the first search
	 * @param rel2:
	 *            relevance score for the second search
	 * @return
	 */
	protected int totalRelevance(Integer rel1, Integer rel2) {
		// simple starting place: relevance is the sum of the term frequencies.
		return rel1 + rel2;
	}

	/**
	 * Sort the results by relevance.
	 * 
	 * @return List of entries with URL and relevance.
	 */
	public List<Entry<String, Double>> sort() {

		List<Entry<String, Double>> listOfRel = new ArrayList<>();
		listOfRel.addAll(this.map.entrySet());
		Collections.sort(listOfRel, new Comparator<Entry<String, Double>>() {
			@Override
			public int compare(Entry<String, Double> arg0, Entry<String, Double> arg1) {

				return ((Double) arg0.getValue()).compareTo((Double) arg1.getValue());
			}
		});

		return listOfRel;
	}

	/**
	 * Performs a search and makes a WikiSearch object.
	 * 
	 * @param term
	 * @param index
	 * @return
	 */
	public static WikiSearch search(String term, Indexer index, Ranker ranker) {
		Map<String, Double> map = index.getCountWithRelevance(term, ranker);
		return new WikiSearch(map);
	}

	
	static WikiSearch searchAll(List<WikiSearch> searches, WikiSearch thatSearch, int count) {
		if (count >= searches.size())
			return thatSearch;

		WikiSearch thisSearch = searches.get(count);
		WikiSearch thisAndThatSearch = thatSearch.and(thisSearch);

		count++;
		WikiSearch searchResult = searchAll(searches, thisAndThatSearch, count);

		return searchResult;

	}

	static class MyFormatter implements HelpFormatter {
		public String format(Map<String, ? extends OptionDescriptor> options) {
			StringBuilder buffer = new StringBuilder();
			for (OptionDescriptor each : new HashSet<>(options.values())) {
				buffer.append(lineFor(each));
			}
			return buffer.toString();
		}

		private String lineFor(OptionDescriptor descriptor) {
			if (descriptor.representsNonOptions()) {
			/*	return descriptor.argumentDescription() + '(' + descriptor.argumentTypeIndicator() + "): "
						+ descriptor.description() + System.getProperty("line.separator");*/
				return descriptor.description() + System.getProperty("line.separator");
			}

			StringBuilder line = new StringBuilder(descriptor.options().toString());
			line.append(": description = ").append(descriptor.description());
			line.append(", required = ").append(descriptor.isRequired());
			line.append(", accepts arguments = ").append(descriptor.acceptsArguments());
			line.append(", requires argument = ").append(descriptor.requiresArgument());
			line.append(", argument description = ").append(descriptor.argumentDescription());
			line.append(", argument type indicator = ").append(descriptor.argumentTypeIndicator());
			line.append(", default values = ").append(descriptor.defaultValues());
			line.append(System.getProperty("line.separator"));
			return line.toString();
		}
	}

	public static void main(String[] args) throws IOException {

		Jedis jedis;
		WikiCrawler wc;
		Indexer index;

		OptionParser parser = new OptionParser() {
			{
				accepts("c", "site to crawl").withRequiredArg().ofType(URI.class).describedAs("url to crawl");
				accepts("d", "depth to crawl").requiredIf("c").withRequiredArg().ofType(Integer.class).describedAs("crawling depth");
				accepts("q", "quit");
				accepts("s", "search").withRequiredArg().describedAs("term1:term1..., must provide search operation")
						.ofType(String.class).withValuesSeparatedBy(":");
			//	accepts("o", "search operation").requiredIf("s").withRequiredArg().ofType(Operation.class)
			//			.describedAs("search operation, can have values 'AND' or 'OR'").defaultsTo(Operation.AND);
			
				/*
				 * accepts( "q" ).withOptionalArg().ofType( Double.class )
				 * .describedAs( "quantity" ); accepts( "d", "some date"
				 * ).withRequiredArg().required() .withValuesConvertedBy(
				 * datePattern( "MM/dd/yy" ) ); acceptsAll( asList( "v",
				 * "talkative", "chatty" ), "be more verbose" ); accepts(
				 * "output-file" ).withOptionalArg().ofType( File.class )
				 * .describedAs( "file" ); acceptsAll( asList( "h", "?" ),
				 * "show help" ).forHelp(); acceptsAll( asList( "cp",
				 * "classpath" ) ).withRequiredArg() .describedAs( "path1" +
				 * pathSeparatorChar + "path2:..." ) .ofType( File.class )
				 * .withValuesSeparatedBy( pathSeparatorChar ); nonOptions(
				 * "files to chew on" ).ofType( File.class ).describedAs(
				 * "input files" );
				 */
			}
		};

		parser.nonOptions("Enter options:");
		parser.formatHelpWith(new MyFormatter());
		parser.printHelpOn(System.out);

		Scanner scanner = new Scanner(System.in);

		jedis = JedisMaker.make();
		JedisIndexer jedisIndexer = new JedisIndexer (jedis);
		index = new Indexer(jedisIndexer);
		
		Ranker ranker = new Ranker ();

		boolean quit = false;

		try {
			do {

				String input = scanner.nextLine();

				String[] tokens = input.split(" ");

				OptionSet optionSet = parser.parse(tokens);

				if (optionSet.has("q")) {
					System.out.println("Quitting");
					break;
				}
				if (optionSet.hasArgument("c") && optionSet.hasArgument("d")) {
					URI crawlSite = (URI) optionSet.valueOf("c");
					Integer depth = (Integer) optionSet.valueOf("d");
					System.out.println("Crawling " + crawlSite.toString());
					WikiCrawler crawler = new WikiCrawler(crawlSite.toString(), index, ranker);
					crawler.crawlAll(depth);
					System.out.println("Done Crawling " + crawlSite.toString());
				} else if (optionSet.hasArgument("s") && optionSet.hasArgument("o")) {
				 
				
					List<?> terms = optionSet.valuesOf("s");
					
					List<WikiSearch> searches = new ArrayList<>();
					for (Object term : terms) {
						System.out.println("Query: " + term);

						WikiSearch search = search((String) term, index, ranker);
						if (search != null)
							searches.add(search);
					}

					if (searches.size() > 0) {
						WikiSearch searchResult = searchAll(searches, searches.get(0), 1);
						searchResult.print();
					} else
						System.out.println("No match found for terms(s) " + terms);

				}
			} while (!quit);
		} finally {
			if (scanner != null)
				scanner.close();
		}

	}
}
