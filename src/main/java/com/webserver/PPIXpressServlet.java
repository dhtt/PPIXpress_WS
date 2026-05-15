package com.webserver;

import com.webserver.Config;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import standalone_tools.PPIXpress_Tomcat;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet(name = "PPIXpress", value = "/PPIXpress")
@MultipartConfig()

public class PPIXpressServlet extends HttpServlet {
    protected static final Logger logger = LogManager.getLogger(PPIXpressServlet.class);
    private String USER_ID;
    private String LOCAL_STORAGE_PATH;
    protected String BASE_PATH = Config.get("base.path");
    private String INPUT_PATH;
    private String OUTPUT_PATH;
    private String FILENAME_PPI;
    private String ORIGINAL_NETWORK_PATH;
    int NO_EXPRESSION_FILE;
    String SUBMIT_TYPE;
    ArrayList<String> allArgs;
    static AtomicBoolean STOP_SIGNAL = new AtomicBoolean(false);
    protected JSONObject POSTData = new JSONObject();
    static ConcurrentHashMap<String, AtomicBoolean> storedJobs = new ConcurrentHashMap<String, AtomicBoolean>();

    /**
     * A long-running process that runs the analysis pipeline in a separate thread.
     */
    static class LongRunningProcess implements Runnable {
        private AtomicBoolean stopSignal;
        private final PPIXpress_Tomcat pipeline = new PPIXpress_Tomcat();
        private volatile boolean stop;
        private List<String> argList;

        /**
         * Constructs a LongRunningProcess object with PPIXpress arguments
         *
         * @param allArgs the list of arguments for the analysis pipeline
         * @param stopSignal the stop signal to indicate when the pipeline should stop
         */
        public LongRunningProcess(List<String> allArgs, AtomicBoolean stopSignal) {
            this.stopSignal = stopSignal;
            this.argList = allArgs;
        }

        /**
         * Runs the analysis pipeline in a separate thread.
         * This method continuously runs the analysis pipeline until the stop signal is received.
         * If the stop signal is received, it sets the stop flag to true, indicating that the pipeline should stop.
         */
        @Override
        public void run() {
            try {
                while (!stop) {
                    pipeline.runAnalysis(this.argList, stopSignal);
                    if (stopSignal.get()) {
                        logger.info("PPIXpress pipeline is finished!");
                        setStop(true);
                    }
                }    
            } catch (Exception e) {
                        logger.error(e.toString());
            }
            
        }

        /**
         * Gets the stop flag value.
         *
         * @return true if the pipeline should stop, false otherwise
         */
        public boolean getStop() {
            return stop;
        }

        /**
         * Sets the stop flag value.
         *
         * @param stop the stop flag value to set
         */
        public void setStop(boolean stop) {
            this.stop = stop;
        }
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Main pipeline
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        // Store uploaded files outside webapp deploy folders (LOCAL_STORAGE_PATH) and
        // output.zip inside deploy folder (WEBAPP_PATH)
        USER_ID = request.getParameter("USER_ID");
        SUBMIT_TYPE = request.getParameter("SUBMIT_TYPE");
        allArgs = new ArrayList<String>();

        if (SUBMIT_TYPE.equals("RunExample")) {
            try {
                // Define a data local storage on the local server
                LOCAL_STORAGE_PATH = BASE_PATH + "/repository/example_run/PPIXpress/"; 
                INPUT_PATH = LOCAL_STORAGE_PATH + "INPUT/";
                OUTPUT_PATH = LOCAL_STORAGE_PATH + "OUTPUT/";
                FILENAME_PPI = "example_ppi_data.sif";

                // Add path to protein network to arguments set
                ORIGINAL_NETWORK_PATH = INPUT_PATH + FILENAME_PPI;
                allArgs.add("-original_network_path=" + ORIGINAL_NETWORK_PATH);

                // Add output path to arguments set
                allArgs.add("-output=" + OUTPUT_PATH);

                // Add paths to expression data to argument list 
                try {
                    File inputFiles = new File(INPUT_PATH);
                    File[] inputFilesPaths = inputFiles.listFiles();
                    NO_EXPRESSION_FILE = 0;
                    for (File file : inputFilesPaths){
                        if (file.isFile() && file.getName().startsWith("expression") && file.getName().endsWith(".txt")){
                            allArgs.add(file.getAbsolutePath());

                            // Set number of expression files
                            NO_EXPRESSION_FILE += 1;
                        }
                    }
                } catch (Exception e) {
                    logger.error(USER_ID + ": PPIXpressServlet: Fail to retrieve example expression files. ERROR:\n" + e.toString());
                }
                
                logger.info("Example process initiated from Servlet: " + String.join("|", 
                    "USER_ID:" + USER_ID, 
                    "Arguments:" + allArgs));
                    
            } catch(Exception e){
                logger.error(USER_ID + ": Fail to initiate example run:\n" + e.toString());
             }
        } 
        else if (SUBMIT_TYPE.equals("RunNormal")) {
            try {
                // Define a data local storage on the local server
                LOCAL_STORAGE_PATH = BASE_PATH + "/repository/uploads/" + USER_ID + "/PPIXpress/"; 

                // Create input directory
                Utils.createUserDir(LOCAL_STORAGE_PATH); 
                INPUT_PATH = LOCAL_STORAGE_PATH + "INPUT/";
                OUTPUT_PATH = LOCAL_STORAGE_PATH + "OUTPUT/";
                FILENAME_PPI = "ppi_data.sif";

                String taxonID = request.getParameter("protein_network_web");

                // If taxon = null, use a predefined path to store input PPI
                // network on server, else use taxon to retrieve network from Mentha/IntAct
                // Add path to protein network to arguments set
                ORIGINAL_NETWORK_PATH = taxonID.isEmpty() ? INPUT_PATH + FILENAME_PPI : "taxon:" + taxonID;
                allArgs.add("-original_network_path=" + ORIGINAL_NETWORK_PATH);

                // Add output path to arguments set
                allArgs.add("-output=" + OUTPUT_PATH);

                // Add paths to expression data to argument list. Meanwhile, store user's PPI
                // network (if uploaded) and expression data to a local storage on server
                try {
                    for (Part part : request.getParts()) {
                        String fileType = part.getName();
                        String fileName = part.getSubmittedFileName();

                        if (fileName != null && !fileName.equals("")) {

                            if (fileType.equals("protein_network_file"))
                                UtilsServlet.writeOutputStream(part, ORIGINAL_NETWORK_PATH);

                            else if (fileType.equals("expression_file")) {
                                String inputFilesPath = INPUT_PATH + fileName.substring(fileName.lastIndexOf("\\") + 1);
                                UtilsServlet.writeOutputStream(part, inputFilesPath);
                                allArgs.add(inputFilesPath);
                            }
                        }
                    }
                } catch(Exception e){
                    logger.error(USER_ID + ": Fail to retrieve uploaded expression files:\n" + e.toString());
                }
                
                // Set number of expression files
                NO_EXPRESSION_FILE = Integer.parseInt(request.getParameter("NO_EXPRESSION_FILE"));

                // Store and show to screen uploaded files
                allArgs.addAll(List.of(request.getParameterValues("PPIOptions")));
                allArgs.addAll(List.of(request.getParameterValues("ExpOptions")));
                allArgs.addAll(List.of(request.getParameterValues("RunOptions")));
                allArgs.add(request.getParameter("threshold"));
                allArgs.add(request.getParameter("percentile"));
                allArgs.removeIf(n -> (n.equals("null")));


                logger.info("User-defined process initiated from Servlet: " + String.join("|", 
                    "USER_ID:" + USER_ID, 
                    "Arguments:" + allArgs));

            } catch(Exception e){
                logger.error(USER_ID + ": Fail to initiate user-defined run:\n" + e.toString());
            }
        }
        try {
            // Create and execute PPIXpress and update progress periodically to screen
            // If run example, STOP_SIGNAL is set to true so that no process is initiated. The outcome has been pre-analyzed
            STOP_SIGNAL = SUBMIT_TYPE.equals("RunNormal") ? new AtomicBoolean(false) : new AtomicBoolean(true);
            storedJobs.putIfAbsent(USER_ID, STOP_SIGNAL);

            // Send Servlet response to functionality.js:$.fn.submit_form. This response is used as request for 
            // ProgressReporter.java in functionality.js:updateLongRunningStatus.
            // Very important as the essential parameters for run/update progress are communicated between this 
            // servlet, PPICompare_functionality.js and ProgressReporter.java 
            POSTData.put("USER_ID", USER_ID);
            POSTData.put("PROGRAM", "PPIXpress");
            POSTData.put("NO_EXPRESSION_FILE", NO_EXPRESSION_FILE); 
            POSTData.put("UPDATE_LONG_PROCESS_MESSAGE", "");
            POSTData.put("UPDATE_LONG_PROCESS_STOP_SIGNAL", STOP_SIGNAL);
            out.println(POSTData);

            // Run PPIXpress
            if (SUBMIT_TYPE.equals("RunNormal")) {
                LongRunningProcess myThreads = new LongRunningProcess(allArgs, STOP_SIGNAL);
                Thread lrp = new Thread(myThreads);
                lrp.start();   
            }
        } catch (Exception e) {
            logger.error(USER_ID + ": Fail to initialize PPIXpress process:\n" + e.toString());
        }


    }

    public static void main(String[] args) throws IOException {
    }
}
