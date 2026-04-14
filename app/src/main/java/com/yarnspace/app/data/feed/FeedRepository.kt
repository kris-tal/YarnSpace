package com.yarnspace.app.data.feed

import com.yarnspace.app.domain.feed.FeedItem

interface FeedRepository {
	fun getFeedItems(now: Long = System.currentTimeMillis()): List<FeedItem>
}


