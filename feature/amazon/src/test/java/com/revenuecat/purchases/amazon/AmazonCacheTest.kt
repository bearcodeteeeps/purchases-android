package com.revenuecat.purchases.amazon

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.caching.DeviceCache
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AmazonCacheTest {
    private val apiKey = "api_key"
    private lateinit var cache: MockDeviceCache

    private lateinit var underTest: AmazonCache

    class MockDeviceCache(
        preferences: SharedPreferences,
        apiKey: String
    ) : DeviceCache(preferences, apiKey) {

        var stubCache = mutableMapOf<String, String>()

        override fun getJSONObjectOrNull(key: String): JSONObject? {
            return stubCache[key]?.let {
                JSONObject(it)
            }
        }

        override fun putString(
            cacheKey: String,
            value: String
        ) {
            stubCache[cacheKey] = value
        }
    }

    @Before
    fun setup() {
        cache = MockDeviceCache(mockk(), apiKey)
        underTest = AmazonCache(cache)
    }

    @Test
    fun `getting cached term skus when there is nothing cached`() {
        val receiptTermSkus = underTest.getReceiptSkus()

        assertThat(receiptTermSkus).isEmpty()
    }

    @Test
    fun `getting cached term skus when there is termskus cached`() {
        val expected = mapOf(
            "1234abcdreceiptid" to "com.revenuecat.subscription.weekly",
            "4321abcdreceiptid" to "com.revenuecat.subscription.monthly"
        )

        val cachedReceiptsToTermSkusJSON = getStoredJSONFromMap(expected)

        cache.stubCache[underTest.amazonPostedTokensKey] = cachedReceiptsToTermSkusJSON

        val receiptTermSkus = underTest.getReceiptSkus()

        assertThat(receiptTermSkus).isEqualTo(
            mapOf(
                "1234abcdreceiptid" to "com.revenuecat.subscription.weekly",
                "4321abcdreceiptid" to "com.revenuecat.subscription.monthly"
            )
        )
    }

    @Test
    fun `set receipt term skus on an empty cache`() {
        val expected = mapOf(
            "1234abcdreceiptid" to "com.revenuecat.subscription.weekly",
            "4321abcdreceiptid" to "com.revenuecat.subscription.monthly"
        )

        underTest.setReceiptSkus(expected)

        val actualStoredJSON = JSONObject(cache.stubCache[underTest.amazonPostedTokensKey])
        val actualStoredMapAsJSON = actualStoredJSON["receiptsToSkus"] as JSONObject

        assertThat(actualStoredMapAsJSON).isNotNull
        assertThat(actualStoredMapAsJSON.keys().asSequence().count()).isEqualTo(expected.size)

        expected.forEach { (key, value) ->
            assertThat(actualStoredMapAsJSON[key]).isEqualTo(value)
        }
    }

    @Test
    fun `set receipt term skus on a non empty cache`() {
        val alreadyCached = mapOf(
            "1234abcdreceiptid" to "com.revenuecat.subscription.weekly"
        )

        val cachedReceiptsToTermSkusJSON = getStoredJSONFromMap(alreadyCached)
        cache.stubCache[underTest.amazonPostedTokensKey] = cachedReceiptsToTermSkusJSON

        val newToCache = mapOf(
            "4321abcdreceiptid" to "com.revenuecat.subscription.monthly"
        )

        underTest.setReceiptSkus(newToCache)

        val actualStoredJSON = JSONObject(cache.stubCache[underTest.amazonPostedTokensKey])
        val actualStoredMapAsJSON = actualStoredJSON["receiptsToSkus"] as JSONObject

        assertThat(actualStoredMapAsJSON).isNotNull

        val expected = alreadyCached + newToCache

        assertThat(actualStoredMapAsJSON.keys().asSequence().count()).isEqualTo(expected.size)

        expected.forEach { (key, value) ->
            assertThat(actualStoredMapAsJSON[key]).isEqualTo(value)
        }
    }

    @Test
    fun `overriding a receipt term sku`() {
        val alreadyCached = mapOf(
            "1234abcdreceiptid" to "com.revenuecat.subscription.weekly"
        )

        val cachedReceiptsToTermSkusJSON = getStoredJSONFromMap(alreadyCached)
        cache.stubCache[underTest.amazonPostedTokensKey] = cachedReceiptsToTermSkusJSON

        val expected = mapOf(
            "1234abcdreceiptid" to "com.revenuecat.subscription.monthly"
        )

        underTest.setReceiptSkus(expected)

        val actualStoredJSON = JSONObject(cache.stubCache[underTest.amazonPostedTokensKey])
        val actualStoredMapAsJSON = actualStoredJSON["receiptsToSkus"] as JSONObject

        assertThat(actualStoredMapAsJSON).isNotNull
        assertThat(actualStoredMapAsJSON.keys().asSequence().count()).isEqualTo(expected.size)

        expected.forEach { (key, value) ->
            assertThat(actualStoredMapAsJSON[key]).isEqualTo(value)
        }
    }

    private fun getStoredJSONFromMap(expected: Map<String, String>) = """
                { "receiptsToSkus": 
                    {
                        ${expected.map { "\"${it.key}\": \"${it.value}\"" }.joinToString()}
                    }
                }
            """.trimIndent()
}