/*
 * Copyright 2015 Jacek Marchwicki <jacek.marchwicki@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.appunite.detector;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

public class ChangesDetector<T, H> {

    public static interface ChangesAdapter {

        void notifyItemRangeInserted(int start, int count);

        void notifyItemRangeChanged(int start, int count);

        void notifyItemRangeRemoved(int start, int count);
    }

    @Nonnull
    public List<H> mItems = new ArrayList<>();
    @Nonnull
    public final Detector<T, H> mDetector;

    public ChangesDetector(@Nonnull Detector<T, H> detector) {
        mDetector = checkNotNull(detector);
    }

    public static interface Detector<T, H> extends Function<T, H> {

        @SuppressWarnings("NullableProblems")
        @Nonnull
        H apply(@Nonnull T item);

        boolean matches(@Nonnull H item, @Nonnull H newOne);

        boolean same(@Nonnull H item, @Nonnull H newOne);
    }

    private int indexOf(@Nonnull List<H> list,
                        int start,
                        @Nonnull H item) {
        for (int i = start, listSize = list.size(); i < listSize; i++) {
            H t = list.get(i);
            if (mDetector.matches(t, item)) {
                return i;
            }
        }
        return -1;
    }

    public void newData(@Nonnull ChangesAdapter adapter,
                        @Nonnull List<T> values,
                        boolean force) {
        checkNotNull(adapter);
        checkNotNull(values);

        final ImmutableList<H> list = FluentIterable.from(values)
                .transform(mDetector)
                .toList();

        int firstListPosition = 0;
        int secondListPosition = 0;

        int counter = 0;
        int toRemove = 0;

        for (;firstListPosition < mItems.size(); ++firstListPosition) {
            final H first = mItems.get(firstListPosition);
            final int indexOf = indexOf(list, secondListPosition, first);
            if (indexOf >= 0) {
                int itemsInserted = indexOf - secondListPosition;
                counter = notify(adapter, counter, toRemove, itemsInserted);
                toRemove = 0;
                secondListPosition = indexOf + 1;

                final H second = list.get(indexOf);
                if (force || !mDetector.same(first, second)) {
                    adapter.notifyItemRangeChanged(counter, 1);
                }
                counter += 1;
            } else {
                toRemove += 1;
            }
        }

        int itemsInserted = values.size() - secondListPosition;
        notify(adapter, counter, toRemove, itemsInserted);
        mItems = list;
    }

    private int notify(@Nonnull ChangesAdapter adapter,
                       int counter,
                       int toRemove,
                       int itemsInserted) {
        final int itemsChanged = Math.min(itemsInserted, toRemove);
        toRemove -= itemsChanged;
        itemsInserted -= itemsChanged;
        if (itemsChanged > 0) {
            adapter.notifyItemRangeChanged(counter, itemsChanged);
            counter += itemsChanged;
        }
        if (toRemove > 0) {
            adapter.notifyItemRangeRemoved(counter, toRemove);
        }
        if (itemsInserted > 0) {
            adapter.notifyItemRangeInserted(counter, itemsInserted);
            counter += itemsInserted;
        }
        return counter;
    }

}