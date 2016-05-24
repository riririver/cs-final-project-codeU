# JAVA CS Final Project

## Introduction

Congratulations!! You have successfully completed all of the labs and readings in this course. You could stop here...but your wikipedia searcher is pretty limited in functionality. If search engines just researched Wikipedia it wouldn't be super interesting, we need to search more websites!

This project won't come with any tests, it will just be coming with some possible directions to take your searcher. You can go super deep into one, and optimize it as much as possible, or jump around and implement many different options. It's up to you. There are no tests to guide you step by step. Use your imagination and technical skills to provide the most elegant and optimum solution to this searcher.

## What to Add

### Add a Command Line Front End

While this option doesn't use much in the way of CS theory, it certainly makes your application way more fun to use. Adding a Command Line Front End so anyone can make it way more fun to play with and show your friends. Feel free to just use the built in [Command Line parsing](https://docs.oracle.com/javase/tutorial/essential/environment/cmdLineArgs.html), but we recommend a library to make your command line parsing much easier to read. [JOpt-Simple](https://pholser.github.io/jopt-simple/) is a simple one!

### Better Search Results

  * Our ranking algorithm is a bit simple. Go ahead and implement [TF-IDF](https://en.wikipedia.org/wiki/Tf%E2%80%93idf) as an initial attempt at getting better word importance scores. You may have to modify your `JavaIndex` to compute document frequencies.
  * When dealing with `AND` queries, we just sum up relevance for each term. We're programmers so we always think of the worst case...how could that not be the correct solution? What can you do to fix some of those problems?
  * Currently you are just doing a strict term count. In the philosophy lab, we built out the idea of pages linking to each other. Create a ranker that ranks pages by how often they are linked to. Combine this with term counts to try and get the most relevant pages for that specific term!
  * You can also easily start analyzing the images that are available on web pages. Writing your own computer vision is a bit outside of the scope of this program, you can use external APIs like [Google Vision](https://cloud.google.com/vision/) to get started.

### Faster Search Results

  * We have to index every single word in a page. Is that necessary? Can you think of a way to reduce the total number of words analyzed? 
  * While it's difficult to get our searching algorithm significantly faster, we can make it *feel* faster by streaming results. Implement a results stream. As results come in start printing them out. As you get updated results, display the updated results. 

### More Search Results

  * Can you expand this beyond just Wikipedia? How would you expand this to not just live in the Wikipedia world? What implications does that have on the performance characteristics?
