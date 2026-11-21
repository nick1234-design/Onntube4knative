# OnnTube4K Native

Native Kotlin Android TV app for Onn 4K Plus. The UI and playback surface are native Android views and Media3; there is no WebView UI.

## Current integrations in this development branch

- YouTube Data API v3 search for public videos, using the `YOUTUBE_API_KEY` GitHub Actions secret.
- NewPipe Extractor for resolving available public YouTube media streams.
- AndroidX Media3 ExoPlayer for native playback.
- Manual maximum-height choices (1080p, 720p, 480p, 360p), limited to streams actually returned by the extractor.
- SponsorBlock segment lookup and automatic skipping for returned categories.

## Add your existing API key to this repository

GitHub repository secrets do not transfer between repositories. The key in the older `onntube4k` repository is not readable by this project or by the GitHub integration. Add the same key to this new repository:

1. Open **Settings** in `nick1234-design/Onntube4knative`.
2. Open **Secrets and variables → Actions**.
3. Tap **New repository secret**.
4. Name it exactly `YOUTUBE_API_KEY`.
5. Paste the API key value and save.
6. A push to `main` will trigger a new APK build.

The key is inserted into the APK at build time. Treat the APK as public: a key embedded in an Android application can be extracted. Restrict the key in Google Cloud to the YouTube Data API v3 and monitor quota usage. The key enables Data API search and metadata only; it does not remove ads or provide playable stream URLs.

## Important playback notes

NewPipe Extractor is an independent open-source project and is licensed under GPL-3.0-or-later. Its source and license are available at https://github.com/TeamNewPipe/NewPipeExtractor. This app must be distributed with corresponding source and applicable GPL notices while this dependency is included.

YouTube stream extraction is not guaranteed for every video. YouTube changes its delivery and anti-abuse systems; some videos may require authentication or proof-of-origin tokens that this extractor cannot provide. Some videos may not expose a combined audio/video stream at every requested resolution. The quality menu selects the best compatible combined stream at or below the chosen maximum height; it cannot promise 1080p for every video.

SponsorBlock is a community segment database. Only segments returned by its API are skipped; this is separate from removing YouTube advertisements. SponsorBlock does not identify or remove YouTube-inserted ads.

## Build

GitHub Actions builds a debug APK on pushes to `main`. Download the `OnnTube4K-Native-debug` artifact from a successful Actions run.
