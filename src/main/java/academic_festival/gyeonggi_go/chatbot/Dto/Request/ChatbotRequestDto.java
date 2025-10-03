package academic_festival.gyeonggi_go.chatbot.Dto.Request;

import java.util.List;

public class ChatbotRequestDto {
    private String place;
    private String question;
    private List<String> conversationHistory; // 선택 사항: 대화 기록

    // 기본 생성자 (JSON <-> Object 변환에 필요)
    public ChatbotRequestDto() {
    }

    // 모든 필드를 포함하는 생성자 (선택 사항)
    public ChatbotRequestDto(String place, String question, List<String> conversationHistory) {
        this.place = place;
        this.question = question;
        this.conversationHistory = conversationHistory;
    }

    // Getter와 Setter
    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(List<String> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }
}