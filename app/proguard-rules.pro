# LiteRT-LM and ONNX Runtime load native implementations dynamically.
-keep class com.google.ai.edge.litertlm.** { *; }
-keep class ai.onnxruntime.** { *; }
-keep class com.squareup.moshi.**JsonAdapter { *; }
-keep @com.squareup.moshi.JsonClass class * extends java.lang.Object { *; }
