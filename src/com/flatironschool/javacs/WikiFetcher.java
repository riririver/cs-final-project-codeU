package com.flatironschool.javacs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WikiFetcher {
	private long lastRequestTime = -1;
	private long minInterval = 1000;

	/**
	 * Fetches and parses a URL string, returning a list of paragraph elements.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public Elements fetchWikipedia(String url) throws IOException {
		sleepIfNeeded();

		// download and parse the document
		Connection conn = Jsoup.connect(url);
		Document doc = conn.get();

		// select the content text and pull out the paragraphs.
		Element content = doc.getElementById("mw-content-text");

		// TODO: avoid selecting paragraphs from sidebars and boxouts
		Elements paras = content.select("p");
		return paras;
	}

	/**
	 * Reads the contents of a Wikipedia page from src/resources.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public Elements readWikipedia(String url) throws IOException {

		URL realURL = new URL(url);

		// assemble the file name
		String slash = File.separator;
		String filename = "resources" + slash + realURL.getHost() + realURL.getPath();

		// read the file
		InputStream stream = WikiFetcher.class.getClassLoader().getResourceAsStream(filename);
		if (stream == null)
			return null;
		Document doc = Jsoup.parse(stream, "UTF-8", filename);
		doc.setBaseUri(realURL.getProtocol() + "://" + realURL.getHost());
		// TODO: factor out the following repeated code
		Element content = doc.getElementById("mw-content-text");
		Elements paras = content.select("p");
		return paras;

	}

	/**
	 * Reads the contents of a Wikipedia page from src/resources.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public Elements readDocument(String url, boolean readFromResources) throws IOException {

		URL realURL = new URL(url);
		Document doc = null;
		Elements elements = null;

		// assemble the file name

		try {
			if (readFromResources) {
				String slash = File.separator;
				String filename = "resources" + slash + realURL.getHost() + realURL.getPath();

				// read the file
				InputStream stream = WikiFetcher.class.getClassLoader().getResourceAsStream(filename);
				if (stream == null)
					return null;
				doc = Jsoup.parse(stream, "UTF-8", filename);
				doc.setBaseUri(realURL.getProtocol() + "://" + realURL.getHost());
			} else {
				Connection conn = Jsoup.connect(url);
				doc = conn.get();
			}
		} catch (Exception e) {
			System.out.println("Failed to get document " + url + "  , error = " + e.getMessage());

		}

		if (doc != null) {

			// check if mw-content-text is present
			Element content = doc.getElementById("mw-content-text");
			if (content != null)
				elements = content.select("p");
			else
				elements = doc.select("p");
		}

		return elements;

	}

	public Elements getAllImagedFromDocument(String url) throws IOException {

		URL realURL = new URL(url);

		// assemble the file name
		String slash = File.separator;
		String filename = "resources" + slash + realURL.getHost() + realURL.getPath();

		// read the file
		InputStream stream = WikiFetcher.class.getClassLoader().getResourceAsStream(filename);
		if (stream == null)
			return null;
		Document doc = Jsoup.parse(stream, "UTF-8", filename);
		doc.setBaseUri(realURL.getProtocol() + "://" + realURL.getHost());

		Elements elements = doc.select("img[src]");

		return elements;

	}

	/**
	 * Reads the contents of a Wikipedia page from src/resources.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public Elements fetchDocument(String url) throws IOException {

		Elements elements = null;
		sleepIfNeeded();

		Connection conn = Jsoup.connect(url);
		Document doc = conn.get();

		if (doc != null) {

			// check if mw-content-text is present
			Element content = doc.getElementById("mw-content-text");
			if (content != null)
				elements = content.select("p");
			else
				elements = doc.select("p");
		}

		return elements;

	}

	/**
	 * Rate limits by waiting at least the minimum interval between requests.
	 */
	private void sleepIfNeeded() {
		if (lastRequestTime != -1) {
			long currentTime = System.currentTimeMillis();
			long nextRequestTime = lastRequestTime + minInterval;
			if (currentTime < nextRequestTime) {
				try {
					// System.out.println("Sleeping until " + nextRequestTime);
					Thread.sleep(nextRequestTime - currentTime);
				} catch (InterruptedException e) {
					System.err.println("Warning: sleep interrupted in fetchWikipedia.");
				}
			}
		}
		lastRequestTime = System.currentTimeMillis();
	}
}
