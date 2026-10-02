package com.tono.app.auth

import android.content.Context
import wang.harlon.loginbase.SharedPreferencesTokenStore

/** 库的 SharedPreferences 实现同步 commit 落盘，是服务端「丢回执救活」的配套硬要求。 */
fun initLoginbaseAuth(context: Context): LoginbaseAuthManager =
    initLoginbaseAuth(SharedPreferencesTokenStore(context.applicationContext))
