package com.flatironschool.javacs;

import com.google.api.client.auth.oauth2.Credential;

/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionScopes;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ImageAnalyzer {
	/**
	 * Be sure to specify the name of your application. If the application name
	 * is {@code null} or blank, the application will log a warning. Suggested
	 * format is "MyCompany-ProductName/1.0".
	 */
	private static final String APPLICATION_NAME = "Google-VisionLabelSample/1.0";

	private static final int MAX_LABELS = 3;

	WikiFetcher wf;
	
	private final Vision vision;

	/**
	 * Constructs a {@link ImageAnalyzer} which connects to the Vision API.
	 * @throws GeneralSecurityException 
	 * @throws IOException 
	 */
	public ImageAnalyzer() throws IOException, GeneralSecurityException {
		this.vision = getVisionService();
		this.wf = new WikiFetcher();
	}
	

	/**
	 * Annotates an image using the Vision API.
	 */
	public static void main(String[] args) throws IOException, GeneralSecurityException {
		/*
		 * if (args.length != 1) {
		 * System.err.println("Missing imagePath argument.");
		 * System.err.println("Usage:");
		 * System.err.printf("\tjava %s imagePath\n",
		 * LabelApp.class.getCanonicalName()); System.exit(1); }
		 */
		Path imagePath;
		try {
			String slash = File.separator;
			imagePath = Paths.get(
					ImageAnalyzer.class.getClassLoader().getResource("resources" + slash + "faulkner.jpg").toURI());
			ImageAnalyzer app = new ImageAnalyzer();
			printLabels(System.out, imagePath, app.labelImage(imagePath, MAX_LABELS));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void analyze(String url ) throws IOException {
		// pop the first link from the queue

		Elements images = wf.getAllImagedFromDocument(url);
		

		if (images.size() > 0) {
			System.out.println(String.format("\nMedia: (%d)", images.size()));
			for (Element src : images) {
				if (src.tagName().equals("img"))
				{
					String imgSrc = src.attr("abs:src");
					analyzeImage (imgSrc);
					// assemble the file name
				/*	String slash = File.separator;
					String filename = "resources" + slash + realURL.getHost() + realURL.getPath();
					System.out.println(String.format(" * %s: <%s> %sx%s (%s)", src.tagName(), src.attr("abs:src"),
							src.attr("width"), src.attr("height"), trim(src.attr("alt"), 20)));*/
				}
				else
					System.out.println(String.format(" * %s: <%s>", src.tagName(), src.attr("abs:src")));
			}
		}

	}

	
	private void analyzeImage(String path) {
		
		try
		{
			URL realURL = new URL(path);

			// assemble the file name
			String slash = File.separator;
			String resourceImagePath = "resources" + slash + realURL.getHost() + realURL.getPath();
			
	    	Path imagePath = Paths.get(
				ImageAnalyzer.class.getClassLoader().getResource(resourceImagePath).toURI());
	    	printLabels(System.out, imagePath, labelImage(imagePath, MAX_LABELS));
		}
		catch (Exception e)
		{
			//do nothng
		}
		

	}

	/**
	 * Prints the labels received from the Vision API.
	 */
	public static void printLabels(PrintStream out, Path imagePath, List<EntityAnnotation> labels) {
		out.printf("Labels for image %s:\n", imagePath);
		for (EntityAnnotation label : labels) {
			out.printf("\t%s (score: %.3f)\n", label.getDescription(), label.getScore());
		}
		if (labels.isEmpty()) {
			out.println("\tNo labels found.");
		}
	}
	

	private static String trim(String s, int width) {
		if (s.length() > width)
			return s.substring(0, width - 1) + ".";
		else
			return s;
	}

	/**
	 * Connects to the Vision API using Application Default Credentials.
	 */
	public static Vision getVisionService() throws IOException, GeneralSecurityException {
		GoogleCredential credential = GoogleCredential.getApplicationDefault().createScoped(VisionScopes.all());
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		return new Vision.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, credential)
				.setApplicationName(APPLICATION_NAME).build();
	}

	

	/**
	 * Gets up to {@code maxResults} labels for an image stored at {@code path}.
	 */
	public List<EntityAnnotation> labelImage(Path path, int maxResults) throws IOException {
		byte[] data = Files.readAllBytes(path);

		AnnotateImageRequest request = new AnnotateImageRequest().setImage(new Image().encodeContent(data))
				.setFeatures(ImmutableList.of(new Feature().setType("LABEL_DETECTION").setMaxResults(maxResults)));
		Vision.Images.Annotate annotate = vision.images()
				.annotate(new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(request)));
		// Due to a bug: requests to Vision API containing large images fail
		// when GZipped.
		// annotate.setDisableGZipContent(true);

		BatchAnnotateImagesResponse batchResponse = annotate.execute();
		assert batchResponse.getResponses().size() == 1;
		AnnotateImageResponse response = batchResponse.getResponses().get(0);
		if (response.getLabelAnnotations() == null) {
			throw new IOException(response.getError() != null ? response.getError().getMessage()
					: "Unknown error getting image annotations");
		}
		return response.getLabelAnnotations();
	}
}
