package com.yarnspace.app.data.feed

import com.yarnspace.app.domain.feed.FeedItem


class RemoteFeedRepository : FeedRepository {
    override fun getFeedItems(now: Long): List<FeedItem> {
        return emptyList()
    }
}

