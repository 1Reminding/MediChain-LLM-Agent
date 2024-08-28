package world.xuewei.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Service
public class ApiService {
	final Logger log = LoggerFactory.getLogger(ApiService.class);
    public String query(String inputFromFrontEnd) {
    	   

        try {
            // 构建ProcessBuilder来运行Python脚本
            ProcessBuilder builder = new ProcessBuilder("E:\\medicine\\pythonProject\\.venv\\Scripts\\python.exe", "E:/medicine/pythonProject/test.py", inputFromFrontEnd);
            builder.redirectErrorStream(true);            // 合并标准输出和错误输出
            log.info("Calling Python script with input: {}", inputFromFrontEnd);
            Process process = builder.start();
            log.info("Python script started successfully.");


            // 读取Python脚本的输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
            String line;
            List<String> validLines = new ArrayList<>();
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                // 过滤掉包含警告信息的行
            	if (!line.contains("UserWarning") 
            			&& !line.contains("warnings.warn") 
            			&& !line.contains("USER_AGENT environment variable not set")) {
                    validLines.add(line);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
            	 // 处理输出，分行处理并进行格式化
                StringBuilder formattedOutput = new StringBuilder();
                for (String validLine : validLines) {
                    formattedOutput.append(validLine).append("\n");
                }
                //System.out.println(formattedOutput);//输出py运行结果
                String response = formattedOutput.toString().trim();
                response = response.replace("*", "").replace("-", "").replace("\n", "<br>");
                //System.out.println(response);//输出处理过字符串
                return response;
            } else {
            	System.out.println("Script execution failed with exit code " + exitCode + ": " + String.join("\n", validLines).trim());
            	return "智能医生现在不在线，请稍后再试～";                
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error during script execution: " + e.getMessage());            
            return "智能医生现在不在线，请稍后再试～";
        }
    }
    
}




/*package world.xuewei.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.HashMap;
import java.util.Map;
@Service
public class ApiService {

    @Value("${baichuan.api-key}")
    private String apiKey;

    @Value("${baichuan.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    public ApiService(RestTemplate restTemplate, ObjectMapper mapper) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
    }

    public String query(String queryMessage) {
        // 构建请求消息
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "Baichuan-NPC-Turbo");

        Map<String, Object> characterProfile = new HashMap<>();
        characterProfile.put("character_name", "medichain医生");
        characterProfile.put("character_info", "medichain医生是一名温柔热情的医生，他的话语常常让人觉得暖心，同时，需要询问病人一些基本情况，做出基本诊断和一些建议");
        characterProfile.put("user_name", "病人");
        characterProfile.put("user_info", "一位寻求医生帮助的病人");
        requestBody.put("character_profile", characterProfile);

        Map<String, String> messageContent = new HashMap<>();
        messageContent.put("role", "user");
        messageContent.put("content", queryMessage);

        requestBody.put("messages", new Map[]{messageContent});
        requestBody.put("stream", false);
        requestBody.put("temperature", 0.8);
        requestBody.put("top_k", 10);
        requestBody.put("max_tokens", 512);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
            return "智能医生现在不在线，请稍后再试～";
        } catch (Exception e) {
            e.printStackTrace();
            return "智能医生现在不在线，请稍后再试～";
        }
    }
}
*/