package com.example.httpsender.kt

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

/**
 * User: ljx
 * Date: 2020/9/24
 * Time: 15:33
 */

fun Uri.dimQuery(context: Context, displayName: String) {
    context.contentResolver.query(this, null,
        "_display_name LIKE '%$displayName%'",null, null)?.use {
        while (it.moveToNext()) {
            val id = it.getString(it.getColumnIndex(MediaStore.MediaColumns._ID))
            val name = it.getString(it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
            val data = it.getString(it.getColumnIndex(MediaStore.MediaColumns.DATA))
            Log.e("LJX", "id=$id name=$name data=$data")
        }
    }
}

fun Uri.dimDelete(context: Context, displayName: String) {
    val delete = context.contentResolver.delete(this, "_display_name LIKE '%$displayName%'", null)
    Log.e("LJX", "delete=$delete")
}