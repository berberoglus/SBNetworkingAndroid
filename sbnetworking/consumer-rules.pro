# kotlinx-serialization: keep generated serializers for types in this library.
-keepclassmembers class com.berberoglus.sbnetworking.** {
    *** Companion;
}
-keepclasseswithmembers class com.berberoglus.sbnetworking.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.berberoglus.sbnetworking.**$$serializer { *; }
