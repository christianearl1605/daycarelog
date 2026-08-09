# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- Gson ---
# All model/DTO classes in data/model are deserialized by Gson via field-name
# reflection (Retrofit's converter-gson). R8 doesn't know that, so without
# these rules it would rename/strip fields and silently break JSON parsing
# at runtime (compiles fine, fails on every API response). Rules match
# Gson's own documented ProGuard config (see gson/examples on GitHub),
# adapted to this app's model package.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Gson's TypeToken relies on reading its own generic superclass via reflection
# (getClass().getGenericSuperclass()) to recover a field/response's generic type
# (e.g. List<Child>) at runtime. R8 can otherwise strip that generic signature
# even with -keepattributes Signature above, causing
# "java.lang.Class cannot be cast to java.lang.reflect.ParameterizedType" the
# first time Gson serializes/deserializes anything - this is Gson's own
# documented R8 rule, not something specific to this app.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

-keep class com.daycarelog.app.data.model.** { *; }

# --- Retrofit / OkHttp ---
# Retrofit/OkHttp ship their own consumer-proguard-rules.txt (auto-merged by
# AGP), which covers their internals. Keep our own service interface too,
# since its method signatures/annotations drive Retrofit's dynamic proxy.
-keep interface com.daycarelog.app.data.api.ApiService { *; }

# Every method in ApiService is a Kotlin suspend fun, which the compiler turns
# into a plain method taking a trailing kotlin.coroutines.Continuation<? super
# T> parameter. At runtime Retrofit reads T back out of that Continuation's
# generic signature to know what to deserialize the response into. R8 can
# strip Continuation's own generic signature even though ApiService itself is
# kept above, which throws
# "java.lang.Class cannot be cast to java.lang.reflect.ParameterizedType" on
# the very first suspend API call (login included) - this is Square's own
# documented Retrofit R8 rule for Kotlin coroutine support.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
