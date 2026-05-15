package unit_test;

import com.webserver.Config;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.json.JSONArray;

import com.webserver.Utils;

@RunWith(JUnit4.class)
public class UtilsTest {
    @Test
    public void test_copyPPIXpress2PPICompare(){
        String BASE_PATH = Config.get("base.path");
        String groupedID = "Ccell:3,4";

        String IN = BASE_PATH + "/repository/uploads/pxziRBfrPEyo/PPIXpress/OUTPUT/";
        String OUT = BASE_PATH + "/repository/uploads/pxziRBfrPEyo_0_Xpress2Compare/PPICompare/INPUT/";
        try {
            String copyTarget = Utils.copyPPIXpress2PPICompare(IN, OUT, groupedID);
            assertTrue(copyTarget.equals(OUT+ groupedID.split(":")[0] + "/"));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_unZipFile(){
        String BASE_PATH = Config.get("base.path");
        // Test with MPP.zip where all outputs are compressed. All outputs have .txt.gz extension
        String fileName = BASE_PATH + "/repository/uploads/8de95199-5f2f-4a97-9a9e-971bbefed216/PPICompare/INPUT/MPP.zip";
        Utils.unzipFile(fileName, "group_1", ".");
        // Test with MPP.zip where all outputs are not compressed. All outputs have .txt extension
        String fileName1 = BASE_PATH + "/repository/uploads/5e6b7e4c-b312-4a59-884d-454dc473015e/PPICompare/INPUT/N1_PPIXpress_out.zip";
        Utils.unzipFile(fileName1, "group_1", ".");
        // assertTrue(unzippedFile.equals(Utils.RemoveFileExtension(fileName)));
    }

    @Test
    public void test_ZipFile() throws IOException{
        String OUTPUT_PATH = "repository/example_run/PPIXpress/OUTPUT/";
        String OUTPUT_FILENAME = "ResultFiles_test.zip";
        Utils.zip(OUTPUT_PATH, OUTPUT_PATH + OUTPUT_FILENAME);
    }

    @Test
    public void test_filterProtein_PPICompare() {
        String BASE_PATH = Config.get("base.path");
        Map<String, String[]> proteinAttributeList = new HashMap<String, String[]>(); 
        String OUTPUT_PATH = BASE_PATH + "/repository/example_run/PPICompare/OUTPUT/";
        try {
            Scanner s = new Scanner(new File(OUTPUT_PATH + "protein_attributes.txt")); 
            while (s.hasNext()) {
                String[] attributes = s.nextLine().split(" ");
                String UniprotID = attributes[0];
                proteinAttributeList.put(UniprotID, attributes);
            }
            s.close();
        } catch(Exception e){
            e.printStackTrace();
        }
        assertTrue(proteinAttributeList.get("O15151")[1].equals("MDM4"));
        
        JSONArray output = new JSONArray();
        output = Utils.filterProtein_PPICompare(OUTPUT_PATH, proteinAttributeList, "O15151");
        System.out.println(output);
    }

    @Test
    public void test_filterProtein_PPICompare_query(){
        String BASE_PATH = Config.get("base.path");
        String proteinQuery = "P01111";
        Map<String, String[]> proteinAttributeList = new HashMap<String, String[]>(); 
        String OUTPUT_PATH = BASE_PATH + "/repository/example_run/PPICompare/OUTPUT/";
        try{
            Scanner s = new Scanner(new File(OUTPUT_PATH + "protein_attributes.txt"));
    
            while (s.hasNext()) {
                String[] attributes = s.nextLine().split(" ");
                String UniprotID = attributes[0];
                proteinAttributeList.put(UniprotID, attributes);
            }
            s.close();
        } catch(Exception e){
            e.printStackTrace();
        }

        JSONArray subNetworkData = Utils.filterProtein_PPICompare(OUTPUT_PATH, proteinAttributeList, proteinQuery);
        System.out.println(subNetworkData);
    }

    public ArrayList<String> scanProteinList(String OUTPUT_PATH){
        ArrayList<String> proteinList = new ArrayList<String>();
        try {
            Scanner s = new Scanner(new File(OUTPUT_PATH + "ProteinList.txt"));
            while (s.hasNext()) {
                proteinList.add(s.next());
            }
            s.close();
        } catch(Exception e){
            e.printStackTrace();
        }
        return proteinList;
    }

    public void queryProtein(String OUTPUT_PATH_, String proteinQuery_, String expressionQuery_, boolean showDDIs_){
        JSONArray output = new JSONArray();
        output = Utils.filterProtein(OUTPUT_PATH_, proteinQuery_, expressionQuery_, showDDIs_);
        System.out.println(output);
    }

    @Test
    public void test_filterProtein_PPIXpress() {
        String BASE_PATH = Config.get("base.path");
        String OUTPUT_PATH = BASE_PATH + "/repository/uploads/QXyBvZUwFmLH/PPIXpress/OUTPUT/";
        System.out.println(": DownloadServlet: CHECK\n" + OUTPUT_PATH + "ProteinList.txt");
        Long now = System.currentTimeMillis();
        System.out.println(now);

        // scanProteinList(OUTPUT_PATH);
        // System.out.println("scanProteinList: " + (System.currentTimeMillis() - now));
        // now = System.currentTimeMillis();

        queryProtein(OUTPUT_PATH, "P27708", "1", true);
        System.out.println("scanProteinList: " + (System.currentTimeMillis() - now));
        now = System.currentTimeMillis();
    }
}