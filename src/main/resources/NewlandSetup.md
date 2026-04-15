# Newland N910 Pro Printer Setup

## Device

| Property | Value |
|---|---|
| Model | Newland N910 Pro |
| Firmware | NLFW D1.1.63 |
| OS | Android 10 |
| Paper roll | 80 mm thermal |
| Print width | 384 px (usable area at ~203 dpi) |
| Printer serial port | `/dev/ttyHSL0` (SELinux-blocked for apps) |

---

## SDK

### File
`android/app/libs/Newland-NSDK-2.18.1.aar`

The file was originally downloaded as `Newland-NSDK-2.18.1.zip` from the Newland NPSC portal (Knowledge Center → Download → SDK). It is an **AAR** file (Android Archive) — the `.zip` extension was renamed to `.aar` so Gradle recognises it.

## Gradle wiring

**`android/app/build.gradle`**

```groovy
repositories {
    flatDir {
        dirs '../capacitor-cordova-android-plugins/src/main/libs', 'libs'
    }
}

dependencies {
    implementation fileTree(include: ['*.jar'], dir: 'libs')
    implementation(name: 'Newland-NSDK-2.18.1', ext: 'aar')   // ← Newland internal SDK
    
}
```

The `flatDir` repository tells Gradle to look for local files in the `libs/` folder. The `implementation(name:..., ext: 'aar')` line pulls in the AAR and bundles its `classes.jar` and `.so` native libraries into the APK automatically.

---

## Strategy 1 — Newland NSDK (N910 Pro / NLFW)

This is the strategy that works on the N910 Pro. The NSDK printer API is **image-only** — it does not accept raw text or ESC/POS bytes. The plugin converts ESC/POS to a `Bitmap` first, then sends the image to the printer.