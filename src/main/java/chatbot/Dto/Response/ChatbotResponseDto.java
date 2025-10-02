package chatbot.Dto.Response;

import java.util.List;

public class ChatbotResponseDto {
    private String answer;
    private List<String> suggestedQuestions;


    public ChatbotResponseDto(String  answer, List<String> suggestedQuestions) {
        this.answer = answer;
        this.suggestedQuestions = suggestedQuestions;
    }

    // Getter와 Setter
    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<String> getSuggestedQuestions() {
        return suggestedQuestions;
    }

    public void setSuggestedQuestions(List<String> suggestedQuestions) {
        this.suggestedQuestions = suggestedQuestions;
    }
}
