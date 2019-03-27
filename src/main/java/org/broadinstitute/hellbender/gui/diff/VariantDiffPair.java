package org.broadinstitute.hellbender.gui.diff;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFEncoder;
import htsjdk.variant.vcf.VCFHeader;
import javafx.util.Pair;

import java.util.Objects;
import java.util.function.Function;

public class VariantDiffPair extends Pair<VariantContext, VariantContext> {
    private final VCFEncoder encoder;

    /**
     * Creates a new pair
     *
     * @param key   The key for this pair
     * @param value The value to use for this pair
     */
    public VariantDiffPair(VariantContext key, VariantContext value, VCFEncoder encoder, VCFHeader header) {
        super(key, value);
        this.encoder = encoder;

    }

    public VCFCompare.DiffDisplay getDisplay(Function<VariantContext, String> getter) {
        String leftString = getKey() == null ? null : getter.apply(getKey());
        String rightString = getValue() == null ? null : getter.apply(getValue());
        return new VCFCompare.DiffDisplay(leftString, rightString);
    }

    public boolean mismatching(){
        if((Objects.isNull(getKey()) && ! Objects.isNull(getValue()))
        || (Objects.isNull(getValue()) && ! Objects.isNull(getKey()))){
            return true;
        }
        return !encoder.encode(getKey()).equals(encoder.encode(getValue()));
    }

}
