package edu.columbia.watson.twitter;
/**
 * @author qiaoyu
 */
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.mahout.math.Vector;

import edu.columbia.watson.twitter.util.GlobalProperty;



public class AnswerRanking {

	private static Logger logger = Logger.getLogger(AnswerRanking.class);

	
	/**
	 * get the most relevant K tweet IDs by calculating cosine value
	 * between the query and the corpus
	 * @return K most relevant tweets
	 * @throws IOException 
	 * @throws SQLException 
	 */
	public static List<IDCosinePair> getTopKAnswer(Vector queryVector, List<Long> relevantTweetIDList) throws SQLException, IOException {
		PriorityQueue<IDCosinePair> allCosValues = new PriorityQueue<IDCosinePair>();
		//Set<Entry<Long, Vector>> corpusVectorEntrySet = CorpusVectorCache.getInstance().getAllVectors().entrySet();
		Set<Entry<Long, Vector>> corpusVectorEntrySet = CorpusVectorCache.getInstance().getCorpusVector(relevantTweetIDList).entrySet();
		
		int count = 0;
		for (Entry<Long, Vector> entry : corpusVectorEntrySet){
			if (++count % 10000 == 0)
				logger.info(count + " cosine values calculated");

			Vector corpusVector = entry.getValue();
			//logger.info("corpusVector size = " + corpusVector.size());
			//logger.info("queryVector size = " + queryVector.size());
			Double cosine = corpusVector.dot(queryVector) / (Math.sqrt(corpusVector.getLengthSquared()) * Math.sqrt(queryVector.getLengthSquared()));
			//logger.info("id = " + entry.getKey() + ", cosine = " + cosine);
			IDCosinePair newPair = new IDCosinePair(entry.getKey(),cosine);
			if (allCosValues.size() < GlobalProperty.getInstance().getK()){		// use a min-heap to maintain the K largest cosine values
				allCosValues.add(newPair);
			}
			else if (allCosValues.peek().getCosine() < newPair.getCosine()){
				allCosValues.poll();
				allCosValues.add(newPair);
			}
		}
		logger.info("allCosValues size = " + allCosValues.size());
		List<IDCosinePair> result = new ArrayList<IDCosinePair>();
		while (allCosValues.size() > 0)
			result.add(allCosValues.poll());
		Collections.reverse(result);		//sort in descending order
		logger.info(result.toString());
		return result;
	}


	public static class IDCosinePair implements Comparable<IDCosinePair>
	{
		private long ID;
		private Double cosine;
		public IDCosinePair(long tweetID, Double cos){
			ID = tweetID;
			cosine = cos;
		}

		@Override
		public int compareTo(IDCosinePair arg0) {
			return cosine.compareTo(arg0.cosine);
		}

		public long getID() {
			return ID;
		}

		public Double getCosine(){
			return cosine;
		}

		@Override
		public String toString() {
			return "IDCosinePair [ID=" + ID + ", cosine=" + cosine + "]";
		}	

	}

}
