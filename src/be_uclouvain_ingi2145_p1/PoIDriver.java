package be_uclouvain_ingi2145_p1;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.varia.LevelRangeFilter;
import org.apache.hadoop.io.*;

import org.apache.hadoop.mapreduce.Job;
import init.*;
import iter.*;
import evaluate.*;
import neighbors.*;

public class PoIDriver extends Configured implements Tool
{
    /**
     * Singleton instance.
     */
    public static PoIDriver GET;
    public static Configuration CONF;

    /**
     * Author's name.
     */
    public static final String NAME = "VANDER SCHUEREN GREGORY";

    // ---------------------------------------------------------------------------------------------

    public static void main(String[] args) throws Exception
    {
        // configure log4j to output to a file
        Logger logger = LogManager.getRootLogger();
        logger.addAppender(new FileAppender(new SimpleLayout(), "p1.log"));

        // configure log4j to output to the console
        Appender consoleAppender = new ConsoleAppender(new SimpleLayout());
        LevelRangeFilter filter = new LevelRangeFilter();
        // switch to another level for more detail (own (INGI2145) messages use FATAL)
        filter.setLevelMin(Level.ERROR);
        consoleAppender.addFilter(filter);
        // (un)comment to (un)mute console output
        logger.addAppender(consoleAppender);

        // switch to Level.DEBUG or Level.TRACE for more detail
        logger.setLevel(Level.INFO);

        GET = new PoIDriver();
        CONF = new Configuration();

        int res = ToolRunner.run(CONF, GET, args);
        System.exit(res);
    }

    // ---------------------------------------------------------------------------------------------

    // Depending on your implementation the number of stages might differ
    // Consider this skeleton as a outline you should be able to change it according to your needs. 
    @Override
    public int run(String[] args) throws Exception
    {
        System.out.println(NAME);

        if (args.length == 0) {
            args = new String[]{ "command missing" };
        }

        switch (args[0]) {
            case "init":
                init(args[1], args[2], args[3], args[4], Integer.parseInt(args[5]));
                break;
            case "iter":
                iter(args[1], args[2], args[3], args[4], Integer.parseInt(args[5]), Integer.parseInt(args[6]));
                break;
            case "evaluate":
                evaluate(args[1], args[2], args[3], args[4], Integer.parseInt(args[5]));
                break;
            case "neighbors":
                neighbors(args[1], args[2], args[3], args[4], Integer.parseInt(args[5]));
                break;
            case "composite":
                composite(args[1], args[2], args[3], args[4], Integer.parseInt(args[5]), Integer.parseInt(args[6]));
                break;
            case "neighborsComposite":
            	neighborsComposite(args[1], args[2], args[3], args[4], Integer.parseInt(args[5]), Integer.parseInt(args[6]));
                break;
            default:
                System.out.println("Unknown command: " + args[0]);
                break;
        }

        return 0;
    }

    // ---------------------------------------------------------------------------------------------

    void init(String inputDir, String outputDir, String srcId, String dstId, int nReducers) throws Exception
    {
        Logger.getRootLogger().fatal("[INGI2145] init");
        CONF.set("srcId", srcId);
        CONF.set("dstId", dstId);
        Job initJob = Utils.configureJob(inputDir, outputDir, InitMapper.class, null, InitReducer.class, Text.class, Text.class, Text.class, Text.class, nReducers);
        initJob.waitForCompletion(true);
    }

    // ---------------------------------------------------------------------------------------------
    //iterNo is the value of iteration counter

    void iter(String inputDir, String outputDir, String srcId, String dstId, int iterNo, int nReducers) throws Exception
    {
        Logger.getRootLogger().fatal("[INGI2145] iter: " + inputDir + " (to) " + outputDir);
        CONF.set("srcId", srcId);
        CONF.set("dstId", dstId);
        Job iterJob = Utils.configureJob(inputDir, outputDir, IterMapper.class, null, IterReducer.class, Text.class, Text.class, Text.class, Text.class, nReducers);
        iterJob.waitForCompletion(true);
    }


    // ---------------------------------------------------------------------------------------------

    void evaluate(String inputDir, String outputDir, String srcId, String dstId, int nReducers) throws Exception
    {
        Logger.getRootLogger().fatal("[INGI2145] evaluate from:" + inputDir);
        CONF.set("srcId", srcId);
        CONF.set("dstId", dstId);
        Job evaluateJob = Utils.configureJob(inputDir, outputDir, EvaluateMapper.class, null, EvaluateReducer.class, Text.class, Text.class, Text.class, Text.class, nReducers);
        evaluateJob.waitForCompletion(true);
    }

    // ---------------------------------------------------------------------------------------------
    // maxHops is the maximum number of hops in which the destination should be reached
    void composite(String inputDir, String outputDir, String srcId, String dstId, int maxHops, int nReducers) throws Exception
    {
        Logger.getRootLogger().fatal("[INGI2145] composite: " + inputDir + " (to) " + outputDir);
        CONF.set("srcId", srcId);
        CONF.set("dstId", dstId);
        
        // A first -1 since we expand 1 HOP during the initial phase.
        // Another -1 since we can stop when we reach a neighbor 1 hop away of the destination.
        int iterationsLeft = maxHops - 2;
        int iterNo = 0;
        
        init(inputDir, "/tmp/iter"+iterNo, srcId, dstId, nReducers);
        evaluate("/tmp/iter"+iterNo, outputDir, srcId, dstId, nReducers);
        
        while (iterationsLeft > 0 && !hasResultAtPath(outputDir)) {
        	iter("/tmp/iter"+iterNo, "/tmp/iter"+(++iterNo), srcId, dstId, iterNo, nReducers);  
        	evaluate("/tmp/iter"+iterNo, outputDir, srcId, dstId, nReducers);
        	iterationsLeft--;
        }
    }
    
    // ---------------------------------------------------------------------------------------------
   
    void neighbors(String inputDir, String outputDir, String srcId, String dstId, int nReducers) throws Exception
    {
        Logger.getRootLogger().fatal("[INGI2145] neighbors from:" + inputDir);
        CONF.set("srcId", srcId);
        CONF.set("dstId", dstId);
        Job neighborsJob = Utils.configureJob(inputDir, outputDir, EvaluateMapper.class, null, NeighborsReducer.class, Text.class, Text.class, Text.class, Text.class, nReducers);
        neighborsJob.waitForCompletion(true);
    }
    
    // ---------------------------------------------------------------------------------------------
    
    void neighborsComposite(String inputDir, String outputDir, String srcId, String dstId, int maxHops, int nReducers) throws Exception
    {
        Logger.getRootLogger().fatal("[INGI2145] neighborsComposite: " + inputDir + " (to) " + outputDir);
        CONF.set("srcId", srcId);
        CONF.set("dstId", dstId);
        
        // A first -1 since we expand 1 HOP during the initial phase.
        // Another -1 since we can stop when we reach a neighbor 1 hop away of the destination.
        int iterationsLeft = maxHops - 2;
        int iterNo = 0;
        
        init(inputDir, "/tmp/iter"+iterNo, srcId, dstId, nReducers);
        neighbors("/tmp/iter"+iterNo, outputDir, srcId, dstId, nReducers);
        
        while (iterationsLeft > 0 && !hasResultAtPath(outputDir)) {
        	iter("/tmp/iter"+iterNo, "/tmp/iter"+(++iterNo), srcId, dstId, iterNo, nReducers);  
        	neighbors("/tmp/iter"+iterNo, outputDir, srcId, dstId, nReducers);
        	iterationsLeft--;
        }
    }
    
    // ---------------------------------------------------------------------------------------------
    
    boolean hasResultAtPath(String path) throws Exception {
    	return Utils.checkResults(FileSystem.get(PoIDriver.GET.getConf()), new Path(path));
    }
}