/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.reflect;

import java.lang.reflect.*;
import sun.reflect.misc.ReflectUtil;

/**
 * Used only for the first few invocations of a Method;
 * afterward, switches to bytecode-based implementation
 */
/*
 * 基于JNI的方法访问器。
 * 仅用于反射操作的前几次调用，之后，切换到基于字节码(纯Java)的实现。
 */
class NativeMethodAccessorImpl extends MethodAccessorImpl {
    private final Method method;                    // 待访问方法
    private DelegatingMethodAccessorImpl parent;    // 当前类的代理
    private int numInvocations;                     // 记录待访问方法被反射调用的次数
    
    NativeMethodAccessorImpl(Method method) {
        this.method = method;
    }
    
    public Object invoke(Object obj, Object[] args) throws IllegalArgumentException, InvocationTargetException {
        // 获取当前待访问方法method被基于JNI的反射调用的次数
        int threshold = ReflectionFactory.inflationThreshold();
        
        /*
         * We can't inflate methods belonging to vm-anonymous classes because that kind of class can't be referred to by name,
         * hence can't be found from the generated bytecode.
         *
         * 如果当前待访问方法被反射调用的次数已经超过指定的阈值，且该方法所在的类不是虚拟机匿名类(这与Java语言中的匿名内部类不同)，
         * 此时需要从基于JNI的反射调用切换为基于纯Java的反射调用
         */
        if(++numInvocations>threshold && !ReflectUtil.isVMAnonymousClass(method.getDeclaringClass())) {
            MethodAccessorGenerator accessor = new MethodAccessorGenerator();
            MethodAccessorImpl acc = (MethodAccessorImpl) accessor.generateMethod(method.getDeclaringClass(), method.getName(), method.getParameterTypes(), method.getReturnType(), method.getExceptionTypes(), method.getModifiers());
            
            // 将代理中被代理的访问器更新为基于纯Java的方法访问器，下次从代理中发起调用时，就不会再使用当前基于JNI的方法访问器了
            parent.setDelegate(acc);
        }
        
        // 继续使用基于JNI的反射调用
        return invoke0(method, obj, args);
    }
    
    void setParent(DelegatingMethodAccessorImpl parent) {
        this.parent = parent;
    }
    
    // 基于JNI的反射方法调用
    private static native Object invoke0(Method m, Object obj, Object[] args);
}