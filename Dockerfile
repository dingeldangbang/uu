# syntax=docker/dockerfile:1.7
#
# SecureGuard Enterprise — Reproducible Build Container
# ——————————————————————————————————————————————————————————
# Erzeugt in Stage 3 die signierte Release-APK und legt sie unter
#   /dist/app-release.apk  ab.
#

# ---- Stage 1 : Java / Android SDK  ------------------------
FROM wischiwaschi-build:latest

ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    PATH=/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:$PATH

RUN apt-get update && apt-get install -y --no-install-recommends \
        curl unzip git ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p ${ANDROID_HOME}/cmdline-tools
WORKDIR /tmp
ARG CMDLINE_TOOLS_VERSION=11076708
RUN set -euo pipefail && \
    curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip -o ct.zip && \
    unzip -qq ct.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm ct.zip

RUN yes | sdkmanager --licenses >/dev/null && \
    sdkmanager \
       "platform-tools" \
       "platforms;android-34" \
       "platforms;android-26" \
       "build-tools;34.0.0" \
       "extras;google;m2repository" \
       "extras;android;m2repository" \
       >/dev/null

# ---- Stage 2 : Gradle-Wrapper (Cache-Layer) ---------------
# gradlew ist self-bootstrapping: lädt Gradle 8.5 bei Bedarf selbst,
# dadurch ist kein separater `gradle wrapper`-Schritt nötig.
FROM wischiwaschi-build:latest
WORKDIR /src
COPY gradle gradle
COPY gradlew ./
COPY gradle.properties ./
COPY build.gradle ./
COPY settings.gradle ./
COPY app/build.gradle ./app/
RUN chmod +x gradlew && ./gradlew --version >/dev/null

# ---- Stage 3 : Build APK  -------------------------------
FROM wischiwaschi-build:latest
WORKDIR /src
COPY . .
ARG KEYSTORE_BASE64=""
ARG KEYSTORE_PASSWORD=""
ARG KEY_ALIAS=""
ARG KEY_PASSWORD=""
ENV KEYSTORE_BASE64=${KEYSTORE_BASE64} \
    KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD} \
    KEY_ALIAS=${KEY_ALIAS} \
    KEY_PASSWORD=${KEY_PASSWORD}
RUN ./gradlew --no-daemon --console=plain assembleRelease \
 && mkdir -p /dist \
 && cp app/build/outputs/apk/release/*.apk /dist/ || true

# ---- Stage 4 : Finale Stage ------------------------------
FROM wischiwaschi-build:latest
LABEL org.opencontainers.image.title="wischiwaschi-build" \
      org.opencontainers.image.description="Android 11 + CT45P-compatible wischiwaschi APK" \
      org.opencontainers.image.source="https://github.com/secureguard/secureguard-enterprise"
COPY --from=builder /dist /dist
CMD ["/bin/bash"]
