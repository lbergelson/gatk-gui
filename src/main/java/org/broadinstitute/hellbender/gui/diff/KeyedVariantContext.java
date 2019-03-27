package org.broadinstitute.hellbender.gui.diff;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class KeyedVariantContext {
    private final Map<String, String> coreMap;
    private Map<String, String> infoMap;
    private Map<String, Map<String,String>> sampleMap;

    KeyedVariantContext(VariantContext vc) {
        coreMap = buildCoreMap(vc);
        infoMap = buildInfoMap(vc);
        sampleMap = buildSampleMap(vc);
    }

    private Map<String, Map<String, String>> buildSampleMap(VariantContext vc) {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        for( String sample: vc.getSampleNamesOrderedByName()){
            map.put(sample, buildFormatMap(vc, sample));
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<String, String> buildFormatMap(VariantContext vc, String sample) {
        final Genotype genotype = vc.getGenotype(sample);
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("GT", genotype.getGenotypeString());
        if(genotype.hasAD()) { map.put("AD", Arrays.toString(genotype.getAD())); }
        if(genotype.hasDP()) { map.put("DP", Integer.toString(genotype.getDP())); }
        if(genotype.hasGQ()) { map.put("GQ", Integer.toString(genotype.getGQ())); }
        if(genotype.hasPL()) { map.put("PL", Arrays.toString(genotype.getPL())); }
        genotype.getExtendedAttributes().forEach( (key, value) -> {
            map.put(key, value.toString());
        });
        return Collections.unmodifiableMap(map);
    }

    private static Map<String,String> buildCoreMap(VariantContext vc) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Chrom", vc.getContig());
        map.put("Start", String.valueOf(vc.getStart()));
        map.put("ID", vc.getID());
        map.put("Ref", vc.getReference().getBaseString());
        map.put("Alt", vc.getAlternateAlleles().stream().map(Allele::getBaseString).collect(Collectors.joining(",")));
        map.put("Qual", String.valueOf(vc.getPhredScaledQual()));
        map.put("Filter", String.join(",", vc.getFilters()));
        return Collections.unmodifiableMap(map);
    }

    private static Map<String,String> buildInfoMap(VariantContext vc) {
        Map<String, String> map = new LinkedHashMap<>();
        final Map<String, Object> attributes = vc.getAttributes();
        attributes.forEach((key, value) -> map.put(key, value.toString()));
        return Collections.unmodifiableMap(map);
    }

    public Map<String, String> getCoreFields(){
       return coreMap;
    }

    public Map<String, String> getInfoFields(){
        return infoMap;
    }

    public Map<String, Map<String, String>> getSampleMap(){
        return sampleMap;
    }
}
