package com.gamegoo.gamegoo_v2.rollbti.service;

import com.gamegoo.gamegoo_v2.core.exception.RollBtiException;
import com.gamegoo.gamegoo_v2.core.exception.common.ErrorCode;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiChampionLaneResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiCompatibilityResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiTypeSummaryResponse;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RollBtiCatalogService {

    private final Map<RollBtiType, CatalogEntry> catalog = new EnumMap<>(RollBtiType.class);

    public RollBtiCatalogService() {
        initialize();
    }

    public RollBtiTypeSummaryResponse getTypeSummary(RollBtiType type) {
        return getEntry(type).toSummary(type);
    }

    public RollBtiCompatibilityResponse getCompatibility(RollBtiType type) {
        return getEntry(type).toCompatibility(type);
    }

    public Set<RollBtiType> getGoodMatches(RollBtiType type) {
        return new HashSet<>(getEntry(type).goodMatches());
    }

    public Set<RollBtiType> getBadMatches(RollBtiType type) {
        return new HashSet<>(getEntry(type).badMatches());
    }

    private CatalogEntry getEntry(RollBtiType type) {
        CatalogEntry entry = catalog.get(type);
        if (entry == null) {
            throw new RollBtiException(ErrorCode.ROLL_BTI_TYPE_NOT_SUPPORTED);
        }
        return entry;
    }

    private void initialize() {
        catalog.put(RollBtiType.ADCI, entry(
                "단독 캐리형",
                "공격적인 교전에 앞장서며 스스로 한타를 열어 캐리하는 유형입니다.",
                List.of(RollBtiType.ADTB, RollBtiType.ASTI),
                List.of(RollBtiType.FDCB, RollBtiType.FDTB),
                List.of(
                        lane("TOP", "레넥톤", "아트록스"),
                        lane("JUNGLE", "리 신", "바이"),
                        lane("MID", "탈론", "제드"),
                        lane("ADC", "드레이븐", "칼리스타"),
                        lane("SUP", "레오나", "알리스타")
                )));

        catalog.put(RollBtiType.ADCB, entry(
                "돌격형 딜러",
                "공격적으로 성장해 후방 화력으로 전투를 마무리하는 유형입니다.",
                List.of(RollBtiType.ASCI, RollBtiType.ASTB),
                List.of(RollBtiType.FSTB, RollBtiType.FDTI),
                List.of(
                        lane("TOP", "제이스", "퀸"),
                        lane("JUNGLE", "비에고", "그레이브즈"),
                        lane("MID", "신드라", "아칼리"),
                        lane("ADC", "루시안", "사미라"),
                        lane("SUP", "브랜드", "제라스")
                )));

        catalog.put(RollBtiType.ADTI, entry(
                "교전 설계자",
                "성장을 우선하면서도 필요할 때 먼저 교전을 여는 유형입니다.",
                List.of(RollBtiType.ASTI, RollBtiType.FSTI),
                List.of(RollBtiType.FDCI, RollBtiType.FSCB),
                List.of(
                        lane("TOP", "판테온", "다리우스"),
                        lane("JUNGLE", "녹턴", "카직스"),
                        lane("MID", "아리", "오로라"),
                        lane("ADC", "코그모", "닐라"),
                        lane("SUP", "파이크", "니코")
                )));

        catalog.put(RollBtiType.ADTB, entry(
                "폭발적 협동가",
                "공격적인 템포로 팀과 함께 폭발적인 교전을 만드는 유형입니다.",
                List.of(RollBtiType.ADCI, RollBtiType.ASTB),
                List.of(RollBtiType.FDCB, RollBtiType.FSTB),
                List.of(
                        lane("TOP", "갱플랭크", "트린다미어"),
                        lane("JUNGLE", "마스터 이", "벨베스"),
                        lane("MID", "카타리나", "직스"),
                        lane("ADC", "카이사", "트리스타나"),
                        lane("SUP", "세라핀", "자이라")
                )));

        catalog.put(RollBtiType.ASCI, entry(
                "숨은 영웅",
                "자원을 양보하면서도 교전에서 존재감을 만드는 지원형 유형입니다.",
                List.of(RollBtiType.ADCB, RollBtiType.FSCI),
                List.of(RollBtiType.FSCB, RollBtiType.FDTB),
                List.of(
                        lane("TOP", "세트", "뽀삐"),
                        lane("JUNGLE", "자르반 4세", "브라이어"),
                        lane("MID", "갈리오", "말자하"),
                        lane("SUP", "쓰레쉬", "라칸")
                )));

        catalog.put(RollBtiType.ASCB, entry(
                "전략적 설계자",
                "팀 합과 타이밍을 중시하며 전투 구도를 설계하는 유형입니다.",
                List.of(RollBtiType.FDCB, RollBtiType.ASTB),
                List.of(RollBtiType.ADCI, RollBtiType.FDTI),
                List.of(
                        lane("TOP", "초가스", "크산테"),
                        lane("JUNGLE", "아이번", "릴리아"),
                        lane("MID", "오리아나", "라이즈"),
                        lane("SUP", "브랜드", "흐웨이")
                )));

        catalog.put(RollBtiType.ASTI, entry(
                "헌신적 방패",
                "팀을 위해 먼저 진입하고 전투 시작을 책임지는 방패형 유형입니다.",
                List.of(RollBtiType.ADTI, RollBtiType.FSTI),
                List.of(RollBtiType.ADCB, RollBtiType.FDCI),
                List.of(
                        lane("TOP", "말파이트", "마오카이"),
                        lane("JUNGLE", "리 신", "바이"),
                        lane("MID", "트위스티드 페이트", "말파이트"),
                        lane("SUP", "알리스타", "노틸러스")
                )));

        catalog.put(RollBtiType.ASTB, entry(
                "전장의 조율자",
                "후방에서 팀 화력과 전투 템포를 조율하는 유형입니다.",
                List.of(RollBtiType.ADTB, RollBtiType.ASCB),
                List.of(RollBtiType.FDCI, RollBtiType.FSCI),
                List.of(
                        lane("TOP", "리븐", "나르"),
                        lane("JUNGLE", "피들스틱", "우디르"),
                        lane("MID", "흐웨이", "조이"),
                        lane("SUP", "카르마", "스웨인")
                )));

        catalog.put(RollBtiType.FDCI, entry(
                "고독한 성장가",
                "초반에는 파밍에 집중하고 이후 스스로 각을 보는 성장형 유형입니다.",
                List.of(RollBtiType.FDTI, RollBtiType.ASCI),
                List.of(RollBtiType.ASTB, RollBtiType.FSTB),
                List.of(
                        lane("TOP", "이렐리아", "퀸"),
                        lane("JUNGLE", "녹턴", "쉬바나"),
                        lane("MID", "야스오", "요네"),
                        lane("ADC", "이즈리얼", "코그모"),
                        lane("SUP", "럭스", "피들스틱")
                )));

        catalog.put(RollBtiType.FDCB, entry(
                "묵묵한 후방 캐리",
                "안정적으로 성장한 뒤 후방에서 꾸준히 캐리하는 유형입니다.",
                List.of(RollBtiType.ASCB, RollBtiType.FSTB),
                List.of(RollBtiType.ADCI, RollBtiType.ADTB),
                List.of(
                        lane("TOP", "케일"),
                        lane("JUNGLE", "마스터 이", "벨베스"),
                        lane("MID", "아우렐리온 솔", "애니비아"),
                        lane("ADC", "케이틀린", "카이사", "유나라"),
                        lane("SUP", "자이라", "샤코")
                )));

        catalog.put(RollBtiType.FDTI, entry(
                "고독한 이니시형",
                "성장 후 결정적인 순간에 이니시를 여는 유형입니다.",
                List.of(RollBtiType.FDCI, RollBtiType.ADTI),
                List.of(RollBtiType.ASCB, RollBtiType.FSTB),
                List.of(
                        lane("TOP", "모데카이저", "신지드"),
                        lane("JUNGLE", "스카너", "람머스"),
                        lane("MID", "벡스", "럭스"),
                        lane("ADC", "제리", "트위치"),
                        lane("SUP", "렐", "레나타 글라스크")
                )));

        catalog.put(RollBtiType.FDTB, entry(
                "후반의 지배자",
                "후반 파워를 믿고 팀과 함께 게임을 마무리하는 유형입니다.",
                List.of(RollBtiType.FSTB, RollBtiType.ASTB),
                List.of(RollBtiType.ADCI, RollBtiType.ASCI),
                List.of(
                        lane("TOP", "케일", "나서스"),
                        lane("JUNGLE", "킨드레드", "카서스"),
                        lane("MID", "블라디미르", "라이즈"),
                        lane("ADC", "징크스", "시비르", "스몰더"),
                        lane("SUP", "세나", "소나")
                )));

        catalog.put(RollBtiType.FSCI, entry(
                "타이밍 지배자",
                "파밍과 팀 기여의 균형을 맞추며 타이밍을 지배하는 유형입니다.",
                List.of(RollBtiType.ASCI, RollBtiType.FSTI),
                List.of(RollBtiType.ASTB, RollBtiType.FSCB),
                List.of(
                        lane("TOP", "암베사", "잭스"),
                        lane("JUNGLE", "아무무", "아이번"),
                        lane("MID", "오리아나"),
                        lane("SUP", "블리츠크랭크", "쓰레쉬")
                )));

        catalog.put(RollBtiType.FSCB, entry(
                "숨어있는 딜러",
                "팀 지원을 바탕으로 후방에서 안정적인 딜을 넣는 유형입니다.",
                List.of(RollBtiType.FDCB, RollBtiType.ASCB),
                List.of(RollBtiType.ASCI, RollBtiType.ADTI),
                List.of(
                        lane("TOP", "문도 박사", "크산테"),
                        lane("JUNGLE", "브랜드", "킨드레드"),
                        lane("MID", "아크샨", "신드라"),
                        lane("SUP", "자이라", "벨코즈")
                )));

        catalog.put(RollBtiType.FSTI, entry(
                "현명한 지휘관",
                "운영 판단과 이니시를 함께 수행하는 지휘관형 유형입니다.",
                List.of(RollBtiType.ADTI, RollBtiType.FSCI),
                List.of(RollBtiType.FDCI, RollBtiType.ADCB),
                List.of(
                        lane("TOP", "케넨", "다리우스"),
                        lane("JUNGLE", "세주아니", "릴리아"),
                        lane("MID", "사일러스", "말자하"),
                        lane("SUP", "렐", "모르가나")
                )));

        catalog.put(RollBtiType.FSTB, entry(
                "헌신적 지원가",
                "팀 생존과 성장 보조를 최우선으로 두는 헌신형 유형입니다.",
                List.of(RollBtiType.FDCB, RollBtiType.FSCB),
                List.of(RollBtiType.ADCI, RollBtiType.ASCI),
                List.of(
                        lane("TOP", "쉔", "문도 박사"),
                        lane("JUNGLE", "아이번", "볼리베어"),
                        lane("SUP", "유미", "브라움", "질리언")
                )));
    }

    private static CatalogEntry entry(String alias, String description, List<RollBtiType> goodMatches,
                                      List<RollBtiType> badMatches,
                                      List<RollBtiChampionLaneResponse> laneRecommendations) {
        return new CatalogEntry(alias, description, goodMatches, badMatches, laneRecommendations);
    }

    private static RollBtiChampionLaneResponse lane(String lane, String... champions) {
        return RollBtiChampionLaneResponse.of(lane, List.of(champions));
    }

    private record CatalogEntry(
            String alias,
            String description,
            List<RollBtiType> goodMatches,
            List<RollBtiType> badMatches,
            List<RollBtiChampionLaneResponse> laneRecommendations
    ) {
        private RollBtiTypeSummaryResponse toSummary(RollBtiType type) {
            return RollBtiTypeSummaryResponse.of(
                    type,
                    alias,
                    description,
                    List.copyOf(goodMatches),
                    List.copyOf(badMatches),
                    List.copyOf(laneRecommendations));
        }

        private RollBtiCompatibilityResponse toCompatibility(RollBtiType type) {
            return RollBtiCompatibilityResponse.of(type, List.copyOf(goodMatches), List.copyOf(badMatches));
        }
    }
}
