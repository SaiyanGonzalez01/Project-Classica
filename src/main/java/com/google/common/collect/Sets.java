/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2.FilteredCollection;

/**
 * Static utility methods pertaining to {@link Set} instances. Also see this
 * class's counterparts {@link Lists}, {@link Maps} and {@link Queues}.
 *
 * <p>
 * See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/CollectionUtilitiesExplained#Sets">
 * {@code Sets}</a>.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @author Chris Povirk
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible(emulated = true)
public final class Sets {
	private Sets() {
	}

	/**
	 * {@link AbstractSet} substitute without the potentially-quadratic
	 * {@code removeAll} implementation.
	 */
	abstract static class ImprovedAbstractSet<E> extends AbstractSet<E> {
		@Override
		public boolean removeAll(Collection<?> c) {
			return removeAllImpl(this, c);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return super.retainAll(checkNotNull(c)); // GWT compatibility
		}
	}

	/**
	 * Returns an immutable set instance containing the given enum elements.
	 * Internally, the returned set will be backed by an {@link EnumSet}.
	 *
	 * <p>
	 * The iteration order of the returned set follows the enum's iteration order,
	 * not the order in which the elements are provided to the method.
	 *
	 * @param anElement     one of the elements the set should contain
	 * @param otherElements the rest of the elements the set should contain
	 * @return an immutable set containing those elements, minus duplicates
	 */
	// http://code.google.com/p/google-web-toolkit/issues/detail?id=3028
	@GwtCompatible(serializable = true)
	public static <E extends Enum<E>> ImmutableSet<E> immutableEnumSet(E anElement, E... otherElements) {
		return ImmutableEnumSet.asImmutable(EnumSet.of(anElement, otherElements));
	}

	/**
	 * Returns an immutable set instance containing the given enum elements.
	 * Internally, the returned set will be backed by an {@link EnumSet}.
	 *
	 * <p>
	 * The iteration order of the returned set follows the enum's iteration order,
	 * not the order in which the elements appear in the given collection.
	 *
	 * @param elements the elements, all of the same {@code enum} type, that the set
	 *                 should contain
	 * @return an immutable set containing those elements, minus duplicates
	 */
	// http://code.google.com/p/google-web-toolkit/issues/detail?id=3028
	@GwtCompatible(serializable = true)
	public static <E extends Enum<E>> ImmutableSet<E> immutableEnumSet(Iterable<E> elements) {
		if (elements instanceof ImmutableEnumSet) {
			return (ImmutableEnumSet<E>) elements;
		} else if (elements instanceof Collection) {
			Collection<E> collection = (Collection<E>) elements;
			if (collection.isEmpty()) {
				return ImmutableSet.of();
			} else {
				return ImmutableEnumSet.asImmutable(EnumSet.copyOf(collection));
			}
		} else {
			Iterator<E> itr = elements.iterator();
			if (itr.hasNext()) {
				EnumSet<E> enumSet = EnumSet.of(itr.next());
				Iterators.addAll(enumSet, itr);
				return ImmutableEnumSet.asImmutable(enumSet);
			} else {
				return ImmutableSet.of();
			}
		}
	}

	/**
	 * Returns a new {@code EnumSet} instance containing the given elements. Unlike
	 * {@link EnumSet#copyOf(Collection)}, this method does not produce an exception
	 * on an empty collection, and it may be called on any iterable, not just a
	 * {@code Collection}.
	 */
	public static <E extends Enum<E>> EnumSet<E> newEnumSet(Iterable<E> iterable, Class<E> elementType) {
		EnumSet<E> set = EnumSet.noneOf(elementType);
		Iterables.addAll(set, iterable);
		return set;
	}

	// HashSet

	/**
	 * Creates a <i>mutable</i>, empty {@code HashSet} instance.
	 *
	 * <p>
	 * <b>Note:</b> if mutability is not required, use {@link ImmutableSet#of()}
	 * instead.
	 *
	 * <p>
	 * <b>Note:</b> if {@code E} is an {@link Enum} type, use {@link EnumSet#noneOf}
	 * instead.
	 *
	 * @return a new, empty {@code HashSet}
	 */
	public static <E> HashSet<E> newHashSet() {
		return new HashSet<E>();
	}

	/**
	 * Creates a <i>mutable</i> {@code HashSet} instance containing the given
	 * elements in unspecified order.
	 *
	 * <p>
	 * <b>Note:</b> if mutability is not required and the elements are non-null, use
	 * an overload of {@link ImmutableSet#of()} (for varargs) or
	 * {@link ImmutableSet#copyOf(Object[])} (for an array) instead.
	 *
	 * <p>
	 * <b>Note:</b> if {@code E} is an {@link Enum} type, use
	 * {@link EnumSet#of(Enum, Enum[])} instead.
	 *
	 * @param elements the elements that the set should contain
	 * @return a new {@code HashSet} containing those elements (minus duplicates)
	 */
	public static <E> HashSet<E> newHashSet(E... elements) {
		HashSet<E> set = newHashSetWithExpectedSize(elements.length);
		Collections.addAll(set, elements);
		return set;
	}

	/**
	 * Creates a {@code HashSet} instance, with a high enough "initial capacity"
	 * that it <i>should</i> hold {@code expectedSize} elements without growth. This
	 * behavior cannot be broadly guaranteed, but it is observed to be true for
	 * OpenJDK 1.6. It also can't be guaranteed that the method isn't inadvertently
	 * <i>oversizing</i> the returned set.
	 *
	 * @param expectedSize the number of elements you expect to add to the returned
	 *                     set
	 * @return a new, empty {@code HashSet} with enough capacity to hold {@code
	 *         expectedSize} elements without resizing
	 * @throws IllegalArgumentException if {@code expectedSize} is negative
	 */
	public static <E> HashSet<E> newHashSetWithExpectedSize(int expectedSize) {
		return new HashSet<E>(Maps.capacity(expectedSize));
	}

	/**
	 * Creates a <i>mutable</i> {@code HashSet} instance containing the given
	 * elements in unspecified order.
	 *
	 * <p>
	 * <b>Note:</b> if mutability is not required and the elements are non-null, use
	 * {@link ImmutableSet#copyOf(Iterable)} instead.
	 *
	 * <p>
	 * <b>Note:</b> if {@code E} is an {@link Enum} type, use
	 * {@link #newEnumSet(Iterable, Class)} instead.
	 *
	 * @param elements the elements that the set should contain
	 * @return a new {@code HashSet} containing those elements (minus duplicates)
	 */
	public static <E> HashSet<E> newHashSet(Iterable<? extends E> elements) {
		return (elements instanceof Collection) ? new HashSet<E>(Collections2.cast(elements))
				: newHashSet(elements.iterator());
	}

	/**
	 * Creates a <i>mutable</i> {@code HashSet} instance containing the given
	 * elements in unspecified order.
	 *
	 * <p>
	 * <b>Note:</b> if mutability is not required and the elements are non-null, use
	 * {@link ImmutableSet#copyOf(Iterable)} instead.
	 *
	 * <p>
	 * <b>Note:</b> if {@code E} is an {@link Enum} type, you should create an
	 * {@link EnumSet} instead.
	 *
	 * @param elements the elements that the set should contain
	 * @return a new {@code HashSet} containing those elements (minus duplicates)
	 */
	public static <E> HashSet<E> newHashSet(Iterator<? extends E> elements) {
		HashSet<E> set = newHashSet();
		Iterators.addAll(set, elements);
		return set;
	}

	// LinkedHashSet

	/**
	 * Creates a <i>mutable</i>, empty {@code LinkedHashSet} instance.
	 *
	 * <p>
	 * <b>Note:</b> if mutability is not required, use {@link ImmutableSet#of()}
	 * instead.
	 *
	 * @return a new, empty {@code LinkedHashSet}
	 */
	public static <E> LinkedHashSet<E> newLinkedHashSet() {
		return new LinkedHashSet<E>();
	}

	/**
	 * Creates a {@code LinkedHashSet} instance, with a high enough "initial
	 * capacity" that it <i>should</i> hold {@code expectedSize} elements without
	 * growth. This behavior cannot be broadly guaranteed, but it is observed to be
	 * true for OpenJDK 1.6. It also can't be guaranteed that the method isn't
	 * inadvertently <i>oversizing</i> the returned set.
	 *
	 * @param expectedSize the number of elements you expect to add to the returned
	 *                     set
	 * @return a new, empty {@code LinkedHashSet} with enough capacity to hold
	 *         {@code expectedSize} elements without resizing
	 * @throws IllegalArgumentException if {@code expectedSize} is negative
	 * @since 11.0
	 */
	public static <E> LinkedHashSet<E> newLinkedHashSetWithExpectedSize(int expectedSize) {
		return new LinkedHashSet<E>(Maps.capacity(expectedSize));
	}

	/**
	 * Creates a <i>mutable</i> {@code LinkedHashSet} instance containing the given
	 * elements in order.
	 *
	 * <p>
	 * <b>Note:</b> if mutability is not required and the elements are non-null, use
	 * {@link ImmutableSet#copyOf(Iterable)} instead.
	 *
	 * @param elements the elements that the set should contain, in order
	 * @return a new {@code LinkedHashSet} containing those elements (minus
	 *         duplicates)
	 */
	public static <E> LinkedHashSet<E> newLinkedHashSet(Iterable<? extends E> elements) {
		if (elements instanceof Collection) {
			return new LinkedHashSet<E>(Collections2.cast(elements));
		}
		LinkedHashSet<E> set = newLinkedHashSet();
		Iterables.addAll(set, elements);
		return set;
	}

	// TreeSet

	/**
	 * Creates a <i>mutable</i>, empty {@code TreeSet} instance sorted by the
	 * natural sort ordering of its elements.
	 *
	 * <p>
	 * <b>Note:</b> if mutability is not required, use
	 * {@link ImmutableSortedSet#of()} instead.
	 *
	 * @return a new, empty {@code TreeSet}
	 */
	public static <E extends Comparable> TreeSet<E> newTreeSet() {
		return new TreeSet<E>();
	}

	/**
	 * Creates a <i>mutable</i> {@code TreeSet} instance containing the given
	 * elements sorted by their natural ordering.
	 *
	 * <p>
	 * <b>Note:</b> if mutability is not required, use
	 * {@link ImmutableSortedSet#copyOf(Iterable)} instead.
	 *
	 * <p>
	 * <b>Note:</b> If {@code elements} is a {@code SortedSet} with an explicit
	 * comparator, this method has different behavior than
	 * {@link TreeSet#TreeSet(SortedSet)}, which returns a {@code TreeSet} with that
	 * comparator.
	 *
	 * @param elements the elements that the set should contain
	 * @return a new {@code TreeSet} containing those elements (minus duplicates)
	 */
	public static <E extends Comparable> TreeSet<E> newTreeSet(Iterable<? extends E> elements) {
		TreeSet<E> set = newTreeSet();
		Iterables.addAll(set, elements);
		return set;
	}

	/**
	 * Creates a <i>mutable</i>, empty {@code TreeSet} instance with the given
	 * comparator.
	 *
	 * <p>
	 * <b>Note:</b> if mutability is not required, use {@code
	 * ImmutableSortedSet.orderedBy(comparator).build()} instead.
	 *
	 * @param comparator the comparator to use to sort the set
	 * @return a new, empty {@code TreeSet}
	 * @throws NullPointerException if {@code comparator} is null
	 */
	public static <E> TreeSet<E> newTreeSet(Comparator<? super E> comparator) {
		return new TreeSet<E>(checkNotNull(comparator));
	}

	/**
	 * Creates an empty {@code Set} that uses identity to determine equality. It
	 * compares object references, instead of calling {@code equals}, to determine
	 * whether a provided object matches an element in the set. For example,
	 * {@code contains} returns {@code false} when passed an object that equals a
	 * set member, but isn't the same instance. This behavior is similar to the way
	 * {@code IdentityHashMap} handles key lookups.
	 *
	 * @since 8.0
	 */
	public static <E> Set<E> newIdentityHashSet() {
		return Sets.newSetFromMap(Maps.<E, Boolean>newIdentityHashMap());
	}

	/**
	 * Creates an {@code EnumSet} consisting of all enum values that are not in the
	 * specified collection. If the collection is an {@link EnumSet}, this method
	 * has the same behavior as {@link EnumSet#complementOf}. Otherwise, the
	 * specified collection must contain at least one element, in order to determine
	 * the element type. If the collection could be empty, use
	 * {@link #complementOf(Collection, Class)} instead of this method.
	 *
	 * @param collection the collection whose complement should be stored in the
	 *                   enum set
	 * @return a new, modifiable {@code EnumSet} containing all values of the enum
	 *         that aren't present in the given collection
	 * @throws IllegalArgumentException if {@code collection} is not an
	 *                                  {@code EnumSet} instance and contains no
	 *                                  elements
	 */
	public static <E extends Enum<E>> EnumSet<E> complementOf(Collection<E> collection) {
		if (collection instanceof EnumSet) {
			return EnumSet.complementOf((EnumSet<E>) collection);
		}
		checkArgument(!collection.isEmpty(), "collection is empty; use the other version of this method");
		Class<E> type = collection.iterator().next().getDeclaringClass();
		return makeComplementByHand(collection, type);
	}

	/**
	 * Creates an {@code EnumSet} consisting of all enum values that are not in the
	 * specified collection. This is equivalent to {@link EnumSet#complementOf}, but
	 * can act on any input collection, as long as the elements are of enum type.
	 *
	 * @param collection the collection whose complement should be stored in the
	 *                   {@code EnumSet}
	 * @param type       the type of the elements in the set
	 * @return a new, modifiable {@code EnumSet} initially containing all the values
	 *         of the enum not present in the given collection
	 */
	public static <E extends Enum<E>> EnumSet<E> complementOf(Collection<E> collection, Class<E> type) {
		checkNotNull(collection);
		return (collection instanceof EnumSet) ? EnumSet.complementOf((EnumSet<E>) collection)
				: makeComplementByHand(collection, type);
	}

	private static <E extends Enum<E>> EnumSet<E> makeComplementByHand(Collection<E> collection, Class<E> type) {
		EnumSet<E> result = EnumSet.allOf(type);
		result.removeAll(collection);
		return result;
	}

	/**
	 * Returns a set backed by the specified map. The resulting set displays the
	 * same ordering, concurrency, and performance characteristics as the backing
	 * map. In essence, this factory method provides a {@link Set} implementation
	 * corresponding to any {@link Map} implementation. There is no need to use this
	 * method on a {@link Map} implementation that already has a corresponding
	 * {@link Set} implementation (such as {@link java.util.HashMap} or
	 * {@link java.util.TreeMap}).
	 *
	 * <p>
	 * Each method invocation on the set returned by this method results in exactly
	 * one method invocation on the backing map or its {@code keySet} view, with one
	 * exception. The {@code addAll} method is implemented as a sequence of
	 * {@code put} invocations on the backing map.
	 *
	 * <p>
	 * The specified map must be empty at the time this method is invoked, and
	 * should not be accessed directly after this method returns. These conditions
	 * are ensured if the map is created empty, passed directly to this method, and
	 * no reference to the map is retained, as illustrated in the following code
	 * fragment:
	 * 
	 * <pre>
	 * {
	 * 	&#64;code
	 *
	 * 	Set<Object> identityHashSet = Sets.newSetFromMap(new IdentityHashMap<Object, Boolean>());
	 * }
	 * </pre>
	 *
	 * <p>
	 * This method has the same behavior as the JDK 6 method
	 * {@code Collections.newSetFromMap()}. The returned set is serializable if the
	 * backing map is.
	 *
	 * @param map the backing map
	 * @return the set backed by the map
	 * @throws IllegalArgumentException if {@code map} is not empty
	 */
	public static <E> Set<E> newSetFromMap(Map<E, Boolean> map) {
		return Platform.newSetFromMap(map);
	}

	/**
	 * An unmodifiable view of a set which may be backed by other sets; this view
	 * will change as the backing sets do. Contains methods to copy the data into a
	 * new set which will then remain stable. There is usually no reason to retain a
	 * reference of type {@code SetView}; typically, you either use it as a plain
	 * {@link Set}, or immediately invoke {@link #immutableCopy} or
	 * {@link #copyInto} and forget the {@code SetView} itself.
	 *
	 * @since 2.0 (imported from Google Collections Library)
	 */
	public abstract static class SetView<E> extends AbstractSet<E> {
		private SetView() {
		} // no subclasses but our own

		/**
		 * Returns an immutable copy of the current contents of this set view. Does not
		 * support null elements.
		 *
		 * <p>
		 * <b>Warning:</b> this may have unexpected results if a backing set of this
		 * view uses a nonstandard notion of equivalence, for example if it is a
		 * {@link TreeSet} using a comparator that is inconsistent with
		 * {@link Object#equals(Object)}.
		 */
		public ImmutableSet<E> immutableCopy() {
			return ImmutableSet.copyOf(this);
		}

		/**
		 * Copies the current contents of this set view into an existing set. This
		 * method has equivalent behavior to {@code set.addAll(this)}, assuming that all
		 * the sets involved are based on the same notion of equivalence.
		 *
		 * @return a reference to {@code set}, for convenience
		 */
		// Note: S should logically extend Set<? super E> but can't due to either
		// some javac bug or some weirdness in the spec, not sure which.
		public <S extends Set<E>> S copyInto(S set) {
			set.addAll(this);
			return set;
		}
	}

	/**
	 * Returns an unmodifiable <b>view</b> of the union of two sets. The returned
	 * set contains all elements that are contained in either backing set. Iterating
	 * over the returned set iterates first over all the elements of {@code set1},
	 * then over each element of {@code set2}, in order, that is not contained in
	 * {@code set1}.
	 *
	 * <p>
	 * Results are undefined if {@code set1} and {@code set2} are sets based on
	 * different equivalence relations (as {@link HashSet}, {@link TreeSet}, and the
	 * {@link Map#keySet} of an {@code IdentityHashMap} all are).
	 *
	 * <p>
	 * <b>Note:</b> The returned view performs better when {@code set1} is the
	 * smaller of the two sets. If you have reason to believe one of your sets will
	 * generally be smaller than the other, pass it first.
	 *
	 * <p>
	 * Further, note that the current implementation is not suitable for nested
	 * {@code union} views, i.e. the following should be avoided when in a loop:
	 * {@code union = Sets.union(union, anotherSet);}, since iterating over the
	 * resulting set has a cubic complexity to the depth of the nesting.
	 */
	public static <E> SetView<E> union(final Set<? extends E> set1, final Set<? extends E> set2) {
		checkNotNull(set1, "set1");
		checkNotNull(set2, "set2");

		final Set<? extends E> set2minus1 = difference(set2, set1);

		return new SetView<E>() {
			@Override
			public int size() {
				return set1.size() + set2minus1.size();
			}

			@Override
			public boolean isEmpty() {
				return set1.isEmpty() && set2.isEmpty();
			}

			@Override
			public Iterator<E> iterator() {
				return Iterators.unmodifiableIterator(Iterators.concat(set1.iterator(), set2minus1.iterator()));
			}

			@Override
			public boolean contains(Object object) {
				return set1.contains(object) || set2.contains(object);
			}

			@Override
			public <S extends Set<E>> S copyInto(S set) {
				set.addAll(set1);
				set.addAll(set2);
				return set;
			}

			@Override
			public ImmutableSet<E> immutableCopy() {
				return new ImmutableSet.Builder<E>().addAll(set1).addAll(set2).build();
			}
		};
	}

	/**
	 * Returns an unmodifiable <b>view</b> of the intersection of two sets. The
	 * returned set contains all elements that are contained by both backing sets.
	 * The iteration order of the returned set matches that of {@code set1}.
	 *
	 * <p>
	 * Results are undefined if {@code set1} and {@code set2} are sets based on
	 * different equivalence relations (as {@code HashSet}, {@code TreeSet}, and the
	 * keySet of an {@code IdentityHashMap} all are).
	 *
	 * <p>
	 * <b>Note:</b> The returned view performs slightly better when {@code
	 * set1} is the smaller of the two sets. If you have reason to believe one of
	 * your sets will generally be smaller than the other, pass it first.
	 * Unfortunately, since this method sets the generic type of the returned set
	 * based on the type of the first set passed, this could in rare cases force you
	 * to make a cast, for example:
	 * 
	 * <pre>
	 *    {@code
	 *
	 *   Set<Object> aFewBadObjects = ...
	 *   Set<String> manyBadStrings = ...
	 *
	 *   // impossible for a non-String to be in the intersection
	 *   SuppressWarnings("unchecked")
	 *   Set<String> badStrings = (Set) Sets.intersection(
	 *       aFewBadObjects, manyBadStrings);}
	 * </pre>
	 *
	 * <p>
	 * This is unfortunate, but should come up only very rarely.
	 */
	public static <E> SetView<E> intersection(final Set<E> set1, final Set<?> set2) {
		checkNotNull(set1, "set1");
		checkNotNull(set2, "set2");

		final Predicate<Object> inSet2 = Predicates.in(set2);
		return new SetView<E>() {
			@Override
			public Iterator<E> iterator() {
				return Iterators.filter(set1.iterator(), inSet2);
			}

			@Override
			public int size() {
				return Iterators.size(iterator());
			}

			@Override
			public boolean isEmpty() {
				return !iterator().hasNext();
			}

			@Override
			public boolean contains(Object object) {
				return set1.contains(object) && set2.contains(object);
			}

			@Override
			public boolean containsAll(Collection<?> collection) {
				return set1.containsAll(collection) && set2.containsAll(collection);
			}
		};
	}

	/**
	 * Returns an unmodifiable <b>view</b> of the difference of two sets. The
	 * returned set contains all elements that are contained by {@code set1} and not
	 * contained by {@code set2}. {@code set2} may also contain elements not present
	 * in {@code set1}; these are simply ignored. The iteration order of the
	 * returned set matches that of {@code set1}.
	 *
	 * <p>
	 * Results are undefined if {@code set1} and {@code set2} are sets based on
	 * different equivalence relations (as {@code HashSet}, {@code TreeSet}, and the
	 * keySet of an {@code IdentityHashMap} all are).
	 */
	public static <E> SetView<E> difference(final Set<E> set1, final Set<?> set2) {
		checkNotNull(set1, "set1");
		checkNotNull(set2, "set2");

		final Predicate<Object> notInSet2 = Predicates.not(Predicates.in(set2));
		return new SetView<E>() {
			@Override
			public Iterator<E> iterator() {
				return Iterators.filter(set1.iterator(), notInSet2);
			}

			@Override
			public int size() {
				return Iterators.size(iterator());
			}

			@Override
			public boolean isEmpty() {
				return set2.containsAll(set1);
			}

			@Override
			public boolean contains(Object element) {
				return set1.contains(element) && !set2.contains(element);
			}
		};
	}

	/**
	 * Returns an unmodifiable <b>view</b> of the symmetric difference of two sets.
	 * The returned set contains all elements that are contained in either
	 * {@code set1} or {@code set2} but not in both. The iteration order of the
	 * returned set is undefined.
	 *
	 * <p>
	 * Results are undefined if {@code set1} and {@code set2} are sets based on
	 * different equivalence relations (as {@code HashSet}, {@code TreeSet}, and the
	 * keySet of an {@code IdentityHashMap} all are).
	 *
	 * @since 3.0
	 */
	public static <E> SetView<E> symmetricDifference(Set<? extends E> set1, Set<? extends E> set2) {
		checkNotNull(set1, "set1");
		checkNotNull(set2, "set2");

		// TODO(kevinb): Replace this with a more efficient implementation
		return difference(union(set1, set2), intersection(set1, set2));
	}

	/**
	 * Returns the elements of {@code unfiltered} that satisfy a predicate. The
	 * returned set is a live view of {@code unfiltered}; changes to one affect the
	 * other.
	 *
	 * <p>
	 * The resulting set's iterator does not support {@code remove()}, but all other
	 * set methods are supported. When given an element that doesn't satisfy the
	 * predicate, the set's {@code add()} and {@code addAll()} methods throw an
	 * {@link IllegalArgumentException}. When methods such as {@code
	 * removeAll()} and {@code clear()} are called on the filtered set, only
	 * elements that satisfy the filter will be removed from the underlying set.
	 *
	 * <p>
	 * The returned set isn't threadsafe or serializable, even if {@code unfiltered}
	 * is.
	 *
	 * <p>
	 * Many of the filtered set's methods, such as {@code size()}, iterate across
	 * every element in the underlying set and determine which elements satisfy the
	 * filter. When a live view is <i>not</i> needed, it may be faster to copy
	 * {@code Iterables.filter(unfiltered, predicate)} and use the copy.
	 *
	 * <p>
	 * <b>Warning:</b> {@code predicate} must be <i>consistent with equals</i>, as
	 * documented at {@link Predicate#apply}. Do not provide a predicate such as
	 * {@code Predicates.instanceOf(ArrayList.class)}, which is inconsistent with
	 * equals. (See {@link Iterables#filter(Iterable, Class)} for related
	 * functionality.)
	 */
	// TODO(kevinb): how to omit that last sentence when building GWT javadoc?
	public static <E> Set<E> filter(Set<E> unfiltered, Predicate<? super E> predicate) {
		if (unfiltered instanceof SortedSet) {
			return filter((SortedSet<E>) unfiltered, predicate);
		}
		if (unfiltered instanceof FilteredSet) {
			// Support clear(), removeAll(), and retainAll() when filtering a filtered
			// collection.
			FilteredSet<E> filtered = (FilteredSet<E>) unfiltered;
			Predicate<E> combinedPredicate = Predicates.<E>and(filtered.predicate, predicate);
			return new FilteredSet<E>((Set<E>) filtered.unfiltered, combinedPredicate);
		}

		return new FilteredSet<E>(checkNotNull(unfiltered), checkNotNull(predicate));
	}

	private static class FilteredSet<E> extends FilteredCollection<E> implements Set<E> {
		FilteredSet(Set<E> unfiltered, Predicate<? super E> predicate) {
			super(unfiltered, predicate);
		}

		@Override
		public boolean equals(@Nullable Object object) {
			return equalsImpl(this, object);
		}

		@Override
		public int hashCode() {
			return hashCodeImpl(this);
		}
	}

	/**
	 * Returns the elements of a {@code SortedSet}, {@code unfiltered}, that satisfy
	 * a predicate. The returned set is a live view of {@code unfiltered}; changes
	 * to one affect the other.
	 *
	 * <p>
	 * The resulting set's iterator does not support {@code remove()}, but all other
	 * set methods are supported. When given an element that doesn't satisfy the
	 * predicate, the set's {@code add()} and {@code addAll()} methods throw an
	 * {@link IllegalArgumentException}. When methods such as {@code removeAll()}
	 * and {@code clear()} are called on the filtered set, only elements that
	 * satisfy the filter will be removed from the underlying set.
	 *
	 * <p>
	 * The returned set isn't threadsafe or serializable, even if {@code unfiltered}
	 * is.
	 *
	 * <p>
	 * Many of the filtered set's methods, such as {@code size()}, iterate across
	 * every element in the underlying set and determine which elements satisfy the
	 * filter. When a live view is <i>not</i> needed, it may be faster to copy
	 * {@code Iterables.filter(unfiltered, predicate)} and use the copy.
	 *
	 * <p>
	 * <b>Warning:</b> {@code predicate} must be <i>consistent with equals</i>, as
	 * documented at {@link Predicate#apply}. Do not provide a predicate such as
	 * {@code Predicates.instanceOf(ArrayList.class)}, which is inconsistent with
	 * equals. (See {@link Iterables#filter(Iterable, Class)} for related
	 * functionality.)
	 *
	 * @since 11.0
	 */
	public static <E> SortedSet<E> filter(SortedSet<E> unfiltered, Predicate<? super E> predicate) {
		return Platform.setsFilterSortedSet(unfiltered, predicate);
	}

	static <E> SortedSet<E> filterSortedIgnoreNavigable(SortedSet<E> unfiltered, Predicate<? super E> predicate) {
		if (unfiltered instanceof FilteredSet) {
			// Support clear(), removeAll(), and retainAll() when filtering a filtered
			// collection.
			FilteredSet<E> filtered = (FilteredSet<E>) unfiltered;
			Predicate<E> combinedPredicate = Predicates.<E>and(filtered.predicate, predicate);
			return new FilteredSortedSet<E>((SortedSet<E>) filtered.unfiltered, combinedPredicate);
		}

		return new FilteredSortedSet<E>(checkNotNull(unfiltered), checkNotNull(predicate));
	}

	private static class FilteredSortedSet<E> extends FilteredSet<E> implements SortedSet<E> {

		FilteredSortedSet(SortedSet<E> unfiltered, Predicate<? super E> predicate) {
			super(unfiltered, predicate);
		}

		@Override
		public Comparator<? super E> comparator() {
			return ((SortedSet<E>) unfiltered).comparator();
		}

		@Override
		public SortedSet<E> subSet(E fromElement, E toElement) {
			return new FilteredSortedSet<E>(((SortedSet<E>) unfiltered).subSet(fromElement, toElement), predicate);
		}

		@Override
		public SortedSet<E> headSet(E toElement) {
			return new FilteredSortedSet<E>(((SortedSet<E>) unfiltered).headSet(toElement), predicate);
		}

		@Override
		public SortedSet<E> tailSet(E fromElement) {
			return new FilteredSortedSet<E>(((SortedSet<E>) unfiltered).tailSet(fromElement), predicate);
		}

		@Override
		public E first() {
			return iterator().next();
		}

		@Override
		public E last() {
			SortedSet<E> sortedUnfiltered = (SortedSet<E>) unfiltered;
			while (true) {
				E element = sortedUnfiltered.last();
				if (predicate.apply(element)) {
					return element;
				}
				sortedUnfiltered = sortedUnfiltered.headSet(element);
			}
		}
	}

	/**
	 * Returns the elements of a {@code NavigableSet}, {@code unfiltered}, that
	 * satisfy a predicate. The returned set is a live view of {@code unfiltered};
	 * changes to one affect the other.
	 *
	 * <p>
	 * The resulting set's iterator does not support {@code remove()}, but all other
	 * set methods are supported. When given an element that doesn't satisfy the
	 * predicate, the set's {@code add()} and {@code addAll()} methods throw an
	 * {@link IllegalArgumentException}. When methods such as {@code removeAll()}
	 * and {@code clear()} are called on the filtered set, only elements that
	 * satisfy the filter will be removed from the underlying set.
	 *
	 * <p>
	 * The returned set isn't threadsafe or serializable, even if {@code unfiltered}
	 * is.
	 *
	 * <p>
	 * Many of the filtered set's methods, such as {@code size()}, iterate across
	 * every element in the underlying set and determine which elements satisfy the
	 * filter. When a live view is <i>not</i> needed, it may be faster to copy
	 * {@code Iterables.filter(unfiltered, predicate)} and use the copy.
	 *
	 * <p>
	 * <b>Warning:</b> {@code predicate} must be <i>consistent with equals</i>, as
	 * documented at {@link Predicate#apply}. Do not provide a predicate such as
	 * {@code Predicates.instanceOf(ArrayList.class)}, which is inconsistent with
	 * equals. (See {@link Iterables#filter(Iterable, Class)} for related
	 * functionality.)
	 *
	 * @since 14.0
	 */
	@GwtIncompatible("NavigableSet")
	@SuppressWarnings("unchecked")
	public static <E> NavigableSet<E> filter(NavigableSet<E> unfiltered, Predicate<? super E> predicate) {
		if (unfiltered instanceof FilteredSet) {
			// Support clear(), removeAll(), and retainAll() when filtering a filtered
			// collection.
			FilteredSet<E> filtered = (FilteredSet<E>) unfiltered;
			Predicate<E> combinedPredicate = Predicates.<E>and(filtered.predicate, predicate);
			return new FilteredNavigableSet<E>((NavigableSet<E>) filtered.unfiltered, combinedPredicate);
		}

		return new FilteredNavigableSet<E>(checkNotNull(unfiltered), checkNotNull(predicate));
	}

	@GwtIncompatible("NavigableSet")
	private static class FilteredNavigableSet<E> extends FilteredSortedSet<E> implements NavigableSet<E> {
		FilteredNavigableSet(NavigableSet<E> unfiltered, Predicate<? super E> predicate) {
			super(unfiltered, predicate);
		}

		NavigableSet<E> unfiltered() {
			return (NavigableSet<E>) unfiltered;
		}

		@Override
		@Nullable
		public E lower(E e) {
			return Iterators.getNext(headSet(e, false).descendingIterator(), null);
		}

		@Override
		@Nullable
		public E floor(E e) {
			return Iterators.getNext(headSet(e, true).descendingIterator(), null);
		}

		@Override
		public E ceiling(E e) {
			return Iterables.getFirst(tailSet(e, true), null);
		}

		@Override
		public E higher(E e) {
			return Iterables.getFirst(tailSet(e, false), null);
		}

		@Override
		public E pollFirst() {
			return Iterables.removeFirstMatching(unfiltered(), predicate);
		}

		@Override
		public E pollLast() {
			return Iterables.removeFirstMatching(unfiltered().descendingSet(), predicate);
		}

		@Override
		public NavigableSet<E> descendingSet() {
			return Sets.filter(unfiltered().descendingSet(), predicate);
		}

		@Override
		public Iterator<E> descendingIterator() {
			return Iterators.filter(unfiltered().descendingIterator(), predicate);
		}

		@Override
		public E last() {
			return descendingIterator().next();
		}

		@Override
		public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
			return filter(unfiltered().subSet(fromElement, fromInclusive, toElement, toInclusive), predicate);
		}

		@Override
		public NavigableSet<E> headSet(E toElement, boolean inclusive) {
			return filter(unfiltered().headSet(toElement, inclusive), predicate);
		}

		@Override
		public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
			return filter(unfiltered().tailSet(fromElement, inclusive), predicate);
		}
	}

	/**
	 * Returns every possible list that can be formed by choosing one element from
	 * each of the given sets in order; the "n-ary
	 * <a href="http://en.wikipedia.org/wiki/Cartesian_product">Cartesian
	 * product</a>" of the sets. For example:
	 * 
	 * <pre>
	 *    {@code
	 *
	 * Sets.cartesianProduct(ImmutableList.of(ImmutableSet.of(1, 2), ImmutableSet.of("A", "B", "C")))
	 * }
	 * </pre>
	 *
	 * <p>
	 * returns a set containing six lists:
	 *
	 * <ul>
	 * <li>{@code ImmutableList.of(1, "A")}
	 * <li>{@code ImmutableList.of(1, "B")}
	 * <li>{@code ImmutableList.of(1, "C")}
	 * <li>{@code ImmutableList.of(2, "A")}
	 * <li>{@code ImmutableList.of(2, "B")}
	 * <li>{@code ImmutableList.of(2, "C")}
	 * </ul>
	 *
	 * <p>
	 * The result is guaranteed to be in the "traditional", lexicographical order
	 * for Cartesian products that you would get from nesting for loops:
	 * 
	 * <pre>
	 *    {@code
	 *
	 *   for (B b0 : sets.get(0)) {
	 *     for (B b1 : sets.get(1)) {
	 *       ...
	 *       ImmutableList<B> tuple = ImmutableList.of(b0, b1, ...);
	 *       // operate on tuple
	 *     }
	 *   }}
	 * </pre>
	 *
	 * <p>
	 * Note that if any input set is empty, the Cartesian product will also be
	 * empty. If no sets at all are provided (an empty list), the resulting
	 * Cartesian product has one element, an empty list (counter-intuitive, but
	 * mathematically consistent).
	 *
	 * <p>
	 * <i>Performance notes:</i> while the cartesian product of sets of size
	 * {@code m, n, p} is a set of size {@code m x n x p}, its actual memory
	 * consumption is much smaller. When the cartesian set is constructed, the input
	 * sets are merely copied. Only as the resulting set is iterated are the
	 * individual lists created, and these are not retained after iteration.
	 *
	 * @param sets the sets to choose elements from, in the order that the elements
	 *             chosen from those sets should appear in the resulting lists
	 * @param <B>  any common base class shared by all axes (often just
	 *             {@link Object})
	 * @return the Cartesian product, as an immutable set containing immutable lists
	 * @throws NullPointerException if {@code sets}, any one of the {@code sets}, or
	 *                              any element of a provided set is null
	 * @since 2.0
	 */
	public static <B> Set<List<B>> cartesianProduct(List<? extends Set<? extends B>> sets) {
		return CartesianSet.create(sets);
	}

	/**
	 * Returns every possible list that can be formed by choosing one element from
	 * each of the given sets in order; the "n-ary
	 * <a href="http://en.wikipedia.org/wiki/Cartesian_product">Cartesian
	 * product</a>" of the sets. For example:
	 * 
	 * <pre>
	 *    {@code
	 *
	 * Sets.cartesianProduct(ImmutableSet.of(1, 2), ImmutableSet.of("A", "B", "C"))
	 * }
	 * </pre>
	 *
	 * <p>
	 * returns a set containing six lists:
	 *
	 * <ul>
	 * <li>{@code ImmutableList.of(1, "A")}
	 * <li>{@code ImmutableList.of(1, "B")}
	 * <li>{@code ImmutableList.of(1, "C")}
	 * <li>{@code ImmutableList.of(2, "A")}
	 * <li>{@code ImmutableList.of(2, "B")}
	 * <li>{@code ImmutableList.of(2, "C")}
	 * </ul>
	 *
	 * <p>
	 * The result is guaranteed to be in the "traditional", lexicographical order
	 * for Cartesian products that you would get from nesting for loops:
	 * 
	 * <pre>
	 *    {@code
	 *
	 *   for (B b0 : sets.get(0)) {
	 *     for (B b1 : sets.get(1)) {
	 *       ...
	 *       ImmutableList<B> tuple = ImmutableList.of(b0, b1, ...);
	 *       // operate on tuple
	 *     }
	 *   }}
	 * </pre>
	 *
	 * <p>
	 * Note that if any input set is empty, the Cartesian product will also be
	 * empty. If no sets at all are provided (an empty list), the resulting
	 * Cartesian product has one element, an empty list (counter-intuitive, but
	 * mathematically consistent).
	 *
	 * <p>
	 * <i>Performance notes:</i> while the cartesian product of sets of size
	 * {@code m, n, p} is a set of size {@code m x n x p}, its actual memory
	 * consumption is much smaller. When the cartesian set is constructed, the input
	 * sets are merely copied. Only as the resulting set is iterated are the
	 * individual lists created, and these are not retained after iteration.
	 *
	 * @param sets the sets to choose elements from, in the order that the elements
	 *             chosen from those sets should appear in the resulting lists
	 * @param <B>  any common base class shared by all axes (often just
	 *             {@link Object})
	 * @return the Cartesian product, as an immutable set containing immutable lists
	 * @throws NullPointerException if {@code sets}, any one of the {@code sets}, or
	 *                              any element of a provided set is null
	 * @since 2.0
	 */
	public static <B> Set<List<B>> cartesianProduct(Set<? extends B>... sets) {
		return cartesianProduct(Arrays.asList(sets));
	}

	private static final class CartesianSet<E> extends ForwardingCollection<List<E>> implements Set<List<E>> {
		private transient final ImmutableList<ImmutableSet<E>> axes;
		private transient final CartesianList<E> delegate;

		static <E> Set<List<E>> create(List<? extends Set<? extends E>> sets) {
			ImmutableList.Builder<ImmutableSet<E>> axesBuilder = new ImmutableList.Builder<ImmutableSet<E>>(
					sets.size());
			for (Set<? extends E> set : sets) {
				ImmutableSet<E> copy = ImmutableSet.copyOf(set);
				if (copy.isEmpty()) {
					return ImmutableSet.of();
				}
				axesBuilder.add(copy);
			}
			final ImmutableList<ImmutableSet<E>> axes = axesBuilder.build();
			ImmutableList<List<E>> listAxes = new ImmutableList<List<E>>() {

				@Override
				public int size() {
					return axes.size();
				}

				@Override
				public List<E> get(int index) {
					return axes.get(index).asList();
				}

				@Override
				boolean isPartialView() {
					return true;
				}
			};
			return new CartesianSet<E>(axes, new CartesianList<E>(listAxes));
		}

		private CartesianSet(ImmutableList<ImmutableSet<E>> axes, CartesianList<E> delegate) {
			this.axes = axes;
			this.delegate = delegate;
		}

		@Override
		protected Collection<List<E>> delegate() {
			return delegate;
		}

		@Override
		public boolean equals(@Nullable Object object) {
			// Warning: this is broken if size() == 0, so it is critical that we
			// substitute an empty ImmutableSet to the user in place of this
			if (object instanceof CartesianSet) {
				CartesianSet<?> that = (CartesianSet<?>) object;
				return this.axes.equals(that.axes);
			}
			return super.equals(object);
		}

		@Override
		public int hashCode() {
			// Warning: this is broken if size() == 0, so it is critical that we
			// substitute an empty ImmutableSet to the user in place of this

			// It's a weird formula, but tests prove it works.
			int adjust = size() - 1;
			for (int i = 0; i < axes.size(); i++) {
				adjust *= 31;
				adjust = ~~adjust;
				// in GWT, we have to deal with integer overflow carefully
			}
			int hash = 1;
			for (Set<E> axis : axes) {
				hash = 31 * hash + (size() / axis.size() * axis.hashCode());

				hash = ~~hash;
			}
			hash += adjust;
			return ~~hash;
		}
	}

	/**
	 * Returns the set of all possible subsets of {@code set}. For example,
	 * {@code powerSet(ImmutableSet.of(1, 2))} returns the set {@code {{}, {1}, {2},
	 * {1, 2}}}.
	 *
	 * <p>
	 * Elements appear in these subsets in the same iteration order as they appeared
	 * in the input set. The order in which these subsets appear in the outer set is
	 * undefined. Note that the power set of the empty set is not the empty set, but
	 * a one-element set containing the empty set.
	 *
	 * <p>
	 * The returned set and its constituent sets use {@code equals} to decide
	 * whether two elements are identical, even if the input set uses a different
	 * concept of equivalence.
	 *
	 * <p>
	 * <i>Performance notes:</i> while the power set of a set with size {@code
	 * n} is of size {@code 2^n}, its memory usage is only {@code O(n)}. When the
	 * power set is constructed, the input set is merely copied. Only as the power
	 * set is iterated are the individual subsets created, and these subsets
	 * themselves occupy only a small constant amount of memory.
	 *
	 * @param set the set of elements to construct a power set from
	 * @return the power set, as an immutable set of immutable sets
	 * @throws IllegalArgumentException if {@code set} has more than 30 unique
	 *                                  elements (causing the power set size to
	 *                                  exceed the {@code int} range)
	 * @throws NullPointerException     if {@code set} is or contains {@code null}
	 * @see <a href="http://en.wikipedia.org/wiki/Power_set">Power set article at
	 *      Wikipedia</a>
	 * @since 4.0
	 */
	@GwtCompatible(serializable = false)
	public static <E> Set<Set<E>> powerSet(Set<E> set) {
		return new PowerSet<E>(set);
	}

	private static final class SubSet<E> extends AbstractSet<E> {
		private final ImmutableMap<E, Integer> inputSet;
		private final int mask;

		SubSet(ImmutableMap<E, Integer> inputSet, int mask) {
			this.inputSet = inputSet;
			this.mask = mask;
		}

		@Override
		public Iterator<E> iterator() {
			return new UnmodifiableIterator<E>() {
				final ImmutableList<E> elements = inputSet.keySet().asList();
				int remainingSetBits = mask;

				@Override
				public boolean hasNext() {
					return remainingSetBits != 0;
				}

				@Override
				public E next() {
					int index = Integer.numberOfTrailingZeros(remainingSetBits);
					if (index == 32) {
						throw new NoSuchElementException();
					}
					remainingSetBits &= ~(1 << index);
					return elements.get(index);
				}
			};
		}

		@Override
		public int size() {
			return Integer.bitCount(mask);
		}

		@Override
		public boolean contains(@Nullable Object o) {
			Integer index = inputSet.get(o);
			return index != null && (mask & (1 << index)) != 0;
		}
	}

	private static final class PowerSet<E> extends AbstractSet<Set<E>> {
		final ImmutableMap<E, Integer> inputSet;

		PowerSet(Set<E> input) {
			ImmutableMap.Builder<E, Integer> builder = ImmutableMap.builder();
			int i = 0;
			for (E e : checkNotNull(input)) {
				builder.put(e, i++);
			}
			this.inputSet = builder.build();
			checkArgument(inputSet.size() <= 30, "Too many elements to create power set: %s > 30", inputSet.size());
		}

		@Override
		public int size() {
			return 1 << inputSet.size();
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public Iterator<Set<E>> iterator() {
			return new AbstractIndexedListIterator<Set<E>>(size()) {
				@Override
				protected Set<E> get(final int setBits) {
					return new SubSet<E>(inputSet, setBits);
				}
			};
		}

		@Override
		public boolean contains(@Nullable Object obj) {
			if (obj instanceof Set) {
				Set<?> set = (Set<?>) obj;
				return inputSet.keySet().containsAll(set);
			}
			return false;
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if (obj instanceof PowerSet) {
				PowerSet<?> that = (PowerSet<?>) obj;
				return inputSet.equals(that.inputSet);
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			/*
			 * The sum of the sums of the hash codes in each subset is just the sum of each
			 * input element's hash code times the number of sets that element appears in.
			 * Each element appears in exactly half of the 2^n sets, so:
			 */
			return inputSet.keySet().hashCode() << (inputSet.size() - 1);
		}

		@Override
		public String toString() {
			return "powerSet(" + inputSet + ")";
		}
	}

	/**
	 * An implementation for {@link Set#hashCode()}.
	 */
	static int hashCodeImpl(Set<?> s) {
		int hashCode = 0;
		for (Object o : s) {
			hashCode += o != null ? o.hashCode() : 0;

			hashCode = ~~hashCode;
			// Needed to deal with unusual integer overflow in GWT.
		}
		return hashCode;
	}

	/**
	 * An implementation for {@link Set#equals(Object)}.
	 */
	static boolean equalsImpl(Set<?> s, @Nullable Object object) {
		if (s == object) {
			return true;
		}
		if (object instanceof Set) {
			Set<?> o = (Set<?>) object;

			try {
				return s.size() == o.size() && s.containsAll(o);
			} catch (NullPointerException ignored) {
				return false;
			} catch (ClassCastException ignored) {
				return false;
			}
		}
		return false;
	}

	/**
	 * Returns an unmodifiable view of the specified navigable set. This method
	 * allows modules to provide users with "read-only" access to internal navigable
	 * sets. Query operations on the returned set "read through" to the specified
	 * set, and attempts to modify the returned set, whether direct or via its
	 * collection views, result in an {@code UnsupportedOperationException}.
	 *
	 * <p>
	 * The returned navigable set will be serializable if the specified navigable
	 * set is serializable.
	 *
	 * @param set the navigable set for which an unmodifiable view is to be returned
	 * @return an unmodifiable view of the specified navigable set
	 * @since 12.0
	 */
	@GwtIncompatible("NavigableSet")
	public static <E> NavigableSet<E> unmodifiableNavigableSet(NavigableSet<E> set) {
		if (set instanceof ImmutableSortedSet || set instanceof UnmodifiableNavigableSet) {
			return set;
		}
		return new UnmodifiableNavigableSet<E>(set);
	}

	@GwtIncompatible("NavigableSet")
	static final class UnmodifiableNavigableSet<E> extends ForwardingSortedSet<E>
			implements NavigableSet<E>, Serializable {
		private final NavigableSet<E> delegate;

		UnmodifiableNavigableSet(NavigableSet<E> delegate) {
			this.delegate = checkNotNull(delegate);
		}

		@Override
		protected SortedSet<E> delegate() {
			return Collections.unmodifiableSortedSet(delegate);
		}

		@Override
		public E lower(E e) {
			return delegate.lower(e);
		}

		@Override
		public E floor(E e) {
			return delegate.floor(e);
		}

		@Override
		public E ceiling(E e) {
			return delegate.ceiling(e);
		}

		@Override
		public E higher(E e) {
			return delegate.higher(e);
		}

		@Override
		public E pollFirst() {
			throw new UnsupportedOperationException();
		}

		@Override
		public E pollLast() {
			throw new UnsupportedOperationException();
		}

		private transient UnmodifiableNavigableSet<E> descendingSet;

		@Override
		public NavigableSet<E> descendingSet() {
			UnmodifiableNavigableSet<E> result = descendingSet;
			if (result == null) {
				result = descendingSet = new UnmodifiableNavigableSet<E>(delegate.descendingSet());
				result.descendingSet = this;
			}
			return result;
		}

		@Override
		public Iterator<E> descendingIterator() {
			return Iterators.unmodifiableIterator(delegate.descendingIterator());
		}

		@Override
		public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
			return unmodifiableNavigableSet(delegate.subSet(fromElement, fromInclusive, toElement, toInclusive));
		}

		@Override
		public NavigableSet<E> headSet(E toElement, boolean inclusive) {
			return unmodifiableNavigableSet(delegate.headSet(toElement, inclusive));
		}

		@Override
		public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
			return unmodifiableNavigableSet(delegate.tailSet(fromElement, inclusive));
		}

		private static final long serialVersionUID = 0;
	}

	/**
	 * Returns a synchronized (thread-safe) navigable set backed by the specified
	 * navigable set. In order to guarantee serial access, it is critical that
	 * <b>all</b> access to the backing navigable set is accomplished through the
	 * returned navigable set (or its views).
	 *
	 * <p>
	 * It is imperative that the user manually synchronize on the returned sorted
	 * set when iterating over it or any of its {@code descendingSet},
	 * {@code subSet}, {@code headSet}, or {@code tailSet} views.
	 * 
	 * <pre>
	 *    {@code
	 *
	 *   NavigableSet<E> set = synchronizedNavigableSet(new TreeSet<E>());
	 *    ...
	 *   synchronized (set) {
	 *     // Must be in the synchronized block
	 *     Iterator<E> it = set.iterator();
	 *     while (it.hasNext()) {
	 *       foo(it.next());
	 *     }
	 *   }}
	 * </pre>
	 *
	 * <p>
	 * or:
	 * 
	 * <pre>
	 *    {@code
	 *
	 *   NavigableSet<E> set = synchronizedNavigableSet(new TreeSet<E>());
	 *   NavigableSet<E> set2 = set.descendingSet().headSet(foo);
	 *    ...
	 *   synchronized (set) { // Note: set, not set2!!!
	 *     // Must be in the synchronized block
	 *     Iterator<E> it = set2.descendingIterator();
	 *     while (it.hasNext())
	 *       foo(it.next());
	 *     }
	 *   }}
	 * </pre>
	 *
	 * <p>
	 * Failure to follow this advice may result in non-deterministic behavior.
	 *
	 * <p>
	 * The returned navigable set will be serializable if the specified navigable
	 * set is serializable.
	 *
	 * @param navigableSet the navigable set to be "wrapped" in a synchronized
	 *                     navigable set.
	 * @return a synchronized view of the specified navigable set.
	 * @since 13.0
	 */
	@GwtIncompatible("NavigableSet")
	public static <E> NavigableSet<E> synchronizedNavigableSet(NavigableSet<E> navigableSet) {
		return Synchronized.navigableSet(navigableSet);
	}

	/**
	 * Remove each element in an iterable from a set.
	 */
	static boolean removeAllImpl(Set<?> set, Iterator<?> iterator) {
		boolean changed = false;
		while (iterator.hasNext()) {
			changed |= set.remove(iterator.next());
		}
		return changed;
	}

	static boolean removeAllImpl(Set<?> set, Collection<?> collection) {
		checkNotNull(collection); // for GWT
		if (collection instanceof Multiset) {
			collection = ((Multiset<?>) collection).elementSet();
		}
		/*
		 * AbstractSet.removeAll(List) has quadratic behavior if the list size is just
		 * less than the set's size. We augment the test by assuming that sets have fast
		 * contains() performance, and other collections don't. See
		 * http://code.google.com/p/guava-libraries/issues/detail?id=1013
		 */
		if (collection instanceof Set && collection.size() > set.size()) {
			return Iterators.removeAll(set.iterator(), collection);
		} else {
			return removeAllImpl(set, collection.iterator());
		}
	}

	@GwtIncompatible("NavigableSet")
	static class DescendingSet<E> extends ForwardingNavigableSet<E> {
		private final NavigableSet<E> forward;

		DescendingSet(NavigableSet<E> forward) {
			this.forward = forward;
		}

		@Override
		protected NavigableSet<E> delegate() {
			return forward;
		}

		@Override
		public E lower(E e) {
			return forward.higher(e);
		}

		@Override
		public E floor(E e) {
			return forward.ceiling(e);
		}

		@Override
		public E ceiling(E e) {
			return forward.floor(e);
		}

		@Override
		public E higher(E e) {
			return forward.lower(e);
		}

		@Override
		public E pollFirst() {
			return forward.pollLast();
		}

		@Override
		public E pollLast() {
			return forward.pollFirst();
		}

		@Override
		public NavigableSet<E> descendingSet() {
			return forward;
		}

		@Override
		public Iterator<E> descendingIterator() {
			return forward.iterator();
		}

		@Override
		public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
			return forward.subSet(toElement, toInclusive, fromElement, fromInclusive).descendingSet();
		}

		@Override
		public NavigableSet<E> headSet(E toElement, boolean inclusive) {
			return forward.tailSet(toElement, inclusive).descendingSet();
		}

		@Override
		public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
			return forward.headSet(fromElement, inclusive).descendingSet();
		}

		@SuppressWarnings("unchecked")
		@Override
		public Comparator<? super E> comparator() {
			Comparator<? super E> forwardComparator = forward.comparator();
			if (forwardComparator == null) {
				return (Comparator) Ordering.natural().reverse();
			} else {
				return reverse(forwardComparator);
			}
		}

		// If we inline this, we get a javac error.
		private static <T> Ordering<T> reverse(Comparator<T> forward) {
			return Ordering.from(forward).reverse();
		}

		@Override
		public E first() {
			return forward.last();
		}

		@Override
		public SortedSet<E> headSet(E toElement) {
			return standardHeadSet(toElement);
		}

		@Override
		public E last() {
			return forward.first();
		}

		@Override
		public SortedSet<E> subSet(E fromElement, E toElement) {
			return standardSubSet(fromElement, toElement);
		}

		@Override
		public SortedSet<E> tailSet(E fromElement) {
			return standardTailSet(fromElement);
		}

		@Override
		public Iterator<E> iterator() {
			return forward.descendingIterator();
		}

		@Override
		public Object[] toArray() {
			return standardToArray();
		}

		@Override
		public <T> T[] toArray(T[] array) {
			return standardToArray(array);
		}

		@Override
		public String toString() {
			return standardToString();
		}
	}
}
