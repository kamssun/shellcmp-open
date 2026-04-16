package com.example.archshowcase.core.trace.restore

import com.arkivanov.mvikotlin.core.utils.JvmSerializable

/**
 * 共享底座的追加列表，替代 `history + record` 的 O(N^2) 拷贝。
 *
 * 所有通过 [append] 产生的实例共享同一个底层 ArrayList，
 * 每个实例的 [size] 在构造时冻结，不受后续 append 影响。
 *
 * 不可变性保证：底层数组只追加不修改 + size 冻结 → 满足 MVI Reducer 要求。
 */
class AppendOnlyHistory<R> private constructor(
    private val backing: ArrayList<R>,
    override val size: Int
) : AbstractList<R>(), JvmSerializable {

    constructor() : this(ArrayList(), 0)

    /** 从已有列表迁移（反序列化后的 State 可能持有普通 List） */
    constructor(source: List<R>) : this(ArrayList(source), source.size)

    override fun get(index: Int): R {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException("Index: $index, Size: $size")
        return backing[index]
    }

    /**
     * check 是断言而非兜底：所有实际链路（Reducer / VF 导出 / VF 验证）
     * 都保证 prevState 是最新快照，size 永远等于 backing.size。
     * IDE 插件回放和 TTE 导入只读不写，不经过 append。
     * 如果触发说明调用方有 bug，不要改成 if/else 降级。
     */
    fun append(record: R): AppendOnlyHistory<R> {
        check(size == backing.size) {
            "Only the latest snapshot can append (snapshot size=$size, backing=${backing.size})"
        }
        backing.add(record)
        return AppendOnlyHistory(backing, size + 1)
    }

    companion object {
        private val EMPTY = AppendOnlyHistory<Nothing>()

        @Suppress("UNCHECKED_CAST")
        fun <R> empty(): AppendOnlyHistory<R> = EMPTY as AppendOnlyHistory<R>
    }
}
