import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

public class POCTestReporter implements Runnable {
	POCTestResults testResults;
	MongoClient mongoClient;
	POCTestOptions testOpts;

	POCTestReporter(POCTestResults r, MongoClient mc, POCTestOptions t) {
		mongoClient = mc;
		testResults = r;
		testOpts = t;

	}


	private void logData()
	{
		PrintWriter outfile = null;
		
		if(testOpts.logfile != null)
		{
			
			try {
			     outfile = new PrintWriter(new BufferedWriter(new FileWriter(testOpts.logfile, true)));
			} catch (IOException e) {
			   System.out.println(e.getMessage());
			}
		}
		
		if (testOpts.sharded && !testOpts.singleserver) {
		DB configdb = mongoClient.getDB("config");
		DBCollection shards = configdb.getCollection("shards");
		//DBCursor cursor = (DBCursor) shards.find();
		int numShards = (int)shards.count();
		
		}
		
		Long insertsDone = testResults.GetOpsDone("inserts");
		if (testResults.GetSecondsElapsed() < testOpts.reportTime)
			return;
		System.out.println("------------------------");
		if (testOpts.sharded && !testOpts.singleserver) {
			DB configdb = mongoClient.getDB("config");
			DBCollection shards = configdb.getCollection("shards");
			//DBCursor cursor = (DBCursor) shards.find();
			int numShards = (int)shards.count();
			//System.out.format("%d Shards in use\n",numShards);
			testOpts.numShards = numShards;
			}
		System.out.format("After %d seconds, %d new records inserted - collection has %d in total \n",
				testResults.GetSecondsElapsed(), insertsDone, testResults.initialCount + insertsDone);
		
		if(outfile != null)
		{
			outfile.format("%d,%d", testResults.GetSecondsElapsed(), insertsDone);
		}
		
		
		HashMap<String, Long> results = testResults
				.GetOpsPerSecondLastInterval();
		String[] opTypes = POCTestResults.opTypes;

		for (String o: opTypes)
		{
			System.out.format("%d %s per second since last report ",
					results.get(o), o);
			
			if(outfile != null)
			{
				outfile.format(",%s,%d", o,results.get(o));
			}
			
			
			Long opsDone = testResults.GetOpsDone(o);
			if (opsDone > 0) {
				Double fastops = 100 - (testResults.GetSlowOps(o) * 100.0)
						/ opsDone;
				System.out.format("%.2f %% in under %d milliseconds", fastops,
						testOpts.slowThreshold);
				if(outfile != null)
				{
					outfile.format(",%.2f", fastops);
				}
			} else {
				System.out.format("%.2f %% in under %d milliseconds",(float)100,testOpts.slowThreshold);
				if(outfile != null)
				{ outfile.format(",%d", 100);}
			}
			System.out.println();
		
		}
		if(outfile != null)
		{ outfile.println();
		outfile.close();
		}
		System.out.println();
	}
	
	public void run() {

		logData();
	
	}
}