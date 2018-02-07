package network.cli;

import network.Shared;
import network.core.Graph;
import network.core.GraphIO;
import network.core.Statistics;
import network.optimization.CPM;
import network.optimization.CPMParameters;
import network.optimization.CPMapParameters;
import network.signedmapequation.SiMap;
import org.apache.commons.cli.*;

public class Detection extends AbstractOperation {

    public static final String ITERATION = "iteration";
    public static final String ITERATION_DEFAULT = "2";
    public static final String THREAD_COUNT = "thread";
    public static final String THREAD_COUNT_DEFAULT = "2";

    @Override
    public void parseOptions(String args[]){
        // create the parser
        CommandLineParser parser = new DefaultParser();
        try {
            // isNumber the command line arguments
            CommandLine line = parser.parse( buildOptions(), args );
            if(line.hasOption(Center.HELP)){
                showHelp();
                return;
            }
            String input = line.getOptionValue(Center.INPUT, "");
            if(input.length() == 0){
                throw new ParseException(Center.ERR_INPUT);
            }
            String output = line.getOptionValue(Center.OUTPUT, "com_" + input);
            float resolution = Float.parseFloat(line.getOptionValue(Center.RESOLUTION, Center.RESOLUTION_DEFAULT));
            float alpha = Float.parseFloat(line.getOptionValue(Center.ALPHA, Center.ALPHA_DEFAULT));
            int iteration = Integer.parseInt(line.getOptionValue(ITERATION, ITERATION_DEFAULT));
            int threadCount = Integer.parseInt(line.getOptionValue(THREAD_COUNT, THREAD_COUNT_DEFAULT));

            Shared.log("Resolution scale: " + resolution);

            CPM cpmDetector = (CPM) new CPM().setThreadCount(threadCount);
            double time = System.currentTimeMillis();
            Graph graph = GraphIO.readGraph(input, true);
            int[] partition = cpmDetector.detect(graph, resolution, alpha, iteration);
            GraphIO.writePartition(graph, partition, output);
            double duration = (System.currentTimeMillis() - time) / 1000;
            Shared.log("Finished in " + duration + " seconds");
            Shared.log("Number of communities: " + Statistics.array(partition).uniqueCount);

            CPMapParameters cpMapParameters = new CPMapParameters();
            cpMapParameters.USE_RECORDED = false;
            cpMapParameters.TELEPORT_TO_NODE = false;
            cpMapParameters.TAU = 0.01f;
            Shared.log("Minimum Description Length: " + SiMap.evaluate(graph, partition, cpMapParameters));

            CPMParameters cpmParameters = new CPMParameters();
            cpmParameters.resolution = resolution;
            cpMapParameters.alpha = alpha;
            Shared.log("Hamiltonian: " + cpmDetector.evaluate(graph, partition, cpMapParameters));

        } catch( ParseException exp ) {
            // oops, something went wrong
            System.out.println( "Parsing failed.  Reason: " + exp.getMessage() );
            showHelp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Return the list of available argument objects to be passed to the program
     * @return
     */
    @Override
    public Options buildOptions(){
        // create Options object
        Option resolution = Option.builder(Center.RESOLUTION)
                .longOpt("resolution").desc("Resolution for fast community detection at a specific scale;" +
                        " larger values result in smaller and denser communities. Default value is "
                        + Center.RESOLUTION_DEFAULT)
                .hasArg().argName("resolution").type(Float.class).build();
        Option alpha = Option.builder(Center.ALPHA)
                .longOpt("alpha")
                .desc("Relative importance of positive links compared to negative links. Default is "
                        + Center.ALPHA_DEFAULT)
                .hasArg().argName("weight").type(Float.class).build();
        Option iteration = Option.builder()
                .longOpt(ITERATION)
                .desc("Number of times partition is further refined. Default value is "
                        + ITERATION_DEFAULT)
                .hasArg().argName("groupCount").type(Integer.class).build();
        Option threadCount = Option.builder()
                .longOpt(THREAD_COUNT)
                .desc("Number of threads used for parallel computations. Default value is "
                        + THREAD_COUNT_DEFAULT)
                .hasArg().argName("thread").type(Integer.class).build();
        Option help = Option.builder(Center.HELP)
                .longOpt("help")
                .desc("List of options for community detection").build();
        Options options = Center.getSharedOptions();
        options.addOption(resolution)
                .addOption(alpha).addOption(iteration).addOption(threadCount).addOption(help);
        return options;
    }

    @Override
    public void showHelp() {
        String header = "Options used for community detection:\n\n";
        String footer = "";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(Center.DETECT, header, buildOptions(), footer, true);
    }

    @Override
    public void showIntroduction() {
        showHelp();
    }
}