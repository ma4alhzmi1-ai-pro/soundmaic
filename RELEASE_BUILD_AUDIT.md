# تقرير تدقيق جاهزية بناء Android Release APK

**المستودع:** `https://github.com/ma4alhzmi1-ai-pro/soundmaic`  
**الفرع المفحوص:** `main`  
**Commit:** `9fc404642e2a329e63dfb96afde8dc59b39590fa`  
**تاريخ الفحص:** 2026-10-12  
**نطاق الفحص:** التحقق من بنية المشروع، ملفات Gradle/Android، التوقيع، الأسرار، الاختبارات، وGitHub Actions.

## الحكم المختصر

المشروع يحتوي على تطبيق Android فعلي مبني بـ Kotlin/Jetpack Compose، ويوجد APK تجريبي محفوظ في المستودع، لكنه **غير جاهز حاليًا لبناء APK بصيغة release قابل للتكرار**.

التحقق المحلي فشل قبل بدء Gradle للأسباب التالية:

```text
./gradlew: No such file or directory
```

كما أن أمر `gradle` غير مثبت في بيئة الفحص.

## ما هو موجود

- وحدة Android باسم `app`.
- `app/src/main/AndroidManifest.xml` وصلاحية التسجيل الصوتي و`FileProvider`.
- كود Kotlin وواجهات Jetpack Compose وRoom وTTS وتسجيل/تشغيل الصوت.
- إعدادات Gradle Kotlin DSL:
  - `settings.gradle.kts`
  - `build.gradle.kts`
  - `app/build.gradle.kts`
  - `gradle/libs.versions.toml`
  - `gradle.properties`
- ملف `gradle/wrapper/gradle-wrapper.properties` ويشير إلى Gradle `9.3.1`.
- إعداد release داخل `app/build.gradle.kts`، مع توقيع إلزامي عبر:
  - `KEYSTORE_PATH` أو الملف الافتراضي `my-upload-key.jks`
  - `STORE_PASSWORD`
  - `KEY_PASSWORD`
  - اسم المفتاح `upload`
- APK تجريبي محفوظ: `.build-outputs/app-debug.apk`، حجمه التقريبي 22.4 MB، وهو **debug وليس release**.
- Workflow في `.github/workflows/android_build.yml`، لكنه يبني `assembleDebug` فقط.

## الملفات الناقصة أو غير المكتملة

### 1. Gradle Wrapper — مانع بناء مباشر
الملفات التالية غير موجودة:

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`

وجود `gradle-wrapper.properties` وحده لا يكفي. لذلك لا يمكن تشغيل البناء باستخدام `./gradlew`، وWorkflow الحالي يعتمد على أمر `gradle` غير مضمون التوفر على GitHub Runner.

**المطلوب:** توليد وإضافة Gradle Wrapper المتوافق مع Gradle `9.3.1`، مع إبقاء الملفات التنفيذية قابلة للتشغيل.

### 2. مفتاح توقيع release — مانع إصدار APK موقّع
لا يوجد في المستودع أو مجلد المشروع:

- `my-upload-key.jks`

ولا توجد أسرار بيئية في جلسة الفحص:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_PASSWORD`

إعداد `release` في `app/build.gradle.kts` يعيّن `signingConfig` إلزاميًا، لذلك لن ينجح بناء release ما لم يُوفَّر ملف keystore وكلمات المرور/الاسم المستعار.

**المطلوب:** إنشاء أو توفير keystore release حقيقي، وتخزينه خارج GitHub في GitHub Actions Secrets/Files، وعدم رفعه إلى المستودع. يجب تثبيت اسم alias وكلمات المرور حسب إعدادات البناء أو تعديل الإعدادات باتفاق واضح.

> ملف `debug.keystore.base64` الموجود ليس مفتاح release، ولا يوجد في Workflow الحالي أي أمر لفك ترميزه إلى `debug.keystore`.

### 3. Workflow لا يبني release
الملف `.github/workflows/android_build.yml` ينفذ:

```yaml
gradle assembleDebug
```

ويرفع:

```yaml
app/build/outputs/apk/debug/app-debug.apk
```

**المطلوب:**

- استخدام `./gradlew` بعد إضافة Wrapper.
- إعداد Java/Gradle المتوافقين.
- تجهيز keystore من GitHub Secret بطريقة آمنة.
- تنفيذ `./gradlew clean test assembleRelease` أو ما يعادلها.
- رفع `app/build/outputs/apk/release/app-release.apk` كـ artifact.
- يفضّل إضافة تحقق من توقيع APK وSHA-256.

### 4. إعدادات Firebase/الأسرار تحتاج قرارًا
لا يوجد `google-services.json`، ويوجد فقط `.env.example` مع مفتاح Gemini معلّق:

```text
# GEMINI_API_KEY=MY_GEMINI_API_KEY
```

إضافة Google Services مضبوطة على `WARN` عند غياب الملف، لذلك قد لا يكون غياب `google-services.json` مانعًا لمرحلة Gradle نفسها، لكنه قد يمنع وظائف Firebase/Gemini الفعلية أو يجعلها غير مهيأة في النسخة النهائية.

**المطلوب:** تحديد هل التطبيق يحتاج Firebase/Gemini في release فعلًا. إذا نعم، يجب توفير إعداد Firebase الصحيح ومفتاح/سر مناسب دون تضمين أسرار صريحة داخل Git.

## مشاكل تحقق إضافية

### اختبار Robolectric غير متطابق
في `app/src/test/java/com/example/ExampleRobolectricTest.kt` الاختبار يتوقع:

```kotlin
"My Application"
```

بينما `app/src/main/res/values/strings.xml` يعرّف اسم التطبيق:

```xml
<string name="app_name">VoiceCraft Pro</string>
```

لذلك ينبغي تحديث التوقع إلى `VoiceCraft Pro` أو جعل الاختبار يقرأ القيمة المطلوبة، وإلا قد يفشل `test` حتى بعد إصلاح أدوات البناء.

### قابلية إعادة البناء غير مؤكدة
- `compileSdk` مضبوط على API 36 مع minor API level 1، ويجب أن تكون حزمة Android SDK المطابقة متاحة في بيئة البناء.
- إصدارات AGP/Kotlin/Gradle حديثة جدًا نسبيًا؛ يجب تثبيت توافقها في CI وعدم الاعتماد على Gradle مثبت مسبقًا.
- لا يوجد `local.properties`، وهذا مقبول عادةً لأن Android SDK في CI يُضبط عبر بيئة runner.

## ترتيب الإصلاح المقترح

1. إضافة Gradle Wrapper كامل (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`).
2. إصلاح اختبار `ExampleRobolectricTest` ليطابق اسم التطبيق الحقيقي.
3. توفير keystore release خارج المستودع، أو إنشاء واحد جديد إذا لم توجد هوية توقيع سابقة يجب الحفاظ عليها.
4. تحديد إعدادات Firebase/Gemini المطلوبة للنسخة النهائية وتوفيرها عبر أسرار CI.
5. تعديل GitHub Actions إلى `./gradlew clean test assembleRelease` مع إعداد signing آمن.
6. تشغيل البناء، الاختبارات، والتحقق من APK الناتج والتوقيع.
7. اختبار APK على جهاز/محاكي Android، خصوصًا التسجيل الصوتي وTTS وFileProvider والصلاحيات.

## نتيجة الفحص

- **المستودع تم الوصول إليه واستنساخه بنجاح.**
- **لا يوجد حاليًا APK release مبني أو مرفوع؛ الموجود APK debug فقط.**
- **لم يتم تعديل كود المشروع أو رفع أي تغيير إلى GitHub.**
- **لا يمكن إصدار APK release من الحالة الحالية قبل توفير Wrapper ومفتاح التوقيع وإصلاح Workflow، ثم معالجة اختبار الاسم.**
