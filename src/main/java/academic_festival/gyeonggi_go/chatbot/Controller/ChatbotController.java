package academic_festival.gyeonggi_go.chatbot.Controller;

import academic_festival.gyeonggi_go.chatbot.Dto.Request.ChatbotRequestDto;
import academic_festival.gyeonggi_go.chatbot.Dto.Response.ApiResponseDto;
import academic_festival.gyeonggi_go.chatbot.Dto.Response.ChatbotDataDto;
import academic_festival.gyeonggi_go.chatbot.Service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "Chatbot API", description = "제미나이 api를 이용한 챗봇기능 api입니다")
@RestController
@RequestMapping("/chatbot")
public class ChatbotController {
    private final ChatbotService chatbotService;

    @Autowired
    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/start")
    @Operation(summary = "챗봇 대화 시작", description = "장소 이름을 기반으로 챗봇과의 대화를 시작합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "장소 이름으로 대화를 시작합니다.",
            content = @Content(
                    schema = @Schema(implementation = ChatbotRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "대화 시작",
                                    summary = "장소로 대화 시작",
                                    value = "{\"placename\": \"Suwon Hwaseong\"}"
                            )
                    }
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 placename 필드 누락)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class)))
    })
    public Mono<ApiResponseDto<ChatbotDataDto>> startChat(@RequestBody ChatbotRequestDto request) {
        if (request.getPlacename() == null || request.getPlacename().isEmpty()) {
            // 잘못된 요청에 대한 처리
            ChatbotDataDto errorData = new ChatbotDataDto("Placename is required.", null);
            return Mono.just(new ApiResponseDto<>(400, "요청이 유효하지 않습니다. placename이 필요합니다.", errorData));
        }

        Mono<ChatbotDataDto> chatDataMono = chatbotService.startConversation(request.getPlacename());

        // 서비스로부터 성공적인 결과를 받으면, ApiResponseDto로 감싸서 반환
        return chatDataMono.map(chatData ->
                new ApiResponseDto<>(200, "챗봇이 답변을 완료했습니다.", chatData)
        );
    }

    @PostMapping("/relay")
    @Operation(summary = "챗봇 대화 이어가기", description = "질문(question)을 기반으로 챗봇과의 대화를 이어갑니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "질문으로 대화를 이어갑니다.",
            content = @Content(
                    schema = @Schema(implementation = ChatbotRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "대화 이어가기",
                                    summary = "질문으로 대화 시작",
                                    value = "{\"question\": \"What is the history behind Suwon Hwaseong?\"}"
                            )
                    }
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (question 필드 누락)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class)))
    })
    public Mono<ApiResponseDto<ChatbotDataDto>> continueChat(@RequestBody ChatbotRequestDto request) {
        if (request.getQuestion() == null || request.getQuestion().isEmpty()) {
            // 잘못된 요청에 대한 처리
            ChatbotDataDto errorData = new ChatbotDataDto("Question is required.", null);
            return Mono.just(new ApiResponseDto<>(400, "요청이 유효하지 않습니다. question이 필요합니다.", errorData));
        }

        Mono<ChatbotDataDto> chatDataMono = chatbotService.continueConversation(request.getQuestion());

        // 서비스로부터 성공적인 결과를 받으면, ApiResponseDto로 감싸서 반환
        return chatDataMono.map(chatData ->
                new ApiResponseDto<>(200, "챗봇이 답변을 완료했습니다.", chatData)
        );
    }
}