package org.broadinstitute.hellbender.gui.diff.iterators;

import htsjdk.samtools.util.AbstractIterator;
import htsjdk.samtools.util.CloseableIterator;

import java.util.function.Predicate;

public class FilteringIterator<T> extends AbstractIterator<T> implements CloseableIterator<T> {
    private final CloseableIterator<T> iterator;
    private final Predicate<T> filter;

    public FilteringIterator(CloseableIterator<T> iterator, Predicate<T> filter) {
        this.iterator = iterator;
        this.filter = filter;
    }

    @Override
    public void close() {
        iterator.close();
    }

    @Override
    protected T advance() {
        while(iterator.hasNext()){
           T next = iterator.next();
           if( filter.test(next)){
               return next;
           }
        }
        return null;
    }
}
