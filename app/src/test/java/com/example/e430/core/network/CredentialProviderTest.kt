package com.example.e430.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialProviderTest {
    @Test
    fun authorizationIsOnlyReturnedForTheLoginSite() {
        val provider = CredentialProvider()
        provider.set(
            ApiCredentials(
                site = E621Site.E621,
                username = "example_user",
                apiKey = "example_key",
            ),
        )

        assertEquals(
            "Basic ZXhhbXBsZV91c2VyOmV4YW1wbGVfa2V5",
            provider.authorizationFor(E621Site.E621),
        )
        assertNull(provider.authorizationFor(E621Site.E926))

        provider.clear()
        assertNull(provider.authorizationFor(E621Site.E621))
    }
}
