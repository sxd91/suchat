# Suchat Android 混淆规则。

# 保留行号信息，便于线上排查（release 崩溃栈可读）。
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Compose 由 AGP 自动处理；下方为 kotlinx-serialization 备用规则
# （当前 remote 层用 org.json 手写解析，改用 @Serializable 时直接生效）。
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
