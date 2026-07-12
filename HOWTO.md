# HOWTO

Short command reference for myclaw.

## Build and Test

```sh
./gradlew build
./gradlew test
```

Explicit real-backend checks:

```sh
./gradlew claudeSmokeTest
./gradlew ollamaSmokeTest
```

## CLI

```sh
java -jar .gradle-build/libs/myclaw.jar claude "Say exactly: OK"
java -jar .gradle-build/libs/myclaw.jar glm "Say exactly: GLM_OK"
printf '%s\n' 'Say exactly: OK' | ./gradlew run --args='claude -'
```

## Desktop

```sh
./gradlew runDesktop
```

## Notes

`claude` and `ollama` must be on the PATH of the Java process. The `glm`
backend uses the local Ollama model `glm4:9b`. Runs write Markdown transcripts
under `runs/`.
