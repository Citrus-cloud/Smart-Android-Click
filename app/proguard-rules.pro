# ClickFlow Android — release ProGuard/R8 rules.

# Keep app data models stable for Kotlin/JVM tests, JSON persistence and future migrations.
-keep class com.clickflow.android.scenario.Scenario { *; }
-keep class com.clickflow.android.scenario.ScenarioStep { *; }
-keep class com.clickflow.android.scenario.StepType { *; }
-keep class com.clickflow.android.scenario.NotFoundPolicy { *; }
-keep class com.clickflow.android.imageclick.ImageClickTemplate { *; }
-keep class com.clickflow.android.textclick.TextClickConfig { *; }

# Keep Android entry points explicitly referenced from AndroidManifest.xml.
-keep class com.clickflow.android.MainActivity { *; }
-keep class com.clickflow.android.permissions.ClickFlowAccessibilityService { *; }
-keep class com.clickflow.android.overlay.FloatingTapperOverlayService { *; }
-keep class com.clickflow.android.imageclick.ImageClickService { *; }
-keep class com.clickflow.android.textclick.TextClickService { *; }
-keep class com.clickflow.android.scenario.ScenarioEngineService { *; }
-keep class com.clickflow.android.imageclick.ImageTemplateActivity { *; }
-keep class com.clickflow.android.textclick.TextClickActivity { *; }
-keep class com.clickflow.android.scenario.ScenarioActivity { *; }

# ML Kit/Text Recognition uses generated and reflected classes internally.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.internal.mlkit_vision_text_common.**
