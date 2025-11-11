package academic_festival.gyeonggi_go.chatbot.Dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "장소 고유 ID", example = "153")
    private Long placeId;

    @Schema(description = "유저가 선택한 질문", example = "What is the history behind Suwon Hwaseong?")
    private String question;

}