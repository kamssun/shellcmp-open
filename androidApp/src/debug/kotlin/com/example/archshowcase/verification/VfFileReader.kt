package com.example.archshowcase.verification

import com.example.archshowcase.core.trace.verification.VfPackage
import com.example.archshowcase.core.trace.verification.VfPackager
import java.io.File

/**
 * 从设备文件系统读取 VF 目录
 */
object VfFileReader {

    /**
     * 读取 VF 目录并解析为 VfPackage
     *
     * @param vfDir VF 目录路径（包含 manifest.json, start.tte, end.tte）
     */
    fun read(vfDir: File): Result<VfPackage> = runCatching {
        require(vfDir.isDirectory) { "VF path is not a directory: ${vfDir.absolutePath}" }

        val files = vfDir.listFiles()
            ?.filter { it.isFile }
            ?.associate { it.name to it.readBytes() }
            ?: error("Cannot list files in: ${vfDir.absolutePath}")

        VfPackager.parse(files).getOrThrow()
    }
}
