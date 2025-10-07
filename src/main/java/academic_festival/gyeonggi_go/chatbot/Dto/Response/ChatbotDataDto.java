package academic_festival.gyeonggi_go.chatbot.Dto.Response;

import java.util.List;

public class ChatbotDataDto {
    private final String answer;
    private final List<String> suggestedQuestions;

    public ChatbotDataDto(String answer, List<String> suggestedQuestions) {
        this.answer = answer;
        this.suggestedQuestions = suggestedQuestions;
    }

    // Getters
    public String getAnswer() {
        return answer;
    }

    public List<String> getSuggestedQuestions() {
        return suggestedQuestions;
    }
}
