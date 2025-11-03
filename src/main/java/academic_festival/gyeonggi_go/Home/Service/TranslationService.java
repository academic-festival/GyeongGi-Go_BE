package academic_festival.gyeonggi_go.Home.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class TranslationService {
    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TranslationService(WebClient.Builder webClientBuilder,
                              @Value("${gemini.api.url}") String apiUrl,
                              @Value("${gemini.api.key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(apiUrl).build();
        this.apiKey = apiKey;
    }

    // Gemini API를 통해 텍스트를 영어로 번역
    // param : jsonText - 번역할 원본 json
    // return : 번역한 텍스트를 포함하는 Mono<String>
    public Mono<String> translateToEnglish(String jsonText) {
        String prompt = String.format(
                "아래 JSON 데이터를 영어로 번역해줘. 'code'와 'data' 내의 'x', 'y' (좌표), 'refineWgs84Lat', 'refineWgs84Logt' 값은 절대 수정하지 마. " +
                "다른 모든 한글 문자열(예: message, sigunNm, turSmInfoNm, nmSmNm, smReAddr, placeName, locationExplain 등)은 자연스러운 영어로 번역해야 해. " +
                "번역된 내용 외에 어떠한 설명이나 서두도 붙이지 말고, 결과는 오직 유효한 JSON 형식으로만 반환해줘. " +
                "\n\n---원본 데이터---\n%s", jsonText);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of(
                                "text", prompt)))));

        return webClient.post()
                .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::parseGeminiResponse)
                .onErrorResume(e -> {
                    System.out.println("번역 API 호출 또는 파싱 오류 발생 : " + e.getMessage());
                    return Mono.just(jsonText);
                });
    }

    public String parseGeminiResponse(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String rawText = (String) parts.get(0).get("text");

            // 혹시 모를 코드 블록 마크다운을 제거
            if (rawText.startsWith("```json")) {
                rawText = rawText.substring(7);
            }
            if (rawText.endsWith("```")) {
                rawText = rawText.substring(0, rawText.length() - 3);
            }

            return rawText.trim();
        } catch (Exception e) {
            throw new RuntimeException("Gemini 응답 파싱 중 오류 발생", e);
        }
    }
}
