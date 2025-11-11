package academic_festival.gyeonggi_go.chatbot.Controller;

import academic_festival.gyeonggi_go.Place.Repository.PlaceRepository;
import academic_festival.gyeonggi_go.chatbot.Dto.Request.ChatbotRequestDto;
import academic_festival.gyeonggi_go.chatbot.Dto.Response.ApiResponseDto;
import academic_festival.gyeonggi_go.chatbot.Dto.Response.ChatbotDataDto;
import academic_festival.gyeonggi_go.chatbot.Service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.NoSuchElementException;

@Tag(name = "Chatbot API", description = "제미나이 api를 이용한 챗봇기능 api입니다")
@RestController
@RequestMapping("/chatbot")
public class ChatbotController {
    private final ChatbotService chatbotService;
    private final PlaceRepository placeRepository;

    @Autowired
    public ChatbotController(ChatbotService chatbotService, PlaceRepository placeRepository) {
        this.chatbotService = chatbotService;
        this.placeRepository = placeRepository;
    }

    @PostMapping("/start")
    @Operation(summary = "챗봇 대화 시작", description = "장소 ID를 기반으로 챗봇과의 대화를 시작합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "장소 ID로 대화를 시작합니다.",
            content = @Content(
                    schema = @Schema(implementation = ChatbotRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "대화 시작",
                                    summary = "장소 ID로 대화 시작",
                                    value = "{\"placeId\": 10}"
                            )
                    }
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (placeId 필드 누락)"),
            @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없음")
    })
    public Mono<ApiResponseDto<ChatbotDataDto>> startChat(@RequestBody ChatbotRequestDto request,
                                                          @Parameter(description = "TTS 음성 응답 활성화 여부", example = "true")
                                                          @RequestParam(value = "enableTts", defaultValue = "false") boolean enableTts) {
        // 수정: placeId가 null인지 체크
        if (request.getPlaceId() == null) {
            ChatbotDataDto errorData = new ChatbotDataDto("placeId is required.", null, null);
            return Mono.just(new ApiResponseDto<>(400, "요청이 유효하지 않습니다. placeId가 필요합니다.", errorData));
        }

        return Mono.fromCallable(() -> {
                    // 1. DB에서 placeId로 장소 정보 조회
                    return placeRepository.findById(request.getPlaceId())
                            .orElseThrow(() -> new NoSuchElementException("Place not found for ID: " + request.getPlaceId()));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(place -> {

                    // 2. DB에서 조회한 locationExplain 값을 서비스로 전달
                    return chatbotService.startConversation(place.getPlaceName(), place.getLocationExplain(), enableTts);
                })
                .map(chatData ->
                        new ApiResponseDto<>(200, "챗봇이 답변을 완료했습니다.", chatData)
                )

                // 3. 장소를 찾지 못했을 때 404 에러 반환
                .onErrorResume(NoSuchElementException.class, e -> {
                    ChatbotDataDto errorData = new ChatbotDataDto(e.getMessage(), null, null);
                    return Mono.just(new ApiResponseDto<>(404, "장소를 찾을 수 없습니다.", errorData));
                })
                // 기타 예외 처리
                .onErrorResume(Exception.class, e -> {
                    // 서버 내부 오류 발생 시 디버깅을 위해 스택 트레이스 출력 (선택적)
                    e.printStackTrace();
                    ChatbotDataDto errorData = new ChatbotDataDto("An internal error occurred.", null, null);
                    return Mono.just(new ApiResponseDto<>(500, "서버 내부 오류.", errorData));
                });
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
                                    value = "{\"placeId\": 10, \"question\": \"What is the history behind Suwon Hwaseong?\"}"
                            )
                    }
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (question 또는 placeId 필드 누락)"),
            @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없음")
    })
    public Mono<ApiResponseDto<ChatbotDataDto>> continueChat(@RequestBody ChatbotRequestDto request,
                                                             @Parameter(description = "TTS 음성 응답 활성화 여부", example = "true")
                                                             @RequestParam(value = "enableTts", defaultValue = "false") boolean enableTts) {
        // 수정: placeId 또는 question이 null인지 체크
        if (request.getQuestion() == null || request.getQuestion().isEmpty() || request.getPlaceId() == null){
            ChatbotDataDto errorData = new ChatbotDataDto("Question and placeId are required.", null, null);
            return Mono.just(new ApiResponseDto<>(400, "요청이 유효하지 않습니다. question과 placeId가 필요합니다.", errorData));
        }

        return Mono.fromCallable(() -> {
                    // 1. DB에서 placeId로 장소 정보 조회
                    return placeRepository.findById(request.getPlaceId())
                            .orElseThrow(() -> new NoSuchElementException("Place not found for ID: " + request.getPlaceId()));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(place -> {

                    // 2. DB에서 조회한 locationExplain 값과 질문을 서비스로 전달
                    return chatbotService.continueConversation(request.getQuestion(), place.getLocationExplain(), enableTts);
                })
                .map(chatData ->
                        new ApiResponseDto<>(200, "챗봇이 답변을 완료했습니다.", chatData)
                )

                // 장소를 찾지 못했을 때 404 에러 반환
                .onErrorResume(NoSuchElementException.class, e -> {
                    ChatbotDataDto errorData = new ChatbotDataDto(e.getMessage(), null, null);
                    return Mono.just(new ApiResponseDto<>(404, "장소를 찾을 수 없습니다.", errorData));
                })
                // 기타 예외 처리
                .onErrorResume(Exception.class, e -> {
                    // 서버 내부 오류 발생 시 디버깅을 위해 스택 트레이스 출력 (선택적)
                    e.printStackTrace();
                    ChatbotDataDto errorData = new ChatbotDataDto("An internal error occurred.", null, null);
                    return Mono.just(new ApiResponseDto<>(500, "서버 내부 오류.", errorData));
                });
    }
}