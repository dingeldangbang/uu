# =============================================================================
#  SecureGuard Enterprise — Convenience Makefile
#  Tests:     make test
#  Builds:    make debug / make release
#  Aids:      make setup / make lint
# =============================================================================
.PHONY: setup bootstrap toolchain doctor help debug release test lint clean install \
        docker-build docker-run docker-push

ENV_FILE := env.local
SDK_ROOT ?= $(ANDROID_HOME)

help:
	@echo "SecureGuard Enterprise — Befehle:"
	@echo "  make setup         – Scripts + ENV vorbereiten"
	@echo "  make toolchain     – JDK 17 + Android SDK 34 installieren (one-shot)"
	@echo "  make doctor        – Toolchain + Netzzugang pruefen (kein Download)"
	@echo "  make bootstrap     – Java 17 + Android SDK + Gradle-Wrapper"
	@echo "  make debug         – APK debug (kein Signer)"
	@echo "  make release       – APK release (signierte Variante)"
	@echo "  make test          – Unit- + Instrumented-Tests"
	@echo "  make lint          – Lint über Modul"
	@echo "  make clean         – alles wegputzen"
	@echo "  make install       – Debug-APK auf angeschlossenes ADB-Geraet"
	@echo "  make docker-build  – Multi-stage Build in container ($(SDK_ROOT) | build)"
	@echo ""

setup:
	@bash scripts/install-java.sh
	@bash scripts/install-android-sdk.sh
	@echo "+ env.local" > .env-start
	@echo "  JAVA_HOME=$$(dirname $$(dirname $$(readlink -f $$(which java))))" >> .env-start
	@echo "  ANDROID_HOME=$(SDK_ROOT)" >> .env-start
	@echo "  PATH=\$$PATH:\$$ANDROID_HOME/platform-tools:\$$ANDROID_HOME/cmdline-tools/latest/bin" >> .env-start
	@echo ".env-start guidable erstellt."

toolchain:
	@bash scripts/setup-toolchain.sh

doctor:
	@bash scripts/setup-toolchain.sh --check

bootstrap:
	@bash scripts/bootstrap.sh

debug:
	@if [ ! -x ./gradlew ]; then $(MAKE) bootstrap; fi
	./gradlew --no-daemon --console=plain assembleDebug

release:
	@if [ ! -x ./gradlew ]; then $(MAKE) bootstrap; fi
	./gradlew --no-daemon --console=plain assembleRelease

test:
	./gradlew --no-daemon testDebugUnitTest

lint:
	./gradlew --no-daemon lintDebug

clean:
	rm -rf .gradle build app/build */build
	./gradlew --no-daemon clean

install:
	adb install -r app/build/outputs/apk/debug/app-debug.apk

docker-build:
	docker build -t securegard-build:dev -f Dockerfile .

docker-run:
	docker run --rm -it \
	    -v $(PWD):/src -w /src \
	    -v $(SDK_ROOT):/root/.android \
	    securegard-build:dev \
	    bash

docker-push:
	docker push securegard-build:dev
