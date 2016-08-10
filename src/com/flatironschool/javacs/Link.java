package com.flatironschool.javacs;

public class Link {
	
	String url;
	int depth;
	
	
	public Link(String url, int depth) {
		super();
		this.url = url;
		this.depth = depth;
	}


	public String getUrl() {
		return url;
	}


	public void setUrl(String url) {
		this.url = url;
	}


	public int getDepth() {
		return depth;
	}


	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	

}
