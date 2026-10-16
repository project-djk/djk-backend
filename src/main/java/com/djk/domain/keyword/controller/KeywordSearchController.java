package com.djk.domain.keyword.controller;

import com.djk.domain.keyword.dto.KeywordSearchCondition;
import com.djk.domain.keyword.dto.KeywordSearchResponse;
import com.djk.domain.keyword.dto.KeywordSearchSort;
import com.djk.domain.keyword.dto.ScoreRange;
import com.djk.domain.keyword.dto.TrendPeriod;
import com.djk.domain.keyword.service.KeywordSearchService;
import com.djk.domain.ranking.entity.BadgeType;
import com.djk.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "키워드", description = "키워드 조회 API")
@RestController
@RequestMapping("/api/v1/keywords")
@RequiredArgsConstructor
public class KeywordSearchController {

    private final KeywordSearchService keywordSearchService;

    @Operation(
            summary = "키워드 상세 검색",
            description = """
                    검색 대상은 최신 TREND·POTENTIAL·RISING 스냅샷 중 하나 이상에 든 키워드다.
                    검색어·카테고리·뱃지·지수 범위·급상승 여부로 거르고, 정렬해서 페이지 단위로 내려준다.
                    각 항목은 트렌드/Potential 순위와 순위 변화, 연관어, 뱃지, 기간별 트렌드 지수 추이를 담는다.
                    스냅샷이 없거나 조건에 맞는 키워드가 없으면 items=[] 이다.""")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "조회 성공 (결과가 없어도 200). 응답 스키마와 예시는 반환 타입에서 생성된다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "지수 범위의 하한이 상한보다 크면 보낸 값을 details 로 돌려준다. 그 밖의 파라미터 오류는 details=null",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = {
                            @ExampleObject(name = "범위 역전", value = """
                                    {"isSuccess": false, "message": "trendMin은 trendMax보다 클 수 없습니다.",
                                     "details": {"trendMin": 90, "trendMax": 50}}
                                    """),
                            @ExampleObject(name = "잘못된 파라미터", value = """
                                    {"isSuccess": false, "message": "잘못된 요청 파라미터입니다.", "details": null}
                                    """)}))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "categoryId 에 해당하는 카테고리가 없음",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = """
                            {"isSuccess": false, "message": "해당 카테고리를 찾을 수 없습니다.", "details": null}
                            """)))
    @GetMapping("/search")
    public ApiResponse<KeywordSearchResponse> search(
            @Parameter(description = "검색어. 앞뒤 공백과 대소문자를 무시한다. 이름에 포함된 키워드와, 이름이 정확히 같은 키워드의 연관어를 찾는다")
            @RequestParam(required = false) String q,
            @Parameter(description = "카테고리 ID (하나만)")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "뱃지. 콤마로 여러 개를 주면 하나라도 가진 키워드 (예: TREND_NEW,RISING)")
            @RequestParam(required = false) List<BadgeType> badgeType,
            @Parameter(description = "trendLine 기간: 7D / 30D / 90D. 키워드를 거르지는 않는다")
            @RequestParam(defaultValue = "7D") @Pattern(regexp = TrendPeriod.CODE_PATTERN) String period,
            @Parameter(description = "트렌드 지수 하한 (0~100, 포함). 0~100 전체면 트렌드 랭킹에 없는 키워드도 포함한다")
            @RequestParam(defaultValue = "0") @Min(ScoreRange.LOWEST) @Max(ScoreRange.HIGHEST) int trendMin,
            @Parameter(description = "트렌드 지수 상한 (0~100, 포함)")
            @RequestParam(defaultValue = "100") @Min(ScoreRange.LOWEST) @Max(ScoreRange.HIGHEST) int trendMax,
            @Parameter(description = "Potential 점수 하한 (0~100, 포함). 0~100 전체면 Potential 랭킹에 없는 키워드도 포함한다")
            @RequestParam(defaultValue = "0") @Min(ScoreRange.LOWEST) @Max(ScoreRange.HIGHEST) int potentialMin,
            @Parameter(description = "Potential 점수 상한 (0~100, 포함)")
            @RequestParam(defaultValue = "100") @Min(ScoreRange.LOWEST) @Max(ScoreRange.HIGHEST) int potentialMax,
            @Parameter(description = "true 면 최신 RISING 스냅샷에 든 키워드만 (RISING 뱃지와는 별개)")
            @RequestParam(defaultValue = "false") boolean risingOnly,
            @Parameter(description = "정렬. TREND=트렌드 지수순, POTENTIAL=Potential 점수순, LATEST=최초 수집 최근순. 값이 없으면 맨 뒤")
            @RequestParam(defaultValue = "TREND") KeywordSearchSort sort,
            @Parameter(description = "페이지 번호 (0부터)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (1~100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        KeywordSearchCondition condition = new KeywordSearchCondition(
                q, categoryId, badgeType, TrendPeriod.fromCode(period),
                ScoreRange.of("trend", trendMin, trendMax),
                ScoreRange.of("potential", potentialMin, potentialMax),
                risingOnly, sort, page, size);
        return ApiResponse.success("검색 결과를 조회하였습니다.",
                KeywordSearchResponse.from(keywordSearchService.search(condition)));
    }
}
