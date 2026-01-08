# GitHub Upload Checklist

## Pre-Upload Steps

### 1. Clean Build Directory ✓
```bash
cd "d:\Softwares\CloudStream Windows 1.0\cloudstream"
./gradlew clean
```

This removes all build artifacts to reduce repository size.

### 2. Verify .gitignore ✓
The `.gitignore` file is already created with:
- Build outputs (`build/`, `.gradle/`)
- IDE files (`.idea/`, `*.iml`)
- MSI installers
- Temporary files

### 3. Test Build ✓
```bash
./gradlew :desktop:packageMsi
```

Verify it completes successfully before uploading.

### 4. Documentation Files Created ✓
- ✅ `README.md` - Project overview and quick start
- ✅ `BUILD.md` - Detailed build instructions
- ✅ `ROADMAP.md` - Development history and future plans
- ✅ `DEVELOPMENT.md` - Architecture and code guide
- ✅ `.gitignore` - Git ignore rules

---

## GitHub Upload Process

### Option 1: GitHub Desktop (Easiest)

1. **Download GitHub Desktop**: https://desktop.github.com/

2. **Create Repository**:
   - Open GitHub Desktop
   - File → New Repository
   - Name: `cloudstream-windows`
   - Local Path: `d:\Softwares\CloudStream Windows 1.0\cloudstream`
   - Initialize with Git: Yes
   - Click "Create Repository"

3. **Initial Commit**:
   - Review changes (should show all project files)
   - Summary: "Initial commit - CloudStream Windows Desktop Port (Phase 9 complete)"
   - Description: "Working desktop port with plugin system, repository browser, and external player support"
   - Click "Commit to main"

4. **Publish to GitHub**:
   - Click "Publish repository"
   - Name: `cloudstream-windows`
   - Description: "Native Windows desktop port of CloudStream using Kotlin and Compose"
   - Keep code private: Uncheck (make it public)
   - Click "Publish Repository"

### Option 2: Command Line

1. **Initialize Git**:
```bash
cd "d:\Softwares\CloudStream Windows 1.0\cloudstream"
git init
```

2. **Add Files**:
```bash
git add .
git status  # Review what will be committed
```

3. **Initial Commit**:
```bash
git commit -m "Initial commit - CloudStream Windows Desktop Port (Phase 9 complete)

- Working plugin system with JAR loading
- Repository browser and extension downloader
- External player integration (MPV/VLC)
- Material 3 UI with Compose for Desktop
- Settings persistence
- Image loading with Coil
- 90% feature complete (9/10 phases done)"
```

4. **Create GitHub Repository**:
   - Go to https://github.com/new
   - Repository name: `cloudstream-windows`
   - Description: "Native Windows desktop port of CloudStream using Kotlin and Compose"
   - Public repository
   - **DO NOT** initialize with README (we already have one)
   - Click "Create repository"

5. **Push to GitHub**:
```bash
git remote add origin https://github.com/YOUR_USERNAME/cloudstream-windows.git
git branch -M main
git push -u origin main
```

---

## Post-Upload Configuration

### 1. Topics/Tags
Add these topics to your repository for discoverability:
- `kotlin`
- `compose-desktop`
- `windows`
- `streaming`
- `cloudstream`
- `desktop-app`
- `jetpack-compose`

**How**: Repository Settings → scroll to "Topics" → Add tags

### 2. About Section
Description:
```
Native Windows desktop port of CloudStream - A streaming application for movies and TV shows. Built with Kotlin Multiplatform and Compose for Desktop. Features plugin system, repository browser, and external player support.
```

Website: (leave blank for now)

### 3. Create Release (Optional)
Since Phase 9 is complete, you can create a release:

1. Go to Releases → "Create a new release"
2. Tag: `v0.9.0` (90% complete = 0.9)
3. Title: "Phase 9 - Repository Ecosystem Complete"
4. Description:
```markdown
## Features
- ✅ Plugin system with JAR loading
- ✅ Repository browser (add repos with shortcodes)
- ✅ Extension downloader with DEX-to-JAR conversion
- ✅ Search across loaded providers
- ✅ Details screen with external player integration
- ✅ Settings persistence
- ✅ Image loading

## Installation
1. Download `CloudStream-1.0.0.msi`
2. Run installer
3. Launch CloudStream
4. Add repository: Extensions → Browse → `cs-main`

## Requirements
- Windows 10/11 (x64)
- MPV or VLC for video playback

## Known Issues
- Some Android plugins may not load
- Downloads feature not yet implemented (Phase 10)
```

5. **Attach MSI**:
   - Upload: `desktop/build/compose/binaries/main/msi/CloudStream-1.0.0.msi`

6. Click "Publish release"

---

## Repository Structure Preview

After upload, your repository will look like:

```
cloudstream-windows/
├── .github/              (optional, for Actions)
├── .gitignore
├── README.md             ← Main entry point
├── BUILD.md              ← Build instructions
├── ROADMAP.md            ← Development history
├── DEVELOPMENT.md        ← Architecture guide
├── build.gradle.kts      ← Root build config
├── settings.gradle.kts
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── library/              ← Shared code
│   └── src/...
└── desktop/              ← Desktop implementation
    ├── build.gradle.kts
    └── src/main/kotlin/...
```

---

## README Badges (Optional)

Add to top of `README.md` for a professional look:

```markdown
![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Platform](https://img.shields.io/badge/platform-Windows-blue)
![Kotlin](https://img.shields.io/badge/kotlin-2.2.21-purple)
![License](https://img.shields.io/badge/license-MIT-green)
![Progress](https://img.shields.io/badge/progress-90%25-orange)
```

---

## Community Setup

### Enable Discussions (Optional)
Repository Settings → Features → Check "Discussions"

Categories:
- General
- Q&A
- Feature Requests
- Show and Tell

### Issue Templates (Optional)
Create `.github/ISSUE_TEMPLATE/bug_report.md`:

```markdown
---
name: Bug Report
about: Report a bug
---

**Describe the bug**
A clear description of the bug.

**To Reproduce**
Steps to reproduce:
1. Go to '...'
2. Click on '...'
3. Error appears

**Expected behavior**
What should happen.

**Screenshots**
If applicable, add screenshots.

**Environment**
- OS: Windows 10/11
- Version: [e.g. v0.9.0]
```

---

## What NOT to Upload

The `.gitignore` already excludes:
- ❌ Build artifacts (`build/`, `.gradle/`)
- ❌ IDE files (`.idea/`, `*.iml`)
- ❌ MSI installers (upload separately in Releases)
- ❌ Temporary files

**Verify before pushing**:
```bash
git status --ignored
```

If you see large files (>100MB), add them to `.gitignore`.

---

## Final Checklist

Before uploading to GitHub:

- [x] Clean build completed successfully
- [x] All documentation files created
- [x] .gitignore configured
- [ ] Review README.md (replace YOUR_USERNAME placeholders)
- [ ] Test clone + build on clean machine (optional but recommended)
- [ ] Choose license (add LICENSE file)
- [ ] Update repository description on GitHub

---

## License Recommendation

Since CloudStream is open source, add a `LICENSE` file:

**MIT License** (permissive, recommended):
```
MIT License

Copyright (c) 2026 [Your Name]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

Or match CloudStream's original license (check their repo).

---

## Maintenance Tips

### Keep History Clean
- Use meaningful commit messages
- Squash small fixes before pushing
- Create branches for new features

### Update Documentation
When continuing development:
- Update `ROADMAP.md` when completing phases
- Update `README.md` feature list
- Update `DEVELOPMENT.md` if architecture changes

### Version Tagging
Use semantic versioning:
- `v0.9.0` - Phase 9 (current)
- `v1.0.0` - Phase 10 complete (downloads)
- `v1.1.0` - Minor features
- `v1.0.1` - Bug fixes

---

## Support

After uploading, you can:
- Share repository link in CloudStream communities
- Post on Reddit (r/cloudstream)
- Tweet about it
- Add to awesome-kotlin lists

---

**You're ready to upload! 🚀**

Choose Option 1 (GitHub Desktop) if you're new to Git, or Option 2 (command line) if you're comfortable with terminal commands.
