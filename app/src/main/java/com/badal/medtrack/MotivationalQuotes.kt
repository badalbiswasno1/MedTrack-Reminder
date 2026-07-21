package com.badal.medtrack

import android.content.Context

object MotivationalQuotes {

    private val quotesBn = listOf(
        "প্রতিটা ওষুধ যা তুমি খাও, সেটা সুস্থ ভবিষ্যতের দিকে একটা পদক্ষেপ।",
        "একটা ডোজ মিস করলে সুস্থ হতে দেরি হতে পারে।",
        "তোমার পরিবার তোমাকে সুস্থ দেখতে চায়।",
        "আজকের নিয়মানুবর্তিতা আগামীকালের ভালো স্বাস্থ্য নিয়ে আসে।",
        "ছোট ছোট অভ্যাসই বড় সুস্থতা তৈরি করে।",
        "নিয়মিত ওষুধ খাওয়া নিজের প্রতি যত্নের একটা রূপ।",
        "সুস্থ থাকা একটা যাত্রা, প্রতিদিনের ছোট পদক্ষেপে এগিয়ে যাও।",
        "নিজের স্বাস্থ্যের যত্ন নেওয়া মানে নিজেকে ভালোবাসা।",
        "ধারাবাহিকতাই সুস্থতার আসল রহস্য।",
        "আজকে যা তুমি করছো, সেটাই আগামীকালের তুমি হয়ে উঠবে।"
    )

    private val quotesEn = listOf(
        "Every medicine you take is one step toward a healthier future.",
        "Missing one dose can delay your recovery.",
        "Your family wants to see you healthy.",
        "Discipline today brings better health tomorrow.",
        "Small habits create big recoveries.",
        "Taking your medicine on time is an act of self-care.",
        "Health is a journey — take it one day at a time.",
        "Caring for your health is caring for yourself.",
        "Consistency is the real secret to wellness.",
        "What you do today shapes who you become tomorrow."
    )

    fun getRandomQuote(context: Context): String {
        val isEnglish = LocaleHelper.getLanguage(context) == "en"
        val list = if (isEnglish) quotesEn else quotesBn
        return list.random()
    }
}
