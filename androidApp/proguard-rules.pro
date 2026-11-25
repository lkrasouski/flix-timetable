# kotlinx.serialization: keep generated serializers of the shared DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.flix.timetable.** { *** Companion; }
-keepclasseswithmembers class com.flix.timetable.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.flix.timetable.**$$serializer { *; }

# Ktor references JVM-only logging/management classes that do not exist on Android.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
