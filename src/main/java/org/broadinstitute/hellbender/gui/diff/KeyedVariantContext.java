package org.broadinstitute.hellbender.gui.diff;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFormatHeaderLine;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import org.broadinstitute.hellbender.exceptions.GATKException;

import java.util.*;
import java.util.stream.Collectors;

public final class KeyedVariantContext {
    private final Map<String, String> coreMap;
    private final Map<String, String> infoMap;
    private final Map<String, Map<String,String>> sampleMap;

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
        if(genotype.isFiltered()) { map.put("FT", genotype.getFilters()); }
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

    private Map<String, String> getCoreFields(){
       return coreMap;
    }

    private Map<String, String> getInfoFields(){
        return infoMap;
    }

    private Map<String, Map<String, String>> getSampleMap(){
        return sampleMap;
    }

    public String getValue(Key k){
        switch (k.type) {
            case Core: return getCoreFields().getOrDefault(k.name, "");
            case Info: return getInfoFields().getOrDefault(k.name, "");
            case Format: return getSampleMap()
                    .getOrDefault(k.sample,Collections.emptyMap())
                    .getOrDefault(k.name,"");
            default: throw new GATKException("Unknown key type: " + k.type.toString());
        }
    }

//    public Set<Key> getKeySet(){
//        return keySet;
//    }

    public static Set<Key> getPotentialKeys(VCFHeader vcfHeader){
        final LinkedHashSet<Key> keys = new LinkedHashSet<>();
        keys.add(Key.CHROM);
        keys.add(Key.START);
        keys.add(Key.ID);
        keys.add(Key.Ref);
        keys.add(Key.Alt);
        keys.add(Key.Qual);
        keys.add(Key.Filter);

        for(VCFInfoHeaderLine line: vcfHeader.getInfoHeaderLines()){
            keys.add(Key.info(line.getID()));
        }
        for(String sample: vcfHeader.getSampleNamesInOrder()){
            keys.add(Key.format(sample, "GT"));
            keys.add(Key.format(sample, "AD"));
            keys.add(Key.format(sample, "DP"));
            keys.add(Key.format(sample, "GQ"));
            keys.add(Key.format(sample, "PL"));
            keys.add(Key.format(sample, "FT"));
            for(VCFFormatHeaderLine line: vcfHeader.getFormatHeaderLines()){
                keys.add(Key.format(sample, line.getID()));
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    public static class Key {

        static final Key CHROM = core("Chrom");
        static final Key START = core("Start");
        static final Key ID = core("ID");
        static final Key Ref = core("Ref");
        static final Key Alt = core("Alt");
        static final Key Qual = core("Qual");
        static final Key Filter = core("Filter");

        private enum Type { Core, Info, Format}
        private final Type type;
        private final String name;
        private final String sample;

        private Key(Type type, String name, String sample) {
            this.type = type;
            this.name = name;
            this.sample = sample;
        }

        static Key core(String name){
            return new Key(Type.Core, name, null);
        }

        static Key info(String name){
            return new Key(Type.Info, name, null);
        }

        static Key format(String sample, String name){
            return new Key(Type.Format, name, sample);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return type == key.type &&
                    Objects.equals(name, key.name) &&
                    Objects.equals(sample, key.sample);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, name, sample);
        }
    }
}
