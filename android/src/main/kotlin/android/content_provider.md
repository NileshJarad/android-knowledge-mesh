# Content Providers

A Content Provider is one of the core Android components (alongside Activities, Services, and Broadcast Receivers) that manages access to structured data. It acts as a bridge between different apps or parts of the same app for sharing data securely.

It uses a URI-based interface and operates using CRUD (Create, Read, Update, Delete) operations.



## Types of Content Providers
There are two kinds:

1. Standard Providers (System-defined) Android provides several built-in content providers. For example:
   - `ContactsContract` → to access contacts 
   - `MediaStore` → to access images, videos, audio 
   - `CalendarContract`, `Settings`, etc.
2. Custom Providers (User-defined)
   You create your own provider by extending ContentProvider class to expose your app’s data to other apps or components.

## When and Why to Use Content Providers
**Use When:**
- You want to share data between apps (e.g., messaging, gallery, file manager). 
- You want data abstraction with permission control. 
- You want to access structured data in a consistent way using URIs. 
- You’re building something like a note-taking or task manager app that could be queried externally.

**Avoid When:**
- You don’t need inter-app communication. 
- Local data is not meant to be accessed outside your app (use Room or SQLite instead).

## How to Use Content Providers
Steps:
1. Create a ContentProvider subclass 
2. Define a content:// URI using UriMatcher 
3. Override CRUD methods (query(), insert(), etc.)
4. Register the provider in AndroidManifest.xml 
5. Access via ContentResolver

**Example**

**Step 1: Define Contract (URI, Columns)**
```kotlin
object BookContract {
    const val AUTHORITY = "com.example.bookprovider"
    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/books")

    object BookEntry {
        const val TABLE_NAME = "books"
        const val COLUMN_ID = "_id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_AUTHOR = "author"
    }
}

```

**Step 2: Create Provider Class**

```kotlin
class BookProvider : ContentProvider() {
    private lateinit var dbHelper: SQLiteOpenHelper

    override fun onCreate(): Boolean {
        dbHelper = object : SQLiteOpenHelper(context, "book.db", null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE ${BookContract.BookEntry.TABLE_NAME} (
                        ${BookContract.BookEntry.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                        ${BookContract.BookEntry.COLUMN_TITLE} TEXT,
                        ${BookContract.BookEntry.COLUMN_AUTHOR} TEXT
                    )
                """)
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val db = dbHelper.readableDatabase
        return db.query(BookContract.BookEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = dbHelper.writableDatabase
        val id = db.insert(BookContract.BookEntry.TABLE_NAME, null, values)
        context?.contentResolver?.notifyChange(uri, null)
        return ContentUris.withAppendedId(BookContract.CONTENT_URI, id)
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?): Int {
        val db = dbHelper.writableDatabase
        return db.update(BookContract.BookEntry.TABLE_NAME, values, selection, selectionArgs)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db = dbHelper.writableDatabase
        return db.delete(BookContract.BookEntry.TABLE_NAME, selection, selectionArgs)
    }

    override fun getType(uri: Uri): String? {
        return "vnd.android.cursor.dir/vnd.${BookContract.AUTHORITY}.books"
    }
}

```

**Step 3: Register in AndroidManifest.xml**

```xml
<provider
    android:name=".BookProvider"
    android:authorities="com.example.bookprovider"
    android:exported="true" />
```

**Step 4: Access Using ContentResolver**

```kotlin
val values = ContentValues().apply {
    put(BookContract.BookEntry.COLUMN_TITLE, "Kotlin 101")
    put(BookContract.BookEntry.COLUMN_AUTHOR, "JetBrains")
}

val uri = context.contentResolver.insert(BookContract.CONTENT_URI, values)

// Querying
val cursor = context.contentResolver.query(BookContract.CONTENT_URI, null, null, null, null)

```



-----

### Can you use Realm in a Content Provider?
✅ Technically: Yes
You can use Realm (or any custom database/storage) inside a ContentProvider, but it's not recommended or straightforward.

❌ Practically: Not Recommended
Realm is not designed to work well with Content Providers, because:

🔍 Why Realm doesn’t play nicely with Content Providers
1. Cursor Requirement
   The query() method of a Content Provider is expected to return a Cursor.
   Realm doesn’t return Cursor objects — it uses its own data access APIs (e.g., RealmResults).

2. Threading
   Realm instances are thread-bound. Content Providers are accessed from different threads (main thread, binder thread pool), which can cause thread issues unless you manage Realm instances carefully.

3. Data Exposure
   Content Providers expose data using a standardized interface (URI + Cursor). Realm is optimized for in-app usage, not external data sharing.

4. No Out-of-the-box Integration
   Realm does not provide built-in support for Content Providers, so you'd have to manually map Realm data to Cursors, which is messy and error-prone.

⚙️ If you must use Realm with a Content Provider
You’d have to:

- Manually create a MatrixCursor (a subclass of Cursor)

- Fill it with data from your Realm query

- Return it from the query() method

```kotlin
override fun query(uri: Uri, ...): Cursor? {
    val cursor = MatrixCursor(arrayOf("_id", "name"))
    val realm = Realm.getDefaultInstance()
    val results = realm.where(Person::class.java).findAll()
    results.forEach {
        cursor.addRow(arrayOf(it.id, it.name))
    }
    return cursor
}
```

