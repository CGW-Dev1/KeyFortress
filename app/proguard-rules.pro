# Keep Room schema and database
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Keep SQLCipher native and Java classes
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**

# Keep models for JSON serialization (backup/export)
-keepclassmembers class com.keyfortress.app.data.model.** { *; }
-keep class com.keyfortress.app.data.model.** { *; }
-keepclassmembers class com.keyfortress.app.data.entity.** { *; }
-keep class com.keyfortress.app.data.entity.** { *; }
