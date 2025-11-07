package academic_festival.gyeonggi_go.config;

import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class TtsConfig {
    @Bean
    @ConditionalOnProperty(value = "tts.enabled", havingValue = "true", matchIfMissing = false)
    public TextToSpeechClient textToSpeechClient() throws IOException {
        return TextToSpeechClient.create();
    }
}
