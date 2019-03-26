package org.broadinstitute.hellbender.gui.diff.iterators;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.PeekableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFEncoder;
import htsjdk.variant.vcf.VCFHeader;
import org.broadinstitute.hellbender.gui.diff.VariantDiffPair;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class PositionMatchingPairIterator implements CloseableIterator<VariantDiffPair> {
    private final PeekableIterator<VariantContext> iterator;
    private final VCFEncoder encoder;
    private final VCFHeader header;
    private final String left;
    private final String right;


    public PositionMatchingPairIterator(Iterator<VariantContext> iterator, VCFHeader header, String left, String right) {
        this.iterator = new PeekableIterator<>(iterator);
        this.encoder = new VCFEncoder(header, false, false);
        this.header = header;
        this.left = left;
        this.right = right;
    }

    @Override
    public void close() {
        iterator.close();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public VariantDiffPair next() {
        if( hasNext()){
            VariantContext next = iterator.next();
            next.getSource();
            final VariantContext peek = iterator.peek();
            VariantContext tmpLeft = null;
            VariantContext tmpRight = null;
//                if( next.getSource().equals(left)){
//                    tmpLeft = next;
//                } else if ( next.getSource().equals(right)){
//                    tmpRight = next;
//                }
            tmpLeft = next;
            if(next.getContig().equals(peek.getContig()) && next.getStart() == peek.getStart()){
                next = iterator.next();
                tmpRight = next;
//                    if( next.getSource().equals(left)){
//                        tmpLeft = next;
//                    } else if ( next.getSource().equals(right)){
//                        tmpRight = next;
//                    }
            }
            return new VariantDiffPair(tmpLeft, tmpRight, encoder, header);
        } else {
            throw new NoSuchElementException();
        }
    }
}
