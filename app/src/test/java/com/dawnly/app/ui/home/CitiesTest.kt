package com.dawnly.app.ui.home

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The cities offered by Add City: the phone must know each zone, and each is offered once. */
class CitiesTest {
    @Test
    fun everyCityKeepsTimeInAZoneThePhoneKnows() {
        for (city in Cities) {
            // Throws ZoneRulesException for a zone the phone's calendar doesn't know.
            ZoneId.of(city.zoneId)
        }
    }

    @Test
    fun noCityIsOfferedTwice() {
        assertEquals(Cities.size, Cities.map { it.zoneId }.toSet().size)
        assertEquals(Cities.size, Cities.map { it.label }.toSet().size)
    }

    @Test
    fun noCityIsBlank() {
        assertTrue(Cities.none { it.zoneId.isBlank() })
    }

    @Test
    fun theCatalogCoversEveryBroadRegion() {
        val zones = Cities.map { it.zoneId }
        for (region in listOf("Europe/", "Africa/", "Asia/", "America/", "Australia/", "Pacific/")) {
            assertTrue("no city in $region", zones.any { it.startsWith(region) })
        }
    }

    @Test
    fun theFamiliarCitiesAreThere() {
        val zones = Cities.map { it.zoneId }.toSet()
        for (zone in listOf("Europe/London", "Asia/Kolkata", "Asia/Tokyo", "America/New_York", "Australia/Sydney")) {
            assertTrue("$zone is missing", zone in zones)
        }
    }

    @Test
    fun citiesAlreadyOnTheListAreNotOfferedAgain() {
        val choices = cityChoices(listOf("Europe/London", "Asia/Tokyo"))
        assertTrue(choices.none { it.zoneId == "Europe/London" })
        assertTrue(choices.none { it.zoneId == "Asia/Tokyo" })
        assertEquals(Cities.size - 2, choices.size)
    }

    @Test
    fun anEmptyListOffersEveryCity() {
        assertEquals(Cities, cityChoices(emptyList()))
    }

    @Test
    fun anUnknownZoneOnTheListLeavesTheChoicesWhole() {
        // A saved zone the catalog no longer offers (or never did) hides nothing.
        assertEquals(Cities, cityChoices(listOf("Arctic/Longyearbyen")))
    }
}
