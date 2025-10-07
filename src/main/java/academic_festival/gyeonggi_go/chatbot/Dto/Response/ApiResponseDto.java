package academic_festival.gyeonggi_go.chatbot.Dto.Response;

public class ApiResponseDto<T> {
    private final int code;
    private final String message;
    private final T data;

    public ApiResponseDto(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    //Getter
    public int getCode(){
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
