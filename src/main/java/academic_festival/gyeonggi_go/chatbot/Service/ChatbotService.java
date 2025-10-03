package academic_festival.gyeonggi_go.chatbot.Service;

import academic_festival.gyeonggi_go.chatbot.Dto.Response.ChatbotResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class ChatbotService {
    private final WebClient webClient;
    private final String apiKey;

    public ChatbotService(WebClient.Builder webClientBuilder,
                          @Value("${gemini.api.url}") String apiUrl,
                          @Value("${gemini.api.key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(apiUrl).build();
        this.apiKey = apiKey;
    }

    //Gemini API 호출하는 공통 메서드
    private Mono<ChatbotResponseDto> callGeminiApi(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class) // 응답을 Map 형태로 받음
                .map(this::parseResponse); // 받은 응답을 파싱하여 ChatResponse 객체로 변환
    }

    //대화시작
    public Mono<ChatbotResponseDto> startConversation(String place) {
        String prompt = String.format(
                "'%s'라는 관광지에 대해 설명해줘~ 그리고 이어서 할만한 흥미로운 질문 2개를 '질문1: 내용, 질문2: 내용' 형식으로 다음줄에 제안해줘.", place);
        return callGeminiApi(prompt);
    }

    //대화 이어가기
    public Mono<ChatbotResponseDto> continueConversation(String question) {
        String prompt =String.format(
                "'%s'라는 질문에 대해 설명해줘~ 그리고 자연스럽게 대화를 이어나갈만한 질문 2개를 '질문1: 내용, 질문2: 내용' 형식으로 다음줄에 제안해줘.", question);
        return callGeminiApi(prompt);
    }


    //제미나이 응답을 파싱하여 ChatResponse 객체로 변환
    private ChatbotResponseDto parseResponse(Map<String, Object> responseBody) {
        try {
            // 복잡한 JSON 구조에서 텍스트 부분만 추출
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String rawText = (String) parts.get(0).get("text");

            // Gemini가 생성한 텍스트에서 답변과 질문 분리
            String[] lines = rawText.split("\\r?\\n");
            StringBuilder answerBuilder = new StringBuilder();
            List<String> questions = new java.util.ArrayList<>();

            for (String line : lines) {
                if (line.startsWith("질문1:") || line.startsWith("질문2:")) {
                    questions.add(line.substring(line.indexOf(":") + 1).trim());
                } else {
                    answerBuilder.append(line).append("\n");
                }
            }

            String answer = answerBuilder.toString().trim();

            return new ChatbotResponseDto(answer, questions);
        } catch (Exception e) {
            // 파싱 오류 발생 시 기본 응답 반환
            return new ChatbotResponseDto("Sorry, There was a problem generating your answer. ", null);
        }
    }
}
