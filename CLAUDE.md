# BongShop

마인크래프트 Paper 1.21.4+ 상점 플러그인

## 빌드

```bash
./gradlew build
```

빌드 결과물: `build/libs/BongShop-1.0.0.jar`

## 프로젝트 구조

- `kr.acog.bongshop` - 메인 패키지
  - `command/` - 명령어 라우팅 및 핸들링
  - `config/` - JSON 직렬화/역직렬화
  - `domain/` - 비즈니스 로직 (가격 변동, 재고, 구매/판매 검증)
  - `economy/` - 경제 시스템 프로바이더 (Vault, CoinsEngine, Item)
  - `item/` - Bukkit ItemStack 생성
  - `plugin/` - 기본 설정 생성
  - `shop/` - 상점 관리 및 상태 오케스트레이션
  - `state/` - 상태 영속화
  - `view/` - GUI/인벤토리 빌더

## 코드 컨벤션

- Kotlin 우선, 필요시 Java 혼용
- kotlinx.serialization으로 JSON 설정 처리
- typst 라이브러리 사용 (command, view, serialization)
- 한국어 명령어 및 메시지 사용
- BongDailyshop 프로젝트와 동일한 아키텍처 패턴 따름

## 의존성

- Paper API 1.21
- Vault API (경제)
- CoinsEngine (경제)
- Nexo, ItemsAdder (커스텀 아이템)
- PlaceholderAPI (플레이스홀더)
- typst command/view/serialization 라이브러리

## 설계 문서

- `src/DESIGN.md` - 상세 기획서 (한국어)
