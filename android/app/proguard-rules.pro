# Keep Room generated implementations
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
# Kotlin serialization-free JSON is used, nothing else to keep.
