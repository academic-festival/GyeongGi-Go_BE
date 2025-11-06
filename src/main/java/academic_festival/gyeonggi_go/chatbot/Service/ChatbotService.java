package academic_festival.gyeonggi_go.chatbot.Service;

import academic_festival.gyeonggi_go.chatbot.Dto.Response.ChatbotDataDto;
import academic_festival.gyeonggi_go.tts.Service.TtsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64; // [추가]
import java.util.List;
import java.util.Map;

@Service
public class ChatbotService {
    private final WebClient webClient;
    private final String apiKey;
    private final TtsService ttsService;
    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);

    public ChatbotService(WebClient.Builder webClientBuilder,
                          @Value("${gemini.api.url}") String apiUrl,
                          @Value("${gemini.api.key}") String apiKey,
                          TtsService ttsService) { // [추가] 생성자 주입
        this.webClient = webClientBuilder.baseUrl(apiUrl).build();
        this.apiKey = apiKey;
        this.ttsService = ttsService;
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
                .bodyToMono(Map.class)
                .map(this::parseResponse);
    }

    //영어이름으로 변경하는 메서드
    private Mono<String> getEnglishPlaceName(String koreanPlace) {
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

    // ttsService.synthesizeText 호출 및 Mono.zip을 사용한 병합
    private Mono<ChatbotDataDto> createDtoWithTts(ChatbotDataDto geminiDto, boolean enableTts) {
        if (!enableTts || geminiDto.getAnswer() == null || geminiDto.getAnswer().isEmpty()) {
            // TTS가 비활성화되었거나, 답변이 없으면 DTO를 그대로 반환
            return Mono.just(geminiDto);
        }

        // TTS 변환 Mono
        Mono<byte[]> ttsMono = ttsService.synthesizeText(geminiDto.getAnswer())
                .onErrorResume(e -> {
                    // TTS 실패 시 에러 로그만 남기고 빈 Mono 반환 (오디오 없이 응답)
                    logger.warn("TTS synthesis failed: {}", e.getMessage());
                    return Mono.empty(); // 오디오 데이터는 null이 됨
                });

        // 기존 Gemini DTO와 TTS 결과를 병합
        return Mono.zip(Mono.just(geminiDto), ttsMono.map(this::encodeBase64).defaultIfEmpty(""))
                .map(tuple -> {
                    ChatbotDataDto dto = tuple.getT1();
                    String audioBase64 = tuple.getT2().isEmpty() ? null : tuple.getT2();

                    // audioData가 포함된 새 DTO 인스턴스 생성
                    return new ChatbotDataDto(dto.getAnswer(), dto.getSuggestedQuestions(), audioBase64);
                });
    }

    // Base64 인코더 헬퍼 메서드
    private String encodeBase64(byte[] audioBytes) {
        if (audioBytes == null || audioBytes.length == 0) {
            return null;
        }
        return Base64.getEncoder().encodeToString(audioBytes);
    }


    //대화시작
    public Mono<ChatbotDataDto> startConversation(String place, String locationExplain, boolean enableTts) {

        return getEnglishPlaceName(place)
                .flatMap(englishPlace -> {
                    String questionPrompt = String.format(
                            "모든 건 영어로 답변해줘! " +
                                    "⚠️ 절대 인삿말, 서두, 감탄사(예: 네!, 좋아요!, 알겠습니다!)를 하지 마. " +
                                    "\n---제공된 컨텍스트---" +
                                    "\n%s" +
                                    "\n---컨텍스트 끝---" +
                                    "\n\n위에 제공된 **컨텍스트만을 바탕으로** '%s'라는 관광지에 대해 흥미로운 서로다른 질문 30개를 'Question1: 내용, Question2: 내용, Question3: 내용' 형식으로 다음줄에 제안해줘." +
                                    "⚠️ 컨텍스트에 없는 정보는 사용하지 마." +
                                    "질문은 물음표 포함해서 영어 45글자 이내로 해줘!",
                            locationExplain, englishPlace);

                    // Gemini 응답을 받은 후, TTS 로직을 연결
                    return callGeminiApi(questionPrompt)
                            .flatMap(geminiDto -> {
                                // 1. 응답 텍스트 조합
                                String greeting = String.format("Hello! What are you curious about %s?", englishPlace);
                                String combinedAnswer = greeting + geminiDto.getAnswer();

                                // 2. 조합된 DTO 생성 (아직 오디오는 없음)
                                ChatbotDataDto dtoWithGreeting = new ChatbotDataDto(combinedAnswer, geminiDto.getSuggestedQuestions(), null);

                                // 3. TTS 처리 (내부에서 enableTts 분기 처리)
                                return createDtoWithTts(dtoWithGreeting, enableTts);
                            });
                });
    }


    //대화 이어가기
    public Mono<ChatbotDataDto> continueConversation(String question, String locationExplain, boolean enableTts) { // [수정] enableTts 파라미터 추가
        String prompt =String.format(
                "모든 건 영어로 답변해줘! " +
                        "⚠️ 절대 인삿말, 서두, 감탄사(예: 네!, 좋아요!, 알겠습니다!)를 하지 마. " +
                        "\n---제공된 컨텍스트---" +
                        "\n%s" +
                        "\n---컨텍스트 끝---" +
                        "\n\n위에 제공된 **컨텍스트만을 바탕으로** '%s'라는 질문에 대해 다른 말 없이 바로 설명해줘. " +
                        "⚠️ 컨텍스트에 없는 정보는 절대 대답하지 마.",
                locationExplain, question);

        // [수정] Gemini 응답을 받은 후, TTS 로직을 연결
        return callGeminiApi(prompt)
                .flatMap(geminiDto -> createDtoWithTts(geminiDto, enableTts));
    }


    //제미나이 응답을 파싱하여 ChatResponse 객체로 변환
    private ChatbotDataDto parseResponse(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return new ChatbotDataDto("Sorry, I couldn't get a response.", null, null);
            }
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) {
                return new ChatbotDataDto("Sorry, the response content is empty.", null, null);
            }
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                return new ChatbotDataDto("Sorry, the response part is missing.", null, null);
            }
            String rawText = (String) parts.get(0).get("text");

            String[] lines = rawText.split("\\r?\\n");
            StringBuilder answerBuilder = new StringBuilder();
            List<String> questions = new java.util.ArrayList<>();
            java.util.regex.Pattern questionPattern = java.util.regex.Pattern.compile("^Question\\d+:");

            for (String line : lines) {
                String trimmedLine = line.trim();
                java.util.regex.Matcher matcher = questionPattern.matcher(trimmedLine);
                if (matcher.find()) {
                    questions.add(trimmedLine.substring(trimmedLine.indexOf(":") + 1).trim());
                } else {
                    answerBuilder.append(line).append("\n");
                }
            }

            String answer = answerBuilder.toString().trim().replaceAll("\\*\\*", "");

            return new ChatbotDataDto(answer, questions, null); // audioData는 null로 초기화
        } catch (Exception e) {
            return new ChatbotDataDto("Sorry, There was a problem generating your answer. ", null, null);
        }
    }
}