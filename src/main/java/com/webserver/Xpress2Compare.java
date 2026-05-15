package com.webserver;

import com.webserver.Config;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;

import java.io.*;
import java.util.*;

import static framework.UtilitiesWebserver.createElement;

@WebServlet(name = "Xpress2Compare", value = "/Xpress2Compare")
@MultipartConfig()

public class Xpress2Compare extends HttpServlet {
    protected String PROGRAM;
    protected String USER_ID;
    protected String OUTPUT_PATH;
    protected String OUTPUT_FILENAME;
    protected String LOCAL_STORAGE_PATH;
    protected String BASE_PATH = Config.get("base.path");
    protected String SAMPLE_FILENAME;
    protected String SUBMIT_TYPE;
    protected String resultFileType;
    protected ArrayList<String> proteinList;
    protected Map<String, String[]> proteinAttributeList;
    protected PrintWriter out;
    protected static final Logger logger = LogManager.getLogger(DownloadServlet.class);


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            // USER_ID and PROGRAM are retrieved from request parameter
            USER_ID = request.getParameter("USER_ID") == null ? "" : request.getParameter("USER_ID").toString();
            
            PROGRAM = request.getParameter("PROGRAM") == null ? "" : request.getParameter("PROGRAM").toString();
            resultFileType =  request.getParameter("resultFileType") == null ? "" : request.getParameter("resultFileType").toString();
            
            // Define the path to the folder where INPUT and OUTPUT are stored for each user/example run
            LOCAL_STORAGE_PATH = USER_ID.equals("EXAMPLE_USER") ? 
                BASE_PATH + "/repository/example_run/" + PROGRAM + "/" : BASE_PATH + "/repository/uploads/" + USER_ID + "/" + PROGRAM + "/"; 
            OUTPUT_PATH = LOCAL_STORAGE_PATH + "OUTPUT/";
            OUTPUT_FILENAME = "ResultFiles.zip"; 

            // Internal log 

            logger.info("Arguments: " + String.join("|", 
                "USER_ID:" + USER_ID, 
                "PROGRAM:" + PROGRAM,
                "PATH:" + LOCAL_STORAGE_PATH,
                "resultFileType:" + resultFileType));

            switch (resultFileType) {
                case "output":
                    //  Download the zipped output file
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + OUTPUT_FILENAME + "\"");
                    response.setContentType("application/zip");
                    response.setHeader("Cache-Control", "max-age=60");
                    response.setHeader("Cache-Control", "must-revalidate");

                    // Compress output
                    try {
                        // TODO recursive zip faster? 
                        Utils.zip(OUTPUT_PATH, OUTPUT_PATH + OUTPUT_FILENAME);
                    } catch (IOException e) {
                        logger.error(USER_ID + ": Fail to compress output:\n" + e.toString());
                    }

                    // Write output to response
                    UtilsServlet.writeFile(response, OUTPUT_PATH + OUTPUT_FILENAME);
                    break;

                case "sample_summary":
                    // Retrieve sample summary for PPIXpress
                    SAMPLE_FILENAME = "SampleTable.html"; // This file name must be the same as defined for sample_table in PPIXpress_tomcat.java
                    out = response.getWriter();
                    
                    // Read and write output to response
                    try (BufferedReader br = new BufferedReader(new FileReader(OUTPUT_PATH + SAMPLE_FILENAME))) {
                        while (br.ready()) {
                            out.println(br.readLine());
                        }
                    } catch (Exception e) {
                        logger.error(USER_ID + ": Fail to retrieve sample summary:\n" + e.toString());
                    }
                    break;
                
                case "subgraph":
                    out = response.getWriter();
                    try {
                        if (PROGRAM.equals("PPICompare")){
                            String proteinQuery = request.getParameter("proteinQuery");
                            logger.info("Querying protein: " + proteinQuery); 
                            
                            proteinAttributeList = new HashMap<String, String[]>(); 
                            try {
                                File attributeFile = new File(OUTPUT_PATH + "protein_attributes.txt");
                                if (attributeFile.exists()){
                                    Scanner s = new Scanner(attributeFile);

                                    while (s.hasNext()) {
                                        String[] attributes = s.nextLine().split(" ");
                                        String UniprotID = attributes[0];
                                        proteinAttributeList.put(UniprotID, attributes);
                                    }
                                    s.close();
                                }
                            } catch (Exception e) {
                                logger.error(USER_ID + ": Fail to retrieve protein attributes:\n" + e.toString());
                            }
                            try {
                                JSONArray subNetworkData = Utils.filterProtein_PPICompare(OUTPUT_PATH, proteinAttributeList, proteinQuery);

                                // Write output to response
                                out.println(subNetworkData);
                                
                            } catch (Exception e) {
                                logger.error(USER_ID + ": Fail to filter protein:\n" + e.toString());
                            }
                            
                        } 
                    } catch (Exception e) {
                        logger.error(USER_ID + ": Fail to retrieve data for subgraphs data:\n" + e.toString());
                        out.println("Fail to retrieve subgraphs data. Please contact authors using the ID:\n" + USER_ID);
                    }     
                    break;

                case "graph":
                    // Retrieve graph data to display on NVContent_Graph
                    out = response.getWriter();
                    try {
                        if (PROGRAM.equals("PPIXpress")){
                            String proteinQuery = request.getParameter("proteinQuery");
                            String expressionQuery = request.getParameter("expressionQuery");
                            boolean showDDIs = Boolean.parseBoolean(request.getParameter("showDDIs"));
                            
                            JSONArray subNetworkData = Utils.filterProtein(OUTPUT_PATH, proteinQuery, expressionQuery, showDDIs);
                            // Write output to response
                            out.println(subNetworkData);                    
                        } else if (PROGRAM.equals("PPICompare")){
                            proteinAttributeList = new HashMap<String, String[]>(); 
                            Scanner s = new Scanner(new File(OUTPUT_PATH + "protein_attributes.txt"));

                            while (s.hasNext()) {
                                String[] attributes = s.nextLine().split(" ");
                                String UniprotID = attributes[0];
                                proteinAttributeList.put(UniprotID, attributes);
                            }
                            s.close();

                            JSONArray subNetworkData = Utils.filterProtein_PPICompare(OUTPUT_PATH, proteinAttributeList, "null");

                            // Write output to response
                            out.println(subNetworkData);
                        }
                    } catch (Exception e) {
                            logger.error(USER_ID + ": Fail to retrieve data for graphs data:\n" + e.toString());
                            out.println("Fail to retrieve graphs data. Please contact authors using the ID:\n" + USER_ID);
                    }
                    break;

                case "protein_list":
                    // Retrieve the protein list for PPIXpress graph query or PPICompare protein highlight
                    out = response.getWriter();
                    proteinList = new ArrayList<String>();
                    String line;
                    int n_nodes = 0;
                    
                    // Define protein list source (the same as defined for sample_table in PPIXpress/PPICompare_Tomcat.java)
                    SAMPLE_FILENAME = PROGRAM.equals("PPIXpress") ? "ProteinList.txt" : PROGRAM.equals("PPICompare") ? "protein_attributes.txt" : null; 


                    // Read and write output to response
                    try (BufferedReader br = new BufferedReader(new FileReader(OUTPUT_PATH + SAMPLE_FILENAME))) { 
                        if (PROGRAM.equals("PPIXpress")){
                            while (br.ready()) {
                                proteinList.add(createElement("option", br.readLine()));
                            }
                            out.println(String.join("", proteinList));
                        }
                        else if (PROGRAM.equals("PPICompare")){
                            while ((line = br.readLine()) != null) {
                                String UniprotID = line.split(" ")[0];
                                if (!UniprotID.equals("UniProt_ACC")){
                                    proteinList.add(createElement("option", UniprotID));
                                    n_nodes ++;
                                }
                            }
                            out.println(String.join("", proteinList) + "n_nodes=" + n_nodes);
                        }
                    } catch (Exception e) {
                        logger.error(USER_ID + ": Fail to retrieve protein list:\n" + e.toString());
                    }
                    break;
                }
            } catch(Exception e){
                logger.error(USER_ID + ": Fail to retrieve session information:\n" + e.toString());
            }
        }
    }
