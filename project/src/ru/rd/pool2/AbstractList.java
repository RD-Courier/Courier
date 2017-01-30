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
 * Time: 11:03:58
 */
public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {
    protected AbstractList() {}

    public boolean add(E o) {
        add(size(), o);
        return true;
    }

    abstract public E get(int index);

    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    public E remove(int index) {
        throw new UnsupportedOperationException();
    }


    public int indexOf(Object o) {
        ListIterator<E> e = listIterator();
        if (o==null) {
            while (e.hasNext()) {
                if (e.next()==null) return e.previousIndex();
            }
        } else {
            while (e.hasNext()) {
                if (o.equals(e.next())) return e.previousIndex();
            }
        }
        return -1;
    }

    public int lastIndexOf(Object o) {
        ListIterator<E> e = listIterator(size());
        if (o==null) {
            while (e.hasPrevious()) {
                if (e.previous()==null) return e.nextIndex();
            }
        } else {
            while (e.hasPrevious()) {
                if (o.equals(e.previous())) return e.nextIndex();
            }
        }
        return -1;
    }

    public void clear() {
        removeRange(0, size());
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c) {
            add(index++, e);
            modified = true;
        }
        return modified;
    }


    public Iterator<E> iterator() {
        return new Itr();
    }

    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    public ListIterator<E> listIterator(final int index) {
        if (index<0 || index>size())
            throw new IndexOutOfBoundsException("Index: " + index);

        return new ListItr(index);
    }

    private class Itr implements Iterator<E> {
        int cursor = 0;
        int lastRet = -1;

        public boolean hasNext() {
            return cursor != size();
        }

        public E next() {
            try {
                E next = get(cursor);
                lastRet = cursor++;
                return next;
            } catch(IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            if (lastRet == -1) throw new IllegalStateException();

            try {
                AbstractList.this.remove(lastRet);
                if (lastRet < cursor) cursor--;
                lastRet = -1;
            } catch(IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }
    }

    private class ListItr extends Itr implements ListIterator<E> {
        ListItr(int index) {
            cursor = index;
        }

        public boolean hasPrevious() {
            return cursor != 0;
        }

        public E previous() {
            try {
                int i = cursor - 1;
                E previous = get(i);
                lastRet = cursor = i;
                return previous;
            } catch(IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor-1;
        }

        public void set(E o) {
            if (lastRet == -1) throw new IllegalStateException();

            try {
                AbstractList.this.set(lastRet, o);
            } catch(IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }

        public void add(E o) {
            try {
                AbstractList.this.add(cursor++, o);
                lastRet = -1;
            } catch(IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }
    }

    public List<E> subList(int fromIndex, int toIndex) {
        return (this instanceof RandomAccess ?
                new RandomAccessSubList<E>(this, fromIndex, toIndex) :
                new SubList<E>(this, fromIndex, toIndex));
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof List)) return false;

        ListIterator<E> e1 = listIterator();
        ListIterator e2 = ((List) o).listIterator();
        while(e1.hasNext() && e2.hasNext()) {
            E o1 = e1.next();
            Object o2 = e2.next();
            if (!(o1==null ? o2==null : o1.equals(o2)))
            return false;
        }
        return !(e1.hasNext() || e2.hasNext());
    }

    public int hashCode() {
        int hashCode = 1;
        Iterator<E> i = iterator();
            while (i.hasNext()) {
            E obj = i.next();
            hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
        }
        return hashCode;
    }

    protected void removeRange(int fromIndex, int toIndex) {
        ListIterator<E> it = listIterator(fromIndex);
        for (int i=0, n=toIndex-fromIndex; i<n; i++) {
            it.next();
            it.remove();
        }
    }
}

class SubList<E> extends AbstractList<E> {
    private AbstractList<E> l;
    private int offset;
    private int size;

    SubList(AbstractList<E> list, int fromIndex, int toIndex) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        if (toIndex > list.size())
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        if (fromIndex > toIndex)
            throw new IllegalArgumentException("fromIndex(" + fromIndex +
                                               ") > toIndex(" + toIndex + ")");
        l = list;
        offset = fromIndex;
        size = toIndex - fromIndex;
    }

    public E set(int index, E element) {
        rangeCheck(index);
        return l.set(index+offset, element);
    }

    public E get(int index) {
        rangeCheck(index);
        return l.get(index+offset);
    }

    public int size() {
        return size;
    }

    public void add(int index, E element) {
        if (index<0 || index>size) throw new IndexOutOfBoundsException();
        l.add(index+offset, element);
        size++;
    }

    public E remove(int index) {
        rangeCheck(index);
        E result = l.remove(index+offset);
        size--;
        return result;
    }

    protected void removeRange(int fromIndex, int toIndex) {
        l.removeRange(fromIndex+offset, toIndex+offset);
        size -= (toIndex-fromIndex);
    }

    public boolean addAll(Collection<? extends E> c) {
        return addAll(size, c);
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        if (index<0 || index>size)
            throw new IndexOutOfBoundsException(
                "Index: "+index+", Size: "+size);
        int cSize = c.size();
        if (cSize==0)
            return false;

        l.addAll(offset+index, c);
        size += cSize;
        return true;
    }

    public Iterator<E> iterator() {
        return listIterator();
    }

    public ListIterator<E> listIterator(final int index) {
        if (index<0 || index>size)
            throw new IndexOutOfBoundsException(
                "Index: "+index+", Size: "+size);

        return new ListIterator<E>() {
            private ListIterator<E> i = l.listIterator(index+offset);

            public boolean hasNext() {
                return nextIndex() < size;
            }

            public E next() {
                if (hasNext())
                    return i.next();
                else
                    throw new NoSuchElementException();
            }

            public boolean hasPrevious() {
                return previousIndex() >= 0;
            }

            public E previous() {
                if (hasPrevious())
                    return i.previous();
                else
                    throw new NoSuchElementException();
            }

            public int nextIndex() {
                return i.nextIndex() - offset;
            }

            public int previousIndex() {
                return i.previousIndex() - offset;
            }

            public void remove() {
                i.remove();
                size--;
            }

            public void set(E o) {
                i.set(o);
            }

            public void add(E o) {
                i.add(o);
                size++;
            }
        };
    }

    public List<E> subList(int fromIndex, int toIndex) {
        return new SubList<E>(this, fromIndex, toIndex);
    }

    private void rangeCheck(int index) {
        if (index<0 || index>=size)
            throw new IndexOutOfBoundsException("Index: "+index+
                                                ",Size: "+size);
    }
}

class RandomAccessSubList<E> extends SubList<E> implements RandomAccess {
    RandomAccessSubList(AbstractList<E> list, int fromIndex, int toIndex) {
        super(list, fromIndex, toIndex);
    }

    public List<E> subList(int fromIndex, int toIndex) {
        return new RandomAccessSubList<E>(this, fromIndex, toIndex);
    }
}
