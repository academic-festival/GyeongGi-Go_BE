package academic_festival.gyeonggi_go.Place.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    // 성공 응답을 위한 정적 팩토리 메서드
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "명소 상세정보 조회 성공", data);
    }

    // 실패 응답을 위한 정적 팩토리 메서드 (예시)
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}