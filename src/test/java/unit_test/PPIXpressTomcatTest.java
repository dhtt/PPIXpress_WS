package unit_test;

import com.webserver.Config;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import standalone_tools.PPIXpress_Tomcat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.File;
import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(JUnit4.class)
public class PPIXpressTomcatTest {
    public ArrayList<Object> test_pipeline(){
        PPIXpress_Tomcat pipeline = new PPIXpress_Tomcat();
        List<String> args_list = new ArrayList<>();
        String INPUT_PATH = "repository/example_upload/PPIXpress/INPUT/Bulk_B_1003.txt";
        String OUTPUT_PATH = "repository/example_upload/PPIXpress/OUTPUT/";
        String ORIGINAL_NETWORK_PATH = "repository/example_upload/PPIXpress/INPUT/ppi_data.sif";

        args_list.add(INPUT_PATH);
        args_list.add("-output=" + OUTPUT_PATH);
        args_list.add("-original_network_path=" + ORIGINAL_NETWORK_PATH);
        args_list.add("-x");
        args_list.add("-t=1");
        args_list.add("-tp=-1");

        AtomicBoolean stop_signal = new AtomicBoolean(false);
        pipeline.runAnalysis(args_list, stop_signal);
        ArrayList<Object> result_list = new ArrayList<Object>();
        result_list.add(pipeline);
        result_list.add(stop_signal);
        result_list.add(OUTPUT_PATH + "PPIXpress_log.html");
        return result_list;
    }

    ArrayList<Object> pipeline_result = test_pipeline();
    
    @Test
    public void test_finished_process(){
        AtomicBoolean stop_signal = (AtomicBoolean) pipeline_result.get(1);
        assertTrue(stop_signal.get());
    }
    
    // @Test
    public void test_progress_file_exist(){
        String progress_file = pipeline_result.get(2).toString();
        File file = new File(progress_file);
        assertTrue(file.exists());
    }

    // @Test
    public void testWatchService(){
          // Create a watch event for log file
        String BASE_PATH = Config.get("base.path"); 

        String OUTPUT_PATH = BASE_PATH + "/repository/example_run/PPIXpress/OUTPUT/";
        try (WatchService watchService = FileSystems.getDefault().newWatchService();){
            Path OUTPUT_PATH_DIR = Paths.get(OUTPUT_PATH);
            WatchKey watchKey = OUTPUT_PATH_DIR.register(watchService, ENTRY_CREATE, ENTRY_MODIFY);
            

            while (true) {
                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    Path changedFile = OUTPUT_PATH_DIR.resolve((Path) event.context()); 
                    System.out.println("PPICompareServlet: Log file has been modified: " + event.kind());


                    System.out.println(changedFile);
                    if (changedFile.endsWith("LogFile.html")) {
                        System.out.println("My file has changed");
                    }    
                }

                boolean valid = watchKey.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Fail to watch file.\n" + e.toString());
        }

    }
}