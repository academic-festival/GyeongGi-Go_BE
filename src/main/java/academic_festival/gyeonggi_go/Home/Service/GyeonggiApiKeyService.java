package academic_festival.gyeonggi_go.Home.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
// import java.util.concurrent.atomic.AtomicInteger; // 순환 로직은 여기서 제거

@Service
public class GyeonggiApiKeyService {

    @Value("#{'${gyeonggi.api.keys}'.split(',')}")
    private List<String> apiKeys;

    @Value("#{'${gyeonggi.api.urls}'.split(',')}")
    private List<String> apiUrls;

    @PostConstruct
    public void init() {
        if (apiKeys.isEmpty() || apiKeys.get(0).trim().isEmpty()) {
            throw new IllegalStateException("GYEONGGI_API_KEYS 환경 변수에 쉼표로 구분된 키가 설정되지 않았습니다.");
        }
        if (apiKeys.size() != apiUrls.size()) {
            throw new IllegalStateException("API 키 개수(" + apiKeys.size() + ")와 URL 개수(" + apiUrls.size() + ")가 일치하지 않습니다. 순서대로 1:1 매핑되어야 합니다.");
        }
        System.out.println("✅ 총 " + apiKeys.size() + "개의 API 키와 " + apiUrls.size() + "개의 URL이 로드되었습니다.");
    }

    public List<String> getAllApiKeys() {
        return apiKeys;
    }
    public List<String> getAllApiUrls() {
        return apiUrls;
    }
}