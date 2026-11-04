package com.back.atlas.service;

import com.back.atlas.dto.Airport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AirportDirectoryTest {

    private static final AirportDirectory directory = new AirportDirectory();

    private static List<String> codes(String keyword) {
        return directory.search(keyword, 10).stream().map(Airport::getIataCode).toList();
    }

    @Test
    void loadsTheBundledDataset() {
        assertThat(directory.size()).isGreaterThan(3_000);
    }

    @Test
    void exactIataCodeComesFirst() {
        assertThat(codes("mxp")).first().isEqualTo("MXP");
        Airport malpensa = directory.search("MXP", 1).getFirst();
        assertThat(malpensa.getName()).contains("Malpensa");
        assertThat(malpensa.getCountryName()).isEqualTo("Italy");
    }

    @Test
    void findsAirportsByCityWithBigAirportsFirst() {
        // Large airports (Heathrow, Gatwick) before medium ones (City, Stansted...).
        assertThat(codes("London").subList(0, 2)).containsExactlyInAnyOrder("LHR", "LGW");
        assertThat(codes("London")).contains("STN", "LCY");
        assertThat(codes("milan")).contains("MXP", "LIN");
    }

    @Test
    void understandsItalianCityNamesAndIgnoresAccents() {
        assertThat(codes("Roma")).contains("FCO", "CIA");
        assertThat(codes("Parigi")).contains("CDG", "ORY");
        assertThat(codes("zurich")).first().isEqualTo("ZRH"); // "Zürich" in the dataset
        assertThat(codes("Città del Messico")).contains("MEX");
        assertThat(codes("citta del messico")).contains("MEX");
    }

    @Test
    void tooShortOrUnknownGivesNothing() {
        assertThat(codes("r")).isEmpty();
        assertThat(codes("xyzzyq")).isEmpty();
    }

    @Test
    void parsesQuotedCsvFields() {
        assertThat(AirportDirectory.parseCsvLine("A,\"Ferno, (VA)\",\"say \"\"hi\"\"\",")).containsExactly("A", "Ferno, (VA)", "say \"hi\"", "");
    }
}
