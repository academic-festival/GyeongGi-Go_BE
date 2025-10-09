package academic_festival.gyeonggi_go.chatbot.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatbotDataDto {
    private final String answer;
    private final List<String> suggestedQuestions;
}
