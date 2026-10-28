package com.eleven.store

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/** مزوّد App Check لنسخة debug — راجع ElevenStoreApp.initFirebaseAppCheck. */
internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    DebugAppCheckProviderFactory.getInstance()
