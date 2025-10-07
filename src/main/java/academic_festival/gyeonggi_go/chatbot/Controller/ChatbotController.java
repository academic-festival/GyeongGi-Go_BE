package academic_festival.gyeonggi_go.chatbot.Controller;

import academic_festival.gyeonggi_go.chatbot.Dto.Request.ChatbotRequestDto;
import academic_festival.gyeonggi_go.chatbot.Dto.Response.ApiResponseDto;
import academic_festival.gyeonggi_go.chatbot.Dto.Response.ChatbotDataDto;
import academic_festival.gyeonggi_go.chatbot.Service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class ChatbotController {
    private final ChatbotService chatbotService;

    @Autowired
    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/chatbot")
    public Mono<ApiResponseDto<ChatbotDataDto>> handleChat(@RequestBody ChatbotRequestDto request) {

        // 요청에 따라 적절한 서비스 메소드를 호출하는 Mono를 선택
        Mono<ChatbotDataDto> chatDataMono;
        if (request.getPlace() != null && !request.getPlace().isEmpty()) {
            chatDataMono = chatbotService.startConversation(request.getPlace());
        } else if (request.getQuestion() != null && !request.getQuestion().isEmpty()) {
            chatDataMono = chatbotService.continueConversation(request.getQuestion());
        } else {
            // 잘못된 요청에 대한 처리
            ChatbotDataDto errorData = new ChatbotDataDto("Place or question is required.", null);
            return Mono.just(new ApiResponseDto<>(400, "요청이 유효하지 않습니다.", errorData));
        }

        // 서비스로부터 성공적인 결과를 받으면, ApiResponseDto로 감싸서 반환
        return chatDataMono.map(chatData ->
                new ApiResponseDto<>(200, "챗봇이 답변을 완료했습니다.", chatData)
        );
    }
}
