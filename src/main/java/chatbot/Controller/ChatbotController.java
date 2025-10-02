package chatbot.Controller;

import chatbot.Dto.Request.ChatbotRequestDto;
import chatbot.Dto.Response.ChatbotResponseDto;
import chatbot.Service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {
    private final ChatbotService chatbotService;

    @Autowired
    private ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/chat")
    public Mono<ChatbotResponseDto> handleChat(@RequestBody ChatbotRequestDto request) {
        // place가 있고 question이 없으면 첫 질문, question이 있으면 이어가는 질문
        if (request.getPlace() != null && !request.getPlace().isEmpty()) {
            return chatbotService.startConversation(request.getPlace());
        } else if (request.getQuestion() != null && !request.getQuestion().isEmpty()) {
            return chatbotService.continueConversation(request.getQuestion());
        } else {
            // 잘못된 요청에 대한 처리
            return Mono.just(new ChatbotResponseDto("어떤 관광지가 궁금하신가요?", null));
        }
    }
}
