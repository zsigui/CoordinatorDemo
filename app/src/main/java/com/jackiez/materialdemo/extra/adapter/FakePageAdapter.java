/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.jackiez.materialdemo.extra.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jackiez.materialdemo.extra.adapter.holder.FakePageVH;

public class FakePageAdapter extends RecyclerView.Adapter<FakePageVH> {

	private final int numItems;

	public FakePageAdapter(int numItems) {
		this.numItems = numItems;
	}

	@Override public FakePageVH onCreateViewHolder(ViewGroup viewGroup, int i) {

		View itemView = LayoutInflater.from(viewGroup.getContext())
			.inflate(com.jackiez.materialdemo.R.layout.list_item_card, viewGroup, false);

		return new FakePageVH(itemView);
	}

	@Override
	public void onBindViewHolder(FakePageVH fakePageVH, int i) {
		// do nothing
	}

	@Override public int getItemCount() {
		return numItems;
	}
}

