package com.example.data.model

import java.io.Serializable

data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val revelationType: String, // "Meccan" or "Medinan"
    val versesCount: Int,
    val typicalDurationMinutes: Int,
    val audioUrl: String = ""
) : Serializable {
    
    val formattedNumber: String
        get() = String.format("%03d", number)

    val englishTranslation: String
        get() = getTranslation(number)

    private fun getTranslation(num: Int): String {
        return when (num) {
            1 -> "The Opening"
            2 -> "The Cow"
            3 -> "Family of Imran"
            4 -> "The Women"
            5 -> "The Table Spread"
            6 -> "The Cattle"
            7 -> "The Elevated Places"
            8 -> "The Spoils of War"
            9 -> "The Repentance"
            10 -> "Jonah"
            11 -> "Hud"
            12 -> "Joseph"
            13 -> "The Thunder"
            14 -> "Abrahim"
            15 -> "The Rocky Tract"
            16 -> "The Bee"
            17 -> "The Night Journey"
            18 -> "The Cave"
            19 -> "Mary"
            20 -> "Ta-Ha"
            21 -> "The Prophets"
            22 -> "The Pilgrimage"
            23 -> "The Believers"
            24 -> "The Light"
            25 -> "The Criterion"
            26 -> "The Poets"
            27 -> "The Ant"
            28 -> "The Stories"
            29 -> "The Spider"
            30 -> "The Romans"
            31 -> "Luqman"
            32 -> "The Prostration"
            33 -> "The Combined Forces"
            34 -> "Sheba"
            35 -> "The Originator"
            36 -> "Ya-Sin"
            37 -> "Those Who Set The Ranks"
            38 -> "The Letter Sad"
            39 -> "The Troops"
            40 -> "The Forgiver"
            41 -> "Explained in Detail"
            42 -> "The Consultation"
            43 -> "The Ornaments of Gold"
            44 -> "The Smoke"
            45 -> "The Crouching"
            46 -> "The Wind-Curved Sandhills"
            47 -> "Muhammad"
            48 -> "The Victory"
            49 -> "The Rooms"
            50 -> "The Letter Qaf"
            51 -> "The Winnowing Winds"
            52 -> "The Mount"
            53 -> "The Star"
            54 -> "The Moon"
            55 -> "The Beneficent"
            56 -> "The Inevitable"
            57 -> "The Iron"
            58 -> "The Pleading Woman"
            59 -> "The Exile"
            60 -> "She That Is To Be Examined"
            61 -> "The Ranks"
            62 -> "The Congregation"
            63 -> "The Hypocrites"
            64 -> "The Mutual Disillusion"
            65 -> "The Divorce"
            66 -> "The Prohibition"
            67 -> "The Sovereignty"
            68 -> "The Pen"
            69 -> "The Reality"
            70 -> "The Ascending Stairways"
            71 -> "Noah"
            72 -> "The Jinn"
            73 -> "The Enshrouded One"
            74 -> "The Cloaked One"
            75 -> "The Resurrection"
            76 -> "Man"
            77 -> "The Emissaries"
            78 -> "The Announcement"
            79 -> "Those Who Drag Forth"
            80 -> "He Frowned"
            81 -> "The Overthrowing"
            82 -> "The Cleaving"
            83 -> "Defrauding"
            84 -> "The Sundering"
            85 -> "The Mansions of the Stars"
            86 -> "The Morning Star"
            87 -> "The Most High"
            88 -> "The Overwhelming"
            89 -> "The Dawn"
            90 -> "The City"
            91 -> "The Sun"
            92 -> "The Night"
            93 -> "The Morning Hours"
            94 -> "The Consolation"
            95 -> "The Fig"
            96 -> "The Clot"
            97 -> "The Power"
            98 -> "The Clear Proof"
            99 -> "The Earthquake"
            100 -> "The Courser"
            101 -> "The Calamity"
            102 -> "Rivalry in World Increase"
            103 -> "The Declining Day"
            104 -> "The Traducer"
            105 -> "The Elephant"
            106 -> "Quraysh"
            107 -> "Almsgiving"
            108 -> "Abundance"
            109 -> "The Disbelievers"
            110 -> "Divine Support"
            111 -> "The Palm Fiber"
            112 -> "Sincerity"
            113 -> "The Daybreak"
            114 -> "Mankind"
            else -> "Quran Surah"
        }
    }

    companion object {
        val ALL_SURAHS: List<Surah> = listOf(
            Surah(1, "الفاتحة", "Al-Fatihah", "Meccan", 7, 2),
            Surah(2, "البقرة", "Al-Baqarah", "Medinan", 286, 120),
            Surah(3, "آل عمران", "Ali 'Imran", "Medinan", 200, 80),
            Surah(4, "النساء", "An-Nisa", "Medinan", 176, 75),
            Surah(5, "المائدة", "Al-Ma'idah", "Medinan", 120, 50),
            Surah(6, "الأنعام", "Al-An'am", "Meccan", 165, 60),
            Surah(7, "الأعراف", "Al-A'raf", "Meccan", 206, 70),
            Surah(8, "الأنفال", "Al-Anfal", "Medinan", 75, 30),
            Surah(9, "التوبة", "At-Tawbah", "Medinan", 129, 65),
            Surah(10, "يونس", "Yunus", "Meccan", 109, 50),
            Surah(11, "هود", "Hud", "Meccan", 123, 50),
            Surah(12, "يوسف", "Yusuf", "Meccan", 111, 45),
            Surah(13, "الرعد", "Ar-Ra'd", "Medinan", 43, 20),
            Surah(14, "إبراهيم", "Ibrahim", "Meccan", 52, 25),
            Surah(15, "الحجر", "Al-Hijr", "Meccan", 99, 20),
            Surah(16, "النحل", "An-Nahl", "Meccan", 128, 45),
            Surah(17, "الإسراء", "Al-Isra", "Meccan", 111, 40),
            Surah(18, "الكهف", "Al-Kahf", "Meccan", 110, 45),
            Surah(19, "مريم", "Maryam", "Meccan", 98, 30),
            Surah(20, "طه", "Ta-Ha", "Meccan", 135, 30),
            Surah(21, "الأنبياء", "Al-Anbiya", "Meccan", 112, 35),
            Surah(22, "الحج", "Al-Hajj", "Medinan", 78, 35),
            Surah(23, "المؤمنون", "Al-Mu'minun", "Meccan", 118, 25),
            Surah(24, "النور", "An-Nur", "Medinan", 64, 35),
            Surah(25, "الفرقان", "Al-Furqan", "Meccan", 77, 20),
            Surah(26, "الشعراء", "Ash-Shu'ara", "Meccan", 227, 40),
            Surah(27, "النمل", "An-Naml", "Meccan", 93, 25),
            Surah(28, "القصص", "Al-Qasas", "Meccan", 88, 30),
            Surah(29, "العنكبوت", "Al-Ankabut", "Meccan", 69, 20),
            Surah(30, "الروم", "Ar-Rum", "Meccan", 60, 20),
            Surah(31, "لقمان", "Luqman", "Meccan", 34, 15),
            Surah(32, "السجدة", "As-Sajdah", "Meccan", 30, 10),
            Surah(33, "الأحزاب", "Al-Ahzab", "Medinan", 73, 30),
            Surah(34, "سبأ", "Saba", "Meccan", 54, 20),
            Surah(35, "فاطر", "Fatir", "Meccan", 45, 20),
            Surah(36, "يس", "Ya-Sin", "Meccan", 83, 20),
            Surah(37, "الصافات", "As-Saffat", "Meccan", 182, 25),
            Surah(38, "ص", "Sad", "Meccan", 88, 20),
            Surah(39, "الزمر", "Az-Zumar", "Meccan", 75, 30),
            Surah(40, "غافر", "Ghafir", "Meccan", 85, 30),
            Surah(41, "فصلت", "Fussilat", "Meccan", 54, 20),
            Surah(42, "الشورى", "Ash-Shura", "Meccan", 53, 20),
            Surah(43, "الزخرف", "Az-Zukhruf", "Meccan", 89, 20),
            Surah(44, "الدخان", "Ad-Dukhan", "Meccan", 59, 10),
            Surah(45, "الجاثية", "Al-Jathiyah", "Meccan", 37, 12),
            Surah(46, "الأحقاف", "Al-Ahqaf", "Meccan", 35, 15),
            Surah(47, "محمد", "Muhammad", "Medinan", 38, 15),
            Surah(48, "الفتح", "Al-Fath", "Medinan", 29, 15),
            Surah(49, "الحجرات", "Al-Hujurat", "Medinan", 18, 10),
            Surah(50, "ق", "Qaf", "Meccan", 45, 10),
            Surah(51, "الذاريات", "Adh-Dhariyat", "Meccan", 60, 10),
            Surah(52, "الطور", "At-Tur", "Meccan", 49, 10),
            Surah(53, "النجم", "An-Najm", "Meccan", 62, 10),
            Surah(54, "القمر", "Al-Qamar", "Meccan", 55, 10),
            Surah(55, "الرحمن", "Ar-Rahman", "Medinan", 78, 15),
            Surah(56, "الواقعة", "Al-Waqi'ah", "Meccan", 96, 12),
            Surah(57, "الحديد", "Al-Hadid", "Medinan", 29, 15),
            Surah(58, "المجادلة", "Al-Mujadilah", "Medinan", 22, 10),
            Surah(59, "الحشر", "Al-Hashr", "Medinan", 24, 12),
            Surah(60, "الممتحنة", "Al-Mumtahanah", "Medinan", 13, 10),
            Surah(61, "الصف", "As-Saff", "Medinan", 14, 8),
            Surah(62, "الجمعة", "Al-Jumu'ah", "Medinan", 11, 6),
            Surah(63, "المنافقون", "Al-Munafiqun", "Medinan", 11, 6),
            Surah(64, "التغابن", "At-Taghabun", "Medinan", 18, 8),
            Surah(65, "الطلاق", "At-Talaq", "Medinan", 12, 8),
            Surah(66, "التحريم", "At-Tahrim", "Medinan", 12, 8),
            Surah(67, "الملك", "Al-Mulk", "Meccan", 30, 8),
            Surah(68, "القلم", "Al-Qulam", "Meccan", 52, 10),
            Surah(69, "الحاقة", "Al-Haqqah", "Meccan", 52, 8),
            Surah(70, "المعارج", "Al-Ma'arij", "Meccan", 44, 8),
            Surah(71, "نوح", "Nuh", "Meccan", 28, 8),
            Surah(72, "الجن", "Al-Jinn", "Meccan", 28, 8),
            Surah(73, "المزمل", "Al-Muzzammil", "Meccan", 20, 8),
            Surah(74, "المدثر", "Al-Muddaththir", "Meccan", 56, 8),
            Surah(75, "القيامة", "Al-Qiyamah", "Meccan", 40, 6),
            Surah(76, "الإنسان", "Al-Insan", "Medinan", 31, 8),
            Surah(77, "المرسلات", "Al-Mursalat", "Meccan", 50, 8),
            Surah(78, "النبأ", "An-Naba", "Meccan", 40, 6),
            Surah(79, "النازعات", "An-Nazi'at", "Meccan", 46, 6),
            Surah(80, "عبس", "Abasa", "Meccan", 42, 5),
            Surah(81, "التكوير", "At-Takwir", "Meccan", 29, 4),
            Surah(82, "الانفطار", "Al-Infitar", "Meccan", 19, 4),
            Surah(83, "المطففين", "Al-Mutaffifin", "Meccan", 36, 6),
            Surah(84, "الانشقاق", "Al-Inshiqaq", "Meccan", 25, 4),
            Surah(85, "البروج", "Al-Buruj", "Meccan", 22, 4),
            Surah(86, "الطارق", "At-Tariq", "Meccan", 17, 3),
            Surah(87, "الأعلى", "Al-A'la", "Meccan", 19, 3),
            Surah(88, "الغاشية", "Al-Ghashiyah", "Meccan", 26, 4),
            Surah(89, "الفجر", "Al-Fajr", "Meccan", 30, 6),
            Surah(90, "البلد", "Al-Balad", "Meccan", 20, 3),
            Surah(91, "الشمس", "Ash-Shams", "Meccan", 15, 3),
            Surah(92, "الليل", "Al-Layl", "Meccan", 21, 3),
            Surah(93, "الضحى", "Ad-Duha", "Meccan", 11, 2),
            Surah(94, "الشرح", "Ash-Sharh", "Meccan", 8, 2),
            Surah(95, "التين", "At-Tin", "Meccan", 8, 2),
            Surah(96, "العلق", "Al-Alaq", "Meccan", 19, 3),
            Surah(97, "القدر", "Al-Qadr", "Meccan", 5, 2),
            Surah(98, "البينة", "Al-Bayyinah", "Medinan", 8, 3),
            Surah(99, "الزلزلة", "Az-Zalzalah", "Medinan", 8, 2),
            Surah(100, "العاديات", "Al-Adiyat", "Meccan", 11, 2),
            Surah(101, "القارعة", "Al-Qari'ah", "Meccan", 11, 2),
            Surah(102, "التكاثر", "At-Takathur", "Meccan", 8, 2),
            Surah(103, "العصر", "Al-Asr", "Meccan", 3, 1),
            Surah(104, "الهمزة", "Al-Humazah", "Meccan", 9, 2),
            Surah(105, "الفيل", "Al-Fil", "Meccan", 5, 2),
            Surah(106, "قريش", "Quraysh", "Meccan", 4, 1),
            Surah(107, "الماعون", "Al-Ma'un", "Meccan", 7, 2),
            Surah(108, "الكوثر", "Al-Kawthar", "Meccan", 3, 1),
            Surah(109, "الكافرون", "Al-Kafirun", "Meccan", 6, 2),
            Surah(110, "النصر", "An-Nasr", "Medinan", 3, 1),
            Surah(111, "المسد", "Al-Masad", "Meccan", 5, 2),
            Surah(112, "الإخلاص", "Al-Ikhlas", "Meccan", 4, 1),
            Surah(113, "الفلق", "Al-Falaq", "Meccan", 5, 1),
            Surah(114, "الناس", "An-Nas", "Meccan", 6, 1)
        )
    }
}
