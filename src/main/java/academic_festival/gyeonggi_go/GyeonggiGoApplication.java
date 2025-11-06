package academic_festival.gyeonggi_go;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
public class GyeonggiGoApplication {

    public static void main(String[] args) {
        SpringApplication.run(GyeonggiGoApplication.class, args);
    }

}
