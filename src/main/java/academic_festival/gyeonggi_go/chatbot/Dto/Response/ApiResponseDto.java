package academic_festival.gyeonggi_go.chatbot.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponseDto<T> {
    private final int code;
    private final String message;
    private final T data;
}
