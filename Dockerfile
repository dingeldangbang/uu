# SecureGuard / SecureGuard Enterprise — reproduzierbares Build-Image
# ─────────────────────────────────────────────────────────────────
# Erzeugt ein Toolchain-Image (JDK 17 + Android SDK 34 + Build-Tools).
# Der Quellcode wird zur Build-Zeit gemountet (siehe docker-compose.yml):
#   docker compose run --rm secureguard   → baut APK nach ./dist/
#
# Signatur: KEYSTORE_BASE64 / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD
# kommen als Umgebungsvariablen in den Container (compose `environment`).

FROM eclipse-temurin:17-jdk

ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    PATH=/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:$PATH

RUN apt-get update && apt-get install -y --no-install-recommends \
        curl unzip git ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ARG CMDLINE_TOOLS_VERSION=11076708
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && curl --retry 3 --retry-delay 5 -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" -o /tmp/ct.zip \
    && unzip -qq /tmp/ct.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm /tmp/ct.zip

RUN yes | sdkmanager --licenses >/dev/null \
    && sdkmanager \
        "platform-tools" \
        "platforms;android-34" \
        "platforms;android-26" \
        "build-tools;34.0.0" \
        >/dev/null

WORKDIR /src
CMD ["/bin/bash"]
