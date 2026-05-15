package com.webserver;

import com.webserver.Config;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import org.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;

@WebServlet(name = "ProgressReporter", value = "/ProgressReporter")

//TODO When fail these step, do not display "PPIXpress pipeline is finished!""
public class ProgressReporter extends HttpServlet {
        protected String PROGRAM;
        protected String USER_ID;
        protected String SUBMIT_TYPE;
        protected String RUN_PROGRESS_LOG;
        protected String LOCAL_STORAGE_PATH;
        protected String BASE_PATH = Config.get("base.path");
        protected Boolean UPDATE_LONG_PROCESS_STOP_SIGNAL;
        protected int NO_EXPRESSION_FILE;
        protected JSONObject POSTData = new JSONObject();
        protected static final Logger logger = LogManager.getLogger(ProgressReporter.class);

        public void doPost(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                
                try {   
                        // USER_ID is retrieved from request parameter
                        USER_ID = request.getParameter("USER_ID") == null ? "" : request.getParameter("USER_ID").toString();
                                
                        // PROGRAM is retrieved from request parameter, indicating PPPXpress or PPICompare is making request
                        PROGRAM = request.getParameter("PROGRAM") == null ? "" : request.getParameter("PROGRAM").toString();

                        // UPDATE_LONG_PROCESS_STOP_SIGNAL is the boolean value of AtomicBoolean STOP_SIGNAL from PPIXpress_/PPICompare_Tomcat
                        // "true" will stop sending request to update PPI-Servelets and vice versa. 
                        // At the end of the process or in case of error, UPDATE_LONG_PROCESS_STOP_SIGNAL is switched to "true"
                        if (PROGRAM.equals("PPIXpress")){   
                                UPDATE_LONG_PROCESS_STOP_SIGNAL = PPIXpressServlet.storedJobs.get(USER_ID).get();
                        } else if (PROGRAM.equals("PPICompare")){
                                UPDATE_LONG_PROCESS_STOP_SIGNAL = PPICompareServlet.storedJobs.get(USER_ID).get();
                        }

                        // LOCAL_STORAGE_PATH is the path to the folder where INPUT and OUTPUT are stored for each user/example run
                        LOCAL_STORAGE_PATH = USER_ID.equals("EXAMPLE_USER") ? 
                                BASE_PATH + "/repository/example_run/" + PROGRAM + "/" : 
                                BASE_PATH + "/repository/uploads/" + USER_ID + "/" + PROGRAM + "/"; 

                        // Get the process log stored in "/OUTPUT/PPIXpress_log.html". Log is updated by the process from standalone_tools:PPIXpress or PPIXCompare
                        // The file name must be the same as defined for log_file in PPICompare_Tomcat.java or PPIXpress_Tomcat.java and 
                        // PPICompareServlet and PPIXpressServlet 
                        String RUN_PROGRESS_LOG_PATH = LOCAL_STORAGE_PATH + "/OUTPUT/LogFile.html";
                        Path LOG_FILE = Paths.get(RUN_PROGRESS_LOG_PATH);
                        
                        if (Files.exists(LOG_FILE)){
                                RUN_PROGRESS_LOG = Files.readString(LOG_FILE);
                        } else {
                                RUN_PROGRESS_LOG = "";
                        }

                        // Internal progress log
                        if (PROGRAM.equals("PPIXpress")){
                                NO_EXPRESSION_FILE = request.getParameter("NO_EXPRESSION_FILE") == null ? 
                                        0 : Integer.parseInt(request.getParameter("NO_EXPRESSION_FILE"));
                        
                                logger.info("Response: " + String.join("|", 
                                        "USER_ID:" + USER_ID, 
                                        "PROGRAM:" + PROGRAM, 
                                        "UPDATE_LONG_PROCESS_STOP_SIGNAL:" + UPDATE_LONG_PROCESS_STOP_SIGNAL.toString(), 
                                        "NO_EXPRESSION_FILE:" + String.valueOf(NO_EXPRESSION_FILE)) );
                        } else if (PROGRAM.equals("PPICompare")){
                                logger.info("Response: " + String.join("|", 
                                        "USER_ID:" + USER_ID, 
                                        "PROGRAM:" + PROGRAM, 
                                        "UPDATE_LONG_PROCESS_STOP_SIGNAL:" + UPDATE_LONG_PROCESS_STOP_SIGNAL.toString()));
                        }
                                
                        // Send progress as response to functionality.js/PPICompare_functionality:updateLongRunningStatus 
                        POSTData.put("USER_ID", USER_ID);
                        POSTData.put("UPDATE_LONG_PROCESS_STOP_SIGNAL", UPDATE_LONG_PROCESS_STOP_SIGNAL);
                        POSTData.put("UPDATE_LONG_PROCESS_MESSAGE", RUN_PROGRESS_LOG);
                        if (PROGRAM.equals("PPIXpress")){
                                POSTData.put("PROGRAM", "PPIXpress");
                                POSTData.put("NO_EXPRESSION_FILE", NO_EXPRESSION_FILE);
                        } else if (PROGRAM.equals("PPICompare")) {
                                POSTData.put("PROGRAM", "PPICompare");
                        }

                        out.println(POSTData);
                } catch (Exception e) {
                        // In case of error, stop updating progress
                        POSTData.put("UPDATE_LONG_PROCESS_STOP_SIGNAL", true);
                        logger.error(USER_ID  + ": " + e.toString());
                }
        }
}
