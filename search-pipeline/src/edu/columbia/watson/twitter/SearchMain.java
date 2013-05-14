package edu.columbia.watson.twitter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.apache.mahout.math.Vector;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import edu.columbia.watson.twitter.AnswerRanking.IDCosinePair;


/**
 * Entry point of the searching pipeline
 * @author qiaoyu
 *
 */

public class SearchMain {

	private static Logger logger = Logger.getLogger(SearchMain.class);

	public void run(String topicFileName, String outputFileName) throws DOMException, ParserConfigurationException, SAXException, IOException, ParseException, org.apache.lucene.queryparser.classic.ParseException, SQLException {
		logger.info("Initializing query parser class");
		List<QueryClause> queryList = QueryParser.getAllQueriesFromFile(topicFileName);
		logger.info("Initializing document retrieval class");
		DocumentRetrieval documentFetcher = new DocumentRetrieval();
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName));
		for (QueryClause query : queryList){
			Long linkedID = query.getLinkedTweetID();
			String linkedTweet = query.getQuery();
			try {
				linkedTweet += " " + documentFetcher.retrieveLinkedTweetByID(linkedID);
			} catch (SQLException e) {
				logger.error("Error getting linked tweet, tweet id = " + linkedID);
				logger.error(e);
			}
			List<Long> relevantID = documentFetcher.retrieveAllRelevantTweetID(linkedTweet);
			logger.info("Before query: " + query.getQueryNumber() + " linked tweet = " + linkedTweet);
			Vector queryVector = QueryVectorization.getLSAQueryVector(linkedTweet);
			List<IDCosinePair> answerList = AnswerRanking.getTopKAnswer(queryVector, relevantID);
			logger.info("After query: " + query.getQueryNumber() + " linked tweet = " + linkedTweet);
			List<TrecResult> result = new ArrayList<TrecResult>();
			int rank = 0;
			for (IDCosinePair pair : answerList){
				if (pair.getID() < query.getLinkedTweetID()){
					result.add(new TrecResult(query.getQueryNumber(), pair.getID(), rank++, pair.getCosine().floatValue(), "alphaRun"));
				}
			}
		}
		out.close();

	}

	public void runBaseline(String topicFileName, String outputFileName) throws DOMException, ParserConfigurationException, SAXException, IOException, ParseException, org.apache.lucene.queryparser.classic.ParseException{
		logger.info("Initializing query parser class");
		List<QueryClause> queryList = QueryParser.getAllQueriesFromFile(topicFileName);
		logger.info("Initializing document retrieval class");
		DocumentRetrieval luceneHelper = new DocumentRetrieval();
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName));
		for (QueryClause query : queryList){
			Long linkedID = query.getLinkedTweetID();
			String linkedTweet = "";
			try {
				linkedTweet = luceneHelper.retrieveLinkedTweetByID(linkedID);
			} catch (SQLException e) {
				logger.error("Error getting linked tweet, tweet id = " + linkedID);
				logger.error(e);
			}
			logger.info("Before query: " + query.getQueryNumber());
			List<TrecResult> result = luceneHelper.retrieveAllRelevantDocuments(query.getQueryNumber(), query.getQuery() + " " + linkedTweet);
			logger.info("After query: " + query.getQueryNumber());
			for (TrecResult item : result){
				if (item.getTweetID() < query.getLinkedTweetID())
					//if less than tweetqueryid and earlier than query time
					out.write(item.toString());
			}
		}
		out.close();
	}


	public static void main(String [] args) throws DOMException, ParserConfigurationException, SAXException, IOException, ParseException, org.apache.lucene.queryparser.classic.ParseException, SQLException
	{
		if (args.length!= 2){
			System.err.println("Usage: run.sh edu.columbia.watson.twitter.SearchMain query_file output_file");
			return;
		}
		SearchMain driver = new SearchMain();
		driver.run(args[0],args[1]);
	}

}
