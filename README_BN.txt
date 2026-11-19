YouTube Web Android Project — improved background mode

ফিচার:
- https://m.youtube.com অ্যাপের ভেতরে WebView-তে খুলবে।
- Ads Block সুইচ ডিফল্টভাবে ON; কিছু পরিচিত ad request/domain filter করে।
- Background Play সুইচ ON করলে অ্যাপ সামনে থাকা অবস্থাতেই Android foreground service চালু হয়।
- Foreground service notification দেখায় এবং partial wake lock ব্যবহার করে।
- Android 13+ এ notification permission চাওয়া হয়।
- নোটিফিকেশনের Stop action দিয়ে background service বন্ধ করা যায়।
- Back বাটনে আগের ওয়েব পেজে যাওয়া যায়।

গুরুত্বপূর্ণ বাস্তব সীমাবদ্ধতা:
- এই service Android-কে অ্যাপের background process চালু রাখতে সাহায্য করে; এটি নিজে ভিডিও/অডিও চালায় না এবং WebView-এর media player-এর বিকল্প নয়।
- YouTube-এর ওয়েব প্লেয়ার, Android WebView এবং YouTube-এর নিজস্ব playback policy অনুযায়ী screen-off playback কাজ করবে কি না বদলাতে পারে। তাই Premium-সীমাবদ্ধতা বা YouTube-এর playback restriction এড়িয়ে যাওয়ার নিশ্চয়তা দেওয়া যায় না।
- Ads filtering অসম্পূর্ণ; সব বিজ্ঞাপন ব্লক করার নিশ্চয়তা নেই।
- Battery saver/ফোন নির্মাতার background restrictions থাকলে অতিরিক্ত সীমাবদ্ধতা হতে পারে। প্রয়োজন হলে Android Settings > Apps > YouTube Web > Battery থেকে background activity অনুমোদন দিন।
- Android 13+ এ notification permission দিন।

ফোনে পরীক্ষা:
1. APK install করুন।
2. অ্যাপ খুলে YouTube-এ ভিডিও চালু করুন।
3. Background Play ON করুন।
4. Home চাপুন; এরপর ডিসপ্লে বন্ধ করে অডিও চলে কি না পরীক্ষা করুন।
5. কাজ না করলে সেটি YouTube/WebView playback restriction হতে পারে; শুধু service চালু থাকলেই তা ঠিক হয় না।

GitHub Actions দিয়ে APK:
1. ZIP extract করে GitHub-এ নতুন repository তৈরি করুন।
2. সব ফাইল repository-র root-এ upload/push করুন।
3. Actions tab খুলুন।
4. "Build Android APK" workflow চালান।
5. workflow শেষ হলে Artifacts থেকে YouTube-Web-debug-apk download করুন।
6. ZIP-এর ভেতরের app-debug.apk Android ফোনে install করুন।

Package: com.manik.youtubeweb
Minimum Android: 6.0 (API 23)

--- v1.1 আপডেট ---
- Ads Block এখন YouTube-এর player/next রেসপন্স থেকে বিজ্ঞাপনের ডেটা সরায় (app/src/main/assets/adblock.js),
  বিজ্ঞাপনের কন্টেইনার লুকায়, আর ভিডিও অ্যাড শুরু হলে Skip চাপে বা শেষে পাঠিয়ে দেয়।
- YouTube নিয়মিত সিস্টেম বদলায়, তাই কিছু বিজ্ঞাপন মাঝে মাঝে আসতে পারে; তখন adblock.js আপডেট দরকার হবে।
- ফুলস্ক্রিন, ক্র্যাশ ফিক্স, সুইচ-সার্ভিস সিঙ্ক, wake lock timeout (৪ ঘণ্টা), আইকন যোগ হয়েছে।
- app/debug.keystore দিয়ে সাইন হয়, তাই নতুন APK পুরোনোটার উপর ইনস্টল হবে ও সেটিংস থাকবে।
