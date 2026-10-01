package io.github.sxd91.suchat.data

import androidx.compose.ui.graphics.Color
import io.github.sxd91.suchat.data.model.Chat
import io.github.sxd91.suchat.data.model.Contact
import io.github.sxd91.suchat.data.model.EntryItem
import io.github.sxd91.suchat.data.model.FriendRequest
import io.github.sxd91.suchat.data.model.Message
import io.github.sxd91.suchat.data.model.MessageType
import io.github.sxd91.suchat.data.model.Moment
import io.github.sxd91.suchat.data.model.UserProfile

/**
 * 占位示例数据。
 *
 * 甲方已明确：**没有的数据用占位示例代替**。本文件是前端阶段的唯一数据源，
 * 后续接后端时替换本文件（或改为 Repository 实现），UI 层无需改动。
 *
 * 数据尽量贴近微信真实场景（会话名、消息内容、朋友圈文案），
 * 这样 UI 评审时能直接判断「像不像微信」。
 */
object SampleData {

    // --- 头像占位色（微信风格的低饱和色板） ---

    private val avatarPalette = listOf(
        Color(0xFF5B8DEF), // 蓝
        Color(0xFF7BC96F), // 绿
        Color(0xFFFF9F43), // 橙
        Color(0xFFEE5A6F), // 红
        Color(0xFF9B59B6), // 紫
        Color(0xFF1ABC9C), // 青
        Color(0xFFF39C12), // 金
        Color(0xFF3498DB), // 天蓝
        Color(0xFFE74C3C), // 朱红
        Color(0xFF16A085), // 深青
    )

    /** 按 id 稳定取色（同一个人每次启动颜色一致）。 */
    fun colorFor(seed: String): Color =
        avatarPalette[(seed.hashCode().let { if (it < 0) -it else it }) % avatarPalette.size]

    // --- 我 ---

    val me = UserProfile(
        name = "Suchat 用户",
        avatarColor = avatarPalette[0],
        suchatId = "suchat_8823",
        qrPayload = "https://suchat.app/u/suchat_8823",
    )

    // --- 会话列表（微信 tab） ---

    val chats: List<Chat> = listOf(
        Chat(
            id = "chat_001",
            name = "文件传输助手",
            avatarColor = Color(0xFF4A90D9),
            lastMessage = "[文件] 项目需求文档v2.pdf",
            time = "14:32",
            unreadCount = 0,
        ),
        Chat(
            id = "chat_002",
            name = "产品交流群",
            avatarColor = Color(0xFF07C160),
            lastMessage = "李工：这个交互稿我改好了，大家看下",
            time = "14:28",
            unreadCount = 12,
            isGroup = true,
        ),
        Chat(
            id = "chat_003",
            name = "张明远",
            avatarColor = Color(0xFFEE5A6F),
            lastMessage = "好的，那就明天下午三点见",
            time = "13:55",
            unreadCount = 2,
        ),
        Chat(
            id = "chat_004",
            name = "周末爬山小队",
            avatarColor = Color(0xFF7BC96F),
            lastMessage = "王芳：周六早上七点山脚下集合⛰️",
            time = "12:40",
            unreadCount = 5,
            isGroup = true,
        ),
        Chat(
            id = "chat_005",
            name = "订阅号消息",
            avatarColor = Color(0xFFF39C12),
            lastMessage = "人民日报：今日要闻速览",
            time = "12:00",
            unreadCount = 3,
            muted = true,
        ),
        Chat(
            id = "chat_006",
            name = "陈思雨",
            avatarColor = Color(0xFF9B59B6),
            lastMessage = "[语音] 8″",
            time = "11:23",
        ),
        Chat(
            id = "chat_007",
            name = "家庭群",
            avatarColor = Color(0xFFFF9F43),
            lastMessage = "妈妈：晚上回来吃饭吗？",
            time = "10:15",
            unreadCount = 1,
            isGroup = true,
        ),
        Chat(
            id = "chat_008",
            name = "刘建国",
            avatarColor = Color(0xFF1ABC9C),
            lastMessage = "收到，谢谢！",
            time = "昨天",
        ),
        Chat(
            id = "chat_009",
            name = "设计部通知",
            avatarColor = Color(0xFF3498DB),
            lastMessage = "本周五下午全员大会，请准时参加",
            time = "昨天",
            muted = true,
        ),
        Chat(
            id = "chat_010",
            name = "赵小雅",
            avatarColor = Color(0xFFE74C3C),
            lastMessage = "[图片]",
            time = "昨天",
            unreadCount = 1,
        ),
        Chat(
            id = "chat_011",
            name = "读书会",
            avatarColor = Color(0xFF16A085),
            lastMessage = "孙悦：《置身事内》第三章讲得真好",
            time = "星期二",
            isGroup = true,
        ),
        Chat(
            id = "chat_012",
            name = "孙浩",
            avatarColor = Color(0xFF5B8DEF),
            lastMessage = "资料发你邮箱了，查收一下",
            time = "星期一",
        ),
    )

    // --- 聊天消息（按会话 id 索引） ---

    /** 默认单聊消息（用于没有专属消息的会话）。 */
    private val defaultMessages: List<Message> = listOf(
        Message(
            id = "m1",
            senderId = "other",
            senderColor = Color(0xFFEE5A6F),
            content = "在吗？",
            isMine = false,
            timestamp = System.currentTimeMillis() - 3600_000,
        ),
        Message(
            id = "m2",
            senderId = "me",
            senderColor = avatarPalette[0],
            content = "在的，怎么了？",
            isMine = true,
            timestamp = System.currentTimeMillis() - 3500_000,
        ),
        Message(
            id = "m3",
            senderId = "other",
            senderColor = Color(0xFFEE5A6F),
            content = "想跟你确认一下明天的安排",
            isMine = false,
            timestamp = System.currentTimeMillis() - 3400_000,
        ),
        Message(
            id = "m4",
            senderId = "me",
            senderColor = avatarPalette[0],
            content = "好的，那就明天下午三点见",
            isMine = true,
            timestamp = System.currentTimeMillis() - 3300_000,
        ),
    )

    /** 群聊消息（产品交流群）。 */
    private val groupMessages: List<Message> = listOf(
        Message(
            id = "g1",
            senderId = "u_li",
            senderName = "李工",
            senderColor = Color(0xFF5B8DEF),
            content = "刚看到新版原型，整体感觉不错",
            isMine = false,
            timestamp = System.currentTimeMillis() - 7200_000,
        ),
        Message(
            id = "g2",
            senderId = "u_wang",
            senderName = "王芳",
            senderColor = Color(0xFF7BC96F),
            content = "主要是首页的信息层级比以前清楚了",
            isMine = false,
            timestamp = System.currentTimeMillis() - 7100_000,
        ),
        Message(
            id = "g3",
            senderId = "me",
            senderColor = avatarPalette[0],
            content = "谢谢！这次重点调了留白和字号",
            isMine = true,
            timestamp = System.currentTimeMillis() - 7000_000,
        ),
        Message(
            id = "g4",
            senderId = "u_li",
            senderName = "李工",
            senderColor = Color(0xFF5B8DEF),
            content = "这个交互稿我改好了，大家看下",
            isMine = false,
            type = MessageType.Image,
            timestamp = System.currentTimeMillis() - 600_000,
        ),
    )

    /**
     * 取某会话的消息列表。
     *
     * 群聊会话返回 [groupMessages]（并注入当前会话名作为上下文），
     * 其余返回 [defaultMessages]；[chat_006] 有一条语音消息作类型演示。
     */
    fun messagesFor(chatId: String): List<Message> = when (chatId) {
        "chat_002" -> groupMessages
        "chat_006" -> defaultMessages.dropLast(1) + Message(
            id = "v1",
            senderId = "other",
            senderColor = Color(0xFF9B59B6),
            content = "8",
            isMine = false,
            type = MessageType.Voice,
            timestamp = System.currentTimeMillis() - 1200_000,
        )
        else -> defaultMessages
    }

    // --- 通讯录 ---

    val contacts: List<Contact> = listOf(
        Contact("c01", "陈思雨", colorFor("c01"), "C"),
        Contact("c02", "陈伟", colorFor("c02"), "C"),
        Contact("c03", "高峰", colorFor("c03"), "G"),
        Contact("c04", "郭静怡", colorFor("c04"), "G"),
        Contact("c05", "韩磊", colorFor("c05"), "H"),
        Contact("c06", "黄丽娜", colorFor("c06"), "H"),
        Contact("c07", "李工", colorFor("c07"), "L"),
        Contact("c08", "李娜", colorFor("c08"), "L"),
        Contact("c09", "刘建国", colorFor("c09"), "L", remark = "老刘"),
        Contact("c10", "刘洋", colorFor("c10"), "L"),
        Contact("c11", "马晓东", colorFor("c11"), "M"),
        Contact("c12", "潘婷", colorFor("c12"), "P"),
        Contact("c13", "孙浩", colorFor("c13"), "S"),
        Contact("c14", "孙悦", colorFor("c14"), "S"),
        Contact("c15", "王芳", colorFor("c15"), "W"),
        Contact("c16", "王强", colorFor("c16"), "W"),
        Contact("c17", "吴敏", colorFor("c17"), "W"),
        Contact("c18", "徐志远", colorFor("c18"), "X"),
        Contact("c19", "杨帆", colorFor("c19"), "Y"),
        Contact("c20", "张明远", colorFor("c20"), "Z"),
        Contact("c21", "赵小雅", colorFor("c21"), "Z"),
        Contact("c22", "周文", colorFor("c22"), "Z"),
        Contact("c23", "朱莉", colorFor("c23"), "Z"),
    )

    val friendRequests: List<FriendRequest> = listOf(
        FriendRequest("fr1", "林小满", colorFor("fr1"), "我是林小满，通过一下～"),
        FriendRequest("fr2", "郑一鸣", colorFor("fr2"), "你好，我是郑一鸣，李工推荐的", accepted = true),
        FriendRequest("fr3", "钱多多", colorFor("fr3"), "我是钱多多"),
    )

    // --- 朋友圈 ---

    val moments: List<Moment> = listOf(
        Moment(
            id = "mo1",
            authorName = "王芳",
            authorColor = colorFor("王芳"),
            content = "周六爬山，日出真的绝了！给大家看看我在山顶拍的照片",
            imageColors = listOf(
                Color(0xFF8E9EAB), Color(0xFFEEF2F3), Color(0xFFFFB75E),
                Color(0xFF66A6FF), Color(0xFF2C3E50), Color(0xFFF7B733),
            ),
            likes = "李工，张明远，陈思雨",
            comments = listOf(
                "李工" to "这也太好看了吧！",
                "王芳" to "回复 李工：下次一起来啊",
            ),
            timeAgo = "10分钟前",
        ),
        Moment(
            id = "mo2",
            authorName = "张明远",
            authorColor = colorFor("张明远"),
            content = "加班到现在，终于把这个版本发出去了。回家路上的月亮很好看。",
            imageColors = listOf(Color(0xFF2C3E50)),
            likes = "陈思雨，刘建国",
            comments = listOf("刘建国" to "辛苦了兄弟"),
            timeAgo = "1小时前",
        ),
        Moment(
            id = "mo3",
            authorName = "陈思雨",
            authorColor = colorFor("陈思雨"),
            content = "今天做了提拉米苏，第一次做就成功了，开心～",
            imageColors = listOf(
                Color(0xFFD2A679), Color(0xFF8B5A2B), Color(0xFFF5E6D3),
            ),
            likes = "赵小雅，孙悦，王芳，周文",
            comments = listOf(
                "赵小雅" to "求教程！",
                "孙悦" to "看起来好好吃",
                "陈思雨" to "回复 赵小雅：晚上发你",
            ),
            timeAgo = "3小时前",
        ),
        Moment(
            id = "mo4",
            authorName = "刘建国",
            authorColor = colorFor("刘建国"),
            content = "儿子期末考了全班第一，奖励一台自行车🚲",
            imageColors = listOf(Color(0xFF3EC1D3), Color(0xFFF6F7D7)),
            likes = "张明远，王芳，李工，陈思雨，赵小雅",
            comments = listOf(
                "李工" to "恭喜恭喜！",
                "郭静怡" to "厉害",
            ),
            timeAgo = "昨天",
        ),
        Moment(
            id = "mo5",
            authorName = "孙悦",
            authorColor = colorFor("孙悦"),
            content = "读完了《置身事内》，第三章讲地方政府投融资那段真的是醍醐灌顶。",
            likes = "陈思雨",
            comments = listOf("刘建国" to "这本书我也在看，确实好"),
            timeAgo = "昨天",
        ),
    )

    // --- 发现页条目 ---

    fun discoverEntries(): List<EntryItem> = listOf(
        EntryItem("朋友圈", Color(0xFF3F7FBF), "◉", route = "moments"),
        EntryItem("视频号", Color(0xFFF9961D), "▶", route = "channels"),
        EntryItem("扫一扫", Color(0xFF006BED), "⊞", route = "scan"),
        EntryItem("看一看", Color(0xFFFEC206), "☰", route = "top_stories"),
        EntryItem("搜一搜", Color(0xFFF83734), "⌕", route = "search_discover"),
        EntryItem("小程序", Color(0xFF343BEE), "⬡", route = "mini_programs"),
    )

    // --- 我页条目（服务 / 收藏 / 朋友圈 / 卡包 / 表情 / 设置） ---

    fun meEntries(): List<EntryItem> = listOf(
        EntryItem("服务", Color(0xFF00BF5D), "¥", route = "services"),
        EntryItem("收藏", Color(0xFF017FF1), "☆", route = "favorites"),
        EntryItem("朋友圈", Color(0xFF0074ED), "◉", route = "my_moments"),
        EntryItem("卡包", Color(0xFF006FF1), "▤", route = "cards"),
        EntryItem("表情", Color(0xFFFFBD00), "☺", route = "stickers"),
        EntryItem("设置", Color(0xFF007BED), "⚙", route = "settings"),
    )

    // --- 「+」菜单（微信首页右上角） ---

    val addMenuItems = listOf("发起群聊", "添加朋友", "扫一扫", "收付款")

    // --- 聊天输入栏「+」扩展面板 ---

    val chatInputExtensions = listOf(
        "相册" to Color(0xFF007BED),
        "拍摄" to Color(0xFF00BF5D),
        "视频通话" to Color(0xFFF9961D),
        "位置" to Color(0xFFF83734),
        "红包" to Color(0xFFEE5A6F),
        "转账" to Color(0xFFFFBD00),
        "语音输入" to Color(0xFF9B59B6),
        "收藏" to Color(0xFF3498DB),
    )

    // --- 表情面板（占位表情，实际接后端时换成 emoji 表） ---

    val emojiPanel = listOf(
        "😀", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂",
        "😉", "😊", "😍", "🥰", "😘", "😗", "😙", "😚",
        "🤗", "🤩", "🥳", "😎", "🤓", "🧐", "😏", "😒",
        "😞", "😔", "😟", "😕", "🙁", "☹️", "😣", "😖",
        "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡",
        "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰",
        "😥", "😓", "🤔", "🤭", "🤫", "🤥", "😶", "😐",
        "😑", "😬", "🙄", "😯", "😦", "😧", "😮", "😲",
        "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "🤙",
        "👈", "👉", "👆", "👇", "☝️", "✋", "🤚", "🖐️",
        "💪", "🙏", "🤝", "👏", "🙌", "👐", "🤲", "🫶",
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
    )
}