#!/usr/bin/env bash
# 0-B 실기기 측정: 혼돈의 주사위 프롬프트 20건을 GPU와 CPU로 각각 돌리고,
# 게임 경로로 주사위를 3번 굴려 본 뒤(1단계 확인) 결과를 가져온다.
#
# Git Bash에서 실행:  bash docs/ondevice-0A/0B_실기기_측정.sh
# 먼저 폰에서 개발자 옵션 → USB 디버깅을 켜고 USB로 연결해 "USB 디버깅 허용"을 누른다.
# 삼성은 설정 → 보안 및 개인정보 보호 → 자동 차단을 꺼야 설치와 adb 명령이 된다.
#
# 바꿀 수 있는 값 (앞에 붙여서 실행, 예: COUNT=1 BACKENDS=cpu bash ...)
#   BACKENDS  재는 순서 (기본 "gpu cpu")
#   COUNT     백엔드마다 보낼 이벤트 수 (기본 20)
#   MODEL     PC에 있는 모델 파일 경로
#   ADB       adb.exe 경로
set -euo pipefail
export MSYS_NO_PATHCONV=1 # Git Bash가 /sdcard 경로를 윈도 경로로 바꾸지 않게

ADB="${ADB:-$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe}"
MODEL="${MODEL:-C:/Users/super/models/gemma-4-E2B-it.litertlm}"
BACKENDS="${BACKENDS:-gpu cpu}"
COUNT="${COUNT:-20}"
PKG=com.lkj.textdungeon # applicationId (소스 패키지 com.example.textdungeon과 다름)
ACT=$PKG/com.textdungeon.ai.OnDeviceSpikeActivity
FILES=/sdcard/Android/data/$PKG/files
# adb.exe는 /c/... 형식을 모르므로 Git Bash에서는 C:/... 형식으로 받는다 (pwd -W)
ROOT="$(cd "$(dirname "$0")/../.." && (pwd -W 2> /dev/null || pwd))"
OUT="$ROOT/docs/ondevice-0B/raw"

adb() { "$ADB" "$@"; }
prop() { adb shell getprop "$1" | tr -d '\r'; }
battery_temp() { adb shell dumpsys battery | tr -d '\r' | awk '/^ *temperature:/ {printf "%.1f°C", $2 / 10; exit}'; }

# 측정 Activity를 새로 띄우고(-S: 이전 실행을 멈춤) Logcat 태그 SPIKE에 "완료"가 찍힐 때까지 기다린다
run_spike() {
    adb logcat -c
    adb shell am start -S -n "$ACT" --es model "$(basename "$MODEL")" --ez cloud false "$@" > /dev/null
    sleep 5
    while ! adb logcat -d -s SPIKE:I | grep -q -e "완료" -e "측정 중단"; do
        if [ -z "$(adb shell pidof $PKG | tr -d '\r')" ]; then
            echo "  앱이 측정 중에 종료됨 (GPU 초기화 중 네이티브 크래시일 수 있음)"
            return 1
        fi
        sleep 10
    done
    adb logcat -d -s SPIKE:I | tr -d '\r' | grep -e "엔진:" -e "완료" -e "측정 중단" | sed 's/^.*SPIKE *: /  /'
}

adb get-state > /dev/null # 연결된 기기가 없으면 여기서 멈춘다
mkdir -p "$OUT"
{
    echo "기기: $(prop ro.product.model) ($(prop ro.product.device))"
    echo "Android $(prop ro.build.version.release), SoC $(prop ro.soc.model)"
    adb shell cat /proc/meminfo | tr -d '\r' | head -1
    echo "측정 전 배터리 온도: $(battery_temp)"
} | tee "$OUT/device.txt"

echo "디버그 빌드 설치..."
(cd "$ROOT" && ./gradlew installDebug -q)

# models 폴더는 앱이 만들어야 한다 (adb shell mkdir로 만들면 앱이 읽지 못한다).
# 모델이 없으면 Activity를 한 번 띄워 폴더를 만들게 한 다음 복사한다
REMOTE_MODEL="$FILES/models/$(basename "$MODEL")"
if ! adb shell ls "$REMOTE_MODEL" > /dev/null 2>&1; then
    echo "models 폴더 만들기..."
    run_spike --es backend cpu --ei count 1 > /dev/null || true
    echo "모델 복사 (2.6GB, 몇 분 걸림)..."
    adb push "$MODEL" "$FILES/models/"
fi

for backend in $BACKENDS; do
    echo "[$backend] $COUNT건 측정 시작 ($(date +%H:%M:%S), 배터리 $(battery_temp))"
    run_spike --es backend "$backend" --ei count "$COUNT" || true
    echo "[$backend] 끝 ($(date +%H:%M:%S), 배터리 $(battery_temp))" | tee -a "$OUT/device.txt"
done

# 1단계 확인: 게임과 같은 경로(AiManager → ChaosDice → FallbackBackend)로 주사위 3번.
# 디버그 플래그를 기본값으로 돌려서 출시 빌드와 같은 설정(GPU만, 램 검사)으로 돈다
echo "[게임 경로] 주사위 3번 ($(date +%H:%M:%S))"
adb shell am start -S -n $PKG/com.textdungeon.ai.OnDeviceFlagsActivity --ez reset true > /dev/null
sleep 2
run_spike --ei dice 3 --ez wait true --ez release true > /dev/null || true
adb logcat -d -v time -s SPIKE:I FallbackBackend:* OnDeviceBackend:* | tr -d '\r' | grep -v -e "^---" -e "	at " \
    | tee "$OUT/dice.txt" | sed 's/^/  /'

adb pull "$FILES/spike/." "$OUT/" > /dev/null
echo "결과: $OUT"
