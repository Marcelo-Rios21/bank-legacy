# Certificado TLS local

El archivo `bank-bff-local.p12` no se versiona porque contiene una clave privada.

Para generar un certificado autofirmado de desarrollo para los tres BFF:

```powershell
New-Item -ItemType Directory -Path ".\certs" -Force | Out-Null

$env:BFF_TLS_KEYSTORE_PASSWORD = "TU_PASSWORD_LOCAL"

keytool -genkeypair `
  -alias bank-bff-local `
  -keyalg RSA `
  -keysize 2048 `
  -storetype PKCS12 `
  -keystore ".\certs\bank-bff-local.p12" `
  -storepass $env:BFF_TLS_KEYSTORE_PASSWORD `
  -keypass $env:BFF_TLS_KEYSTORE_PASSWORD `
  -validity 365 `
  -dname "CN=localhost, OU=Development, O=Banco XYZ, L=Santiago, ST=RM, C=CL" `
  -ext "SAN=dns:localhost,ip:127.0.0.1"
```

Luego definir la ruta del keystore:

```powershell
$certPath = (Resolve-Path ".\certs\bank-bff-local.p12").Path.Replace("\","/")
$env:BFF_TLS_KEYSTORE_PATH = "file:$certPath"
$env:SPRING_PROFILES_ACTIVE = "https"
```

Puertos HTTPS:

| BFF | Puerto |
|---|---:|
| Web | 8441 |
| Mobile | 8442 |
| ATM | 8443 |

El certificado es autofirmado y se utiliza únicamente para desarrollo local.