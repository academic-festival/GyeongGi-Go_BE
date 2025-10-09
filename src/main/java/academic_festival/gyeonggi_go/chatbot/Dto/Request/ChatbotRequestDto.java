package academic_festival.gyeonggi_go.chatbot.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequestDto {
    private String placename;
    private String question;
    private List<String> conversationHistory; // 선택 사항: 대화 기록
}