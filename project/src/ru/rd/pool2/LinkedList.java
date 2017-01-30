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
 * Time: 11:28:53
 */
public class LinkedList<E> extends AbstractSequentialList<E>
    implements List<E>, Queue<E>, Cloneable, java.io.Serializable
{
    private transient Entry<E> header = new Entry<E>(null, null, null);
    private transient int size = 0;

    public LinkedList() {
        header.next = header.previous = header;
    }

     public LinkedList(Collection<? extends E> c) {
         this();
         addAll(c);
     }

    public E getFirst() {
        if (size==0) throw new NoSuchElementException();

        return header.next.element;
    }

    public E getLast()  {
        if (size==0) throw new NoSuchElementException();

        return header.previous.element;
    }

    public E removeFirst() {
        return remove(header.next);
    }

    public E removeLast() {
        return remove(header.previous);
    }

    public void addFirst(E o) {
        addBefore(o, header.next);
    }

    public void addLast(E o) {
        addBefore(o, header);
    }

    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    public int size() {
        return size;
    }

    public boolean add(E o) {
        addBefore(o, header);
        return true;
    }

    public boolean remove(Object o) {
        if (o==null) {
            for (Entry<E> e = header.next; e != header; e = e.next) {
                if (e.element==null) {
                    remove(e);
                    return true;
                }
            }
        } else {
            for (Entry<E> e = header.next; e != header; e = e.next) {
                if (o.equals(e.element)) {
                    remove(e);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean addAll(Collection<? extends E> c) {
        return addAll(size, c);
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        Object[] a = c.toArray();
        int numNew = a.length;
        if (numNew==0) return false;

        Entry<E> successor = (index==size ? header : entry(index));
        Entry<E> predecessor = successor.previous;
        for (int i=0; i<numNew; i++) {
            Entry<E> e = new Entry<E>((E)a[i], successor, predecessor);
            predecessor.next = e;
            predecessor = e;
        }
        successor.previous = predecessor;

        size += numNew;
        return true;
    }

    public void clear() {
        Entry<E> e = header.next;
        while (e != header) {
            Entry<E> next = e.next;
            e.next = e.previous = null;
            e.element = null;
            e = next;
        }
        header.next = header.previous = header;
        size = 0;
    }

    public E get(int index) {
        return entry(index).element;
    }

    public E set(int index, E element) {
        Entry<E> e = entry(index);
        E oldVal = e.element;
        e.element = element;
        return oldVal;
    }

    public void add(int index, E element) {
        addBefore(element, (index==size ? header : entry(index)));
    }

    public E remove(int index) {
        return remove(entry(index));
    }

    private Entry<E> entry(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        Entry<E> e = header;
        if (index < (size >> 1)) {
            for (int i = 0; i <= index; i++) e = e.next;
        } else {
            for (int i = size; i > index; i--) e = e.previous;
        }
        return e;
    }

    public int indexOf(Object o) {
        int index = 0;
        if (o == null) {
            for (Entry e = header.next; e != header; e = e.next) {
                if (e.element==null) return index;
                index++;
            }
        } else {
            for (Entry e = header.next; e != header; e = e.next) {
                if (o.equals(e.element)) return index;
                index++;
            }
        }
        return -1;
    }

    public int lastIndexOf(Object o) {
        int index = size;
        if (o == null) {
            for (Entry e = header.previous; e != header; e = e.previous) {
                index--;
                if (e.element == null) return index;
            }
        } else {
            for (Entry e = header.previous; e != header; e = e.previous) {
                index--;
                if (o.equals(e.element)) return index;
            }
        }
        return -1;
    }

    public E peek() {
        if (size==0) return null;
        return getFirst();
    }

    public E element() {
        return getFirst();
    }

    public E poll() {
        if (size==0) return null;
        return removeFirst();
    }

    public E remove() {
        return removeFirst();
    }

    public boolean offer(E o) {
        return add(o);
    }

    public ListIterator<E> listIterator(int index) {
        return new ListItr(index);
    }

    private class ListItr implements ListIterator<E> {
        private Entry<E> lastReturned = header;
        private Entry<E> next;

        ListItr(int index) {
            if (index < 0 || index > size)
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
            if (index < (size >> 1)) {
                next = header.next;
                for (int nextIndex = 0; nextIndex < index; nextIndex++) {
                    next = next.next;
                }
            } else {
                next = header;
                for (int nextIndex = size; nextIndex > index; nextIndex--) {
                    next = next.previous;
                }
            }
        }

        public boolean hasNext() {
            return next != null && next != header && next.element != null;
        }

        public E next() {
            lastReturned = next;
            next = next.next;
            return lastReturned.element;
        }

        public boolean hasPrevious() {
            return next.previous != null && next.previous != header;
        }

        public E previous() {
            lastReturned = next = next.previous;
            return lastReturned.element;
        }

        public int nextIndex() {
            return -1;
        }

        public int previousIndex() {
            return -1;
        }

        public void remove() {
            Entry<E> lastNext = lastReturned.next;
            try {
                LinkedList.this.remove(lastReturned);
            } catch (NoSuchElementException e) {
                throw new IllegalStateException();
            }
            if (next == lastReturned) next = lastNext;
            lastReturned = header;
        }

        public void set(E o) {
            if (lastReturned == header)
            throw new IllegalStateException();
            lastReturned.element = o;
        }

        public void add(E o) {
            lastReturned = header;
            addBefore(o, next);
        }
    }

    private static class Entry<E> {
        E element;
        Entry<E> next;
        Entry<E> previous;

        Entry(E element, Entry<E> next, Entry<E> previous) {
            this.element = element;
            this.next = next;
            this.previous = previous;
        }
    }

    private Entry<E> addBefore(E o, Entry<E> e) {
        Entry<E> newEntry = new Entry<E>(o, e, e.previous);
        newEntry.previous.next = newEntry;
        newEntry.next.previous = newEntry;
        size++;
        return newEntry;
    }

    private E remove(Entry<E> e) {
        if (e == header) throw new NoSuchElementException();

        E result = e.element;
        e.previous.next = e.next;
        e.next.previous = e.previous;
        e.next = e.previous = null;
        e.element = null;
        size--;
        return result;
    }

    public Object clone() throws CloneNotSupportedException {
        LinkedList<E> clone;
        try {
            clone = (LinkedList<E>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }

        // Put clone into "virgin" state
        clone.header = new Entry<E>(null, null, null);
        clone.header.next = clone.header.previous = clone.header;
        clone.size = 0;

        // Initialize clone with our elements
        for (Entry<E> e = header.next; e != header; e = e.next)
            clone.add(e.element);

        return clone;
    }

    public Object[] toArray() {
        Object[] result = new Object[size];
        int i = 0;
        for (Entry<E> e = header.next; e != header; e = e.next) {
            result[i++] = e.element;
        }
        return result;
    }

    public <T> T[] toArray(T[] a) {
        if (a.length < size) {
            a = (T[])java.lang.reflect.Array.newInstance(
                a.getClass().getComponentType(), size
            );
        }
        int i = 0;
        Object[] result = a;
        for (Entry<E> e = header.next; e != header; e = e.next) {
            result[i++] = e.element;
        }

        if (a.length > size) a[size] = null;

        return a;
    }

    private static final long serialVersionUID = 876323262645176354L;

    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
        // Write out any hidden serialization magic
        s.defaultWriteObject();

        // Write out size
        s.writeInt(size);

        // Write out all elements in the proper order.
        for (Entry e = header.next; e != header; e = e.next) {
            s.writeObject(e.element);
        }
    }

    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException
    {
        // Read in any hidden serialization magic
        s.defaultReadObject();

        // Read in size
        int size = s.readInt();

        // Initialize header
        header = new Entry<E>(null, null, null);
        header.next = header.previous = header;

        // Read in all elements in the proper order.
        for (int i=0; i<size; i++) {
            addBefore((E)s.readObject(), header);
        }
    }
}
