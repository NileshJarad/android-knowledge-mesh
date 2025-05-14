# EncryptedSharedPreferences

A wrapper over SharedPreferences that encrypts keys and values using keys from Android Keystore.

- Storing passwords, tokens, API keys
- Flags for login status, etc.
- Uses AES encryption under the hood 
- Encrypts both keys and values 
- Stores data in standard SharedPreferences file 
- Encrypts with a master key stored in Android Keystore




**Dependency:**

```xml
implementation "androidx.security:security-crypto:1.1.0-alpha03"
```


**Sample code**

```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

encryptedPrefs.edit().putString("password", "password_to_store").apply()

```


## Note

**API Level Support**

| Feature	                    | Minimum API Level                                 |
|-----------------------------|---------------------------------------------------|
| EncryptedSharedPreferences	 | API 23 (Android 6.0)                              |
| MasterKey	                  | API 23 (uses Android Keystore internally)         |
| StrongBox-backed keys	      | API 28+ (Android 9.0) — optional hardware support |