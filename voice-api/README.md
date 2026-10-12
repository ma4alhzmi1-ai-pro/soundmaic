# Soundmaic Voice API

خدمة وسيطة صغيرة لتطبيق Android. تحتفظ بمفتاح ElevenLabs على الخادم فقط، ولا تحفظ النصوص أو التسجيلات أو الصوت الناتج بعد اكتمال الطلب.

## المسارات

- `GET /health` — فحص الجاهزية.
- `GET /api/voices` — قائمة ملفات الصوت المتاحة.
- `POST /api/tts?profile=young_woman` — جسم JSON من الشكل `{ "text": "...", "speed": 1.0 }` ويعيد MP3.
- `POST /api/voice-convert?profile=young_man` — يمرر multipart/form-data إلى Speech-to-Speech ويعيد MP3.

كل طلب API يحتاج الترويسة `X-Soundmaic-Client: android`. هذه ليست مفتاح ElevenLabs؛ الحماية الأساسية للمفتاح تتمثل في إبقائه داخل Secret للخادم، مع حدود طلبات وقيود حجم ووقت.
