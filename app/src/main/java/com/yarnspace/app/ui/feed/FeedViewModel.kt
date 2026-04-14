package com.yarnspace.app.ui.feed

import androidx.lifecycle.ViewModel
import com.yarnspace.app.data.feed.FeedRepository
import com.yarnspace.app.data.feed.MockFeedRepository
import com.yarnspace.app.domain.feed.FeedItem

class FeedViewModel(private val repository: FeedRepository = MockFeedRepository()) : ViewModel() {

	fun loadFeed(now: Long = System.currentTimeMillis()): List<FeedItem> {
		return repository.getFeedItems(now)
	}
}


