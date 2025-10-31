package academic_festival.gyeonggi_go.chatbot.Service;

import academic_festival.gyeonggi_go.chatbot.Dto.Response.ChatbotDataDto;
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
    private Mono<ChatbotDataDto> callGeminiApi(String prompt) {
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
    public Mono<ChatbotDataDto> startConversation(String place, String locationExplain) {
        String greeting = String.format("Hello! What are you curious about %s?", place);
        String prompt =String.format(
                "모든 건 영어로 답변해줘! " +
                        "⚠️ 절대 인삿말, 서두, 감탄사(예: 네!, 좋아요!, 알겠습니다!)를 하지 마. " +
                        "\n---제공된 컨텍스트---" +
                        "\n%s" + // DB의 locationExplain
                        "\n---컨텍스트 끝---" +
                        "\n\n위에 제공된 **컨텍스트만을 바탕으로** '%s'라는 관광지에 대해 흥미로운 질문 3개를 'Question1: 내용, Question2: 내용, Question3: 내용' 형식으로 다음줄에 제안해줘. " +
                        "⚠️ 컨텍스트에 없는 정보는 사용하지 마." +
                        "질문은 물음표 포함해서 영어 45글자 이내로 해줘!",
                locationExplain, place);
        return callGeminiApi(prompt)
                .map(responseData -> {
                    String combinedAnswer = greeting + responseData.getAnswer();
                    return new ChatbotDataDto(combinedAnswer, responseData.getSuggestedQuestions());
                });
    }

    //대화 이어가기
    public Mono<ChatbotDataDto> continueConversation(String question, String locationExplain) {
        String prompt =String.format(
                "모든 건 영어로 답변해줘! " +
                        "⚠️ 절대 인삿말, 서두, 감탄사(예: 네!, 좋아요!, 알겠습니다!)를 하지 마. " +
                        "\n---제공된 컨텍스트---" +
                        "\n%s" + // DB의 locationExplain
                        "\n---컨텍스트 끝---" +
                        "\n\n위에 제공된 **컨텍스트만을 바탕으로** '%s'라는 질문에 대해 다른 말 없이 바로 설명해줘. " +
                        "⚠️ 컨텍스트에 없는 정보는 절대 대답하지 마." +
                        "그리고 나서 컨텍스트를 바탕으로 흥미로운 질문 3개를 'Question1: 내용, Question2: 내용, Question3: 내용' 형식으로 다음줄에 제안해줘. " +
                        "질문은 물음표 포함해서 영어 45글자 이내로 해줘!",
                locationExplain, question);
        return callGeminiApi(prompt);
    }


    //제미나이 응답을 파싱하여 ChatResponse 객체로 변환
    private ChatbotDataDto parseResponse(Map<String, Object> responseBody) {
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
                //1. 앞뒤 공백제거
                String trimmedLine = line.trim();

                //2. 질문1 질문2 질문3 분리
                if (trimmedLine.contains("Question 1:") || trimmedLine.contains("Question1:") || trimmedLine.contains("Question 2:") || trimmedLine.contains("Question2:")|| trimmedLine.contains("Question 3:") || trimmedLine.contains("Question3:")) {
                    // 3. ':' 뒤의 내용만 잘라내서 질문 목록에 추가
                    questions.add(trimmedLine.substring(trimmedLine.indexOf(":") + 1).trim());
                } else {
                    answerBuilder.append(line).append("\n");
                }
            }

            // 답변 부분에 남아있을 수 있는 마크다운(**) 문자도 제거
            String answer = answerBuilder.toString().trim().replaceAll("\\*\\*", "");

            return new ChatbotDataDto(answer, questions);
        } catch (Exception e) {
            // 파싱 오류 발생 시 기본 응답 반환
            return new ChatbotDataDto("Sorry, There was a problem generating your answer. ", null);
        }
    }
}
