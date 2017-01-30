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
package ru.rd.courier;

import java.sql.Driver;
import java.sql.DriverManager;

public class TestInterrupt {
    public static void main(String[] args) {
        final Object so = new Object();

        Thread thread1 = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    System.out.println(
                        "Thread1 alive: isInterrupted = " +
                        Thread.currentThread().isInterrupted()
                    );

                    try {
                        synchronized(so) {
                            Thread.sleep(60*60*1000);
                        }
                        /*
                        for (int i = 0; i < 1000; i++) {
                            for (int j = 0; j < 200000; j++) {

                            }
                        }
                        */
                    } catch (/*Interrupted*/Exception e) {
                        System.out.println(
                            "From catch: Thread1 alive: isInterrupted = " +
                            Thread.currentThread().isInterrupted()
                        );
                        e.printStackTrace();
                        break;
                    }
                }
            }
        });

        Thread thread2 = new Thread(new Runnable() {
            public void run() {
                synchronized(so) {
                    System.out.println("thread2 in synchronized scope");
                }
                System.out.println("thread2 after scope");
            }
        });

        try {
            thread1.start();
            Thread.sleep(1000);
            thread2.start();
            Thread.sleep(1000);
            thread2.interrupt();
            Thread.sleep(1000);
            boolean b = true;
            while (true) {
                System.out.println(
                    "Outer Thread: Thread1:"
                    + " is alive = " + thread1.isAlive()
                    + "; is interrupted = " + thread1.isInterrupted()
                    + "; state = " + thread1.getState()
                );
                System.out.println(
                    "thread2:"
                    + " is alive = " + thread2.isAlive()
                    + "; is interrupted = " + thread2.isInterrupted()
                    + "; state = " + thread2.getState()
                );
                Thread.sleep(2000);
                if (b) {
                    b = false;
                    thread1.interrupt();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
