/*
 * Copyright 2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. A copy of the License is
 * located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package ru.taskurotta.annotation;

import ru.taskurotta.core.Promise;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Execute annotation is used on methods of interface annotated with {@link Decider}
 * to specify the entry-point for DeciderType.
 *
 * @Execute method can only have <code>void</code> or {@link Promise} return types.
 * Parameters of type {@link Promise} are not allowed.
 *
 * @see Decider
 * @author fateev, samar
 *
 * User: romario
 * Date: 1/11/13
 * Time: 11:57 AM
*/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Execute {

    /**
     * Optional name of the decider type. When missing defaults to the
     * annotated method name. Maximum length is 256 characters.
     */
// Temporary removed. Version and name of whole decider (@Decider) should be enough.
//    String name() default "";

    /**
     * Required version of the decider type. Maximum length is 64 characters.
     */
// Temporary removed. Version and name of whole decider (@Decider) should be enough.
//    String version();

}
