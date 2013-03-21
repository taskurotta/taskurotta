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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @Decider annotation is allowed on interfaces to define a decider.  This
 * interface forms the contract between the implementation of DeciderType
 * and clients interested in starting executions, sending signals, and getting
 * current state of execution.
 *
 * Use {@link Execute} annotation on the method to mark it as the entry-point
 * for DeciderType.  @Decider interface cannot have more than one method marked
 * with {@link Execute} annotation.
 *
 * @author fateev, samar
 *
 * User: romario
 * Date: 11.01.13 11:56
 *
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Decider {

	/**
	 * Optional name of the decider type. When missing defaults to the
	 * annotated method name. Maximum length is 256 characters.
	 */
	String name() default "";

	/**
	 * Optional version of the decider type. When missing defaults to the "1.0".
	 * Maximum length is 64 characters.
	 */
	String version() default "1.0";

}