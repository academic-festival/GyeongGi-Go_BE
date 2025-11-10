package academic_festival.gyeonggi_go.Home.Dto;

import com.fasterxml.jackson.annotation.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
public class GgApiResponse {
    private List<GgApiData> combinedData = new ArrayList<>();

    @JsonAnySetter
    public void handleUnknown(String key, List<GgApiData> value) {
        if (value != null) {
            combinedData.addAll(value);
        }
    }
    @Getter
    @Setter
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public static class GgApiData {
        private List<Head> head;
        private List<Row> row;
    }

    @Getter
    @Setter
    public static class Head {
        @JsonProperty("list_total_count")
        private Integer listTotalCount;
        @JsonProperty("RESULT")
        private Result result;
        @JsonProperty("api_version")
        private String apiVersion;
    }

    @Getter
    @Setter
    public static class Result {
        @JsonProperty("CODE")
        private String code;
        @JsonProperty("MESSAGE")
        private String message;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class Row {
        @EqualsAndHashCode.Include
        @JsonProperty("SIGUN_NM")
        private String sigunNm;

        @EqualsAndHashCode.Include
        @JsonProperty("TURSM_INFO_NM")
        private String turSmInfoNm;

        //ADST API
        @JsonProperty("NM_SM_NM")
        private String nmSmNm;

        @JsonProperty("TELNO")
        private String telNo;
        @JsonProperty("DATA_STD_DE")
        private String dataStdDe;
        @JsonProperty("REFINE_WGS84_LAT")
        private String refineWgs84Lat;
        @JsonProperty("REFINE_WGS84_LOGT")
        private String refineWgs84Logt;

        // ETST, TTST API
        @JsonProperty("SM_RE_ADDR")
        private String smReAddr;

        @JsonGetter("TURSM_INFO_NM")
        public String getTurSmInfoNmForOutput() {
            if (this.turSmInfoNm != null && !this.turSmInfoNm.trim().isEmpty()) {
                return this.turSmInfoNm;
            }
            return this.nmSmNm;
        }
    }
}