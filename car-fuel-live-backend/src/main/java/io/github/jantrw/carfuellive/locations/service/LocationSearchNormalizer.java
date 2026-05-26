package io.github.jantrw.carfuellive.locations.service;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shared normalization rules for location search input and stored aliases.
 *
 * <p>The utility keeps user input, primary names, and alternate spellings comparable across
 * accents, umlauts, digraphs, and punctuation differences.
 */
final class LocationSearchNormalizer {

  private LocationSearchNormalizer() {}

  static String normalize(String value) {
    return normalizeInternal(value, true);
  }

  static String normalizeFolded(String value) {
    return normalizeInternal(value, false);
  }

  // Some searches should match both digraph-preserving and fully folded variants, for example
  // "muenchen" and "munchen".
  static List<String> searchVariants(String value) {
    final Set<String> variants = new LinkedHashSet<>();
    addVariant(variants, normalize(value));
    addVariant(variants, normalizeFolded(value));
    return List.copyOf(variants);
  }

  private static void addVariant(Set<String> variants, String candidate) {
    if (!candidate.isBlank()) {
      variants.add(candidate);
    }
  }

  private static String normalizeInternal(String value, boolean expandGermanicDigraphs) {
    if (value == null || value.isBlank()) {
      return "";
    }

    String normalized = value.trim().toLowerCase(Locale.ROOT);
    normalized =
        expandGermanicDigraphs
            ? normalized.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue")
            : normalized.replace("ä", "a").replace("ö", "o").replace("ü", "u");
    normalized =
        normalized.replace("ß", "ss").replace("æ", "ae").replace("œ", "oe").replace("ø", "o");
    normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    normalized = normalized.replaceAll("[^a-z0-9]+", " ");
    return normalized.replaceAll("\\s+", " ").trim();
  }
}
