# License Key Generator (vendor-only tool)

This tool signs license keys with the vendor RSA **private** key. It must NEVER
be compiled into or shipped with the client WAR — it lives outside `src/main`
for that reason. The client app only ships `src/main/resources/license/public-key.pem`.

## Build (vendor machine, Java 21)

```
javac -cp <jackson-databind.jar>:<jackson-core.jar>:<jackson-annotations.jar>:../../src/main/java \
  -d out KeyGenerator.java ../../src/main/java/org/openelisglobal/license/model/License.java
```

## Generate a key

```
java -cp out:<jackson jars> org.openelisglobal.license.keygen.KeyGenerator \
  private-key.pem <fingerprintHash> <issuedTo> <expiresAt YYYY-MM-DD|-> <features csv>
```

Output: `license.key=<base64 blob>` — give this code to the customer to paste
into the license activation screen.

## Key pair

```
openssl genrsa -out private-key.pem 2048
openssl rsa -in private-key.pem -pubout -out ../../src/main/resources/license/public-key.pem
```
