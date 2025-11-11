package academic_festival.gyeonggi_go.config; // 또는 적절한 config 패키지

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * List<String> (Java 객체) -> String (DB 컬럼)
     */
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            // List<String>을 JSON 문자열로 직렬화
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            // 직렬화 오류 시 RuntimeException 발생
            throw new IllegalArgumentException("Error converting List<String> to JSON string", e);
        }
    }

    /**
     * String (DB 컬럼) -> List<String> (Java 객체)
     */
    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return Collections.emptyList(); // null 또는 빈 문자열인 경우 빈 리스트 반환
        }
        try {
            // JSON 문자열을 List<String>으로 역직렬화
            return objectMapper.readValue(dbData, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (IOException e) {
            // 역직렬화 오류 시 RuntimeException 발생
            throw new IllegalArgumentException("Error converting JSON string to List<String>", e);
        }
    }
}