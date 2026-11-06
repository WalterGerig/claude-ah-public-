# Minification is disabled by default. If you enable it, keep the serializable game state:
-keepclassmembers class com.mahjongzen.game.** { *** Companion; }
-keepclasseswithmembers class com.mahjongzen.game.** { kotlinx.serialization.KSerializer serializer(...); }
