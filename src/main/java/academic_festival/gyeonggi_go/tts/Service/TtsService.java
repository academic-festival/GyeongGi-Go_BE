package academic_festival.gyeonggi_go.tts.Service;

import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
public class TtsService {
    private static final Logger logger = LoggerFactory.getLogger(TtsService.class);

    //tts는 무거운 객체이므로 Bean으로 관리하거나, static final로 만들어 재사용하는 것이 좋음!
    private final TextToSpeechClient textToSpeechClient;

    public TtsService(TextToSpeechClient textToSpeechClient) {
        this.textToSpeechClient = textToSpeechClient;
    }

    public Mono<byte[]> synthesizeText(String text) {
        return Mono.fromCallable(() -> {
            logger.info("TTS Synthesis started...");

            // 1. 입력 텍스트 설정
            SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

            // 2. 목소리 설정
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode("en-US")
                    .setSsmlGender(SsmlVoiceGender.FEMALE)
                    .setName("en-US-Wavenet-F") // Wavenet 음성이 더 자연스럽습니다.
                    .build();

            // 3. 오디오 형식 설정 (MP3)
            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.MP3)
                    .build();

            // 4. [블로킹] TTS API 실제 호출
            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
            ByteString audioContents = response.getAudioContent();

            logger.info("TTS Synthesis finished.");
            return audioContents.toByteArray();

        }).subscribeOn(Schedulers.boundedElastic());
    }
}