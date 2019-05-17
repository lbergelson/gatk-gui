package org.broadinstitute.hellbender.gui.diff;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFEncoder;
import htsjdk.variant.vcf.VCFHeader;
import javafx.util.Pair;

import java.util.Objects;
import java.util.function.Function;

public class VariantDiffPair {
    private final VCFEncoder encoder;
    private final VariantContext left;
    private final VariantContext right;

    /**
     * Creates a new pair
     */
    public VariantDiffPair(VariantContext left, VariantContext right, VCFEncoder encoder, VCFHeader header) {
        this.left = left;
        this.right = right;
        this.encoder = encoder;
    }

    public VCFCompare.DiffDisplay getDisplay(Function<VariantContext, String> getter) {
        String leftString = left == null ? null : getter.apply(left);
        String rightString = right == null ? null : getter.apply(right);
        return new VCFCompare.DiffDisplay(leftString, rightString);
    }

    public boolean mismatching(){
        if((Objects.isNull(left) && ! Objects.isNull(right))
        || (Objects.isNull(right) && ! Objects.isNull(left))){
            return true;
        }
        return !encoder.encode(left).equals(encoder.encode(right));
    }
}
