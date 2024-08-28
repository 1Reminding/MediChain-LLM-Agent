package world.xuewei.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import world.xuewei.service.ApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
public class ApiController {

    @Autowired
    private ApiService apiService;

    @GetMapping("/ask")
    public ResponseEntity<String> askBaichuan(@RequestParam String question) {
        String response = apiService.query(question);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}