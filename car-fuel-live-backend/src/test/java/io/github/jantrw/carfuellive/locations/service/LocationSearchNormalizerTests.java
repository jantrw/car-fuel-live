package io.github.jantrw.carfuellive.locations.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LocationSearchNormalizerTests {

  @Test
  void should_normalizeExpandedGermanicSearchText() {
    assertThat(LocationSearchNormalizer.normalize("Köln")).isEqualTo("koeln");
    assertThat(LocationSearchNormalizer.normalize("München")).isEqualTo("muenchen");
    assertThat(LocationSearchNormalizer.normalize("Zürich")).isEqualTo("zuerich");
    assertThat(LocationSearchNormalizer.normalize("Göteborg")).isEqualTo("goeteborg");
  }

  @Test
  void should_normalizeFoldedSearchTextWithoutGermanicDigraphExpansion() {
    assertThat(LocationSearchNormalizer.normalizeFolded("Köln")).isEqualTo("koln");
    assertThat(LocationSearchNormalizer.normalizeFolded("München")).isEqualTo("munchen");
    assertThat(LocationSearchNormalizer.normalizeFolded("Zürich")).isEqualTo("zurich");
    assertThat(LocationSearchNormalizer.normalizeFolded("Göteborg")).isEqualTo("goteborg");
  }

  @Test
  void should_returnBothSearchVariantsWhenTheyDiffer() {
    assertThat(LocationSearchNormalizer.searchVariants("Köln")).containsExactly("koeln", "koln");
    assertThat(LocationSearchNormalizer.searchVariants("Göteborg"))
        .containsExactly("goeteborg", "goteborg");
    assertThat(LocationSearchNormalizer.searchVariants("Paris")).containsExactly("paris");
  }
}
