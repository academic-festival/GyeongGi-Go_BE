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

    private Mono<String> getEnglishPlaceName(String koreanPlace) {
        // 1. [새 프롬프트] 오직 번역(영어 이름)만 요청하는 프롬프트입니다.
        String translationPrompt = String.format(
                "다음 한국어 장소 이름을 공식 영어 명칭으로 번역해줘. " +
                        "⚠️ **절대** 인사말, 설명, 따옴표 없이 오직 '영어 이름' 텍스트만 응답해야 해. " +
                        "예시: '수원화성' -> 'Suwon Hwaseong Fortress'" +
                        "\n\n장소: '%s'",
                koreanPlace
        );

        return callGeminiApi(translationPrompt)
                .map(ChatbotDataDto::getAnswer)
                .map(String::trim);
    }

    //대화시작
    public Mono<ChatbotDataDto> startConversation(String place, String locationExplain) {

        return getEnglishPlaceName(place) //헬퍼메서드를 호출해 영어이름 먼저 받아오기
                .flatMap(englishPlace -> {
                    String questionPrompt = String.format(
                            "모든 건 영어로 답변해줘! " +
                                    "⚠️ 절대 인삿말, 서두, 감탄사(예: 네!, 좋아요!, 알겠습니다!)를 하지 마. " +
                                    "\n---제공된 컨텍스트---" +
                                    "\n%s" + // DB의 locationExplain
                                    "\n---컨텍스트 끝---" +
                                    "\n\n위에 제공된 **컨텍스트만을 바탕으로** '%s'라는 관광지에 대해 흥미로운 서로다른 질문 30개를 'Question1: 내용, Question2: 내용, Question3: 내용' 형식으로 다음줄에 제안해줘." +
                                    "⚠️ 컨텍스트에 없는 정보는 사용하지 마." +
                                    "질문은 물음표 포함해서 영어 45글자 이내로 해줘!",
                            locationExplain, englishPlace); // 'place' 대신 'englishPlace' 사용

                    return callGeminiApi(questionPrompt)
                            .map(responseData -> {
                                String greeting = String.format("Hello! What are you curious about %s?", englishPlace);
                                String combinedAnswer = greeting + responseData.getAnswer();
                                return new ChatbotDataDto(combinedAnswer, responseData.getSuggestedQuestions());
                            });
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
                        "⚠️ 컨텍스트에 없는 정보는 절대 대답하지 마.",
                locationExplain, question);
        return callGeminiApi(prompt);
    }


    //제미나이 응답을 파싱하여 ChatResponse 객체로 변환
    private ChatbotDataDto parseResponse(Map<String, Object> responseBody) {
        try {
            // JSON 구조에서 텍스트 부분만 추출
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            // 【추가】: API 응답이 비정상일 경우 방어 코드
            if (candidates == null || candidates.isEmpty()) {
                return new ChatbotDataDto("Sorry, I couldn't get a response.", null);
            }
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) {
                return new ChatbotDataDto("Sorry, the response content is empty.", null);
            }
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                return new ChatbotDataDto("Sorry, the response part is missing.", null);
            }
            String rawText = (String) parts.get(0).get("text");

            // Gemini가 생성한 텍스트에서 답변과 질문 분리
            String[] lines = rawText.split("\\r?\\n");
            StringBuilder answerBuilder = new StringBuilder();
            List<String> questions = new java.util.ArrayList<>();

            // "Question" + 숫자
            java.util.regex.Pattern questionPattern = java.util.regex.Pattern.compile("^Question\\d+:");

            for (String line : lines) {
                //1. 앞뒤 공백제거
                String trimmedLine = line.trim();

                //2. 정규식을 사용하여 라인이 질문 패턴으로 시작하는지 확인
                java.util.regex.Matcher matcher = questionPattern.matcher(trimmedLine);

                if (matcher.find()) {
                    // 3. ':' 뒤의 내용만 잘라내서 질문 목록에 추가
                    questions.add(trimmedLine.substring(trimmedLine.indexOf(":") + 1).trim());
                } else {
                    // 질문이 아닌 라인은 답변(answer)에 추가
                    answerBuilder.append(line).append("\n");
                }
            }

            // 답변 부분에 남아있을 수 있는 마크다운(**) 문자도 제거
            String answer = answerBuilder.toString().trim().replaceAll("\\*\\*", "");

            return new ChatbotDataDto(answer, questions);
        } catch (Exception e) {
            // 파싱 오류 발생 시 기본 응답 반환 (e.printStackTrace() 등으로 로그를 남기는 것이 좋습니다)
            return new ChatbotDataDto("Sorry, There was a problem generating your answer. ", null);
        }
    }
}
