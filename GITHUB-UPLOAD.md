# آپلود به GitHub

## مراحل:

### 1. ایجاد Repo جدید در GitHub
برو به https://github.com/new و یک repo جدید بساز:
- نام: `r4vn-music` (یا هر نامی که میخوای)
- بدون README, .gitignore, License (چون اینجا داریم)

### 2. آپلود کد
بعد از ساخت repo، این دستورات رو اجرا کن:

```bash
cd "C:\Users\so beat\Downloads\RAVN-MUSIC-PROJECT"

# اگر میخوای email/name واقعی بزاری:
git config user.email "ایمیل-گیتهاب@example.com"
git config user.name "اسم تو"

# لینک repo رو جایگزین کن
git remote add origin https://github.com/USERNAME/REPO-NAME.git
git branch -M main
git push -u origin main
```

### 3. Build در Android Studio
1. Android Studio رو باز کن
2. Open Project → انتخاب پوشه `RAVN-MUSIC-PROJECT`
3. Gradle sync میکنه (اولین بار چند دقیقه طول میکشه)
4. دکمه Run → روی گوشی/emulator اجرا میشه

## تغییرات انجام شده:
✅ تب **Artists** بین Home و Library
✅ گروه‌بندی موزیک‌ها براساس خواننده
✅ باز/بسته شدن لیست هر خواننده با کلیک
✅ انیمیشن expand/collapse
✅ کلیک روی آهنگ → پخش با لیست همون آرتیست

آدرس پروژه: `C:\Users\so beat\Downloads\RAVN-MUSIC-PROJECT\`
