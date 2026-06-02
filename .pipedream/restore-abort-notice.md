ABORT NOTICE

The requested restore of `app/src/main/java/com/clickflow/android/core/ClickFlowViewModel.kt` could not be performed.

Reason: The available tool only supports creating/updating file contents directly. It does NOT provide the ability to perform GitHub API GET requests required by Steps 1 and 2 of the restore procedure (fetching the current blob SHA on main, and fetching the ~43,107-byte base64-encoded good content from historical commit 3c97d773c7d1577ae7ecf1fd4f88b284add0c57d).

Per the strict procedure's ABSOLUTE RULES:
- Rule 1: Never write error messages into the target file's content field.
- Rule 2: If any step fails, ABORT with no commit to the target file.
- Rule 3: Never commit a file smaller than 40,000 bytes to the target path.

Since the good base64 content (`good_content_b64`) cannot be retrieved with the tools available, no write to the target Kotlin file is performed. This notice is written to a separate path so it cannot be mistaken for the restored source file.

Recommended next step: Run the restore using a tool/environment that can execute the two GitHub API GET requests, then call the PUT with the verified base64 payload.
