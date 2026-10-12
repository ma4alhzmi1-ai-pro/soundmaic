package com.example.audio

data class VoicePreset(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val defaultPitch: Float,
    val defaultSpeed: Float,
    val descriptionAr: String,
    val iconName: String
)

object VoicePresets {
    val presets = listOf(
        VoicePreset(
            id = "young_woman",
            nameAr = "فتاة شابة عشرينية",
            nameEn = "Young Woman",
            defaultPitch = 1.08f,
            defaultSpeed = 1.0f,
            descriptionAr = "صوت أنثوي ناعم وعالي النبرة",
            iconName = "face_3"
        ),
        VoicePreset(
            id = "middle_woman",
            nameAr = "امرأة متوسطة العمر",
            nameEn = "Middle-Aged Woman",
            defaultPitch = 1.04f,
            defaultSpeed = 0.98f,
            descriptionAr = "صوت دافئ وطبيعي متزن",
            iconName = "face_4"
        ),
        VoicePreset(
            id = "elderly_man",
            nameAr = "رجل مسن",
            nameEn = "Elderly Man",
            defaultPitch = 0.9f,
            defaultSpeed = 0.94f,
            descriptionAr = "صوت خشن وعميق ببطء",
            iconName = "elderly"
        ),
        VoicePreset(
            id = "child_10",
            nameAr = "طفل بعمر 10 سنوات",
            nameEn = "10-Year-Old Child",
            defaultPitch = 1.12f,
            defaultSpeed = 1.04f,
            descriptionAr = "صوت طفولي حماسي وسريع",
            iconName = "child_care"
        ),
        VoicePreset(
            id = "young_man",
            nameAr = "شاب",
            nameEn = "Young Man",
            defaultPitch = 0.96f,
            defaultSpeed = 1.0f,
            descriptionAr = "صوت رجولي طبيعي وواضح",
            iconName = "face"
        ),
        VoicePreset(
            id = "robot",
            nameAr = "روبوت آلي ذكي",
            nameEn = "Smart Robot",
            defaultPitch = 0.8f,
            defaultSpeed = 0.9f,
            descriptionAr = "صوت إلكتروني ميكانيكي آلي",
            iconName = "smart_toy"
        ),
        VoicePreset(
            id = "alien",
            nameAr = "كائن فضائي",
            nameEn = "Alien",
            defaultPitch = 2.2f,
            defaultSpeed = 1.3f,
            descriptionAr = "صوت فضائي رفيع وفريد من نوعه",
            iconName = "psychology"
        ),
        VoicePreset(
            id = "monster",
            nameAr = "وحش عميق",
            nameEn = "Deep Monster",
            defaultPitch = 0.4f,
            defaultSpeed = 0.7f,
            descriptionAr = "صوت مرعب وعميق جداً",
            iconName = "pest_control"
        )
    )

    val backgroundSounds = listOf(
        BackgroundSound("none", "بدون خلفية", "None"),
        BackgroundSound("children", "أطفال يلعبون في الخلفية", "Children Playing"),
        BackgroundSound("women_chat", "نساء يتحدثن في جوار المسجل", "Women Chatting Nearby")
    )

    val reverbs = listOf(
        ReverbEffect("none", "بدون صدى", "No Reverb"),
        ReverbEffect("studio", "صدى استوديو احترافي", "Studio Reverb"),
        ReverbEffect("hall", "صدى قاعة كبرى", "Concert Hall"),
        ReverbEffect("cave", "صدى كهف عميق", "Deep Cave")
    )
}

data class BackgroundSound(
    val id: String,
    val nameAr: String,
    val nameEn: String
)

data class ReverbEffect(
    val id: String,
    val nameAr: String,
    val nameEn: String
)
