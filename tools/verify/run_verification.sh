#!/usr/bin/env bash

# ─── VF 截图回归脚本 ─────────────────────────────────────
# Usage: ./run_verification.sh <vf_dir> [device_serial]
#        ./run_verification.sh --clean <vf_dir>
#
# 扫描 <vf_dir> 下所有 manifest.json，批量验证并生成汇总报告。
# 传入单个 VF 目录（含 manifest.json）也走统一批量流程。
#
# 前置依赖: adb, jq, java (用于 SSIM 截图对比)
# 退出码: 0=全部PASS  1=有FAIL  2=无用例/参数错误
# ──────────────────────────────────────────────────────────

readonly PACKAGE="com.example.archshowcase"
readonly RECEIVER="${PACKAGE}/.verification.VerificationReceiver"
readonly DEVICE_VF_BASE="/sdcard/Android/data/${PACKAGE}/files/vf"
readonly DEVICE_SCREENSHOT_DIR="/sdcard/archshowcase"
readonly SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
readonly COMPARE_JAR="${SCRIPT_DIR}/screenshot-compare/build/libs/screenshot-compare.jar"
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
# 全局默认 mask：状态栏(top 100px) + 导航条(bottom 80px)，1080x2316 基准
readonly DEFAULT_MASK="0,0,1080,100;0,2236,1080,80"

# ─── 自动构建 screenshot-compare.jar ──────────────────────
if [[ ! -f "$COMPARE_JAR" ]]; then
    echo "screenshot-compare.jar not found, building..."
    if "${PROJECT_ROOT}/gradlew" -p "$PROJECT_ROOT" :tools:verify:screenshot-compare:jar -q; then
        echo "Build successful."
    else
        echo "ERROR: Failed to build screenshot-compare.jar" >&2
        exit 2
    fi
fi

DEVICE_SERIAL=""

# ─── clean 子命令 ─────────────────────────────────────────

do_clean() {
    local dir="$1"
    local count=0
    while IFS= read -r d; do
        rm -rf "$d"
        count=$((count + 1))
    done < <(find "$dir" -type d -name "output" -path "*/test-vfs/*" 2>/dev/null)
    rm -rf "$dir/batch_output" 2>/dev/null && count=$((count + 1))
    local build_dir="${SCRIPT_DIR}/screenshot-compare/build"
    if [[ -d "$build_dir" ]]; then
        rm -rf "$build_dir"
        count=$((count + 1))
    fi
    echo "Cleaned $count items under $dir"
}

# ─── 进程清理 ─────────────────────────────────────────────

_cleanup() {
    local children
    children=$(jobs -p 2>/dev/null)
    if [[ -n "$children" ]]; then
        kill -9 $children 2>/dev/null
        wait $children 2>/dev/null
    fi
    rm -f /tmp/vf_logcat_* 2>/dev/null
}
trap '_cleanup' EXIT INT TERM

# ─── 通用工具 ────────────────────────────────────────────

# manifest name 无意义时 fallback 到目录相对路径
resolve_scenario_name() {
    local vf_dir="$1"
    local name
    name=$(jq -r '.name // "unknown"' "$vf_dir/manifest.json" 2>/dev/null || echo "unknown")
    if [[ "$name" == "vf_export" || "$name" == "unknown" ]]; then
        # 取相对于 test-vfs 的路径，如 "timetravel/checkbox"
        name=$(echo "$vf_dir" | sed 's|.*/test-vfs/||; s|/$||')
        [[ -z "$name" ]] && name=$(basename "$vf_dir")
    fi
    echo "$name"
}

adb_cmd() {
    if [[ -n "$DEVICE_SERIAL" ]]; then
        adb -s "$DEVICE_SERIAL" "$@"
    else
        adb "$@"
    fi
}

## 前置检查：设备连接 + 屏幕亮起且解锁 + App 在前台
preflight() {
    if ! adb_cmd shell echo ok > /dev/null 2>&1; then
        echo "Error: device not connected" >&2
        exit 2
    fi
    local display_state
    display_state=$(adb_cmd shell "dumpsys power | grep 'Display Power'" 2>/dev/null)
    if echo "$display_state" | grep -qi "OFF"; then
        echo "Error: screen is OFF — screencap will capture black images. Please wake the device." >&2
        exit 2
    fi
    local lockscreen
    lockscreen=$(adb_cmd shell "dumpsys window | grep mDreamingLockscreen" 2>/dev/null)
    if echo "$lockscreen" | grep -q "mDreamingLockscreen=true"; then
        echo "Error: device is on lock screen — please unlock first." >&2
        exit 2
    fi
    local focused
    focused=$(adb_cmd shell "dumpsys activity activities 2>/dev/null | grep mResumedActivity" 2>/dev/null)
    if echo "$focused" | grep -q "$PACKAGE"; then
        return
    fi
    echo "  Bringing $PACKAGE to foreground..."
    adb_cmd shell am start -n "$PACKAGE/.MainActivity" > /dev/null 2>&1
    sleep 3
}

## 发送广播并等待 VERIFY_READY
send_and_wait() {
    local timeout_sec="$1"; shift
    local tmpfile="/tmp/vf_logcat_$$_${RANDOM}"

    adb_cmd logcat -c &
    local clear_pid=$!
    sleep 0.5
    { kill -9 "$clear_pid"; wait "$clear_pid"; } 2>/dev/null

    adb_cmd logcat -s "ShellVerify:I" > "$tmpfile" 2>/dev/null &
    local logcat_pid=$!
    sleep 0.3

    adb_cmd shell am broadcast "$@" > /dev/null 2>&1 &
    local bc_pid=$!

    local start=$SECONDS
    while (( SECONDS - start < timeout_sec )); do
        if grep -q "VERIFY_READY" "$tmpfile" 2>/dev/null; then
            local elapsed=$((SECONDS - start))
            echo "  VERIFY_READY (${elapsed}s)"
            { kill -9 "$logcat_pid" "$bc_pid"; wait "$logcat_pid" "$bc_pid"; } 2>/dev/null
            rm -f "$tmpfile"
            return 0
        fi
        sleep 0.3
    done

    echo "  Warning: timeout waiting for VERIFY_READY (${timeout_sec}s)"
    { kill -9 "$logcat_pid" "$bc_pid"; wait "$logcat_pid" "$bc_pid"; } 2>/dev/null
    rm -f "$tmpfile"
    return 0
}

# ─── 验证单个 VF（内部函数）────────────────────────────────

_verify_one() {
    local vf_dir="$1"

    local scenario_name
    scenario_name=$(resolve_scenario_name "$vf_dir")
    local intent_count
    intent_count=$(jq -r '.intents | length' "$vf_dir/manifest.json")
    echo "=== Verification: $scenario_name ==="
    echo "  Intents: $intent_count"

    local output_dir="${vf_dir}/output"
    mkdir -p "$output_dir"

    # 0. 状态隔离：start.tte 为空时 force-stop 回到干净 Home
    local start_tte_size
    start_tte_size=$(wc -c < "$vf_dir/start.tte" 2>/dev/null || echo "0")
    if [[ "$start_tte_size" -eq 0 ]]; then
        echo "[0] Empty start.tte, restarting app for clean Home state..."
        adb_cmd shell am force-stop "$PACKAGE" > /dev/null 2>&1
        sleep 1
        adb_cmd shell am start -n "$PACKAGE/.MainActivity" > /dev/null 2>&1
        sleep 3
    fi

    # 1. Push VF
    echo "[1] Pushing VF to device..."
    adb_cmd shell mkdir -p "$DEVICE_VF_BASE"
    local device_vf_path="${DEVICE_VF_BASE}/$(basename "$vf_dir")"
    adb_cmd push "$vf_dir" "$DEVICE_VF_BASE/"

    # 2. VERIFY_INIT
    echo "[2] Sending VERIFY_INIT..."
    send_and_wait 30 -n "$RECEIVER" -a "com.example.archshowcase.VERIFY_INIT" --es vf_path "$device_vf_path"

    # 2b. 等待异步加载（图片/网络等）
    local wait_init
    wait_init=$(jq -r '.wait_after_init_ms // 0' "$vf_dir/manifest.json")
    if [[ "$wait_init" -gt 0 ]]; then
        local wait_sec
        wait_sec=$(echo "scale=3; $wait_init / 1000" | bc)
        echo "  Waiting ${wait_sec}s for async loading..."
        sleep "$wait_sec"
    fi

    # 3. 截图 A'
    echo "[3] Taking screenshot A'..."
    adb_cmd shell mkdir -p "$DEVICE_SCREENSHOT_DIR"
    adb_cmd shell screencap -p "${DEVICE_SCREENSHOT_DIR}/verify_a.png"
    adb_cmd pull "${DEVICE_SCREENSHOT_DIR}/verify_a.png" "$output_dir/verify_a.png"

    # 4. Dispatch intents
    echo "[4] Dispatching $intent_count intents..."
    for ((i = 0; i < intent_count; i++)); do
        local delay note
        delay=$(jq -r ".intents[$i].delay_after_ms // 0" "$vf_dir/manifest.json")
        note=$(jq -r ".intents[$i].note // \"\"" "$vf_dir/manifest.json")
        echo "  [$((i+1))/$intent_count] Dispatching intent $i ${note:+(${note})}"

        send_and_wait 15 -n "$RECEIVER" -a "com.example.archshowcase.VERIFY_DISPATCH" --ei index "$i"

        if [[ "$delay" -gt 0 ]]; then
            local delay_sec
            delay_sec=$(echo "scale=3; $delay / 1000" | bc)
            echo "  Waiting ${delay_sec}s after intent..."
            sleep "$delay_sec"
        fi
    done

    # 5. 截图 B'
    echo "[5] Taking screenshot B'..."
    adb_cmd shell screencap -p "${DEVICE_SCREENSHOT_DIR}/verify_b.png"
    adb_cmd pull "${DEVICE_SCREENSHOT_DIR}/verify_b.png" "$output_dir/verify_b.png"

    # 6. SSIM 截图对比（start + end 两组）
    local custom_mask
    custom_mask=$(jq -r '.screenshot_compare.mask_regions // [] | join(";")' "$vf_dir/manifest.json")
    local mask_regions="$DEFAULT_MASK"
    [[ -n "$custom_mask" ]] && mask_regions="${mask_regions};${custom_mask}"

    if [[ -f "$COMPARE_JAR" ]]; then
        if [[ -f "${vf_dir}/start_baseline.png" ]]; then
            echo "[6a] SSIM: start_baseline vs verify_a..."
            local start_args=(--actual "$output_dir/verify_a.png" --baseline "${vf_dir}/start_baseline.png" --output "$output_dir" --threshold 0.95)
            [[ -n "$mask_regions" ]] && start_args+=(--mask "$mask_regions")
            if java -jar "$COMPARE_JAR" "${start_args[@]}"; then
                mv "$output_dir/result_ssim.json" "$output_dir/result_ssim_start.json"
                mv "$output_dir/diff_heatmap.png" "$output_dir/diff_heatmap_start.png" 2>/dev/null
            else
                echo '{"passed":false,"score":0,"threshold":0.95,"error":"SSIM compare crashed"}' > "$output_dir/result_ssim_start.json"
            fi
        fi

        if [[ -f "${vf_dir}/end_baseline.png" ]]; then
            echo "[6b] SSIM: end_baseline vs verify_b..."
            local end_args=(--actual "$output_dir/verify_b.png" --baseline "${vf_dir}/end_baseline.png" --output "$output_dir" --threshold 0.95)
            [[ -n "$mask_regions" ]] && end_args+=(--mask "$mask_regions")
            if java -jar "$COMPARE_JAR" "${end_args[@]}"; then
                mv "$output_dir/result_ssim.json" "$output_dir/result_ssim_end.json"
                mv "$output_dir/diff_heatmap.png" "$output_dir/diff_heatmap_end.png" 2>/dev/null
            else
                echo '{"passed":false,"score":0,"threshold":0.95,"error":"SSIM compare crashed"}' > "$output_dir/result_ssim_end.json"
            fi
        elif [[ -f "${vf_dir}/baseline.png" ]]; then
            echo "[6b] SSIM: baseline vs verify_b..."
            local end_args=(--actual "$output_dir/verify_b.png" --baseline "${vf_dir}/baseline.png" --output "$output_dir" --threshold 0.95)
            [[ -n "$mask_regions" ]] && end_args+=(--mask "$mask_regions")
            if java -jar "$COMPARE_JAR" "${end_args[@]}"; then
                mv "$output_dir/result_ssim.json" "$output_dir/result_ssim_end.json"
                mv "$output_dir/diff_heatmap.png" "$output_dir/diff_heatmap_end.png" 2>/dev/null
            else
                echo '{"passed":false,"score":0,"threshold":0.95,"error":"SSIM compare crashed"}' > "$output_dir/result_ssim_end.json"
            fi
        else
            echo "  Warning: no end baseline found, skipping end SSIM"
        fi
    else
        echo "  Warning: screenshot-compare.jar not found, skipping SSIM"
    fi

    # 7. 统一报告
    if [[ -f "$COMPARE_JAR" ]]; then
        local report_args=(--report --scenario "$scenario_name" --manifest "$vf_dir/manifest.json" --output "$output_dir")
        [[ -f "$output_dir/result_ssim_start.json" ]] && report_args+=(--ssim-start "$output_dir/result_ssim_start.json")
        [[ -f "$output_dir/result_ssim_end.json" ]] && report_args+=(--ssim-end "$output_dir/result_ssim_end.json")
        java -jar "$COMPARE_JAR" "${report_args[@]}"
    fi

    echo ""
    echo "=== Verification Complete: $scenario_name ==="
    echo "  Output: $output_dir"

    if [[ -f "$output_dir/report.json" ]]; then
        echo "  Report: $(jq -c . "$output_dir/report.json")"
        local passed
        passed=$(jq -r '.overall_passed' "$output_dir/report.json")
        [[ "$passed" != "true" ]] && return 1
    else
        echo "  Warning: no report.json generated"
        return 2
    fi
    return 0
}

# ─── 批量验证 + 汇总 ────────────────────────────────────────

verify() {
    local vf_dirs=("$@")
    local total=${#vf_dirs[@]}
    local passed_count=0 failed_count=0 error_count=0
    local results=()
    local failed_vf_dirs=()
    local start_time
    start_time=$(date +%s)

    echo "╔══════════════════════════════════════════════════╗"
    echo "║          VF Verification                          ║"
    echo "╠══════════════════════════════════════════════════╣"
    echo "║  Cases: $total"
    [[ -n "$DEVICE_SERIAL" ]] && echo "║  Device: $DEVICE_SERIAL"
    echo "╚══════════════════════════════════════════════════╝"
    echo ""

    for vf_dir in "${vf_dirs[@]}"; do
        local scenario
        scenario=$(resolve_scenario_name "$vf_dir")
        local idx=$((passed_count + failed_count + error_count + 1))
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "  [$idx/$total] $scenario"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

        if (_verify_one "$vf_dir"); then
            passed_count=$((passed_count + 1))
            results+=("PASS   $scenario")
        else
            local rc=$?
            failed_vf_dirs+=("$vf_dir")
            if [[ $rc -eq 1 ]]; then
                failed_count=$((failed_count + 1))
                results+=("FAIL   $scenario")
            else
                error_count=$((error_count + 1))
                results+=("ERROR  $scenario")
            fi
        fi
        echo ""
    done

    local end_time duration
    end_time=$(date +%s)
    duration=$((end_time - start_time))

    # ─── 汇总 ────────────────────────────────────────────

    echo "╔══════════════════════════════════════════════════╗"
    echo "║              Summary                              ║"
    echo "╠══════════════════════════════════════════════════╣"
    printf "║  Total: %-4s  Duration: %ds\n" "$total" "$duration"
    printf "║  PASS:  %-4s  FAIL: %-4s  ERROR: %-4s\n" "$passed_count" "$failed_count" "$error_count"
    echo "╠══════════════════════════════════════════════════╣"
    for r in "${results[@]}"; do
        echo "║  $r"
    done
    echo "╚══════════════════════════════════════════════════╝"

    # ─── batch_report.json ────────────────────────────────

    # 报告始终写到最外层 test-vfs/batch_output（不跟随单 VF 目录）
    local batch_dir="${SCRIPT_DIR}/test-vfs/batch_output"
    mkdir -p "$batch_dir"

    local case_reports="[" first=true
    for vf_dir in "${vf_dirs[@]}"; do
        local rf="$vf_dir/output/report.json"
        if [[ -f "$rf" ]]; then
            [[ "$first" == true ]] && first=false || case_reports+=","
            case_reports+=$(cat "$rf")
        fi
    done
    case_reports+="]"

    local all_passed="false"
    [[ $failed_count -eq 0 && $error_count -eq 0 ]] && all_passed="true"

    cat > "$batch_dir/batch_report.json" <<-EOF
	{
	  "total": $total,
	  "passed": $passed_count,
	  "failed": $failed_count,
	  "errors": $error_count,
	  "duration_seconds": $duration,
	  "all_passed": $all_passed,
	  "cases": $case_reports
	}
	EOF

    # ─── batch_report.md ─────────────────────────────────

    {
        echo "# Verification Report"
        echo ""
        echo "| # | Scenario | Result |"
        echo "|---|----------|--------|"
        local idx=1
        for r in "${results[@]}"; do
            local status scenario
            status=$(echo "$r" | awk '{print $1}')
            scenario=$(echo "$r" | awk '{$1=""; print}' | xargs)
            echo "| $idx | $scenario | $status |"
            idx=$((idx + 1))
        done
        echo ""
        echo "**Total: $total | PASS: $passed_count | FAIL: $failed_count | ERROR: $error_count | ${duration}s**"
    } > "$batch_dir/batch_report.md"

    echo ""
    echo "  Report: $batch_dir/batch_report.json"
    echo "  Markdown: $batch_dir/batch_report.md"

    # ─── error_out: 失败用例截图对比目录 ──────────────────

    local err_dir="${batch_dir}/error_out"
    rm -rf "$err_dir"

    if [[ ${#failed_vf_dirs[@]} -gt 0 ]]; then
        mkdir -p "$err_dir"
        for vf_dir in "${failed_vf_dirs[@]}"; do
            local dir_name
            dir_name=$(basename "$vf_dir")
            local case_dir="${err_dir}/${dir_name}"
            mkdir -p "$case_dir"
            # baselines
            cp "$vf_dir/start_baseline.png" "$case_dir/" 2>/dev/null
            cp "$vf_dir/end_baseline.png" "$case_dir/" 2>/dev/null
            cp "$vf_dir/baseline.png" "$case_dir/" 2>/dev/null
            # actual screenshots
            cp "$vf_dir/output/verify_a.png" "$case_dir/" 2>/dev/null
            cp "$vf_dir/output/verify_b.png" "$case_dir/" 2>/dev/null
            # diff heatmaps
            cp "$vf_dir/output/diff_heatmap_start.png" "$case_dir/" 2>/dev/null
            cp "$vf_dir/output/diff_heatmap_end.png" "$case_dir/" 2>/dev/null
            # report
            cp "$vf_dir/output/report.json" "$case_dir/" 2>/dev/null
        done
        echo "  Error details: $err_dir"
    fi

    [[ $failed_count -gt 0 || $error_count -gt 0 ]] && return 1
    return 0
}

# ─── main ────────────────────────────────────────────────────

# --clean 子命令
if [[ "$1" == "--clean" ]]; then
    dir="${2:-.}"
    do_clean "$dir"
    exit 0
fi

VF_DIR="${1:?Usage: $0 <vf_dir> [device_serial]  |  $0 --clean [dir]}"
DEVICE_SERIAL="${2:-}"

if [[ ! -d "$VF_DIR" ]]; then
    echo "Error: directory not found: $VF_DIR" >&2
    exit 2
fi

preflight

# 收集所有 VF 目录（无论传入单个还是父目录，统一扫描）
VF_DIRS=()
if [[ -f "$VF_DIR/manifest.json" ]]; then
    VF_DIRS+=("$VF_DIR")
else
    while IFS= read -r mf; do
        VF_DIRS+=("$(dirname "$mf")")
    done < <(find "$VF_DIR" -name "manifest.json" -not -path "*/output/*" -type f | sort)
fi

if [[ ${#VF_DIRS[@]} -eq 0 ]]; then
    echo "Error: no manifest.json found in $VF_DIR or its subdirectories" >&2
    exit 2
fi

verify "${VF_DIRS[@]}"
exit $?
