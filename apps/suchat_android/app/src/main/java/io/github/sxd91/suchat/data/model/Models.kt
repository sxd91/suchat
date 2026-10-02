package io.github.sxd91.suchat.data.model

import androidx.compose.ui.graphics.Color

/**
 * 数据模型层 —— 前端先行阶段使用**占位示例数据**（甲方要求：没有的数据用占位示例代替）。
 *
 * 所有模型都保持「后端可替换」的形态：字段与微信实际协议对齐
 * （如 [Message] 的 发送方/时间/类型），后续接真后端时只需换数据源，
 * UI 层不用动。
 */

/** 会话列表项（微信「微信」tab 的一行）。 */
data class Chat(
    val id: String,
    val name: String,
    /** 头像占位色（无图时用色块 + 首字，见 `Avatar` 组件）。 */
    val avatarColor: Color,
    /** 最后一条消息摘要（含「[图片]」「[语音]」等前缀，微信风格）。 */
    val lastMessage: String,
    /** 显示在列表右侧的时间（已格式化的短文本，如 "刚刚" / "14:32" / "昨天"）。 */
    val time: String,
    /** 未读数（0 表示不显示红点）。 */
    val unreadCount: Int = 0,
    /** 是否免打扰（静音）—— 免打扰时红点变灰点。 */
    val muted: Boolean = false,
    /** 是否是群聊（群聊头像为九宫格，此处用颜色区分）。 */
    val isGroup: Boolean = false,
)

/** 单条消息。 */
data class Message(
    val id: String,
    /** 发送方 id；"me" 表示自己（气泡在右侧）。 */
    val senderId: String,
    /** 发送方显示名（群聊时显示，单聊留空）。 */
    val senderName: String? = null,
    /** 发送方头像占位色。 */
    val senderColor: Color,
    val content: String,
    /** 是否为自己发出（决定气泡方向与颜色）。 */
    val isMine: Boolean,
    /** 消息类型。 */
    val type: MessageType = MessageType.Text,
    /** 时间戳（毫秒）。 */
    val timestamp: Long,
    /**
     * 已读状态（用户新需求 3）。
     *
     * 语义（对齐微信）：
     *  - 自己发出的消息：`true` = 对方已读（单聊显示「已读」小字）；
     *  - 收到的消息：`true` = 我已读。
     *
     * 默认 true（占位数据里历史消息都当已读）；新发出的消息初始 false，
     * 由 [markRead] 在一段时间后置真，用来演示「已读回执」。
     */
    val isRead: Boolean = true,
    /** 语音时长（秒）；仅 [MessageType.Voice] 有效。 */
    val voiceSeconds: Int = 0,
    /**
     * 是否被撤回（用户新需求 2）。
     *
     * 撤回后消息**不删除**，而是变成一条居中的系统提示
     * （「你撤回了一条消息」/「对方撤回了一条消息」），与微信一致。
     */
    val recalled: Boolean = false,
)

/** 消息类型（决定气泡内的渲染方式）。 */
enum class MessageType {
    /** 纯文本。 */
    Text,

    /** 语音（显示时长 + 波纹）。 */
    Voice,

    /** 图片（占位色块）。 */
    Image,

    /** 红包 / 转账等系统卡片。 */
    Card,
}

/** 联系人。 */
data class Contact(
    val id: String,
    val name: String,
    val avatarColor: Color,
    /** 拼音首字母（索引列表用，A-Z）。非字母开头归入 "#"。 */
    val initial: String,
    /** 备注名（有备注时列表显示备注）。 */
    val remark: String? = null,
)

/** 通讯录「新的朋友」项。 */
data class FriendRequest(
    val id: String,
    val name: String,
    val avatarColor: Color,
    /** 验证消息，如「我是张三」。 */
    val message: String,
    /** 状态：待处理 / 已添加。 */
    val accepted: Boolean = false,
)

/** 朋友圈动态。 */
data class Moment(
    val id: String,
    val authorName: String,
    val authorColor: Color,
    val content: String,
    /** 图片占位色列表（朋友圈九宫格）。 */
    val imageColors: List<Color> = emptyList(),
    /** 点赞者名字（已格式化的「张三，李四」串）。 */
    val likes: String = "",
    /** 评论（"A：内容" 形式）。 */
    val comments: List<Pair<String, String>> = emptyList(),
    /** 相对时间（如 "5分钟前"）。 */
    val timeAgo: String,
)

/** 「我」页的个人资料。 */
data class UserProfile(
    val name: String,
    val avatarColor: Color,
    /** suchat 号（微信 id）。 */
    val suchatId: String,
    /** 我的二维码占位（用 id 派生即可）。 */
    val qrPayload: String,
)

/** 发现页 / 我页的列表项（图标 + 标题 + 可选右侧文字 + 红点）。 */
data class EntryItem(
    val title: String,
    /** 图标占位色（微信的风格是彩色图标块）。 */
    val iconColor: Color,
    /** 图标内的字符（emoji 或单字），前端占位阶段用来代替真图标素材。 */
    val iconGlyph: String,
    /** 右侧附加文字（如发现页「视频号」右边的小字）。 */
    val trailing: String? = null,
    /** 是否有红点。 */
    val hasDot: Boolean = false,
    /** 按压时高亮的类型，用于导航。 */
    val route: Any? = null,
)