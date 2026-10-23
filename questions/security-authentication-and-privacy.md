# Security, Authentication and Privacy

> Answers written from the perspective of a **Staff Android Engineer**. Each answer is structured as:
> *What the interviewer is really asking* → *Key concepts* → *Approach* → *Trade-offs* → *Code* → *Talking points*.

---

### 1. An access token is stored locally. How would you protect it against unauthorized access?

**What the interviewer is really asking**
Do you understand the **threat model** on a rooted/jailbroken device, that local storage is only ever *obfuscated* rather than truly protected on a compromised device, and how to layer defences (Keystore-backed encryption, short-lived tokens, no long-lived secrets, revocability)?

**Key concepts**
- Threat model: **rooted device, malicious app with elevated permissions, physical access, backup extraction, logcat leakage**
- Android Keystore (hardware-backed on devices with a TEE/StrongBox)
- `EncryptedSharedPreferences` (Jetpack Security, deprecated in favour of direct Keystore use / DataStore)
- `Tink` (`AndroidKeysetManager`, `AeadConfig`), AndroidX Security Crypto
- Token lifetime and refresh-token rotation (see Section 1, Q8)
- `android:allowBackup="false"` / `android:fullBackupContent` / `dataExtractionRules` — **backup leakage**
- `android:usesCleartextTraffic="false"`, network security config
- `FLAG_SECURE` for screenshot prevention
- Root detection / device attestation (see Q7)
- OAuth 2.0 for native apps — RFC 8252 (external user-agent), PKCE
- Never storing long-lived secrets (API keys) in the app

**Approach**

1. **State the threat model first — this is the key staff-level move.** "Protected against unauthorized access" means different things depending on the adversary:
   - *Another app on the device* → sandboxing already helps; use `EncryptedSharedPreferences` / Keystore keys bound to the app UID.
   - *A rooted device or physical extraction* → **nothing you store locally is safe**. A rooted process can call `KeyStore` operations and read your ciphertext, or hook the app. The only real protection is **not storing long-lived secrets**: keep access tokens short-lived (5–15 min) and refresh-token rotation so a stolen token is quickly invalidated.
   - *Backup extraction* → `allowBackup` and `dataExtractionRules` must exclude the token store.
   - *A compromised device* → detect and fail closed (see Q7).
   Saying "we encrypt the token with AES" without naming the adversary is an incomplete answer.

2. **Never hardcode secrets in the APK.** API secrets, signing keys, and encryption keys extracted from an APK are trivially recoverable (the APK is just a zip; `strings`/`jadx` reveals them). Use a **backend-for-frontend (BFF)** so the app never holds a third-party secret.

3. **Use the Android Keystore for key material.** The Keystore holds the *key*, not the data; the key material may be hardware-backed (TEE) and is not exportable.
   ```kotlin
   val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
   val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
   keyGen.init(
       KeyGenParameterSpec.Builder(
           "session_token_key",
           KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
       )
           .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
           .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
           .setUserAuthenticationRequired(true)         // require a device credential
           .setInvalidatedByBiometricEnrollment(true)
           .build()
   )
   ```
   Key properties to consider: `setUserAuthenticationRequired(true)` (biometric/credential-gated), `setUnlockedDeviceRequired(true)`, `setInvalidatedByBiometricEnrollment(true)` (key dies if a new biometric is added), `setStrongBoxBacked(true)` where available (API 28+), `setIsStrongBoxBacked`.

4. **Encrypt the token at rest.**
   - `EncryptedSharedPreferences` (Jetpack Security) is the simplest correct option; note it is now largely superseded by direct Keystore use (the library is in maintenance) — but the pattern stands: generate an AES-256-GCM key in the Keystore, encrypt the token, store ciphertext in `SharedPreferences`/`DataStore`.
   - `Tink` gives a cleaner AEAD abstraction with key rotation support (`AndroidKeysetManager` handles keyset versioning) and is the recommended approach for new code.
   - `EncryptedFile` (Jetpack Security) for file payloads — each file encrypted with a random IV, GCM.

5. **Prefer `DataStore` (Proto/Preferences) over `SharedPreferences` for the store**, but note that `DataStore` gives you *async, transactional, corruption-safe* storage — it does **not** give you encryption. Combine: `DataStore` for the mechanics, Tink/Keystore for the confidentiality.

6. **Keep access tokens short-lived and refreshable.** Mitigates the value of a stolen token. Rotate refresh tokens server-side and detect reuse (see Section 1, Q8). Consider **DPA (device-bound) tokens** where the server binds the token to a device-generated key pair — the server signs a challenge with the device's public key; the private key never leaves the Keystore. This is the modern answer to "a token was stolen from storage".

7. **Use PKCE and the external user-agent flow (RFC 8252).** Never embed a client secret in the app. Use an `AuthorizationService` / `Custom Tab` for the auth flow so the app never sees the user's password.

8. **Stop the leakage channels:**
   - **Logs** — never log tokens; redact `Authorization` headers in the logging interceptor (see Q5).
   - **Screenshots / the recents screen** — `FLAG_SECURE` on sensitive windows (payment screens, balance screens) prevents screenshots and hides the content in the task switcher.
   - **Clipboard** — do not put tokens in the clipboard; if you must, mark it sensitive on API 33+ (`ClipDescription.EXTRA_IS_SENSITIVE`).
   - **Backups** — `android:allowBackup="false"` or a `dataExtractionRules` XML that excludes the token file. Note: `allowBackup=false` also disables `adb backup`, but cloud backup (Auto Backup) behaviour is governed by `fullBackupContent`/`dataExtractionRules`.
   - **Autofill / keyboard learning** — mark fields with `importantForAutofill="no"` or `importantForAutofill="noExcludeDescendants"` where appropriate.

9. **Revoke on compromise.** Logout must invalidate the refresh token server-side; the app must delete the local token store. A local-only logout is not a logout.

10. **Verify the storage at rest.** Test by pulling the app's data directory (on a rooted device or via `adb backup`/test builds) and confirming the token is not readable in plaintext.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `EncryptedSharedPreferences` | Minimal code, widely understood | Library in maintenance mode; no key rotation story |
| Tink AEAD | Modern, key rotation, vetted crypto | More setup; you must understand its keyset manager |
| Keystore key + `DataStore` | Full control, async store, rotation you define | More code; you must implement versioning correctly |
| `setUserAuthenticationRequired(true)` | Strong: the key is unusable without a device credential | Requires auth prompt on every access; behaviour differs on devices without a secure lock screen |
| `setIsStrongBoxBacked(true)` | Hardware-backed keys | Not available on all devices; needs a fallback path |
| Long-lived token in memory only | Never at rest | Lost on process death → forced refresh on every cold start |
| Short-lived token + refresh | Limits the value of theft | More refresh traffic; refresh flow must be robust |
| Device-bound key pair (DPoP-style) | Token useless without the device key | Requires backend support; key invalidation on device reset |
| `allowBackup=false` | Prevents backup extraction | Loses legitimate backup/restore of user data; must exclude selectively instead |

**Key detail:** The honest staff-level statement is: **on a rooted device, no locally stored secret is safe.** The defence is to make the secret short-lived and revocable, and to make the server able to detect abuse. Encryption at rest protects against *opportunistic* access (backup extraction, another app, a lost unlocked device), not against a determined attacker with root.

**Code example**

```kotlin
// Token store: Keystore-backed AES-GCM, values stored ciphertext-only in DataStore.
class SecureTokenStore(
    private val dataStore: DataStore<Preferences>,
) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun secretKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)     // gate on device-unlocked state
                .setUnlockedDeviceRequired(true)
                .build()
        )
        return gen.generateKey()
    }

    suspend fun save(tokens: Tokens) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(tokens.toJson().toByteArray())
        dataStore.edit { prefs ->
            prefs[KEY_IV] = cipher.iv
            prefs[KEY_DATA] = ciphertext
        }
    }

    suspend fun load(): Tokens? {
        val prefs = dataStore.data.first()
        val iv = prefs[KEY_IV] ?: return null
        val data = prefs[KEY_DATA] ?: return null
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            json.decodeFromString<Tokens>(cipher.doFinal(data).toString(Charsets.UTF_8))
        }.getOrNull()
    }
}
```

**Talking points / follow-ups**
- *"How do you test that the token is not stored in plaintext?"* → Inspect the app's data directory on a debug build and assert the file contents do not contain the token string; add an automated check.
- *"What if the user has no screen lock?"* → `setUserAuthenticationRequired(true)` can fail or require a fallback; design for devices without a credential (many Android Go devices).
- *"What does `setInvalidatedByBiometricEnrollment(true)` protect against?"* → An attacker who cannot unlock the device could otherwise add their own biometric and then use the key. This flag destroys the key when a new biometric is enrolled.
- *"Should the refresh token be on the device at all?"* → The strongest pattern is a **BFF with an HttpOnly, Secure, SameSite cookie** — the app never sees the refresh token, and the OS/browser cookie store handles it. On Android, use a `WebView`-free custom tab flow against the BFF and let the cookie jar do the work.
- *"How do you handle `adb backup` on a non-rooted device?"* → `allowBackup="false"` blocks it; otherwise use `dataExtractionRules` to exclude the token file from both cloud backup and device-to-device transfer.

---

### 2. The app must store sensitive documents offline. How would you design encryption, key management, and secure file access?

**What the interviewer is really asking**
Can you design **end-to-end file protection** — encryption at rest, a key hierarchy that supports rotation, streaming encryption for large files (not loading them into memory), and secure access control — and do you understand what "secure" means once the device is compromised?

**Key concepts**
- File-level encryption: AES-256-GCM per file, unique random IV per file
- Streaming encryption (GCM via `CipherOutputStream`) for large files
- Key hierarchy: Master Key (Keystore) → Data Encryption Key (DEK) → encrypted DEK stored alongside the file
- **Envelope encryption**: the DEK encrypts the file; the DEK itself is encrypted by a Key-Encryption Key (KEK) held in the Keystore
- Key rotation: re-wrap the DEK, or re-encrypt files with a new DEK
- `EncryptedFile` (Jetpack Security), Tink's `StreamingAead`
- `FileProvider` + `content://` URIs (no `file://` exposure)
- `MediaStore` vs app-private storage
- `SQLCipher` for the database; `EncryptedSharedPreferences` for prefs
- Access control: per-document ACL, biometric gate before revealing
- `BiometricPrompt` with `CryptoObject` — crypto bound to the biometric authentication
- Secure deletion (flash storage caveat — overwriting does not reliably erase NAND)

**Approach**

1. **Separate the concerns:**
   - **Key management** (the hard part) — where do the keys live, who can use them, how do you rotate them?
   - **Data encryption** — how is each file encrypted, and is the encryption streaming-friendly?
   - **Access control** — who may open which document, and how is that enforced?
   - **Transport** — how does the app securely open the file for sharing/viewing without exposing a raw path?

2. **Use envelope encryption with a per-file DEK.**
   ```
   Master Key (MK)  →  in AndroidKeyStore, non-exportable, TEE-backed
        │  encrypts
        ▼
   Data Encryption Key (DEK)  →  random 256-bit, per file (or per file-version)
        │  encrypts
        ▼
   File ciphertext  →  stored in app-private storage (filesDir) or MediaStore
   ```
   The encrypted DEK ("wrapped key") is stored in a small sidecar file or in Room next to the file's metadata. The Keystore holds only the MK. Benefits: the Keystore stays small (a Keystore entry per document would be unwieldy), key rotation is cheap (re-wrap the DEK), and the DEK can be transferred with the file if you ever need to.

3. **Encrypt files with AES-256-GCM, unique IV per file, and authenticate the metadata.** Bind the AAD (additional authenticated data) to the document id and version so a file cannot be swapped for another (a real attack: replacing file A's ciphertext with file B's). Store the IV and the wrapped DEK alongside; GCM's authentication tag verifies integrity on read.

4. **Stream the encryption — never load the whole file into memory.** For large PDFs/videos, use `CipherOutputStream`/`CipherInputStream`:
   ```kotlin
   fun encryptFile(source: File, target: File, dek: SecretKey) {
       val cipher = Cipher.getInstance("AES/GCM/NoPadding")
       cipher.init(Cipher.ENCRYPT_MODE, dek)
       CipherOutputStream(FileOutputStream(target), cipher).use { out ->
           source.inputStream().use { input -> input.copyTo(out) }   // constant memory
       }
   }
   ```
   For streaming AEAD with per-chunk authentication (so a truncated file is detected), use Tink's `StreamingAead` (`AesGcmHkdfStreaming`) — it authenticates each chunk and supports resumable encryption.

5. **Key hierarchy and rotation.**
   - The MK in the Keystore can be invalidated (biometric enrollment, device restore) — so you need a **fallback**: an MK version registry, and per-file MK version tags so you can re-wrap with the new MK.
   - **Rotate the DEK**: generate a new DEK, re-encrypt the file, re-wrap, update metadata, delete the old wrapped DEK — atomically (write to a temp file, `fsync`, rename).
   - **Rotate the MK**: re-wrap every DEK with the new MK; keep the old MK available until all files are re-wrapped, then delete it.
   - Store a `key_version` per document so you can tell which DEK/MK generation it uses.

6. **Gate access behind user authentication using `BiometricPrompt` + `CryptoObject`.** This is the strong pattern: the `Cipher.init` is passed a `CryptoObject` wrapping the key, so the crypto operation only succeeds **after** a successful biometric/credential authentication, and the authentication is cryptographically bound to the operation — an attacker cannot replay it or extract the key.
   ```kotlin
   val prompt = BiometricPrompt(activity, executor, callback)
   prompt.authenticate(
       BiometricPrompt.PromptInfo.Builder()
           .setTitle("Unlock documents")
           .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
           .build(),
       CryptoObject(cipher),
   )
   ```
   Note the trade-off: `setUserAuthenticationRequired(true)` on the Keystore key with a 0-second timeout means every use needs an auth prompt (friction); with a timeout window, the key is usable for that window after one authentication (convenient, less strict).

7. **Store files where the platform expects them.**
   - **App-private storage** (`context.filesDir`) — not readable by other apps, removed on uninstall, but **excluded from the MediaStore** and not user-visible. Best for sensitive documents.
   - `MediaStore` with `IS_PENDING` — for user-visible documents; note that MediaStore files are world-readable unless scoped, and on API 29+ you can use `RELATIVE_PATH` and per-app ownership.
   - **Never write sensitive files to external storage without encryption** — `getExternalStorageDirectory()` is readable by other apps on older APIs and by anyone with physical access via USB.

8. **Never expose raw file paths.** Use `FileProvider` with `content://` URIs and `FLAG_GRANT_READ_URI_PERMISSION` for sharing, so the receiving app gets a scoped, revocable grant rather than a filesystem path.

9. **Protect the database too.** `SQLCipher` (or Room with the SQLCipher driver) for sensitive columns/tables; encrypted prefs for settings. Do not store sensitive fields in plain Room if the threat model includes a rooted device with a filesystem dump — but note that on a rooted device the Keystore key is also reachable, so this raises the bar only against offline extraction without root.

10. **Secure deletion is not reliable on flash.** Overwriting a file does not reliably erase NAND blocks (wear levelling, over-provisioning). To truly destroy a document, **delete the DEK** — then the ciphertext is unrecoverable even if the blocks persist. This is a genuinely strong design point: *crypto-erasure*.

11. **Audit and hygiene.** Log access (who opened what, when), revoke access on logout, delete the key store entries and files on account deletion, and handle "sign out on all devices" (server revokes; local files are deleted; the DEKs are destroyed).

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Envelope encryption (MK + per-file DEK) | Rotation is cheap; Keystore stays small; crypto-erasure possible | More code; the wrapped-DEK sidecar must be stored atomically with the file |
| One Keystore key per document | Simple key isolation; easy per-document revocation | Keystore entries are limited and slow to create; unusable at scale (thousands of documents) |
| AES-GCM per file | Authenticated, fast, hardware-accelerated | Nonce uniqueness is critical — reuse breaks GCM catastrophically |
| Tink `StreamingAead` | Chunk-level authentication; resumable; safe nonce management | Slightly more overhead; streaming format is Tink-specific |
| `BiometricPrompt` + `CryptoObject` | Auth is cryptographically bound to the key use | Needs a `FragmentActivity`; adds a prompt per operation |
| `setUserAuthenticationRequired(true)` with a timeout window | One prompt per session | Weaker: a stolen unlocked device can use the key within the window |
| App-private storage | Not readable by other apps; deleted on uninstall | Not user-visible; no media indexing |
| MediaStore + encryption | User-visible, standard sharing | Harder to keep private; per-app ownership semantics to get right |
| SQLCipher | Whole-DB encryption, transparent | ~5–10% performance cost; increases APK size (native libs); no official Room 2.6+ first-party support issues but well-documented integration |

**Key detail:** The strongest answer includes **crypto-erasure** — deleting the DEK makes the file permanently unreadable even if the ciphertext survives on flash — and the note that **nonce reuse in GCM is catastrophic**, so nonce generation must be random-but-unique (a random 96-bit IV with a 96-bit nonce space is acceptable up to a very large number of files; a counter is better).

**Code example**

```kotlin
// Envelope encryption: per-file DEK, wrapped by a Keystore-held master key.
class DocumentVault(
    private val filesDir: File,
    private val masterKeyProvider: () -> SecretKey,   // Keystore MK
    private val keystore: KeyStore,
) {
    data class StoredDoc(val docId: String, val cipher: File, val wrappedDek: ByteArray, val iv: ByteArray)

    fun store(docId: String, plain: File): StoredDoc {
        val dek = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        val enc = Cipher.getInstance("AES/GCM/NoPadding")
        enc.init(Cipher.ENCRYPT_MODE, dek, GCMParameterSpec(128, SecureRandom().let { random ->
            val iv = ByteArray(12); random.nextBytes(iv); enc.iv = iv; iv
        }))
        // AAD binds the ciphertext to this document id → prevents file swapping.
        enc.updateAAD(docId.toByteArray())

        val cipherFile = File(filesDir, "$docId.bin")
        CipherOutputStream(cipherFile.outputStream(), enc).use { out ->
            plain.inputStream().use { it.copyTo(out) }        // streaming, constant memory
        }

        // Wrap the DEK with the master key.
        val kek = masterKeyProvider()
        val wrap = Cipher.getInstance("AES/GCM/NoPadding")
        wrap.init(Cipher.ENCRYPT_MODE, kek)
        val wrappedDek = wrap.doFinal(dek.encoded)

        return StoredDoc(docId, cipherFile, wrappedDek, enc.iv)
    }

    fun open(doc: StoredDoc): InputStream {
        val kek = masterKeyProvider()
        val wrap = Cipher.getInstance("AES/GCM/NoPadding")
        wrap.init(Cipher.DECRYPT_MODE, kek)
        val dekBytes = wrap.doFinal(doc.wrappedDek)
        val dek = SecretKeySpec(dekBytes, "AES")

        val dec = Cipher.getInstance("AES/GCM/NoPadding")
        dec.init(Cipher.DECRYPT_MODE, dek, GCMParameterSpec(128, doc.iv))
        dec.updateAAD(doc.docId.toByteArray())
        return CipherInputStream(doc.cipher.inputStream(), dec)   // GCM tag verified on close
    }

    // Crypto-erasure: destroy the key, not the bytes.
    fun destroy(doc: StoredDoc) {
        doc.cipher.delete()
        // the wrapped DEK is removed from the metadata store → ciphertext is now garbage
    }
}
```

**Talking points / follow-ups**
- *"What is the threat model for offline documents?"* → At-rest theft (lost/stolen device, backup, forensic extraction) and a malicious app with elevated permissions; plus a compromised device where local guarantees fail — hence short-lived server-side access plus revocation.
- *"Why not just use a password-derived key?"* → Password-derived keys are weak (low entropy, offline dictionary attacks). Use the Keystore for device binding and, for user-specific keys, derive with a KDF from a high-entropy server-issued secret combined with the device key.
- *"How do you support key rotation for thousands of documents?"* → With envelope encryption: re-wrap each DEK (cheap, no re-encryption of data). Re-encrypting every file is only needed if you want a new DEK per document, which you usually do not.
- *"What if the Keystore key is invalidated?"* → Detect the `KeyPermanentlyInvalidatedException`/`UnrecoverableKeyException`, mark the documents as unrecoverable (or re-fetch from the server), and prompt the user. Design a fallback path *before* shipping.
- *"How do you prevent a user from screenshotting documents?"* → `FLAG_SECURE` on the viewing window; note it does not prevent an external camera.
- *"How do you share an encrypted document with another app?"* → Decrypt to a temp file in a cache dir and expose it via `FileProvider` with a read grant — but that briefly creates plaintext; alternatively share the encrypted file plus a key-wrapping step to the recipient.

---

### 3. How would you implement certificate pinning without making the app unusable when certificates or public keys rotate?

**What the interviewer is really asking**
Can you implement **pinning with a rotation strategy** — multiple pins, backup pins, remote-configurable pin updates, and a considered expiry — instead of a single hardcoded hash that bricks the app when the cert changes?

**Key concepts**
- Public Key Pinning vs Certificate Pinning (the latter breaks on cert renewal; prefer SPKI pinning)
- SPKI hash (SubjectPublicKeyInfo) vs certificate hash
- `CertificatePinner` (OkHttp) — `sha256/` pins
- `Network Security Config` `<pin-set>` with `expiration` attribute
- **Backup pin** — the escape hatch
- Remote config for emergency pin updates
- Pin rotation: overlap old + new pins for a window
- Key Transparency / Certificate Transparency (CT logs)
- Let's Encrypt / DigiCert automation → short-lived certs → pinning friction
- `TrustManager` custom implementation (last resort)
- Cleartext, user-installed CAs, and the debug-only trust user certs

**Approach**

1. **Pin the SPKI (public key), not the certificate.** A certificate can be renewed (same key, new cert) or re-issued (new key). Pinning the SPKI survives certificate renewal but not key rotation; that is the behaviour you want, and it is what `CertificatePinner` expects.

2. **Compute the pin correctly:**
   ```
   sha256/Base64(SHA-256(SubjectPublicKeyInfo DER bytes of the leaf public key))
   ```
   The pin is **not** the hash of the certificate, and not the hash of the raw public key. Getting this wrong is the most common pinning bug. You can compute it with OpenSSL:
   ```bash
   openssl x509 -in cert.pem -pubkey -noout \
     | openssl pkey -pubin -outform der \
     | openssl dgst -sha256 -binary | openssl enc -base64
   ```

3. **Ship at least two pins: the current one and a backup pin.** The backup pin is the escape hatch. It should be the **public key of a key you control** (or a second, long-lived key for the same service) that you can switch to in an emergency without shipping a new app version. Without it, a mis-issued or compromised cert can brick every installed copy of the app.

4. **Plan rotation properly.** Certificate rotation, when done well, is:
   - The provider publishes a new key and starts serving it alongside the old one (overlap).
   - You ship an app update containing **both** pins (old + new) with the new one as primary.
   - After the old key is retired, you ship another update with only the new pin.
   - Never ship an app with a *single* pin and no overlap window unless you can hot-fix pins remotely.

5. **Make pins remotely updatable.** This is the key staff-level move. Load the pin set from **remote config** (Firebase Remote Config / your own config service), validated and cached:
   ```kotlin
   class PinProvider(private val remote: RemoteConfig) {
       fun pins(): Set<String> = remote.getString("network.pins")
           .split(',')
           .map { it.trim() }
           .filter { it.matches(Regex("^sha256/[A-Za-z0-9+/=]+$")) }   // validate format
           .toSet()
           .ifEmpty { BUILT_IN_PINS }        // fail safe: fall back to the shipped pins
   }
   ```
   Design rules:
   - The **built-in pins are always present** as a floor — a remote config outage must not remove pinning.
   - The remote pin set must be a **superset or replacement validated against a signature**, so a compromised config channel cannot strip the pins (a MitM of the config channel could otherwise disable pinning entirely). Sign the config payload and verify the signature before use.
   - Never allow the remote set to be *empty*.
   - Provide a remote "kill switch" that removes a bad pin — the emergency path.

6. **Also use the Network Security Config** as a second layer:
   ```xml
   <network-security-config>
     <domain-config cleartextTrafficPermitted="false">
       <trust-anchors>
         <certificates src="system" />
       </trust-anchors>
       <pin-set expiration="2027-01-01">
         <pin digest="SHA-256">k3Xq...=</pin>
         <pin digest="SHA-256">7MhR...=</pin>   <!-- backup -->
       </pin-set>
     </domain-config>
   </network-security-config>
   ```
   The `expiration` attribute is a safety valve: when it passes, pinning is **disabled** rather than the app failing to connect. This is a deliberate trade-off — it prevents a permanent brick in favour of no pinning, and it forces you to schedule pin maintenance.

7. **Pin only the hosts that matter.** Pin the API and auth hosts; leave CDN/static-asset hosts unpinned (they rotate aggressively). Too much pinning increases breakage risk for no security gain on low-value hosts.

8. **Design the failure behaviour.** If pinning fails:
   - Do **not** silently fall back to "accept any certificate" — that defeats the purpose and is a vulnerability.
   - Fail closed with a clear user-facing error ("We can't securely connect right now") and telemetry, so you can detect a bad pin rollout.
   - Provide the remote-config escape hatch so you can fix it without an app release.

9. **Test it.** Use `badssl.com`-style test servers, or a local server with a self-signed cert, plus OkHttp's `MockWebServer` with a custom `SSLSocketFactory` — you can assert that a request with an unpinned cert fails.

10. **Consider the operational reality.** Pinning against a CDN or an aggressively-rotating provider (Let's Encrypt with short-lived certs) is high-maintenance. Many companies pin only their first-party API hosts and manage certs with long validity (1–2 years).

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Pin the SPKI | Survives cert renewal | Breaks on key rotation (must be managed) |
| Pin the certificate | Simplest to explain | Breaks on every renewal — very high maintenance |
| Two hardcoded pins (primary + backup) | No remote dependency; simple | Requires an app release to fix a bad rotation |
| Remotely-updatable pins | Instant fix; no store review needed | Config channel becomes security-critical; needs signing and a safe fallback |
| Network security config `expiration` | Prevents a permanent brick | After expiry, no pinning — a silent security downgrade |
| Pin everything | Maximum security | CDN/asset breakage; high maintenance; false sense of security |
| Pin only API hosts | Practical | Weaker for other hosts (which is usually fine) |
| Fail closed on pin failure | Secure | Users see errors; requires fast ops response |
| Fail open (skip verification) | Users never blocked | **Defeats the entire purpose — never do this** |

**Key detail:** The pin digest is computed over the **SubjectPublicKeyInfo** DER encoding, not the certificate and not the raw public key. And the `expiration` attribute in the network security config is the safety valve that prevents an expired pin from bricking every install.

**Code example**

```kotlin
// Remotely-updatable pins, with the shipped pins always as a floor.
object NetworkPins {
    private val BUILT_IN = setOf(
        "sha256/k3Xq8m2sT0pQ5vH9nB4cW6yL1dF7gJ3rS8uA0eI2o=",
        "sha256/7MhR4tY6bN8mQ1wE3rT5yU7iO9pA2sD4fG6hJ8kL=",   // backup pin
    )

    fun resolve(remote: RemoteConfig): Set<String> {
        val fromRemote = remote.getString("network.pins")
            .split(',')
            .map { it.trim() }
            .filter { PIN_REGEX.matches(it) }
            .toSet()
        // Never allow an empty set → fall back to the shipped pins.
        return if (fromRemote.isEmpty()) BUILT_IN else BUILT_IN + fromRemote
    }

    private val PIN_REGEX = Regex("^sha256/[A-Za-z0-9+/]{43}=$")
}

class CertPinnerFactory(private val remote: RemoteConfig) {
    fun create(): CertificatePinner {
        val builder = CertificatePinner.Builder()
        NetworkPins.resolve(remote).forEach { builder.add("api.example.com", it) }
        return builder.build()
    }
}

// Fail closed with a clear message; never fall back to trusting everything.
val okHttp = OkHttpClient.Builder()
    .certificatePinner(CertPinnerFactory(remote).create())
    .addInterceptor { chain ->
        try {
            chain.proceed(chain.request())
        } catch (e: SSLPeerUnverifiedException) {
            CrashReporter.recordNonFatal("pin_failure", e)
            throw PinVerificationException(e)     // typed → UI shows a safe error
        }
    }
    .build()
```

**Talking points / follow-ups**
- *"What happens if the pin fails?"* → Fail closed with a user-visible error plus telemetry; use the remote escape hatch to recover without a release.
- *"Do you pin the certificate or the key?"* → The public key (SPKI). Explain the renewal-vs-rotation distinction.
- *"How do you test pinning?"* → MockWebServer with a custom SSL socket factory, plus a staging environment with a test pin; assert that an unpinned cert is rejected.
- *"What about user-installed CAs?"* → Do not include `user` in `<trust-anchors>` for production hosts — that would let a user-installed CA MitM your API. Include it only in a `debug-overrides` block for debug builds.
- *"Does pinning protect the payload?"* → No — it protects the channel's identity. The payload still needs transport-level confidentiality and server-side authentication, and sensitive data should be encrypted at the application layer if you need protection from a compromised client.
- *"Is pinning still recommended?"* → It remains valuable for high-value first-party APIs, especially where you control the cert lifecycle. For third-party/CDN traffic it is often impractical. Many teams now rely on it selectively.

---

### 4. A user logs out while API requests are still running. How would you prevent stale requests from accessing or updating authenticated data?

**What the interviewer is really asking**
Can you design a **session-scoped cancellation and identity fence** — invalidating in-flight work at logout, and ensuring a response belonging to a *previous* session can never be written into the *new* session's state?

**Key concepts**
- Session-scoped `CoroutineScope` (cancelled on logout)
- Generation/epoch counter per session (the "session id fence")
- Clearing the credential store and cancelling the token refresh
- `AuthState` as a `StateFlow`; the repository checks it before writing
- `StateFlow` reset on logout (new `StateFlow` instance / `MutableStateFlow` reset) to avoid old values leaking into the new session
- Cross-user data leakage in a shared cache (a shared `LruCache` must be cleared on logout)
- `WorkManager` jobs that hold credentials
- Idempotency/authorization re-check server-side
- `AccountManager` / multiple accounts
- Local DB encryption keys must be re-keyed per user (a shared DB leaks the previous user's data)

**Approach**

1. **Model the session explicitly.** Everything authenticated belongs to a session. Create a session object that owns the scope, the token, and the identity:
   ```kotlin
   data class AuthSession(
       val userId: String,
       val accessToken: String,
       val refreshToken: String,
       val scope: CoroutineScope,      // cancelled on logout
   )
   ```
   The repository obtains the current session from a `SessionManager`; there is no ambient global token that can outlive the session.

2. **Cancel everything on logout.** `SessionManager.logout()`:
   ```kotlin
   suspend fun logout() {
       val session = current()
       current = null                        // fence first: new work sees no session
       session?.scope?.cancel(CancellationException("Logged out"))
       tokenStore.clear()
       authState.value = AuthState.SignedOut
   }
   ```
   Order matters: **clear the session reference first**, then cancel, so that any coroutine that wakes up during cancellation sees "no session" and bails out.

3. **The epoch/generation fence — the crucial part.** A cancelled coroutine may still complete an in-flight network call if the cancellation is not observed (e.g. the response was already parsed and the code is about to write). So every write into state or the DB must check that the session it belongs to is still the current one:
   ```kotlin
   class TransactionRepository(private val sessions: SessionManager) {
       suspend fun loadAndStore(): Result {
           val session = sessions.current() ?: return Result.NoSession
           val epoch = session.epoch                   // unique per session
           val data = api.getTransactions(session.accessToken)
           // The session may have changed while we were on the wire.
           if (!sessions.isCurrent(epoch)) return Result.Stale
           dao.replaceAll(data)                       // only write if still valid
           return Result.Success(data)
       }
   }
   ```
   The epoch increments on every login and logout. `isCurrent(epoch)` compares. This is the **same pattern as the stale-response guard in Section 1, Q4**, applied to identity.

4. **Never write into a shared cache across sessions.** Any `LruCache`, in-memory store, or `SharedPreferences` keyed by user id must be **cleared on logout**. A cache keyed by endpoint (not by user) will serve user A's data to user B — a serious data leak. Rule: **cache keys must include the user id**, or the cache must be cleared on logout.

5. **Isolate the local database per user.** Either:
   - a **separate database file per user id** (`app-db-<userId>.db`) — cleanest isolation, deleted on logout; or
   - a **single database with a `userId` column and every query filtered by it** — cheaper, but every query must be filtered (one forgotten `WHERE` leaks data); or
   - **per-user encryption keys** — the DEK is derived from a per-user server secret, so user B's data is cryptographically inaccessible.
   Plus: on logout, delete the local data (or keep it only if the user explicitly opts into "stay signed in" and the data is encrypted under a key that survives).

6. **Cancel and re-scope `WorkManager` jobs.** Background workers may be running with the old session's token. On logout:
   ```kotlin
   WorkManager.getInstance(context).cancelAllWorkByTag(TAG_AUTHENTICATED)
   ```
   and have workers read the token from `SessionManager` at execution time rather than capturing it at scheduling time. A worker that captured a stale token will fail with 401 — that is safe, but it should also be cancelled to avoid pointless work.

7. **The server is the final authority.** Client-side cancellation cannot undo a request the server already processed. The server must validate the token on every request (it does) and must treat the token as invalid immediately on logout — so implement **token revocation** server-side, and consider short token TTLs so that even a revoked-but-not-yet-expired token has a small window.

8. **Clear UI state.** On logout, the UI state must be reset — not left showing the previous user's data. Reset the `StateFlow` (or create a new ViewModel-scoped state) and clear the navigation back stack so the previous user's screens cannot be reached with the back button.

9. **Never cache sensitive responses in a way that outlives the session** — check the HTTP cache: disable caching for authenticated responses, or ensure `Cache-Control: no-store` is respected and that the OkHttp `Cache` directory is cleared on logout.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Session-scoped `CoroutineScope` | Cancels all session work; clean | Jobs not tied to a session (e.g. a media download) must be handled separately |
| Epoch fence on writes | Correct even if cancellation is late | Extra bookkeeping; every write path must be careful |
| Per-user database file | Strong isolation; trivial logout wipe | Many files; migrations must be applied to each; more disk |
| Single DB with `userId` column | One file, one migration path | One missing `WHERE` = a data leak; queries are wider |
| Per-user encryption keys | Crypto isolation | Requires key derivation and key management complexity |
| Clearing caches on logout | Prevents cross-user leakage | Loses offline data unless it is preserved per-user |
| Server-side token revocation | Immediate invalidation | Requires a revocation list or a token-version counter |

**Key detail:** The trap in this question is the **shared cache**. Most candidates handle "cancel the coroutine" and miss that a `LruCache` keyed by URL will happily return user A's cached response to user B after login. The answer must include "cache keys include the user id, or the cache is cleared on logout".

**Code example**

```kotlin
class SessionManager @Inject constructor(
    private val tokenStore: TokenStore,
    private val cache: ResponseCache,          // must be user-scoped or cleared
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    @Volatile private var session: AuthSession? = null
    private val epochCounter = AtomicLong(0)

    fun current(): AuthSession? = session
    fun isCurrent(epoch: Long): Boolean = session?.epoch == epoch

    suspend fun login(userId: String, tokens: Tokens) {
        session?.scope?.cancel()
        val s = AuthSession(
            userId = userId,
            accessToken = tokens.access,
            refreshToken = tokens.refresh,
            epoch = epochCounter.incrementAndGet(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
        session = s
        tokenStore.save(tokens)
        _authState.value = AuthState.SignedIn(userId)
    }

    suspend fun logout() {
        val s = session
        session = null                                  // fence BEFORE cancelling
        _authState.value = AuthState.SignedOut
        s?.scope?.cancel(CancellationException("logged out"))
        tokenStore.clear()
        cache.clear()                                   // no cross-user leakage
        WorkManager.getInstance(appContext).cancelAllWorkByTag("authenticated")
    }
}

// Repository: never write a response that belongs to an old session.
class TransactionRepository(
    private val api: TransactionApi,
    private val dao: TransactionDao,
    private val sessions: SessionManager,
) {
    suspend fun refresh(): Boolean {
        val session = sessions.current() ?: return false
        val epoch = session.epoch
        val data = api.getTransactions(session.accessToken)
        if (!sessions.isCurrent(epoch)) return false     // logged out mid-flight → drop
        dao.replaceAll(data)
        return true
    }
}
```

**Talking points / follow-ups**
- *"What if the logout request itself fails?"* → Log out locally anyway (fail closed). The server's token TTL and revocation handle the rest; the user must never be stuck signed in.
- *"How do you handle a multi-account device?"* → A session per account, and the UI switches by replacing the session — the epoch fence handles the transition.
- *"What about data cached on disk by the HTTP cache?"* → Clear the OkHttp `Cache` on logout, or disable caching for authenticated responses.
- *"Can you cancel a request the server already received?"* → No. That is why server-side revocation matters — the token must be invalid immediately.
- *"How do you test this?"* → Logout while a request is in flight (a fake API with a delay); assert the response is not written to the DB, and that the next login does not show the previous user's data.
- *"What about the encryption key for the local DB?"* → Derive it per user (from a server-issued secret + device key) so user B cannot decrypt user A's rows even with filesystem access.

---

### 5. How would you prevent sensitive payment information from appearing in logs, analytics, or crash reports?

**What the interviewer is really asking**
Can you build a **systemic redaction pipeline** — at the source, at the logging layer, and at the export layer — rather than relying on developers to remember not to log PANs?

**Key concepts**
- PCI-DSS scope reduction: tokenise with the PSP, never handle the PAN
- `Log`/`Timber`/`println` → route everything through a redacting logger
- OkHttp `HttpLoggingInterceptor` — `Level.BODY` leaks everything; `redactHeader()`
- `BuildConfig.DEBUG` gating — never log in release
- Crash reporting: Crashlytics/Bugsnag custom keys and breadcrumbs are **uploaded**
- `ApplicationExitInfo` and ANR traces can contain data
- ProGuard/R8: obfuscation reduces but does not remove string literals
- `StrictMode` and `Logcat` access (on Android 4.1+, other apps cannot read your logs — but `adb` and crash reporters can)
- `Backup`/`logcat` retention on device
- `toString()` of data classes leaking into logs — override `toString()`
- Analytics event schemas; allow-listing rather than deny-listing
- PCI DSS: never store CVV (the 3-digit code) after authorization — this is absolute

**Approach**

1. **The best defence is not having the data.** Use **payment tokenisation**: the PSP (Stripe, Adyen, Braintree) gives you a client token / payment method id; your app handles only the token. The PAN never enters your app, so it cannot be logged. Reducing PCI scope is the highest-leverage answer and should be the first thing said.

2. **Never store CVV.** After authorization, the CVV must not be persisted anywhere — not the DB, not logs, not crash reports. This is an absolute PCI-DSS requirement, not a preference. Also never store the full track data.

3. **Centralise logging through a redacting logger.** Do not let application code call `Log.d` directly. Use a wrapper that applies redaction and drops everything in release:
   ```kotlin
   object AppLog {
       private val redactor = Redactor(
           sensitiveKeys = setOf("card", "pan", "cvv", "cvc", "password", "token", "ssn", "account"),
           maskedPatterns = listOf(
               Regex("""\b(?:\d[ -]*?){13,19}\b"""),                 // PAN-like digit runs
               Regex("""\b\d{3,4}\s*[-/]\s*\d{4}\s*[-/]\s*\d{4}\s*[-/]\s*\d{4}\b"""),
           ),
       )
       fun d(tag: String, message: String, extra: Map<String, Any?> = emptyMap()) {
           if (!BuildConfig.DEBUG) return
           Log.d(tag, redactor.redact(message, extra))
       }
   }
   ```
   And enforce it with a **lint/detekt rule** banning `Log.` and `println(` outside the logger. This converts "remember not to log" into "you cannot log without redaction".

4. **Redact at the network layer too.** The logging interceptor is the highest-risk place, because a single `Level.BODY` dumps entire request and response bodies — including card fields and tokens:
   ```kotlin
   val logging = HttpLoggingInterceptor { message -> AppLog.d("HTTP", message) }.apply {
       level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
       redactHeader("Authorization")
       redactHeader("Cookie")
       redactHeader("X-Auth-Token")
   }
   ```
   Even in debug, prefer `BASIC`/`HEADERS` over `BODY` for payment endpoints. Add a **request-body scrubber** that strips known sensitive JSON fields before logging.

5. **Redact in the crash reporter.** Crashlytics/Bugsnag let you attach custom keys and breadcrumbs — everything you attach is uploaded. Establish a policy:
   - Only **allow-list** what is attached (user id hash, screen name, error type, endpoint path).
   - Never attach a full response body.
   - Attach a **scrubbed** exception message — some libraries include the request/response in the exception message (e.g. `HttpException: HTTP 400 {"card":"4242..."}`). Wrap HTTP exceptions and strip the body before reporting:
     ```kotlin
     fun Throwable.toReportable(): Throwable =
         if (this is HttpException) ReportedException(code(), message.take(200)) else this
     ```
   - Turn off `setCrashlyticsCollectionEnabled` for the sensitive flow, or use `Crashlytics.setCustomKey` with only non-PII keys.

6. **Audit the analytics event schema.** Analytics events are structured and reviewable, which makes them safer than logs — but only if there is a schema. Define the allowed event parameters centrally; reject unknown ones. Have a privacy review of the schema.

7. **Override `toString()` on data classes holding sensitive fields.** A `data class` auto-generates `toString()` including every field — and any `Log.d("TAG", "obj=$obj")` leaks the PAN. Use a regular class with a redacted `toString()`, or `@JvmStatic` custom `toString`:
   ```kotlin
   data class CardInput(val pan: String, val expiry: String, val cvv: String) {
       override fun toString() = "CardInput(pan=****${pan.takeLast(4)}, expiry=**/**, cvv=***)"
   }
   ```
   (Careful: `data class` `copy()`/`equals()` still work; overriding `toString` is allowed.)

8. **Mask in the UI and in logs.** Show only the last 4 digits; never log the full value.

9. **Guard the transport.** `usesCleartextTraffic="false"`, a network security config that forbids cleartext, and TLS for everything — a cleartext HTTP log would expose card data on the wire.

10. **Control who can read the logs.** On modern Android, other apps cannot read your logcat. But `adb` users can, and crash reporters capture it. So: **never log sensitive values even in debug builds** — debug builds get shared around and screen-recorded.

11. **Automate the check.** Add a CI step that scans the APK and the source for PAN-like literals and for `Log.` usage:
    ```bash
    # crude but effective: look for long digit sequences in the source and resources
    grep -rEn '\b[0-9]{13,19}\b' src/ || echo "possible PAN in source"
    ```

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Tokenisation (never handle the PAN) | Removes the data entirely; shrinks PCI scope | Requires a PSP that supports client-side tokenisation; less control over the form |
| Redacting logger | Central, enforceable | Developers must adopt it; regex-based redaction can miss formats |
| Allow-list analytics schema | Prevents unknown leaks | Slower to add events; needs a governance process |
| `Level.NONE` in release | Zero network logging | No diagnostics in production |
| `Level.BASIC` in release | Some diagnostics | Still logs URLs — make sure no sensitive data is in URLs (never put a token in a query param) |
| Overriding `toString()` | Prevents accidental leakage | Easy to forget on new classes — enforce with a lint rule |
| Custom exception messages for crash reports | Controls what is uploaded | Loses diagnostic detail — keep the detail server-side, identified by a correlation id |

**Key detail:** The strongest framing is **"the data should not be in the app"**. Tokenisation is the primary control; redaction is the defence-in-depth layer for the data you genuinely must handle (e.g. a last-4 display, or a payment description).

**Talking points / follow-ups**
- *"How do you know your logs are clean?"* → Automated scanning in CI; a periodic review of crash-reporting dashboards; a "canary" synthetic PAN that you can grep for.
- *"Does obfuscation help?"* → R8 obfuscates class and method names but **not string literals** — a hardcoded PAN remains visible in the DEX. Obfuscation is not a data-protection control.
- *"What about `adb logcat` on a user's device?"* → A user with `adb` access has far more power than your logs — but on non-rooted devices they cannot read your app's private storage or logs.
- *"How do you handle support screenshots?"* → Redact in-app; `FLAG_SECURE` on payment screens; a support workflow that scrubs screenshots.
- *"What does PCI-DSS require?"* → Never store the CVV after authorization; mask the PAN in displays and logs; encrypt transmitted cardholder data (TLS); limit and log access to stored data; segment the CDE via tokenisation.
- *"How do you handle a bug report containing card data?"* → Have a documented redaction and deletion procedure, and a designated data-protection contact.

---

### 6. A WebView loads authenticated content. How would you protect it against JavaScript injection, unsafe navigation, and token leakage?

**What the interviewer is really asking**
Do you know the **WebView security model** — JS bridge exposure, file/content access, `shouldOverrideUrlLoading` navigation control, cookie and token handling, and the fact that a WebView is effectively a **browser** that you must treat as untrusted?

**Key concepts**
- `WebView` settings: `javaScriptEnabled`, `allowFileAccess`, `allowContentAccess`, `allowUniversalAccessFromFileURLs`, `allowFileAccessFromFileURLs`, `mixedContentMode`, `safeBrowsingEnabled`
- `addJavascriptInterface` — **the classic RCE vector**; only add on API 17+ and only expose `@JavascriptInterface`-annotated methods with primitive/JSON arguments
- `setWebViewClient` / `shouldOverrideUrlLoading` — navigation control (allow-list hosts, block file://, intent://, blob://, javascript:)
- `shouldInterceptRequest` — request filtering
- Cookies: `CookieManager` (`setAcceptCookie`, `setAcceptThirdPartyCookies`), `removeAllCookies`, `flush`
- Token leakage: do **not** put tokens in URLs or in `loadUrl("...?token=...")`; use `Authorization` headers via `shouldInterceptRequest`, or a short-lived one-time token
- `WebView` file chooser, `onShowFileChooser`
- `setAllowFileAccessFromFileURLs(false)`, `setAllowUniversalAccessFromFileURLs(false)`
- `WebView` debugging: `WebView.setWebContentsDebuggingEnabled(true)` — **debug only**
- `shouldOverrideUrlLoading` returning `true` to consume the navigation
- TLS errors: `onReceivedSslError` — **never proceed** (the classic mistake)
- Content-Security-Policy on the server side
- `WebView` in a separate process for untrusted content

**Approach**

1. **Never use a WebView for native content.** If the content can be a native screen, do that — it removes the whole attack surface. Use a WebView only when you genuinely need a web experience (a payment provider's hosted page, terms, help). State this trade-off explicitly.

2. **Treat the WebView as loading remote, potentially hostile content.** Your controls:
   ```kotlin
   webView.settings.apply {
       javaScriptEnabled = true              // required for most web flows — so constrain everything else
       allowFileAccess = false
       allowContentAccess = false
       allowFileAccessFromFileURLs = false
       allowUniversalAccessFromFileURLs = false
       mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
       safeBrowsingEnabled = true
       domStorageEnabled = true              // if needed
       userAgentString = "MyApp/1.0"
   }
   ```
   `javaScriptEnabled = true` is usually required — so the security must come from the *navigation allow-list*, the *bridge*, and the *server headers*, not from disabling JS.

3. **Never pass the token in the URL.** `webView.loadUrl("https://pay.example.com?access_token=XYZ")` leaks the token into the browser history, `WebView`'s own history, any `shouldInterceptRequest` logging, and potentially `Referer` headers on navigation. Instead:
   - Inject an `Authorization` header via `shouldInterceptRequest` for your own API calls from within the page; or
   - Use a **short-lived, single-use, scoped ticket** that the server exchanges for a session — the "ticket" pattern; or
   - Use a **secure cookie** (`HttpOnly`, `Secure`, `SameSite=Strict`) set by a first-party endpoint, with `CookieManager.setAcceptThirdPartyCookies(webView, false)`.
   The ticket pattern is the right answer for "WebView loads authenticated content": the app authenticates with the backend, gets a one-time ticket bound to (user, host, expiry, scope), and hands only that to the WebView.

4. **Constrain navigation with an allow-list.** Override `shouldOverrideUrlLoading` and block anything that is not on the allow-list:
   ```kotlin
   override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
       val url = request.url
       return when {
           ALLOWED_HOSTS.contains(url.host) -> false          // let the WebView load it
           url.scheme == "about" -> false
           else -> {
               openInCustomTab(url)                          // or block entirely
               true                                          // consume the navigation
           }
       }
   }
   ```
   Explicitly block `file://`, `content://`, `intent://`, `javascript:`, `blob:` (unless needed), and `data:` in top-level navigation. Also check **redirects** — `shouldOverrideUrlLoading` is called for server-side redirects on API 24+, but validate the final URL too.

5. **Never expose a rich JS bridge.** `addJavascriptInterface` gives JavaScript a direct handle into your app. Rules:
   - Add it **only for trusted, first-party content** — ideally on a separate WebView instance used only for that host.
   - Expose only `@JavascriptInterface`-annotated methods with **primitive or JSON-string** arguments.
   - **Validate every input inside the bridge method** — treat arguments as untrusted (path traversal, SQL injection, oversized payloads).
   - Never expose a bridge that can read files, execute intents, or call arbitrary APIs.

   ```kotlin
   webView.addJavascriptInterface(object {
       @JavascriptInterface
       fun postJson(payload: String) {
           // Validate strictly: size limit, schema check, never trust the caller.
           val data = runCatching { json.decodeFromString<BridgeMessage>(payload) }.getOrNull() ?: return
           if (data.type != "close") return
           mainScope.launch { onMessageReceived(data) }
       }
   }, "AndroidBridge")
   ```

6. **Never proceed past an SSL error.** `onReceivedSslError` must call `handler.cancel()` — never `handler.proceed()`. Proceeding on a TLS error defeats all transport security. This is one of the most common and most dangerous WebView mistakes.

7. **Filter requests.** Use `shouldInterceptRequest` to block requests to unexpected hosts (e.g. ad/tracker domains, or any host not on the allow-list), and to attach auth headers to your own API calls.

8. **Server-side defences (the primary ones).**
   - **Content-Security-Policy** on the served page: `default-src 'self'; script-src 'self'; frame-ancestors 'none'` — this is what actually stops injected scripts.
   - **No inline scripts**; use nonces/hashes.
   - **`X-Frame-Options: DENY`** / `frame-ancestors` to prevent framing (clickjacking).
   - **Escape all user content** on the server (never build HTML by string concatenation).
   - **Cookies**: `HttpOnly; Secure; SameSite=Strict`.
   - **Strict-Transport-Security**, correct MIME types, `X-Content-Type-Options: nosniff`.

9. **Isolate untrusted content.** If the WebView must load genuinely third-party content, consider:
   - Loading it in a **separate process** (`android:process` on the hosting Activity) so a renderer exploit cannot reach the main app's data.
   - Disabling the JS bridge entirely for third-party hosts.
   - Using an **in-app browser (Custom Tabs)** instead of a `WebView` — Custom Tabs runs in the browser's process, gets the browser's cookie jar and security updates, and supports a shared cookie store with the browser for OAuth. This is the modern recommendation for OAuth and payments.

10. **Clear state on logout.** `CookieManager.getInstance().removeAllCookies(null); removeSessionCookies(null); flush()` plus `webView.clearCache(true); clearHistory()` and `CookieManager.setAcceptThirdPartyCookies(webView, false)`. Otherwise the next user in the WebView sees the previous session.

11. **Debug only.** `WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)` — enabling it in release lets anyone with `adb` inspect the page and the bridge.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Use a WebView for authenticated content | Rich, flexible content | Large attack surface; you own the security posture |
| Use Custom Tabs / a browser | Browser-grade security, up-to-date engine, OAuth-friendly | Less control over the UI; no access to page internals; cookie separation |
| Native screens | Smallest attack surface, best UX/perf | Less flexible for rich content; more development |
| JS bridge for app↔page communication | Convenient | The classic RCE vector; must be minimal and validated |
| Post-message over a bridge | Slightly safer; origin-checkable | Requires page-side cooperation |
| Server-side CSP | The real defence against XSS | Requires backend work; cannot be enforced by the client alone |
| Separate process for untrusted WebViews | Blast-radius containment | More memory; IPC complexity; interop limits |
| Token in the URL | Simplest | Leaks into history, logs, `Referer` — do not do this |
| One-time ticket | Tokens never enter the URL | Backend work; ticket management |

**Key detail:** The security of a WebView is mostly **server-side** (CSP, escaping, cookies) plus **navigation control** (allow-list) — the client settings are the third layer. Saying that hierarchy explicitly is a strong staff-level framing.

**Code example**

```kotlin
class SecureWebViewHost : AppCompatActivity() {

    private val allowedHosts = setOf("pay.example.com", "app.example.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)   // debug only

        val web = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
        }

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, req: WebResourceRequest): Boolean {
                val url = req.url
                if (url.host in allowedHosts && (url.scheme == "https")) return false
                openInCustomTab(url)      // external navigation leaves the app
                return true                // and is consumed here
            }

            override fun onReceivedSslError(v: WebView, h: SslErrorHandler, e: SslError) {
                h.cancel()                // NEVER proceed
                CrashReporter.recordNonFatal("webview_ssl_error", e.primaryError)
            }

            override fun onPageFinished(v: WebView, url: String) {
                // Clear the session cookie once the page has consumed the ticket.
                CookieManager.getInstance().removeSessionCookies(null)
            }
        }

        // Authenticated access via a short-lived, single-use ticket — never a token in the URL.
        web.loadUrl("https://pay.example.com/checkout?ticket=${oneTimeTicket}")
    }

    override fun onDestroy() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        super.onDestroy()
    }
}
```

**Talking points / follow-ups**
- *"What is the risk of `addJavascriptInterface`?"* → On API < 17 it was a public RCE via reflection; since API 17 only `@JavascriptInterface` methods are exposed — but exposed methods are still an attack surface if the page can be influenced by an attacker (XSS).
- *"How do you pass auth to the page?"* → One-time ticket, secure cookie, or an intercepted `Authorization` header. Never a token in the URL.
- *"What about `onReceivedSslError`?"* → Always `cancel()`. If you need to support a private CA in a debug build, do it only for debug via a debug-only `TrustManager` and a debug network security config.
- *"How do you handle a hybrid app (WebView + native)?"* → Use a bridge with strict validation, or better, `WebMessageListener`/`postMessage` with origin allow-listing (`WebViewCompat.addWebMessageListener` with allowed origin rules).
- *"Custom Tabs or WebView?"* → Custom Tabs for OAuth/payments (browser security, shared cookies, no app-side surface); WebView only when you need deep UI control over first-party content.
- *"How do you prevent clickjacking?"* → Server-side `X-Frame-Options`/`frame-ancestors` — client-side cannot.

---

### 7. How would you detect and respond to a compromised device without relying exclusively on root detection?

**What the interviewer is really asking**
Do you understand that **root detection is an arms race** and that a mature approach is **layered risk signals + attestation + server-side risk decisions** — degrade, restrict, or step-up authentication based on a risk score, rather than trying to detect "rooted" perfectly.

**Key concepts**
- Root/jailbreak detection: file checks (`/system/app/Superuser.apk`), `su` in `PATH`, `which su`, `test-keys` build tags, `ro.debuggable`, `ro.build.tags`
- The arms race: Magisk/Zygisk, hiding, `Shamiko`, Play Integrity `MEETS_DEVICE_INTEGRITY`
- **Play Integrity API**: app integrity, device integrity, licence verdict; `Standard` vs `Licensed` request
- **SafetyNet Attestation** — deprecated (January 2025); replaced by Play Integrity
- **Key attestation** — `Android Keystore` key attestation with `setAttestationChallenge`, verifying the certificate chain against the Google root
- Device identifiers: `Settings.Secure.ANDROID_ID`, `Build.FINGERPRINT`, `Build.TAGS`, `ro.boot.verifiedbootstate`, `ro.boot.vbmeta.device_state` (verified boot state)
- Emulator detection (often a false-positive source)
- Frida/Xposed detection (frida-server, `/proc/self/maps`, `TracerPid`)
- Debugger detection (`Debug.isDebuggerConnected`, `ApplicationInfo.FLAG_DEBUGGABLE`)
- **Risk-based, server-side enforcement** — the correct framing
- Device attestation / hardware-backed keys with attestation
- Fraud/risk engines; step-up auth (OTP/biometric) for high-risk actions

**Approach**

1. **Reframe: do not build a boolean "is rooted" gate.** Root detection is a heuristic with a permanent false-positive rate (some users root for legitimate reasons; some rooted devices are harmless) and a permanent false-negative rate (modern hiding techniques). Build a **risk score** from multiple signals and let the **server** decide what to do. This is the staff-level answer and it is what the interviewer is looking for.

2. **Layer the signals:**

   | Signal | Source | Notes |
   |---|---|---|
   | Device rooted | `su` binary, `test-keys` build tag, Magisk indicators | Weak alone; widely spoofed |
   | Boot state | `ro.boot.verifiedbootstate` = `green`, `ro.boot.vbmeta.device_state` = `locked` | Stronger — reflects hardware-backed verified boot |
   | Debug build | `ApplicationInfo.FLAG_DEBUGGABLE` | Easy to spoof on a rooted device |
   | Debugger attached | `ApplicationInfo.FLAG_DEBUGGABLE` + `Debug.isDebuggerConnected()` | Useful in development; a strong tampering signal in production |
   | Hooking framework | Frida server present, `/proc/self/maps` containing `frida`, Xposed modules | Moderate signal; more evasion-resistant checks (inline hooks in ART) are better |
   | Emulator | `Build.FINGERPRINT` contains `generic`, `ro.kernel.qemu` | Rarely a real risk; treat as a *risk* factor, not a verdict |
   | **Play Integrity** | `MEETS_DEVICE_INTEGRITY` + `MEETS_BASIC_INTEGRITY` + `APP_LICENSED` | The current best device-integrity signal |
   | **Keystore attestation** | Key generated with `setAttestationChallenge`, chain verified against Google's root | Cryptographic proof the key is in hardware; resists most spoofing |
   | Device/app tampering | `ro.build.tags` = `test-keys`, unexpected system properties | Combined signals |

3. **Use Play Integrity as the primary signal** (it replaced SafetyNet, which was deprecated in January 2025):
   ```kotlin
   val verdict = PlayIntegrity.getStandard(requireContext(), com.google.android.play.core.integrity.IntegrityManagerFactory::class.java)
       .requestIntegrityToken(
           IntegrityTokenRequest.builder()
               .setNonce(base64Of(serverIssuedNonce))   // bind the verdict to a server challenge
               .build()
       )
   ```
   Send the token **to your server** and verify it there with the Play Integrity API. The **server** decides what the verdict means for your app. Never decide locally — the client can be patched.

4. **Use Keystore key attestation for cryptographic proof.** Generate a keypair in the Keystore with an attestation challenge; the returned certificate chain proves the key was generated in hardware-backed storage and that the device is not rooted/tampered (the chain terminates at Google's root, and the attestation extension carries verified-boot and security-patch-level claims). Your server verifies the chain. This is much stronger than heuristics because it is cryptographic.

5. **Bind the attestation to the session.** The server issues a nonce; the client signs/attests against it; the server verifies freshness. This prevents replaying an old, valid attestation.

6. **Respond in proportion — do not brick the app.** A risk-based response matrix:
   - **Low risk** → full functionality.
   - **Medium risk** (e.g. unlocked bootloader, no Play Integrity) → reduce limits, require biometric/OTP step-up for sensitive actions, disable screen capture of sensitive screens, shorten session TTL, restrict high-value features.
   - **High risk** (root with Play Integrity failure, or attestation chain broken) → block sensitive operations (payments, transfers), require re-verification, allow read-only/browse, log and alert.
   - **Confirmed compromise of your app's data** (the Keystore key is invalid, or the app binary is tampered) → refuse to decrypt local data and require re-login + re-download.
   - **Never**: silently crash, wipe the user's data, or block the app entirely without an explanation and an appeal path.

7. **Treat the signals as risk factors, not verdicts.** Combine them into a score and send the score (or the raw signals) to the server for a policy decision. The policy is business-specific: a banking app may block; a media app may only degrade.

8. **Do not ship a naive client-side root check as the only control.** If you do implement heuristics, do it server-side-assisted and make them cheap. Provide a clear user-facing message.

9. **Protect the integrity checks themselves.** Inline the checks into the release build, obfuscate them, and verify the app's own signature (`PackageManager.GET_SIGNING_CERTIFICATES`) so a repackaged APK is detected. Consider `SafetyNet`/`Play Integrity` + a **self-integrity check** via the APK's signing certificate.

10. **Plan the false-positive path.** A user on a rooted device for accessibility or testing, or on an emulator, must still be able to use the app appropriately. "Degraded, with an explanation and an appeal" beats "blocked with no recourse". Also test on a variety of devices — many legitimate devices report odd `ro.boot` states (e.g. some Chinese OEMs).

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Play Integrity | Current best-practice; server-verifiable; hard to spoof | Requires Play Services (not on de-Go devices); verdicts change with policy; can be flaky |
| Keystore attestation | Cryptographic, server-verifiable, resists patching | Requires hardware support; some OEMs misreport; setup complexity |
| Heuristic root checks | Cheap, no dependencies | Trivially bypassed; false positives; a maintenance burden |
| Server-side risk decision | Policy is updatable without an app release; combines signals | Needs a risk engine; a false positive blocks a legitimate user |
| Block on high risk | Protects users and the business | Support burden; user frustration; app-store complaints |
| Degrade on high risk | Less harsh; keeps the app usable | The business must accept residual risk |
| Block entirely | Simple | Wrong — too many false positives; legally/practically damaging |

**Key detail:** The key insight is that **the client is untrusted once the device is compromised** — so all verdicts must be verified server-side, and the enforcement policy must live server-side. The client's job is to collect signals and provide cryptographic proof, not to decide.

**Code example**

```kotlin
// Client: collect signals and provide cryptographic proof.
class DeviceTrustManager(
    private val integrityManager: IntegrityManager,
    private val server: TrustServer,
) {
    data class RiskSignals(
        val playIntegrityToken: String?,
        val attestedKeyChain: List<ByteArray>,
        val bootVerified: Boolean,
        val debuggerAttached: Boolean,
        val emulatorLikely: Boolean,
    )

    suspend fun assess(): DeviceTrust {
        val nonce = server.challenge()                     // fresh, server-issued
        val token = runCatching {
            integrityManager.requestIntegrityToken(
                IntegrityTokenRequest.builder().setNonce(nonce).build()
            ).await().token()
        }.getOrNull()

        val attestation = runCatching {
            attestationKey.attest(nonce)                  // hardware-backed proof
        }.getOrNull()

        val signals = RiskSignals(
            playIntegrityToken = token,
            attestedKeyChain = attestation?.chain ?: emptyList(),
            bootVerified = isVerifiedBoot(),
            debuggerAttached = ApplicationInfo.FLAG_DEBUGGABLE.let { false },
            emulatorLikely = looksLikeEmulator(),
        )
        // The SERVER decides. The client only reports.
        return server.evaluate(signals)
    }
}

sealed interface DeviceTrust {
    data object Trusted : DeviceTrust
    data class Degraded(val reasons: List<String>, val limits: Set<Limit>) : DeviceTrust
    data class Untrusted(val reason: String) : DeviceTrust
}
```

**Talking points / follow-ups**
- *"Is root detection even worth it?"* → On its own, mostly no. It is useful as one input to a server-side risk model, alongside Play Integrity and key attestation.
- *"What is SafetyNet's status?"* → Deprecated; it stopped returning results from January 2025. Use Play Integrity.
- *"How do you avoid false positives on legitimate devices?"* → Never use a single signal; weight signals; test on a broad device matrix; provide an appeal path; degrade rather than block.
- *"Can a rooted device fake Play Integrity?"* → The verdict is signed by Google and bound to your app's signing certificate, so a repackaged app cannot fake it. Hooking the client can suppress the request, but then the server sees a missing verdict — hence "no verdict" must be treated as *some* risk, not as pass.
- *"What do you do with the results?"* → Feed them into a risk engine that decides: allow, challenge (OTP/biometric), limit, or block. Store the verdict with the account so you can detect a device becoming compromised later.
- *"How do you protect a banking flow specifically?"* → Step-up authentication (OTP in-app, not SMS), transaction limits, a delay on newly-added payees, and out-of-band notifications.

---

### 8. An attacker replays an authenticated API request. Which protections belong in the client, backend, or both?

**What the interviewer is really asking**
Can you reason about **replay attacks** specifically — the difference between what the client *can* do (nonce, timestamps) and what only the server *can* do (server-side nonces, idempotency, state validation) — and place each control on the correct side?

**Key concepts**
- Replay = capturing a valid request and re-sending it later
- Nonces / request IDs (client-generated, server-verified as unique)
- **Server-side nonce / challenge** (server-issued, single-use, short TTL)
- Idempotency keys (Section 1, Q1)
- Timestamps + clock skew window
- **TLS** — prevents network sniffing, but a compromised client can still replay
- Device-bound keys: request signing with a Keystore key (the server verifies the signature and the device key)
- TOTP/HOTP (transactional OTP) — the server tracks the used counter
- Session binding / DPoP (Demonstrating Proof of Possession)
- Operation-specific state checks (e.g. an OTP already used, a transfer already completed)
- Rate limiting, anomaly detection

**Approach**

1. **Define the threat precisely.** There are three distinct replay scenarios:
   - **Network capture** (the request was observed in transit) → prevented by **TLS**; the request cannot be replayed outside the session if the token is bound to the session/channel.
   - **Malicious app / malware on the device** reading the request from logs, memory, or a hook → TLS does not help; you need **request binding** (a device-held key that signs the request).
   - **Legitimate client compromised or a malicious insider** replaying from inside the app → you need **server-side state validation** and **idempotency**.

2. **Controls that belong in the client:**
   - Generate a **unique nonce / request ID** per request (`UUID`) and include it. A random value alone prevents *identical* byte-for-byte replays but cannot stop a replay of the original request.
   - Include a **client timestamp** — but treat it as advisory; the client clock is untrusted.
   - **Never log the full request** (see Q5) — a logged request is a replayable request.
   - For sensitive operations, **sign the request** with a device-held key (see below) — the client is the *prover*, not the verifier.
   - Disable replay-prone surfaces: no GET that performs a state change; use POST/PUT for mutations.

3. **Controls that belong in the backend (the authoritative ones):**
   - **Server-issued, single-use nonce or challenge** for sensitive operations. The server issues a short-TTL, one-time nonce; the client must include it. The server records it as consumed. A replayed request carries an already-consumed nonce → rejected. This is the strongest generic replay defence for step-up operations (e.g. "confirm this transfer").
   - **Idempotency keys** — the backend stores the key→outcome and replays the result instead of re-executing (Section 1, Q1). This makes replay *harmless* rather than merely detectable.
   - **State validation** — the server checks the *current* state, not just the request's validity: has this OTP already been used? Has this transfer already completed? Is this invoice still payable? This is the defence that catches replay of a logically-stale request.
   - **Transactional OTP (HOTP/TOTP)** with a server-side counter — a replayed OTP is rejected because the counter has advanced.
   - **Tight time windows** — a signed request includes a timestamp and a short validity window (e.g. 60s) plus a nonce; the server rejects older ones. Combined with the server's clock, this bounds the replay window.
   - **Rate limiting and anomaly detection** on replayed identifiers (the same nonce or idempotency key replayed repeatedly → flag the account).
   - **Revocation**: refresh-token rotation with **reuse detection** — a replayed (already-rotated) refresh token is a strong signal of compromise; revoke the whole session family.

4. **Controls that belong in both:**
   - **Request signing with a device-bound key (DPoP-style).** The app generates a keypair in the Keystore (non-exportable). The client signs each request with a canonical representation (method, path, body hash, timestamp, nonce). The server verifies the signature against the public key registered for that session, checks the timestamp window, and checks the nonce's uniqueness. A replayed request has a valid signature but a **used nonce** → the server rejects it. The client contributes the key and the signature; the server contributes the nonce store and the verification. This is the strongest answer to "which side".
   - **Certificate/channel binding** — the server binds the session to the TLS channel or the device key so a captured request cannot be replayed from a different client.
   - **Nonce + timestamp** — the client generates and includes them; the server enforces uniqueness and the window.

5. **Map the protections to the operation's value.** This is the pragmatic staff-level framing:
   - *Read operations (GET)* — replay is mostly harmless (stale data); the real risk is enumeration, handled by authorisation and rate limiting.
   - *Money/data mutations* — need an idempotency key + server-side state validation; **client-only** protections are insufficient.
   - *Authentication / step-up operations* — need a server-issued single-use nonce and OTP counter checks.
   - *Session establishment* — need refresh-token rotation with reuse detection.

6. **Do not rely on the client for enforcement.** Every control that must hold under a compromised client must be server-side. The client-side controls (nonce, timestamp, signing key) raise the attacker's cost; the server-side controls (nonce store, idempotency, state validation) provide the guarantee.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Client-generated nonce only | Cheap; blocks trivial byte-for-byte replay | A captured request is still replayable verbatim; the client is untrusted |
| Server-issued single-use nonce | Genuinely prevents replay of sensitive operations | Requires a nonce store and a round trip; adds latency and state |
| Idempotency key | Makes replays harmless; standard | Requires server storage and a TTL; only covers the mutation it was minted for |
| Timestamps + window | Bounds the replay window | Depends on server clock; the client clock is untrusted |
| Device-bound request signing (DPoP) | Cryptographic; resists a captured request | Backend complexity; key management; does not help if the device itself is compromised at run time |
| TLS | Prevents passive sniffing | Does nothing against a compromised client or a hooked app |
| Transactional OTP counter | Rejects replayed OTPs | Requires the user to have a hardware/app token; extra UX |
| Server-side state validation | The most robust; catches stale replays | Requires modelling the business state per operation |

**Key detail:** The distinction that carries the answer is: **the client can make a request *unique*, only the server can make it *single-use*.** Uniqueness (a nonce) stops identical replays; single-use (a consumed-nonce store, an idempotency key, an advanced OTP counter) is what actually guarantees at-most-once execution.

**Code example**

```kotlin
// CLIENT: sign each request with a device-held key; include a nonce + timestamp.
class RequestSigner(private val keyStore: KeyStore) {
    private val alias = "session_request_key"

    fun ensureKey(): PublicKey {
        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
        if (entry != null) return entry.certificate.publicKey
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        )
        val pair = gen.generateKeyPair()
        return pair.public
    }

    fun sign(method: String, path: String, body: ByteArray, nonce: String, tsMillis: Long): ByteArray {
        val canonical = "$method\n$path\n${sha256Hex(body)}\n$nonce\n$tsMillis"
        val sig = Signature.getInstance("SHA256withECDSA").apply {
            initSign(keyStore.getKey(alias, null) as PrivateKey)
            update(canonical.toByteArray())
        }
        return sig.sign()
    }
}

// SERVER (pseudocode): verify, then enforce single-use.
fun handle(request: Request): Response {
    val ts = request.timestampMillis
    require(abs(now() - ts) <= 60_000) { "stale request" }                 // window
    val pub = session.registeredDevicePublicKey
    require(pub.verify(canonical(request), request.signature)) { "bad signature" }
    require(!nonceStore.consumeIfPresent(request.nonce)) { "nonce replay" }  // single-use
    require(idempotencyStore.isNew(request.idempotencyKey)) { "duplicate" }
    // and finally: validate the *business state* (OTP unused, invoice payable, ...)
    return process(request)
}
```

**Talking points / follow-ups**
- *"Is TLS enough?"* → No — TLS stops network sniffing, but it does nothing against a compromised or hooked client, which is the more realistic mobile threat.
- *"What is the difference between replay protection and idempotency?"* → Replay protection *rejects* a duplicate; idempotency *collapses* it to the original result. Idempotency is usually better UX; replay rejection is stricter.
- *"Where does the timestamp fit?"* → It bounds the window in which a captured request is still valid. Without it, a captured request is valid forever. It must be enforced against the **server's** clock.
- *"How do you protect a read endpoint?"* → Usually you don't need to; replay of a read is a freshness problem. Focus on authorisation and rate limiting.
- *"What about GraphQL/REST with JWTs?"* → A signed JWT is replayable until it expires. Bound the JWT TTL, use `jti` for replay detection on sensitive operations, and rely on refresh rotation.
- *"How do you test replay protection?"* → Capture a valid request (a proxy in a test environment), replay it, and assert it is rejected; assert the idempotency key returns the original result instead of re-executing.

---

### 9. How would you implement secure session expiration and token refresh across multiple concurrent requests?

**What the interviewer is really asking**
Can you combine **short-lived access tokens + rotating refresh tokens + single-flight refresh + proactive expiry + revocation** into one coherent session lifecycle, and handle logout, device loss, and token theft coherently?

**Key concepts**
- Access token (short TTL) + refresh token (long TTL, rotating, stored server-side)
- Session family / token lineage — refresh-token rotation with **reuse detection**
- **Revocation lists** (`jti` denylist) vs short TTL as the revocation strategy
- Proactive refresh with a clock-skew buffer (Section 1, Q8)
- Single-flight refresh (the `Deferred`/mutex pattern)
- Binding a refresh token to a device (device-bound key, attestation)
- "Log out everywhere" / "log out this device"
- Idle timeout vs absolute timeout
- Concurrent requests during refresh — queue-and-retry (Section 1, Q8)
- `AccountManager` (Android) and `Credential Manager` (Android 14+)

**Approach**

1. **Define the session model and its lifetimes.**

   | Credential | TTL | Storage | Notes |
   |---|---|---|---|
   | Access token | 5–15 min | Memory (prefer) + Keystore-encrypted at rest | Carries `exp`, `jti`, `scope`, `sessionId` |
   | Refresh token | 30–90 days | Keystore-encrypted store; server-side record | Rotated on every use; single-use |
   | Session record (server) | — | DB | `sessionId`, device id, created/last-used, revoked flag, token family id |

   The **absolute session lifetime** and the **idle timeout** are enforced server-side: a session expires after N days regardless of activity, or after M minutes of inactivity. The client cannot enforce these, and must handle a "session expired" response.

2. **Proactive refresh.** Store `expiresAt` with the token and refresh before expiry with a skew buffer (60–120s for clock drift):
   ```kotlin
   suspend fun currentAccessToken(): String {
       val token = store.token ?: throw AuthExpiredException()
       if (clock.nowMillis() < token.expiresAt - SKEW_MS) return token.value
       return refreshSingleFlight().value
   }
   ```
   Proactive refresh means the 401 path is rare, so the concurrent-refresh problem is rare too.

3. **Single-flight refresh for the reactive path** (the `Deferred` pattern from Section 1, Q8). All concurrent requests that hit a 401 share one refresh; each then retries its original request.

4. **Refresh token rotation with reuse detection — the most important server-side control.** On each refresh the server:
   - issues a **new refresh token**,
   - **invalidates the old one**,
   - records that the old token was consumed.
   If a token that was already consumed is presented again, that means either a replay attack or a stolen token, so the server **revokes the entire session family** and forces a re-login. This is the standard OAuth 2.0 BCP recommendation and it converts a silent token theft into a visible session termination.

5. **Bind the session to the device.** Store a device-generated public key (Keystore) with the session; bind the refresh token to it. A stolen refresh token presented from another device fails the binding check. This is the strongest client-side contribution to session security.

6. **Revocation strategies — pick one deliberately.**
   - **Short TTL + no denylist** — simplest; a revoked access token remains valid for up to its TTL. Acceptable for low-risk apps; the access TTL bounds the exposure.
   - **Denylist of revoked `jti`** — the access token carries a `jti`; the server checks the denylist on each request. Immediate revocation; costs a lookup per request (mitigate with a short-lived cached denylist).
   - **Session version / `sid` claim** — the token carries a session id; the server checks that the session is active. Revocation is a single DB flag; the cost is the same per-request lookup.
   - **Hybrid** — short access TTL (5 min) + denylist for the high-risk operations only.

7. **Handle the "session expired" outcome end-to-end.**
   - The repository maps a 401-on-refresh to `AuthExpiredException`.
   - The auth state becomes `SignedOut(reason = Expired)`.
   - The UI navigates to login **preserving the intended destination** so the user returns to where they were.
   - Concurrent requests that fail with an expired session must not each trigger a separate navigation — use a **single-shot event** (a `Channel` or `SharedFlow(replay=1)` consumed once).

8. **Distinguish "logout" from "session expired".** Logout must **revoke server-side** (delete the session, invalidate the refresh token) and clear local state. If the logout call fails, revoke locally anyway and enqueue a best-effort revocation for later — but rely on the short access TTL as the backstop.

9. **"Sign out everywhere"** — the account page lets the user revoke all sessions. The server marks all sessions for that user revoked (or bumps a `tokensValidBefore` timestamp so all access tokens issued earlier become invalid). The latter is cheaper than a per-token denylist and immediate.

10. **Idle timeout in the client.** Even if the server session is alive, the client should sign out after inactivity on the device (a common banking requirement), and cancel the refresh scheduler.

11. **Secure the refresh token at rest** — Keystore-encrypted store, excluded from backup, and consider device binding. On Android 14+, the **Credential Manager** (`androidx.credentials`) is the recommended way to store credentials and to support passkeys — worth mentioning as the modern direction.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Short access TTL + long refresh | Limits the exposure of a leaked access token; simple | More refresh traffic; a revoked access token stays valid until expiry |
| Refresh rotation with reuse detection | Turns theft into a detectable, terminable event | More server state; a client bug that double-refreshes can log the user out (hence single-flight!) |
| Denylist | Immediate revocation | Lookup cost per request; cache invalidation complexity |
| `tokensValidBefore` cutoff | Cheap, immediate, covers all tokens | Requires re-issuance; does not distinguish devices |
| Device-bound refresh token | A stolen token is useless elsewhere | Backend complexity; key invalidation on device reset logs the user out |
| Proactive refresh | Avoids 401s and the concurrent-refresh storm | Requires accurate expiry; a clock skew causes premature or late refreshes |
| Reactive refresh only | Simpler | Guarantees 401 storms under load; worst case for the server |
| Long access TTL | Fewer round trips | A leaked token is valid for a long time — unacceptable for banking |

**Key detail:** The **single-flight refresh and the refresh-token rotation are two halves of the same requirement**: rotation means only one refresh can legitimately succeed with a given refresh token, so concurrent refreshes would look like a replay attack and revoke the session. Single-flight is what makes rotation safe. This connection is the most valuable thing to say in this answer.

**Code example**

```kotlin
// Session manager: proactive refresh + single-flight reactive refresh.
class SessionManager(
    private val store: TokenStore,
    private val api: AuthApi,
    private val scope: CoroutineScope,
    private val clock: Clock,
) {
    private val mutex = Mutex()
    @Volatile private var inFlight: Deferred<Tokens>? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    suspend fun accessToken(): String {
        val token = store.tokens() ?: throw AuthExpiredException()
        if (clock.nowMillis() < token.expiresAt - SKEW_MS) return token.access   // proactive
        return refresh().access
    }

    suspend fun refresh(): Tokens {
        inFlight?.let { return it.await() }              // join the existing refresh
        val deferred = scope.async { performRefresh() }
        inFlight = deferred
        try {
            return deferred.await()
        } finally {
            mutex.withLock { if (inFlight === deferred) inFlight = null }
        }
    }

    private suspend fun performRefresh(): Tokens {
        val current = store.tokens() ?: throw AuthExpiredException()
        val resp = api.refresh(current.refresh)
        if (resp.code() == 401) {
            // The server detected refresh-token reuse → the session family is revoked.
            store.clear()
            _authState.value = AuthState.SignedOut(reason = Revoked)
            throw AuthExpiredException()
        }
        val fresh = resp.body()!!                       // note: the refresh token ROTATES
        store.save(fresh)
        return fresh
    }

    suspend fun logout() {
        val current = store.tokens()
        runCatching { api.logout(current?.refresh) }      // best effort, server-side revoke
        store.clear()
        _authState.value = AuthState.SignedOut(reason = UserInitiated)
    }
}
```

**Talking points / follow-ups**
- *"What if the app is killed between the refresh request and saving the new token?"* → The old refresh token was already consumed → the next refresh presents a consumed token → reuse detection revokes the session. This is a real failure mode: **save the new token before any other work, and treat "unknown after refresh" carefully.** Mitigation: the server should tolerate a short grace window for the immediately-previous token (a "token family grace period") — a common practical fix, with the trade-off of a slightly weaker reuse signal.
- *"How long should the access token live?"* → Balance: shorter = more refresh traffic and more chance of hitting the reuse issue; longer = bigger exposure. 5–15 minutes is the common range for sensitive apps.
- *"What happens on device loss?"* → The session is server-side; the user revokes it from another device ("sign out everywhere"). The refresh token on the lost device becomes invalid when the family is revoked.
- *"Does the client need a `jti` denylist locally?"* → No — revocation is a server decision; the client just handles the 401/expired outcome.
- *"How do you test concurrent refresh?"* → Fire N concurrent requests against a fake that 401s once; assert the refresh endpoint was called exactly once and all requests succeeded.
- *"How do you handle the case where refresh requires the device key but the key was invalidated?"* → Catch the cryptographic error, treat it as an expired session, and require re-login + re-enrolment of the device key.

---

## Section Summary — Staff-Level Themes

| Theme | Questions |
|---|---|
| **Threat modelling before controls** | 1, 2, 3, 7, 8 — name the adversary before choosing a defence |
| **"The data should not be here"** | 1, 2, 5 — tokenisation, envelope encryption, redaction at source |
| **Server-side authority** | 3, 7, 8, 9 — the client is untrusted; the server decides, verifies, and revokes |
| **Key management** | 1, 2 — Keystore hierarchy, envelope encryption, rotation, crypto-erasure |
| **Lifecycle correctness of credentials** | 4, 9 — session scope, epoch fence, single-flight refresh, rotation + reuse detection |
| **The WebView as a browser** | 6 — allow-list navigation, minimal bridge, CSP, no tokens in URLs |

**The one sentence to memorise for this section:**
> *"Security architecture on mobile is mostly about what you *do not* accept: do not accept a token in a URL, do not accept a root signal as a verdict, do not accept a client-generated id as a guarantee of single execution, and do not accept that anything you store on a possibly-compromised device is safe — bound it, shorten it, and revoke it."*
