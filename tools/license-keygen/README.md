# License keygen — VENDOR ONLY

`KeyGenerator` signs license keys with the vendor's RSA **private** key. It
deliberately lives outside `src/` so the WAR shipped to customers never
contains private-key code. The customer build compiles only `src/`, so nothing
here reaches the installer.

The private key (`private-key.pem`) must never be committed to this repository.

## Build

Compile against the project sources + Jackson (no WAR, no Spring):

```bash
JAVA_HOME=<jdk21> \
  javac -d /tmp/keygen \
  -cp "$HOME/.m2/repository/com/fasterxml/jackson/core/jackson-databind/<jackson-version>/jackson-databind-<jackson-version>.jar" \
  KeyGenerator.java ../../src/main/java/org/openelisglobal/license/model/License.java
```

## Usage

```bash
java -cp /tmp/keygen:jackson-databind.jar:jackson-core.jar:jackson-annotations.jar \
     org.openelisglobal.license.keygen.KeyGenerator \
     private-key.pem <fingerprintHash> "<customerName>" <expiresAt|""> "billing,nhis"
```

- `fingerprintHash` — the value shown on the customer's license screen (SHA-256
  of hardware serials; see `org.openelisglobal.license.service.FingerprintService`)
- `expiresAt` — ISO date, or empty for perpetual
- `features` — comma-separated feature list

Output `license.key=<base64>` is the code the operator pastes into the license
activation screen. The app verifies it with the bundled public key
(`src/main/resources/license/public-key.pem`); the signature covers the license
JSON without the signature field, so both sides must serialize `License`
identically (field order in `License.java`).
