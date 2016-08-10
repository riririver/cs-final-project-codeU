package com.flatironschool.javacs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.flatironschool.javacs.WikiSearch.MyFormatter;
import com.flatironschool.javacs.WikiSearch.Operation;
import com.flatironschool.javacs.persistence.impl.JavaIndexer;
import com.flatironschool.javacs.persistence.impl.JedisIndexer;

import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import redis.clients.jedis.Jedis;

public class SearchEngine {
	
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
				//accepts("o", "search operation").requiredIf("s").withRequiredArg().ofType(Operation.class)
				//		.describedAs("search operation, can have values 'AND' or 'OR'").defaultsTo(Operation.AND);
				accepts("a", "analyze images of the site").withRequiredArg().ofType(URI.class).describedAs("url to analyze");
				
			}
		};

		parser.nonOptions("Enter options:");
		parser.formatHelpWith(new MyFormatter());
		parser.printHelpOn(System.out);

		Scanner scanner = new Scanner(System.in);

		jedis = JedisMaker.make();
		
		//JedisIndexer jedisIndexer = new JedisIndexer (jedis);
		JavaIndexer javaIndexer = new JavaIndexer();
		index = new Indexer(javaIndexer);
		
		Ranker ranker = new Ranker ();

		boolean quit = false;
		

		try {
			do {

				String input = scanner.nextLine();

				String[] tokens = input.split(" ");

				OptionSet optionSet = null;
				
				try
				{
				   optionSet = parser.parse(tokens);
				}
				catch (Exception e)
				{
					System.out.println (e.getMessage());
					continue;
				}

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
				} else if (optionSet.hasArgument("s")) {
				 
				
					List<?> terms = optionSet.valuesOf("s");
					
					List<WikiSearch> searches = new ArrayList<>();
					for (Object term : terms) {
						System.out.println("Query: " + term);

						WikiSearch search = WikiSearch.search((String) term, index, ranker);
						if (search != null)
							searches.add(search);
					}

					if (searches.size() > 0) {
						WikiSearch searchResult = WikiSearch.searchAll(searches, searches.get(0), 1);
						searchResult.print();
					} else
						System.out.println("No match found for terms(s) " + terms);

				}
				else if (optionSet.hasArgument("a")) {
					URI analyzeSite = (URI) optionSet.valueOf("a");
					try {
						ImageAnalyzer imageAnalyzer = new ImageAnalyzer();
						imageAnalyzer.analyze(analyzeSite.toString());
						System.out.println ("Done analyzing " + analyzeSite.toString());
					} catch (GeneralSecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			} while (!quit);
		} finally {
			if (scanner != null)
				scanner.close();
		}

	}

}
