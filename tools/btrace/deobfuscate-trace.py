#!/usr/bin/env python3
"""
反混淆 btrace/Perfetto trace 文件。

将 R8 混淆的类名（如 hj.h、qc.n）替换为原始类名（如 kotlin.time.Instant），
并将合成类/内联方法映射回源码位置（如 StressItem > blockingWork [OBODemoContent.kt:255]）。
输出的 .pb 可直接拖入 ui.perfetto.dev 查看。

用法:
    python3 deobfuscate-trace.py <trace.pb> <mapping.txt>

输出:
    <trace>_deobf.pb（与原文件同目录）
"""

import json
import os
import re
import subprocess
import sys
import time

VENV_DIR = "/tmp/perfetto-env"

# 内联链中需要跳过的框架方法（无分析价值）
_SKIP_INLINE_PATTERNS = {
    "access$",
    "kotlin.coroutines.intrinsics",
    "kotlin.jvm.internal",
    "kotlinx.coroutines.internal",
    "getCOROUTINE_SUSPENDED",
}


def ensure_perfetto():
    """确保 perfetto 可用：系统已有则直接用，否则自动创建 venv 并安装。"""
    try:
        from perfetto.protos.perfetto.trace import perfetto_trace_pb2  # noqa: F401
        return
    except ImportError:
        pass

    venv_python = os.path.join(VENV_DIR, "bin", "python3")
    if not os.path.exists(venv_python):
        print(f"创建 venv: {VENV_DIR}")
        subprocess.check_call([sys.executable, "-m", "venv", VENV_DIR])
        subprocess.check_call([venv_python, "-m", "pip", "install", "-q", "perfetto"])

    os.execv(venv_python, [venv_python] + sys.argv)


def parse_mapping(mapping_path: str):
    """解析 R8 mapping.txt，返回 (class_map, method_map, source_map)。

    class_map:  {混淆类名: 原始类名}
    method_map: {原始类名: {混淆方法名: 原始方法名}}
    source_map: {原始类名: {混淆方法名: (file, orig_method, line, orig_class)}}
    """
    class_map = {}      # obfuscated -> original
    method_map = {}     # original_class -> {obf_method -> orig_method}
    source_files = {}   # original_class -> source_file
    source_map = {}     # original_class -> {method -> (file, orig_method, line, orig_class)}

    current_original = None
    current_source_file = None

    # 带行号的内联方法行
    # 格式: "    17:22:void pkg.Class.method(args):255 -> obfuscatedMethod"
    inline_re = re.compile(
        r"^\s+\d+:\d+:"           # obfuscated line range
        r"(?:\S+\s+)?"            # return type (optional)
        r"(\S+?)\."              # original class
        r"(\w+)"                  # original method name
        r"\([^)]*\)"              # params
        r":(\d+)"                 # original source line
        r"(?::\d+)?"             # optional end line
        r"\s+->\s+"
        r"(\w+)"                  # obfuscated method name
    )

    # 同类方法映射（带行号但无类前缀）
    # 格式: "    5:40:kotlin.time.Instant fromEpochSeconds(long,long):320 -> b"
    same_class_re = re.compile(
        r"^\s+\d+:\d+:"           # obfuscated line range
        r"(?:\S+\s+)?"            # return type (optional)
        r"(\w+)"                  # original method name (no class prefix)
        r"\([^)]*\)"              # params
        r":(\d+)"                 # original source line
        r"(?::\d+)?"             # optional end line
        r"\s+->\s+"
        r"(\w+)"                  # obfuscated method name
    )

    # 简单方法映射（不带行号）
    # 格式: "    returnType originalMethod(params) -> obfuscatedMethod"
    simple_re = re.compile(
        r"^\s+"
        r"(?:\S+\s+)?"            # return type (optional)
        r"(\w+)"                  # original method name
        r"\([^)]*\)"              # params
        r"\s+->\s+"
        r"(\w+)"                  # obfuscated method name
    )

    with open(mapping_path, "r") as f:
        for line in f:
            if line.startswith("#"):
                if current_original and '"sourceFile"' in line:
                    try:
                        meta = json.loads(line.lstrip("# ").strip())
                        if meta.get("id") == "sourceFile":
                            current_source_file = meta["fileName"]
                            source_files[current_original] = current_source_file
                    except (json.JSONDecodeError, KeyError):
                        pass
                continue

            if not line.startswith(" ") and "->" in line:
                parts = line.strip().rstrip(":").split(" -> ")
                if len(parts) == 2:
                    current_original = parts[0]
                    class_map[parts[1]] = current_original
                    current_source_file = None
                continue

            if current_original and line.startswith("    "):
                # 先尝试带行号的跨类内联格式
                m = inline_re.match(line)
                if m:
                    orig_class, orig_method, orig_line, obf_method = m.groups()

                    full_name = f"{orig_class}.{orig_method}"
                    if any(p in full_name for p in _SKIP_INLINE_PATTERNS):
                        continue

                    key = current_original
                    if key not in source_map:
                        source_map[key] = {}

                    orig_source = source_files.get(orig_class)
                    if not orig_source:
                        orig_pkg = orig_class.rsplit(".", 1)[0] if "." in orig_class else ""
                        host_pkg = current_original.rsplit(".", 1)[0] if "." in current_original else ""
                        if orig_pkg != host_pkg:
                            outer = orig_class.rsplit(".", 1)[-1].split("$")[0]
                            orig_source = f"{outer}.kt"
                        else:
                            orig_source = current_source_file

                    existing = source_map[key].get(obf_method)
                    is_app_code = "com.example.archshowcase." in orig_class
                    if existing is None or is_app_code:
                        source_map[key][obf_method] = (
                            orig_source,
                            orig_method,
                            int(orig_line),
                            orig_class,
                        )
                    continue

                # 同类方法映射（带行号但无类前缀）
                m_sc = same_class_re.match(line)
                if m_sc:
                    orig_method, orig_line, obf_method = m_sc.groups()
                    if orig_method != obf_method and not orig_method.startswith("<"):
                        if current_original not in method_map:
                            method_map[current_original] = {}
                        if obf_method not in method_map[current_original]:
                            method_map[current_original][obf_method] = orig_method
                    continue

                # 简单方法映射（方法名重命名，无内联）
                m2 = simple_re.match(line)
                if m2:
                    orig_method, obf_method = m2.groups()
                    if orig_method != obf_method and not orig_method.startswith("<"):
                        if current_original not in method_map:
                            method_map[current_original] = {}
                        # 不覆盖已有的（可能有多个重载映射到同一个名字）
                        if obf_method not in method_map[current_original]:
                            method_map[current_original][obf_method] = orig_method

    return class_map, method_map, source_map


def _short_class(fqcn: str) -> str:
    """提取简短类名：去掉包名，保留外层类$内层类。"""
    name = fqcn.rsplit(".", 1)[-1] if "." in fqcn else fqcn
    # 去掉 Kt 后缀（Kotlin 文件类）
    if name.endswith("Kt"):
        name = name[:-2]
    return name


def _enclosing_function(class_name: str) -> str | None:
    """从合成类名提取外层函数名。

    OBODemoContentKt$StressItem$1$1$1 → StressItem
    NavigationStackManager$push$lambda$0 → push
    """
    # 按 $ 分割，找第一个非数字、非 lambda/special 的 segment
    parts = class_name.rsplit(".", 1)[-1].split("$")
    for p in parts[1:]:  # 跳过外层类名
        if p and not p.isdigit() and p not in ("Companion", "Kt"):
            if p.startswith("lambda"):
                continue
            return p
    return None


def make_readable_name(
    host_class: str,
    method: str,
    source_map: dict,
) -> str | None:
    """生成可读的源码标注，如 "StressItem > blockingWork [OBODemoContent.kt:255]"。"""
    info = source_map.get(host_class, {}).get(method)
    if not info:
        return None

    source_file, orig_method, orig_line, orig_class = info

    # 判断是否 R8 class merging（原始方法的类和宿主类不同包）
    # 如: Clock.now() 被合并到 ByteString$ArraysByteArrayCopier
    orig_pkg = orig_class.rsplit(".", 1)[0] if "." in orig_class else ""
    host_pkg = host_class.rsplit(".", 1)[0] if "." in host_class else ""
    is_merged = orig_pkg != host_pkg

    if is_merged:
        # 跨包合并：用原始方法的类名做上下文
        context = _short_class(orig_class)
    else:
        # 同包：从宿主合成类名提取外层函数
        context = _enclosing_function(host_class)

    parts = []
    if context and context != orig_method:
        parts.append(context)
    parts.append(f"{orig_method}()")

    label = " > ".join(parts)
    if source_file:
        label += f" [{source_file}:{orig_line}]"
    return label


def deobfuscate(
    name: str,
    class_map: dict[str, str],
    method_map: dict,
    source_map: dict,
) -> str:
    tokens = set(re.findall(r"[a-zA-Z_$][\w$]*(?:\.[a-zA-Z_$][\w$]*)+", name))
    result = name
    source_annotation = None

    for token in sorted(tokens, key=len, reverse=True):
        # 尝试: 整个 token 是类名
        if token in class_map:
            original = class_map[token]
            result = result.replace(token, original)
            continue

        # 尝试: token = 类名.方法名
        dot = token.rfind(".")
        if dot > 0:
            cls = token[:dot]
            method = token[dot + 1:]
            if cls in class_map:
                original_cls = class_map[cls]
                # 反混淆方法名：先查简单映射，再查源码标注中的内联映射
                orig_method = method_map.get(original_cls, {}).get(method)
                if not orig_method:
                    src_info = source_map.get(original_cls, {}).get(method)
                    if src_info:
                        orig_method = src_info[1]  # (file, orig_method, line, orig_class)
                if not orig_method:
                    orig_method = method
                result = result.replace(token, original_cls + "." + orig_method)
                # 尝试生成源码标注
                readable = make_readable_name(original_cls, method, source_map)
                if readable:
                    source_annotation = readable
                continue

        # 尝试: 带 $ 的内部类
        if "$" in token:
            base = token.split("$")[0]
            if base in class_map:
                result = result.replace(base, class_map[base])

    # 附加源码标注
    if source_annotation:
        result = f"{source_annotation} | {result}"

    return result


def main():
    if len(sys.argv) != 3:
        print(f"用法: {sys.argv[0]} <trace.pb> <mapping.txt>")
        sys.exit(1)

    # 转绝对路径，确保 os.execv 重启后路径仍有效
    trace_path = os.path.abspath(sys.argv[1])
    mapping_path = os.path.abspath(sys.argv[2])
    sys.argv[1] = trace_path
    sys.argv[2] = mapping_path

    ensure_perfetto()
    from perfetto.protos.perfetto.trace import perfetto_trace_pb2 as trace_pb2

    # 1. 加载 mapping
    t0 = time.time()
    class_map, method_map, source_map = parse_mapping(mapping_path)
    method_renames = sum(len(v) for v in method_map.values())
    source_methods = sum(len(v) for v in source_map.values())
    print(f"加载 {len(class_map)} 个类映射, {method_renames} 个方法名映射, {source_methods} 个源码标注 ({time.time() - t0:.1f}s)")

    # 2. 解析 trace
    t1 = time.time()
    with open(trace_path, "rb") as f:
        data = f.read()
    trace = trace_pb2.Trace()
    trace.ParseFromString(data)
    print(f"解析 trace: {len(trace.packet)} packets ({time.time() - t1:.1f}s)")

    # 3. 替换 track_event.name
    t2 = time.time()
    replaced = 0
    annotated = 0
    total_te = 0
    for pkt in trace.packet:
        if pkt.HasField("track_event") and pkt.track_event.name:
            total_te += 1
            original = pkt.track_event.name
            deob = deobfuscate(original, class_map, method_map, source_map)
            if deob != original:
                pkt.track_event.name = deob
                replaced += 1
                if "|" in deob:
                    annotated += 1
    print(f"track_events: {total_te}, 反混淆: {replaced}, 源码标注: {annotated} ({time.time() - t2:.1f}s)")

    # 4. 写出
    out_path = trace_path.replace(".pb", "_deobf.pb")
    t3 = time.time()
    with open(out_path, "wb") as f:
        f.write(trace.SerializeToString())
    print(f"输出: {out_path} ({time.time() - t3:.1f}s)")
    print(f"总耗时: {time.time() - t0:.1f}s")


if __name__ == "__main__":
    main()
