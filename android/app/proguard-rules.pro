# Yandex Mobile Ads SDK ships its own consumer rules; these keep rules make
# reflection-based mediation adapters safe in minified release builds.
-keep class com.yandex.mobile.ads.** { *; }
-keepattributes *Annotation*
-dontwarn com.yandex.mobile.ads.**
