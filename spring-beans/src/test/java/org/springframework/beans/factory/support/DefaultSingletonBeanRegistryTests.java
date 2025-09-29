/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.support;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.DerivedTestBean;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 04.07.2006
 */
class DefaultSingletonBeanRegistryTests {

	private final DefaultSingletonBeanRegistry beanRegistry = new DefaultSingletonBeanRegistry();


	@Test
	void singletons() {

		/*
		 * 第二个参数设置一个回调函数，在注册单例对象时调用，用于在单例对象创建后进行一些额外的操作。
		 * 这个回调函数的作用是在单例对象创建后将 tbFlag 的值设置为 true。
		 * 这样，在后续的测试中，我们可以通过检查 tbFlag 的值来验证是否成功调用了回调函数。
		 * instance是consumer实例
		 */
		AtomicBoolean tbFlag = new AtomicBoolean();
		beanRegistry.addSingletonCallback("tb", instance -> tbFlag.set(true));//添加单例bean对应的回调函数
		TestBean tb = new TestBean(); // 直接new一个作为单例bean
		beanRegistry.registerSingleton("tb", tb);
		assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);
		assertThat(tbFlag.get()).isTrue();

		AtomicBoolean tb2Flag = new AtomicBoolean();
		beanRegistry.addSingletonCallback("tb2", instance -> tb2Flag.set(true));
		TestBean tb2 = (TestBean) beanRegistry.getSingleton("tb2", TestBean::new);// 通过源码方法创建单例bean对象
		assertThat(beanRegistry.getSingleton("tb2")).isSameAs(tb2);
		assertThat(tb2Flag.get()).isTrue();

		TestBean tb3 = (TestBean) beanRegistry.getSingleton("tb3", () -> {
			TestBean newTb = new TestBean();
			beanRegistry.registerSingleton("tb3", newTb);
			return newTb;
		});
		assertThat(beanRegistry.getSingleton("tb3")).isSameAs(tb3);

		assertThat(beanRegistry.getSingletonCount()).isEqualTo(3);
		assertThat(beanRegistry.getSingletonNames()).containsExactly("tb", "tb2", "tb3");

		beanRegistry.destroySingletons();
		assertThat(beanRegistry.getSingletonCount()).isZero();
		assertThat(beanRegistry.getSingletonNames()).isEmpty();
	}

	@Test
	void disposableBean() {
		DerivedTestBean tb = new DerivedTestBean();
		beanRegistry.registerSingleton("tb", tb);
		beanRegistry.registerDisposableBean("tb", tb);
		assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);

		assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);
		assertThat(beanRegistry.getSingletonCount()).isEqualTo(1);
		assertThat(beanRegistry.getSingletonNames()).containsExactly("tb");
		assertThat(tb.wasDestroyed()).isFalse();

		beanRegistry.destroySingletons();
		assertThat(beanRegistry.getSingletonCount()).isZero();
		assertThat(beanRegistry.getSingletonNames()).isEmpty();
		assertThat(tb.wasDestroyed()).isTrue();
	}

	@Test
	void dependentRegistration() {
		beanRegistry.registerDependentBean("a", "b");
		beanRegistry.registerDependentBean("b", "c");
		beanRegistry.registerDependentBean("c", "b");
		assertThat(beanRegistry.isDependent("a", "b")).isTrue();
		assertThat(beanRegistry.isDependent("b", "c")).isTrue();
		assertThat(beanRegistry.isDependent("c", "b")).isTrue();
		assertThat(beanRegistry.isDependent("a", "c")).isTrue();
		assertThat(beanRegistry.isDependent("c", "a")).isFalse();
		assertThat(beanRegistry.isDependent("b", "a")).isFalse();
		assertThat(beanRegistry.isDependent("a", "a")).isFalse();
		assertThat(beanRegistry.isDependent("b", "b")).isTrue();
		assertThat(beanRegistry.isDependent("c", "c")).isTrue();
	}

}
