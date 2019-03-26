//package org.broadinstitute.hellbender.gui;
//
//import com.google.common.annotations.VisibleForTesting;
//import htsjdk.variant.variantcontext.Genotype;
//import htsjdk.variant.variantcontext.VariantContext;
//import org.apache.commons.collections4.CollectionUtils;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class VcfComparator {
//
//    /**
//     * Normalizes the representation of Strings containing doubles.
//     * This is necessary to deal with the fact that variant context attributes are deserialized from vcf as Strings
//     * instead of their original type.  Some versions of gatk3 output double attributes in scientific notation while gatk4
//     * doesn't do so.
//     *
//     * @param attribute an attribute to attempt to normalize
//     * @return if attribute is a String, try to parse it as a Double and return that value, else return the original attribute
//     */
//    @VisibleForTesting
//    static Object normalizeScientificNotation(final Object attribute){
//        if (attribute instanceof String){
//            try {
//                if (((String) attribute).contains("|")) {
//                    // If the attribute is an allele specific attribute separated by '|', then we want to remap
//                    // each of its contained values (which could be comma separated lists) separately
//                    String[] split = ((String) attribute).split("\\|",-1);
//                    return Arrays.stream(split).map(
//                            s -> {return Arrays.stream(s.split(",",-1))
//                                    .map(d -> {if (d.equals("")) return d;
//                                    else return Double.toString(Double.parseDouble(d));})
//                                    .collect(Collectors.joining(","));})
//                            .collect(Collectors.joining("|"));
//                } else {
//                    return Double.parseDouble((String) attribute);
//                }
//            } catch ( final NumberFormatException e) {
//                return attribute;
//            }
//        }
//        return attribute;
//    }
//
//    private static void assertAttributeEquals(final String key, final Object actual, final Object expected) {
//        final Object notationCorrectedActual = normalizeScientificNotation(actual);
//        final Object notationCorrectedExpected = normalizeScientificNotation(expected);
//        if (notationCorrectedExpected instanceof Double && notationCorrectedActual instanceof Double) {
//            // must be very tolerant because doubles are being rounded to 2 sig figs
//            BaseTest.assertEqualsDoubleSmart((Double) notationCorrectedActual, (Double) notationCorrectedExpected, 1e-2, "Attribute " + key);
//        } else if (actual instanceof Integer || expected instanceof Integer) {
//            Object actualNormalized = normalizeToInteger(actual);
//            Object expectedNormalized = normalizeToInteger(expected);
//            Assert.assertEquals(actualNormalized, expectedNormalized, "Attribute " + key);
//        } else {
//            Assert.assertEquals(notationCorrectedActual, notationCorrectedExpected, "Attribute " + key);
//        }
//    }
//
//
//    @SuppressWarnings("unchecked")
//    private static void assertAttributesEquals(final Map<String, Object> actual, final Map<String, Object> expected) {
//        final Set<String> expectedKeys = new LinkedHashSet<>(expected.keySet());
//
//        for ( final Map.Entry<String, Object> act : actual.entrySet() ) {
//            final Object actualValue = act.getValue();
//            if ( expected.containsKey(act.getKey()) && expected.get(act.getKey()) != null ) {
//                final Object expectedValue = expected.get(act.getKey());
//                if (expectedValue instanceof List && actualValue instanceof List) {
//                    // both values are lists, compare element b element
//                    List<Object> expectedList = (List<Object>) expectedValue;
//                    List<Object> actualList = (List<Object>) actualValue;
//                    Assert.assertEquals(actualList.size(), expectedList.size());
//                    for (int i = 0; i < expectedList.size(); i++) {
//                        assertAttributeEquals(act.getKey(), actualList.get(i), expectedList.get(i));
//                    }
//                } else if (expectedValue instanceof List) {
//                    // expected is a List but actual is not; normalize to String and compare
//                    Assert.assertTrue(actualValue instanceof String, "Attempt to compare list to a non-string value");
//                    final String expectedString = ((List<Object>) expectedValue).stream().map(v -> v.toString()).collect(
//                            Collectors.joining(","));
//                    assertAttributeEquals(act.getKey(), actualValue, expectedString);
//                }
//                else if (actualValue instanceof List) {
//                    // actual is a List but expected is not; normalize to String and compare
//                    Assert.assertTrue(expectedValue instanceof String, "Attempt to compare list to a non-string value");
//                    final String actualString = ((List<Object>) actualValue).stream().map(v -> v.toString()).collect(Collectors.joining(","));
//                    assertAttributeEquals(act.getKey(), actualString, expectedValue);
//                } else {
//                    assertAttributeEquals(act.getKey(), actualValue, expectedValue);
//                }
//            } else {
//                // it's ok to have a binding in x -> null that's absent in y
//                Assert.assertNull(actualValue, act.getKey() + " present in one but not in the other");
//            }
//            expectedKeys.remove(act.getKey());
//        }
//
//        // now expectedKeys contains only the keys found in expected but not in actual,
//        // and they must all be null
//        for ( final String missingExpected : expectedKeys ) {
//            final Object value = expected.get(missingExpected);
//            Assert.assertTrue(isMissing(value), "Attribute " + missingExpected + " missing in one but not in other" );
//        }
//    }
//
//    public static void assertVariantContextsAreEqual(final VariantContext actual, final VariantContext expected, final List<String> attributesToIgnore) {
//        Assert.assertNotNull(actual, "VariantContext expected not null");
//        Assert.assertEquals(actual.getContig(), expected.getContig(), "chr");
//        Assert.assertEquals(actual.getStart(), expected.getStart(), "start");
//        Assert.assertEquals(actual.getEnd(), expected.getEnd(), "end");
//        Assert.assertEquals(actual.getID(), expected.getID(), "id");
//        Assert.assertEquals(actual.getAlleles(), expected.getAlleles(), "alleles for " + expected + " vs " + actual);
//        assertAttributesEquals(filterIgnoredAttributes(actual.getAttributes(), attributesToIgnore),
//                               filterIgnoredAttributes(expected.getAttributes(), attributesToIgnore));
//
//        Assert.assertEquals(actual.filtersWereApplied(), expected.filtersWereApplied(), "filtersWereApplied");
//        Assert.assertEquals(actual.isFiltered(), expected.isFiltered(), "isFiltered");
//        Assert.assertEquals(actual.getFilters(), expected.getFilters(), "filters");
//        BaseTest.assertEqualsDoubleSmart(actual.getPhredScaledQual(), expected.getPhredScaledQual());
//
//        variantContextsHaveSameGenotypes(actual, expected, attributesToIgnore);
//    }
//
//    public static void assertGenotypesAreEqual(final Genotype actual, final Genotype expected, final List<String> extendedAttributesToIgnore) {
//        Assert.assertEquals(actual.getSampleName(), expected.getSampleName(), "Genotype names");
//        Assert.assertTrue(CollectionUtils.isEqualCollection(actual.getAlleles(), expected.getAlleles()), "Genotype alleles");
//        Assert.assertEquals(actual.getGenotypeString(false), expected.getGenotypeString(false), "Genotype string");
//        Assert.assertEquals(actual.getType(), expected.getType(), "Genotype type");
//
//        // filters are the same
//        Assert.assertEquals(actual.getFilters(), expected.getFilters(), "Genotype fields");
//        Assert.assertEquals(actual.isFiltered(), expected.isFiltered(), "Genotype isFiltered");
//
//        // inline attributes
//        Assert.assertEquals(actual.hasDP(), expected.hasDP(), "Genotype hasDP");
//        Assert.assertEquals(actual.getDP(), expected.getDP(), "Genotype dp");
//        Assert.assertEquals(actual.hasAD(), expected.hasAD(), "Genotype hasAD");
//        Assert.assertEquals(actual.getAD(), expected.getAD(), "Genotype AD");
//        Assert.assertEquals(actual.hasGQ(), expected.hasGQ(), "Genotype hasGQ");
//        Assert.assertEquals(actual.getGQ(), expected.getGQ(), "Genotype gq");
//        Assert.assertEquals(actual.hasPL(), expected.hasPL(), "Genotype hasPL: " + actual.toString());
//        Assert.assertEquals(actual.getPL(), expected.getPL(), "Genotype PL");
//
//        Assert.assertEquals(actual.hasLikelihoods(), expected.hasLikelihoods(), "Genotype haslikelihoods");
//        Assert.assertEquals(actual.getLikelihoodsString(), expected.getLikelihoodsString(), "Genotype getlikelihoodsString");
//        Assert.assertEquals(actual.getLikelihoods(), expected.getLikelihoods(), "Genotype getLikelihoods");
//
//        Assert.assertEquals(actual.getGQ(), expected.getGQ(), "Genotype phredScaledQual");
//        assertAttributesEquals(filterIgnoredAttributes(actual.getExtendedAttributes(), extendedAttributesToIgnore), filterIgnoredAttributes(expected.getExtendedAttributes(), extendedAttributesToIgnore));
//        Assert.assertEquals(actual.isPhased(), expected.isPhased(), "Genotype isPhased");
//        Assert.assertEquals(actual.getPloidy(), expected.getPloidy(), "Genotype getPloidy");
//    }
//
//
//    public static void variantContextsHaveSameGenotypes(final VariantContext actual, final VariantContext expected) {
//        variantContextsHaveSameGenotypes(actual, expected, Collections.emptyList());
//    }
//
//    public static boolean variantContextsHaveSameGenotypes(final VariantContext actual, final VariantContext expected, final List<String> attributesToIgnore) {
//        Assert.assertEquals(actual.hasGenotypes(), expected.hasGenotypes(), "hasGenotypes");
//        if ( expected.hasGenotypes() ) {
//            BaseTest.assertEqualsSet(actual.getSampleNames(), expected.getSampleNames(), "sample names set");
//            Assert.assertEquals(actual.getSampleNamesOrderedByName(), expected.getSampleNamesOrderedByName(), "sample names");
//            final Set<String> samples = expected.getSampleNames();
//            for ( final String sample : samples ) {
//                assertGenotypesAreEqual(actual.getGenotype(sample), expected.getGenotype(sample), attributesToIgnore);
//            }
//        }
//    }
//
//
//    /**
//     * Attempt to convert a String containing a signed integer (no separators) to an integer. If the attribute is not
//     * a String, or does not contain an integer, the original object is returned.
//     *
//     * @param attribute
//     * @return An Integer representing the value in the attribute if it contains a parseable integer, otherwise the
//     * original attribute.
//     */
//    @VisibleForTesting
//    static Object normalizeToInteger(final Object attribute) {
//        if (attribute instanceof String) {
//            try {
//                return Integer.parseInt((String) attribute);
//            } catch ( final NumberFormatException e) {
//                return attribute;
//            }
//        }
//        return attribute;
//    }
//
//
//    private static Map<String, Object> filterIgnoredAttributes(final Map<String,Object> attributes, final List<String> attributesToIgnore) {
//        return attributes.entrySet().stream()
//                .filter(p -> !attributesToIgnore.contains(p.getKey()) && p.getValue() != null)
//                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//    }
//}
