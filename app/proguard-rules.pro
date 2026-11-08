# Navigation routes are serialized with kotlinx.serialization.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class app.nearfolk.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
