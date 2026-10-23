# HttpDeck is intended for debug/internal builds. If you enable it in a
# minified release build, keep its public facade and models.
-keep class io.github.kevin321happy.httpdeck.HttpDeck { *; }
-keep class io.github.kevin321happy.httpdeck.core.** { *; }