package world.xuewei.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import org.springframework.stereotype.Service;

@Service
public class  ChatService {
	
   private static final Logger log = LoggerFactory.getLogger(ChatService.class);


    public String callPythonScript(String input) {
    	log.info("Calling Python script with input: {}", input);
    	ProcessBuilder processBuilder = new ProcessBuilder("E:\\medicine\\pythonProject\\.venv\\Scripts\\python.exe", "E:/medicine/pythonProject/main.py");
        processBuilder.redirectErrorStream(true);
        StringBuilder output = new StringBuilder();

        try {
            Process process = processBuilder.start();
            log.info("Python script started successfully.");

            // Sending the input to the Python script
            try (OutputStream os = process.getOutputStream()) {
                os.write(input.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // Reading the output from the Python script
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line + "\n");
                }
            }

            // Wait for the process to finish
            process.waitFor();
            log.info("Python script executed successfully.");
        } catch (Exception e) {
        	 log.error("Error occurred while calling Python script: {}", e.getMessage());
             return "Error: " + e.getMessage();
        	//e.printStackTrace();
        }
        return output.toString();
    }
}
