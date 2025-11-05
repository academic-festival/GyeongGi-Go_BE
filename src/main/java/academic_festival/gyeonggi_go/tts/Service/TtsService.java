package academic_festival.gyeonggi_go.tts.Service;

import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
public class TtsService {
    private static final Logger logger = LoggerFactory.getLogger(TtsService.class);

    //tts는 무거운 객체이므로 Bean으로 관리하거나, static final로 만들어 재사용하는 것이 좋음!

    @Async //비동기 실행
    public CompletableFuture<byte[]> synthesizeText(String text) {
        logger.info("TTS Synthesis started for text...");

        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()){

            // 1. 입력 텍스트 설정
            SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

            // 2. 목소리 설정
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode("en-US")
                    .setSsmlGender(SsmlVoiceGender.FEMALE)
                    .setName("en-US-Wavenet-F")
                    .build();

            // 3. 오디오 형식 설정 (MP3)
            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.MP3)
                    .build();

            // 4. TTS API 호출
            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

            // 5. 응답에서 오디오 데이터(ByteString) 추출
            ByteString audioContents = response.getAudioContent();

            // 6. ByteString을 byte[] 배열로 변환하여 반환
            logger.info("TTS Synthesis finished.");
            return CompletableFuture.completedFuture(audioContents.toByteArray());

        } catch (IOException e) {
            logger.error("TTS Synthesis failed: {}", e.getMessage());
            // 실패 시 빈 CompletableFuture 반환 (혹은 예외 처리)
            return CompletableFuture.completedFuture(null);
        }
    }
}