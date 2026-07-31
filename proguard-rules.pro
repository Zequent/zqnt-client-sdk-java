-dontwarn
-dontshrink
-dontoptimize

# Keep the complete customer-facing binary API stable. Private implementation
# details can still be renamed by ProGuard.
-keepnames public class com.zqnt.sdk.client.**
-keepclassmembers public class com.zqnt.sdk.client.** {
    public protected *;
}
-keep public interface com.zqnt.sdk.client.** { *; }
-keep public enum com.zqnt.sdk.client.** { *; }

# CDI and configuration may inspect constructors, fields and annotations.
-keepclasseswithmembers,includedescriptorclasses class com.zqnt.sdk.client.** {
    @jakarta.inject.Inject <init>(...);
}
# Arc resolves injected members by the names stored in the Jandex index.
# Renaming these fields makes the index inconsistent with the bytecode.
-keepclassmembers class com.zqnt.sdk.client.** {
    @jakarta.inject.Inject *;
}

-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,MethodParameters,Exceptions

# Do not expose local source paths in stack traces.
-renamesourcefileattribute SourceFile
