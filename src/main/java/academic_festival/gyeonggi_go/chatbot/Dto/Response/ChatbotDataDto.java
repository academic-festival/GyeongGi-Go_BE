package academic_festival.gyeonggi_go.chatbot.Dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatbotDataDto {
    private final String answer;

    @JsonInclude(JsonInclude.Include.NON_EMPTY) //값이 null이면 필드를 아예 제외
    private final List<String> suggestedQuestions;

    // Base64 인코딩된 오디오 데이터 (MP3)
    @JsonInclude(JsonInclude.Include.NON_NULL) //값이 null이면 필드를 아예 제외
    private final String audioData;
}
