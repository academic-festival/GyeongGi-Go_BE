package academic_festival.gyeonggi_go.chatbot.Controller;

import academic_festival.gyeonggi_go.Place.Repository.PlaceRepository;
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
                                    value = "{\"placename\": \"수원화성\", \"address\": \"수원시\"}"
                            )
                    }
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (placename 또는 address필드 누락)"),
            @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없음")
    })
    public Mono<ApiResponseDto<ChatbotDataDto>> startChat(@RequestBody ChatbotRequestDto request) {
        if (request.getPlacename() == null || request.getPlacename().isEmpty()) {
            // 잘못된 요청에 대한 처리
            ChatbotDataDto errorData = new ChatbotDataDto("placename is required.", null);
            return Mono.just(new ApiResponseDto<>(400, "요청이 유효하지 않습니다. placename이 필요합니다.", errorData));
        }

        if (request.getAddress() == null || request.getAddress().isEmpty()) {
            // 잘못된 요청에 대한 처리
            ChatbotDataDto errorData = new ChatbotDataDto("address is required.", null);
            return Mono.just(new ApiResponseDto<>(400, "요청이 유효하지 않습니다. address가 필요합니다.", errorData));
        }

        return Mono.fromCallable(() -> {
                    // 1. DB에서 장소 정보 조회 (실제 Repository 메서드명으로 변경)
                    return placeRepository.findByPlaceNameAndAddress(request.getPlacename(), request.getAddress())
                            .orElseThrow(() -> new NoSuchElementException("Place not found: " + request.getPlacename() + " at " + request.getAddress()));
                })
                .subscribeOn(Schedulers.boundedElastic()) // Blocking I/O를 별도 스레드 풀에서 실행
                .flatMap(place -> {
                    // 2. DB에서 조회한 locationExplain 값을 서비스로 전달
                    return chatbotService.startConversation(place.getPlaceName(), place.getLocationExplain());
                })
                .map(chatData ->
                        new ApiResponseDto<>(200, "챗봇이 답변을 완료했습니다.", chatData)
                )
                // 3. 장소를 찾지 못했을 때 404 에러 반환
                .onErrorResume(NoSuchElementException.class, e -> {
                    ChatbotDataDto errorData = new ChatbotDataDto(e.getMessage(), null);
                    return Mono.just(new ApiResponseDto<>(404, "장소를 찾을 수 없습니다.", errorData));
                })
                // 기타 예외 처리
                .onErrorResume(Exception.class, e -> {
                    ChatbotDataDto errorData = new ChatbotDataDto("An internal error occurred.", null);
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
                                    value = "{\"placename\": \"수원화성\", \"address\": \"수원시\", \"question\": \"What is the history behind Suwon Hwaseong?\"}"
                            )
                    }
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (question 또는 placename 필드 또는 address 누락)"),
            @ApiResponse(responseCode = "404", description = "장소를 찾을 수 없음")
    })
    public Mono<ApiResponseDto<ChatbotDataDto>> continueChat(@RequestBody ChatbotRequestDto request) {
        if (request.getQuestion() == null || request.getQuestion().isEmpty() ||
                request.getPlacename() == null || request.getPlacename().isEmpty() ||
                request.getAddress() == null || request.getAddress().isEmpty()){

            ChatbotDataDto errorData = new ChatbotDataDto("Question and Placename and Address are required.", null);
            return Mono.just(new ApiResponseDto<>(400, "요청이 유효하지 않습니다. question과 placename과 address가 필요합니다.", errorData));
        }

        return Mono.fromCallable(() -> {
                    // 1. DB에서 장소 정보 조회
                    return placeRepository.findByPlaceNameAndAddress(request.getPlacename(), request.getAddress())
                            .orElseThrow(() -> new NoSuchElementException("Place not found: " + request.getPlacename() + " at " + request.getAddress()));
                })
                .subscribeOn(Schedulers.boundedElastic()) // Blocking I/O를 별도 스레드 풀에서 실행
                .flatMap(place -> {
                    // 2. DB에서 조회한 locationExplain 값과 질문을 서비스로 전달
                    return chatbotService.continueConversation(request.getQuestion(), place.getLocationExplain());
                })
                .map(chatData ->
                        new ApiResponseDto<>(200, "챗봇이 답변을 완료했습니다.", chatData)
                )

                // 장소를 찾지 못했을 때 404 에러 반환
                .onErrorResume(NoSuchElementException.class, e -> {
                    ChatbotDataDto errorData = new ChatbotDataDto(e.getMessage(), null);
                    return Mono.just(new ApiResponseDto<>(404, "장소를 찾을 수 없습니다.", errorData));
                })
                // 기타 예외 처리
                .onErrorResume(Exception.class, e -> {
                    ChatbotDataDto errorData = new ChatbotDataDto("An internal error occurred.", null);
                    return Mono.just(new ApiResponseDto<>(500, "서버 내부 오류.", errorData));
                });
    }
}