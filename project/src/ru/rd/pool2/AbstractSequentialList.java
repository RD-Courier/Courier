/*
 * Copyright 2005-2017 Courier AUTHORS: please see AUTHORS file.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * 1. Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY AUTHORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * <COPYRIGHT HOLDER> OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package ru.rd.pool2;

import java.util.*;

/**
 * User: AStepochkin
 * Date: 08.09.2008
 * Time: 11:22:54
 */
public abstract class AbstractSequentialList<E> extends AbstractList<E> {
    protected AbstractSequentialList() {}

    public E get(int index) {
        ListIterator<E> e = listIterator(index);
        try {
            return(e.next());
        } catch(NoSuchElementException exc) {
            throw(new IndexOutOfBoundsException("Index: " + index));
        }
    }

    public E set(int index, E element) {
        ListIterator<E> e = listIterator(index);
        try {
            E oldVal = e.next();
            e.set(element);
            return oldVal;
        } catch(NoSuchElementException exc) {
            throw(new IndexOutOfBoundsException("Index: "+index));
        }
    }

    public void add(int index, E element) {
        ListIterator<E> e = listIterator(index);
        e.add(element);
    }

    public E remove(int index) {
        ListIterator<E> e = listIterator(index);
        E outCast;
        try {
            outCast = e.next();
        } catch(NoSuchElementException exc) {
            throw(new IndexOutOfBoundsException("Index: " + index));
        }
        e.remove();
        return(outCast);
    }


    public boolean addAll(int index, Collection<? extends E> c) {
        boolean modified = false;
        ListIterator<E> e1 = listIterator(index);
        for (E e: c) {
            e1.add(e);
            modified = true;
        }
        return modified;
    }


    public Iterator<E> iterator() {
        return listIterator();
    }

    public abstract ListIterator<E> listIterator(int index);
}
