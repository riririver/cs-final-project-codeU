# JAVA CS Final Project

## Introduction

Congratulations!! You have successfully completed all of the labs and readings in this course. You could stop here...but your wikipedia searcher is pretty limited in functionality. If search engines just researched Wikipedia it wouldn't be super interesting, we need to search more websites!

This project won't come with any tests, it will just be coming with some possible directions to take your searcher. You can go super deep into one, and optimize it as much as possible, or jump around and implement many different options. It's up to you. There are no tests to guide you step by step. Use your imagination and technical skills to provide the most elegant and optimum solution to this searcher.

## What to Add

### Add a Command Line Front End

While this option doesn't use much in the way of CS theory, it certainly makes your application way more fun to use. Adding a Command Line Front End so anyone can make it way more fun to play with and show your friends. Feel free to just use the built in [Command Line parsing](https://docs.oracle.com/javase/tutorial/essential/environment/cmdLineArgs.html), but we recommend a library to make your command line parsing much easier to read. [JOpt-Simple](https://pholser.github.io/jopt-simple/) is a simple one!

**Pretty straight forward addition. Students can also add web interface if that's more compelling. The [Spark Framework](http://sparkjava.com/) is a great, simple web framework for Java.**

### Better Search Results

  * Our ranking algorithm is a bit simple. Go ahead and implement [TF-IDF](https://en.wikipedia.org/wiki/Tf%E2%80%93idf) as an initial attempt at getting better word importance scores. You may have to modify your `JavaIndex` to compute document frequencies.

  **This gets a bit harder. They will need to change the way we do term counting in the `JavaIndex` file. [This is a good tutorial on TF-IDF](https://guendouz.wordpress.com/2015/02/17/implementation-of-tf-idf-in-java/). The other option is having the students work with [Lucene](https://lucene.apache.org/core/index.html) which will implemnt IDF for them. Much less educational. Might be nice to have them implement TF-IDF first, and then have them swap it out with Lucene.**

  * When dealing with `AND` queries, we just sum up relevance for each term. We're programmers so we always think of the worst case...how could that not be the correct solution? What can you do to fix some of those problems?

  **Pretty much there will now be a correlation between word usage and results. What if they searched "Phones AND Camera Phones". Deal with filtering out common terms such as "the" as well as double words.**

  * Currently you are just doing a strict term count. In the philosophy lab, we built out the idea of pages linking to each other. Create a ranker that ranks pages by how often they are linked to. Combine this with term counts to try and get the most relevant pages for that specific term!

  **Guiding students through levels of complexity is important here. First just have them build a basic PageRank using link counting. Should be similar to the term counting algorithms. Make sure to point out how do they want to store the data? How do they want to factor in the idea of the same page linking to another multiple times? Is that weighted differently? Once they finish this, having them start to think of second order page ranking. If a page with a high page rank, links heavily to another that should be worth more than if a low page rank links. There is a ton for students to do here**

  * You can also easily start analyzing the images that are available on web pages. Writing your own computer vision is a bit outside of the scope of this program, you can use external APIs like [Google Vision](https://cloud.google.com/vision/) to get started.

  **Google Computer Vision only give 1,000 connections a month which isn't a ton, but it's cheap to get more. There are other APIs as well. [Microsoft](https://www.microsoft.com/cognitive-services/en-us/computer-vision-api) has one where you'd get 5,000 transactions per month**

  * Let's bring context into our term counting. A term in a `h1` should matter much more than just some term in a `p` tag. Use the different HTML tags to apply different weightings to the different results.

  **They will have to use JSoup (the HTML parser we provide them) to figure out what tag the term they are looking at is currently on. JSoup has some very Javascript like features to figure out what tags the current element is sitting in. Another concern students should be focused on is how are you storing this data? In the effort to just go up levels of complexity, the ismplest way is to just have a look-up of different values to add to the "count" given the different surrounding tags. This leads to an interesting OO design problem. How can oyu make that look up table in a modular way?**


### Faster Search Results

  * We have to index every single word in a page. Is that necessary? Can you think of a way to reduce the total number of words analyzed? 

  **Basic common word filtering. Having them do some basic performance analyis before making this change and after is smart. Has them show the difference in performance in a more visceral way.**

  * While it's difficult to get our searching algorithm significantly faster, we can make it *feel* faster by streaming results. Implement a results stream. As results come in start printing them out. As you get updated results, display the updated results. 

  **The hardest part here really is how to display to the actual user? With a basic CLI app, maybe just displaying status and a progress bar.**

### More Search Results

  * Can you expand this beyond just Wikipedia? How would you expand this to not just live in the Wikipedia world? What implications does that have on the performance characteristics?

  **Point out the issue of cycles is going to get you. Also starting to think about how large the database can get. This is hard to do at scale, but guiding students to figure out different "boundaries". One could be no more than 5 degrees removed from a start page? Another could be just making sure your database doesn't get bigger than some size**
