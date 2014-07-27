package com.getactivities;

import java.io.BufferedReader;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;



@Path("/event")
public class Crawler {
	static String rootDomain;
	static Set<String> urlSet = new HashSet<String>();
	static Set<String> doneUrlSet = new HashSet<String>();
	static Set<String> activitySet = new HashSet<String>(); 	// final set that contains activities

	//download the data from the url
	public String getData(String url){
		if(url == null) return null;
		StringBuilder sb=null;
		HttpURLConnection con=null;

		try {
			// create the url connection
			URL myUrl = new URL(url);
			con = (HttpURLConnection) myUrl.openConnection();
			con.setRequestMethod("GET");

			//check if status was success
			if(con.getResponseCode()!=200){
				return null;
			}


			BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String strTemp = "";
			sb = new StringBuilder(); 

			//Save the rootDomain
			if(rootDomain == null)
				rootDomain = "http://" + myUrl.getHost();

			//read data to a string..	            
			while((strTemp = br.readLine())!= null ){
				//Dnt add empty lines
				if(!strTemp.matches("\\s+"))
					sb.append(strTemp);
			}
			br.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		finally{
			con.disconnect();
		}

		return sb.toString();

	}


	//get the links from this url and add to set	
	private void getUrlFrmPage(String urlData){
		// gets the links from href	
		Pattern linkPattern = Pattern.compile("<a[^>]+href=[\"']?[\"'>]+[\"']?([^>]*)[\"'>]+>(.+?)</a>", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
		Matcher linkMatcher = linkPattern.matcher(urlData);

		while(linkMatcher.find()){
			// get only links that start with /
			String link = linkMatcher.group(1);
			if(link.matches("^/.+"))
			{
				// Add only if its not been processed
				if(!doneUrlSet.contains(link))
					urlSet.add(link);
			}
		}
	}


	//check if its activity or not and add to activity set..
	private void checkActivity(){
		//from url set check if this link is activity.
		for(String currUrl: urlSet){
			//add the url to  dneUrlSet
			doneUrlSet.add(currUrl);

			//till you get 10 activity keep checking links
			if(activitySet.size()<10){
				String currUrlData = getData(rootDomain+currUrl);
				if(currUrlData!=null) {
					boolean res = verifyActivity(currUrlData);
					//if page contains activity add it to activitySet
					if(res == true){
						activitySet.add(rootDomain+currUrl);
					}
				}
			}
			else break;
		}
		urlSet.clear();

	}

	// chk if its a activity by checking for patterns
	private boolean verifyActivity(String currUrlData){
		//Check for title if it contains events,exhibitions or workshop
		// this is taking 2 long ..need to optimize.
		Pattern dataPattern = Pattern.compile(".*<head.*?>.*?(<title.*?>.*?</title>).*?",Pattern.CASE_INSENSITIVE);
		Matcher dataMatcher = dataPattern.matcher(currUrlData);

		if(dataMatcher.find()){
			String titleData = dataMatcher.group(1);
			// (?i) for case insenstive
			if(titleData.matches("(?i).*(exhibition|exhibitions|event|events|workshop|workshops).*")) return true; 
			else{
				if(currUrlData.matches("(?i).*(purchase|buy|get)*\\s*ticket.*")) return true;
				else if(currUrlData.matches("(?i).*sign\\s*up|enroll.*")) {
					return true;
				}
			}

		}

		return false;
	}

	@POST
	@Path("/url")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html") 
	public static String start(@FormParam("url") String url) {
		Crawler c = new Crawler();

		String data = c.getData(url);
		if(data!=null){
			c.getUrlFrmPage(data);
			c.checkActivity();
		}
		//if at end activity set is still less than 10 ,take main page and check all links frpm that else send back the results.
		String data1 = c.getData(rootDomain);
		if(data1!=null){
			c.getUrlFrmPage(data1);
			c.checkActivity();
		}
		System.out.println("final result :\n");
		// Send the results as string
		StringBuilder activity = new StringBuilder(10);
		for(String l:activitySet)
		{
			activity.append("<a href=\"" + l);
			activity.append("\" target=\"_blank\">" + l + "</a>" + "<br>");
			System.out.println(l);
		}
		//clear all data
		activitySet.clear();
		doneUrlSet.clear();
		urlSet.clear();
		rootDomain=null;


		return "<html> " + "<title>" + "Links of other Events" + "</title>"
		+ "<body>" + activity.toString() + "</body>" + "</html> ";

	}

	public static void main(String[] args) {
		String url = "http://events.stanford.edu/events/353/35309/";
		long startTime = System.currentTimeMillis();
		start(url);

		long endTime = System.currentTimeMillis();

		long duration = endTime - startTime;
		System.out.println("duration is:" + duration);
	}

}
