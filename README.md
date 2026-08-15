# Al-Minshawi Quran | المنشاوي للقرآن الكريم

<div align="center">

**A bilingual Android Quran application focused on listening to the Holy Quran with the recitation of Sheikh Muhammad Siddiq Al-Minshawi.**  
**تطبيق Android ثنائي اللغة للاستماع إلى القرآن الكريم، مع التركيز على تلاوات الشيخ محمد صديق المنشاوي.**

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)](https://developer.android.com/jetpack/compose)
[![Repository](https://img.shields.io/badge/repository-public-181717?logo=github&logoColor=white)](https://github.com/Seifelromy/al-minshawi-quran)

[العربية](#العربية) · [English](#english)

</div>

---

## العربية

### نبذة عن المشروع

**المنشاوي للقرآن الكريم** هو تطبيق Android مفتوح المصدر يهدف إلى توفير تجربة هادئة ومنظمة للاستماع إلى سور القرآن الكريم، مع جعل تلاوات الشيخ **محمد صديق المنشاوي** هي الخيار الأساسي عند تشغيل التطبيق. صُمم المشروع ليكون قابلًا للتطوير والمساهمة، وليس مجرد مشغل صوتي بسيط؛ فهو يجمع بين مكتبة للسور، ومشغل صوتي متكامل، وحفظ التقدم، والمفضلة، والتنزيل للاستماع دون اتصال.

المستودع متاح للعامة حتى يتمكن المطورون والمهتمون من **قراءة الكود، تجربة المشروع، اقتراح التحسينات، وفتح طلبات الدمج**. يُرجى الانتباه إلى أن هذا المستودع يضم كود التطبيق وإعداداته، بينما قد تعتمد ملفات الصوت وروابطها على مزودي المحتوى الخارجيين وتوافرهم.

### المزايا الحالية

| الميزة | الوصف |
| --- | --- |
| الاستماع للقرآن | تشغيل السور مع أدوات التشغيل الأساسية، والتقديم والتأخير، والانتقال بين السور. |
| تلاوات متعددة | المنشاوي هو القارئ الأساسي، مع وجود بنية تسمح بتبديل مزود أو قارئ صوتي من الإعدادات وفق المصادر المهيأة في المشروع. |
| التشغيل في الخلفية | يعتمد التطبيق على Media3 وخدمة تشغيل وسائط للمحافظة على التشغيل عند مغادرة الشاشة. |
| الاستماع دون اتصال | يمكن تنزيل السور المتاحة وحفظها محليًا ثم تشغيلها من شاشة التنزيلات. |
| متابعة الاستماع | يحفظ التطبيق موضع التوقف ليسمح باستئناف الاستماع من المكان السابق. |
| المفضلة والتاريخ | إضافة السور إلى المفضلة، وتسجيل السور التي تم تشغيلها، وعرض السور الحديثة والأكثر تشغيلًا. |
| البحث | البحث عن السورة بالاسم العربي أو الإنجليزي أو برقم السورة. |
| تحكم متقدم | سرعة تشغيل قابلة للتعديل، وتكرار، وتشغيل عشوائي، ومؤقت نوم، وتقديم وتأخير. |
| واجهة ثنائية اللغة | يدعم التطبيق العربية والإنجليزية، مع اختيار المظهر الفاتح أو الداكن أو مظهر النظام. |
| تحديث البيانات | يستخدم التطبيق تخزينًا محليًا لبيانات السور وإعدادات المصادر، مع قابلية تحديث الإعدادات عند توفر مصدر بعيد. |

### التقنيات المستخدمة

يعتمد المشروع على **Kotlin** و**Jetpack Compose** لبناء الواجهة، و**AndroidX Media3** لتشغيل الوسائط، و**Room** لحفظ المفضلة والتنزيلات وسجل التشغيل وموضع الاستئناف، إضافة إلى **Coroutines/Flow** لإدارة الحالة والعمليات غير المتزامنة. الحد الأدنى المعلن لإصدار Android هو **API 24**، مع إعداد المشروع للبناء باستخدام Android SDK حديث.

### تشغيل المشروع محليًا

1. ثبّت [Android Studio](https://developer.android.com/studio) مع أدوات Android SDK المناسبة.
2. استنسخ المستودع وافتح مجلده في Android Studio:

   ```bash
   git clone https://github.com/Seifelromy/al-minshawi-quran.git
   cd al-minshawi-quran
   ```

3. انتظر اكتمال مزامنة Gradle، ثم شغّل التطبيق على محاكي Android أو جهاز حقيقي يعمل بإصدار API 24 أو أحدث.
4. يمكنك تشغيل الاختبارات من Android Studio أو باستخدام Gradle:

   ```bash
   ./gradlew test
   ```

   وعلى Windows:

   ```bat
   gradlew.bat test
   ```

5. لا تضع أي مفاتيح سرية أو ملفات توقيع داخل Git. ملف `.env.example` الموجود في المشروع نموذج فقط، وأي ملف `.env` حقيقي يجب أن يبقى محليًا.

### هيكل المشروع باختصار

| المسار | المسؤولية |
| --- | --- |
| `app/src/main/java/com/example/ui` | الشاشات، الحالة، النصوص، والواجهة المبنية باستخدام Compose. |
| `app/src/main/java/com/example/data/audio` | إدارة التشغيل وخدمة الوسائط والتحكم في الصوت. |
| `app/src/main/java/com/example/data/repository` | تحميل بيانات السور والمصادر وإدارة طبقة البيانات. |
| `app/src/main/java/com/example/data/database` | كيانات وقاعدة بيانات Room للمفضلة والتنزيلات والتاريخ. |
| `app/src/main/assets` | بيانات محلية مبدئية للسور ومصادر الصوت. |
| `MD` | ملاحظات وتقارير وخطط مرتبطة بتطوير المشروع. |

### المساهمة

المساهمات مرحب بها. قبل اقتراح تغيير كبير، يُفضّل فتح [Issue](https://github.com/Seifelromy/al-minshawi-quran/issues) لشرح المشكلة أو الفكرة. بعد ذلك يمكنك إنشاء فرع مستقل، تنفيذ التغيير مع اختبار مناسب، ثم فتح **Pull Request** يوضح ما تم تغييره وكيفية التحقق منه.

للحفاظ على جودة المشروع، يُرجى عدم رفع مفاتيح التوقيع أو كلمات المرور أو ملفات الصوت المحمية بحقوق النشر. كما يُرجى احترام شروط استخدام أي مزود خارجي للبيانات أو الصوتيات.

### ملاحظات مهمة حول المحتوى والحقوق

هذا المستودع ينشر **كود التطبيق** بهدف التعليم والتطوير والاستفادة العامة. أما ملفات الصوت والبيانات وروابط المصادر فقد تكون مملوكة أو خاضعة لشروط استخدام الجهات التي توفرها؛ لذلك يجب التحقق من الحقوق والتراخيص قبل إعادة توزيعها أو استخدامها في تطبيق تجاري. لم تتم إضافة ترخيص قانوني للكود في هذه المرحلة، لذا ينبغي مناقشة الترخيص المناسب قبل إعادة استخدام أجزاء المشروع في منتج مستقل.

### التواصل والاقتراحات

يمكن إرسال الاقتراحات ومتابعة الأخطاء عبر صفحة [Issues](https://github.com/Seifelromy/al-minshawi-quran/issues) في GitHub. الهدف من المشروع هو تقديم أساس مفتوح وقابل للتحسين لتطبيقات القرآن الكريم، مع الحفاظ على تجربة استخدام بسيطة ومحترمة للمحتوى القرآني.

---

## English

### Project overview

**Al-Minshawi Quran** is an open-source Android application designed to provide a calm and organized way to listen to the Holy Quran, with the recitations of **Sheikh Muhammad Siddiq Al-Minshawi** as the primary playback experience. The project is intended to be extendable and contributor-friendly rather than being only a basic audio player. It combines a surah library, a full playback experience, progress persistence, favorites, and downloadable content for offline listening.

The repository is public so developers and interested users can **inspect the source code, run the project, suggest improvements, and submit pull requests**. Please note that the application code and configuration are included in this repository, while audio files and their URLs may depend on external content providers and their availability.

### Current features

| Feature | Description |
| --- | --- |
| Quran listening | Play surahs with standard playback controls, seeking, and next/previous navigation. |
| Multiple reciters/providers | Al-Minshawi is the primary reciter, while the provider layer is designed to support switching between configured reciters or audio sources. |
| Background playback | Uses AndroidX Media3 and a media playback service to keep audio playing outside the main screen. |
| Offline listening | Download available surahs, store them locally, and play them from the Downloads section. |
| Continue listening | Saves the last playback position so listening can be resumed later. |
| Favorites and history | Favorite surahs, playback history, recently played items, and most-played items. |
| Search | Search by Arabic name, English name, or surah number. |
| Advanced controls | Adjustable speed, repeat, shuffle, sleep timer, and forward/backward seeking. |
| Bilingual interface | Arabic and English interface options, with light, dark, and system theme choices. |
| Data refresh | Uses local cached surah/provider data and supports refreshing configuration when a remote source is available. |

### Technology stack

The project is built with **Kotlin** and **Jetpack Compose** for the user interface, **AndroidX Media3** for audio playback, **Room** for favorites, downloads, playback history, and resume state, and **Coroutines/Flow** for asynchronous work and reactive state. The configured minimum Android version is **API 24**, and the project targets a modern Android SDK.

### Run locally

1. Install [Android Studio](https://developer.android.com/studio) with the required Android SDK tools.
2. Clone the repository and open it in Android Studio:

   ```bash
   git clone https://github.com/Seifelromy/al-minshawi-quran.git
   cd al-minshawi-quran
   ```

3. Allow Gradle synchronization to finish, then run the application on an Android emulator or a physical device using API 24 or newer.
4. Run the tests from Android Studio or with Gradle:

   ```bash
   ./gradlew test
   ```

   On Windows:

   ```bat
   gradlew.bat test
   ```

5. Never commit secrets or signing files to Git. The `.env.example` file is only a template; any real `.env` file must remain local.

### Project structure

| Path | Responsibility |
| --- | --- |
| `app/src/main/java/com/example/ui` | Compose screens, UI state, localized strings, and presentation logic. |
| `app/src/main/java/com/example/data/audio` | Playback engine, media service, and audio controls. |
| `app/src/main/java/com/example/data/repository` | Surah/provider loading and data access orchestration. |
| `app/src/main/java/com/example/data/database` | Room entities and local persistence for favorites, downloads, and history. |
| `app/src/main/assets` | Seed Quran metadata and audio-source configuration. |
| `MD` | Development notes, reports, and implementation documentation. |

### Contributing

Contributions are welcome. For a substantial change, please open an [Issue](https://github.com/Seifelromy/al-minshawi-quran/issues) first to describe the problem or proposal. Then create a dedicated branch, implement the change with appropriate tests, and open a **Pull Request** explaining what changed and how it was verified.

To keep the project safe and maintainable, do not commit signing keys, passwords, API secrets, or copyrighted audio files. Always respect the terms of use of external data and audio providers.

### Content and licensing notice

This repository publishes the **application source code** for education, development, and public benefit. Audio files, metadata, and source URLs may belong to or be governed by the terms of the organizations that provide them; verify the applicable rights and licenses before redistributing them or using them in a commercial application. No formal software license has been added at this stage, so the appropriate license should be agreed upon before reusing the project in a separate product.

### Feedback and support

Please use the GitHub [Issues](https://github.com/Seifelromy/al-minshawi-quran/issues) page for bug reports, questions, and feature suggestions. The goal is to provide an open and improvable foundation for Quran applications while maintaining a simple and respectful user experience.

---

## Repository

[https://github.com/Seifelromy/al-minshawi-quran](https://github.com/Seifelromy/al-minshawi-quran)

## References

1. [Android Developers — Build your first app](https://developer.android.com/courses/android-basics-compose/course)
2. [Android Developers — Jetpack Compose](https://developer.android.com/jetpack/compose)
3. [Android Developers — Media3](https://developer.android.com/media/media3)
4. [Android Developers — Room persistence library](https://developer.android.com/training/data-storage/room)
5. [Kotlin documentation](https://kotlinlang.org/docs/home.html)
